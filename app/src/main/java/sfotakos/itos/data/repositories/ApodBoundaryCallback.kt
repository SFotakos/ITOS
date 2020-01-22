package sfotakos.itos.data.repositories

import androidx.paging.PagedList
import androidx.paging.PagingRequestHelper
import com.crashlytics.android.Crashlytics
import com.google.gson.JsonIOException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import sfotakos.itos.ApodDateUtils.calendarToString
import sfotakos.itos.ApodDateUtils.earliestApiDateCalendar
import sfotakos.itos.ApodDateUtils.gmtCalendar
import sfotakos.itos.ApodDateUtils.nextDay
import sfotakos.itos.ApodDateUtils.previousDay
import sfotakos.itos.ApodDateUtils.stringToDate
import sfotakos.itos.BuildConfig
import sfotakos.itos.ItosApp
import sfotakos.itos.R
import sfotakos.itos.data.entities.APOD
import sfotakos.itos.data.repositories.APODService.Companion.DATE_QUERY_PARAM
import sfotakos.itos.data.repositories.db.ApodDb
import sfotakos.itos.data.repositories.db.ContinuityDb
import sfotakos.itos.network.createStatusLiveData
import sfotakos.itos.zonedTime
import java.io.IOException
import java.lang.Exception
import java.lang.IllegalStateException
import java.util.*
import java.util.concurrent.Executors

class ApodBoundaryCallback(private val apodDb: ApodDb, private val continuityDb: ContinuityDb) :
    PagedList.BoundaryCallback<APOD>() {

    companion object {
        const val INTERNAL_SERVER_ERROR = 500
        const val PARSING_ERROR_CODE = -2
        const val INVALID_REQUEST_TYPE_ERROR_CODE = -3
        const val INVALID_DATE_STATE_ERROR_CODE = -4
        const val SUCCESS_NULL_BODY_ERROR_CODE = -200
    }

    private var initialKey: String? = null

    //This is so we can recycle this class instead of creating a new one every time the initial key changes
    fun setInitialKey(key: String?) {
        initialKey = key
    }

    //TODO add multi thread executor?
    val helper = PagingRequestHelper(Executors.newSingleThreadExecutor())
    val networkState = helper.createStatusLiveData()

    override fun onZeroItemsLoaded() {
        super.onZeroItemsLoaded()
        val calendar = Calendar.getInstance()
        initialKey?.let {
            calendar.time = stringToDate(initialKey!!)
        }
        fetchApods(calendar, PagingRequestHelper.RequestType.INITIAL)
    }

    override fun onItemAtFrontLoaded(itemAtFront: APOD) {
        super.onItemAtFrontLoaded(itemAtFront)
        val calendar = Calendar.getInstance()
        calendar.time = stringToDate(itemAtFront.date)
        nextDay(calendar)
        if (calendar < gmtCalendar()) {
            fetchApods(calendar, PagingRequestHelper.RequestType.BEFORE)
        }
    }

    override fun onItemAtEndLoaded(itemAtEnd: APOD) {
        super.onItemAtEndLoaded(itemAtEnd)
        val earliestDateCalendar = earliestApiDateCalendar()
        val calendar = Calendar.getInstance()
        calendar.time = stringToDate(itemAtEnd.date)
        if (calendar >= earliestDateCalendar) {
            fetchApods(previousDay(calendar), PagingRequestHelper.RequestType.AFTER)
        }
    }

    //Check if the APOD exists in the apod.db, if it doesn't fetch it from NASA API
    private fun fetchApods(calendar: Calendar, requestType: PagingRequestHelper.RequestType) {
        helper.runIfNotRunning(requestType) {
            Executors.newSingleThreadExecutor().execute {
                val apod: APOD? = apodDb.apodDao().queryByDate(calendarToString(calendar))
                if (apod != null) {
                    onSuccess(apod, it)
                } else {
                    APODService.createService()
                        .getApodByDate(BuildConfig.NASA_KEY, calendarToString(calendar))
                        .enqueue(createWebserviceCallback(it, requestType))
                }
            }
        }
    }

    private fun createWebserviceCallback(
        it: PagingRequestHelper.Request.Callback,
        requestType: PagingRequestHelper.RequestType
    )
            : Callback<APOD> {
        return object : Callback<APOD> {
            override fun onFailure(
                call: Call<APOD>,
                t: Throwable
            ) {
                onRequestFailure(it, t, getDateParam(call))
            }

            override fun onResponse(
                call: Call<APOD>,
                response: Response<APOD>
            ) {
                if (response.isSuccessful) {
                    val apod: APOD? = response.body()
                    if (apod != null) {
                        Executors.newSingleThreadExecutor().execute {
                            onSuccess(apod, it)
                        }
                    } else {
                        onSuccessfulRequestNullApod(it, getDateParam(call))
                    }
                } else {
                    onSuccessfulRequestHttpError(it, requestType, getDateParam(call), response)
                }
            }
        }
    }


    private fun getDateParam(call: Call<APOD>): String? {
        return call.request().url().queryParameter(DATE_QUERY_PARAM)
    }

    private fun onSuccess(
        apod: APOD,
        requestCallback: PagingRequestHelper.Request.Callback
    ) {
        continuityDb.apodDao().insertApod(apod)
        apodDb.apodDao().insertApod(apod)
        requestCallback.recordSuccess()
    }

    private fun recordCallbackFailure(
        it: PagingRequestHelper.Request.Callback,
        error: String
    ) {
        it.recordFailure(Throwable(error))
    }

    private fun onRequestFailure(
        pagingRequestCallback: PagingRequestHelper.Request.Callback,
        throwable: Throwable,
        date: String?
    ) {
        when (throwable) {
            is IOException -> {
                recordCallbackFailure(
                    pagingRequestCallback,
                    getStringResource(R.string.apod_connection_error)
                )
            }
            else -> {
                recordCallbackFailure(
                    pagingRequestCallback,
                    errorMsg(PARSING_ERROR_CODE)
                )
                logToCrashlytics(
                    JsonIOException(throwable.cause),
                    date,
                    PARSING_ERROR_CODE
                )
            }
        }
    }

    private fun onSuccessfulRequestNullApod(
        pagingRequestCallback: PagingRequestHelper.Request.Callback,
        date: String?
    ) {
        recordCallbackFailure(
            pagingRequestCallback,
            errorMsg(SUCCESS_NULL_BODY_ERROR_CODE)
        )
        logToCrashlytics(
            IllegalStateException(getStringResource(R.string.crashlytics_successful_response_null_apod)),
            date,
            SUCCESS_NULL_BODY_ERROR_CODE
        )
    }

    private fun onSuccessfulRequestHttpError(
        pagingRequestCallback: PagingRequestHelper.Request.Callback,
        requestType: PagingRequestHelper.RequestType,
        date: String?,
        response: Response<APOD>
    ) {
        when (response.code()) {
            INTERNAL_SERVER_ERROR -> {
                onInternalServerError(pagingRequestCallback, requestType, date, response)
            }
            else -> {
                recordCallbackFailure(
                    pagingRequestCallback,
                    errorMsg(response.code())
                )
                logToCrashlytics(
                    IOException(response.errorBody().toString()),
                    date,
                    response.code()
                )
            }
        }
    }

    private fun onInternalServerError(
        pagingRequestCallback: PagingRequestHelper.Request.Callback,
        requestType: PagingRequestHelper.RequestType,
        date: String?,
        response: Response<APOD>
    ) {
        if (date != null) {
            var calendar = Calendar.getInstance()
            calendar.time = stringToDate(date)
            calendar = when (requestType) {
                PagingRequestHelper.RequestType.BEFORE -> nextDay(calendar)
                PagingRequestHelper.RequestType.AFTER -> previousDay(calendar)
                else -> {
                    recordCallbackFailure(
                        pagingRequestCallback,
                        errorMsg(INVALID_REQUEST_TYPE_ERROR_CODE)
                    )
                    logToCrashlytics(
                        IllegalStateException(getStringResource(R.string.crashlytics_selected_date_not_available)),
                        date,
                        INVALID_REQUEST_TYPE_ERROR_CODE
                    )
                    return
                }
            }
            // Need to record failure before retrying due to runIfNotRunning
            recordCallbackFailure(
                pagingRequestCallback,
                errorMsg(response.code())
            )

            logToCrashlytics(
                IOException(response.errorBody().toString()),
                date,
                response.code()
            )

            fetchApods(calendar, requestType)
        } else {
            recordCallbackFailure(
                pagingRequestCallback,
                errorMsg(INVALID_DATE_STATE_ERROR_CODE)
            )
            logToCrashlytics(
                IllegalStateException(getStringResource(R.string.crashlytics_date_should_not_be_null)),
                date,
                INVALID_DATE_STATE_ERROR_CODE
            )
        }
    }

    private fun getStringResource(id: Int): String {
        return ItosApp.getContext().resources.getString(id)
    }

    private fun errorMsg(errorCode: Int): String {
        return ItosApp.getContext().resources.getString(R.string.apod_error, errorCode)
    }

    private fun logToCrashlytics(
        exception: Exception,
        date: String?,
        internalErrorCode: Int
    ) {
        Crashlytics.setString(getStringResource(R.string.crashlytics_customkey_date), date)
        Crashlytics.setInt(
            getStringResource(R.string.crashlytics_customkey_internal_error_code),
            internalErrorCode
        )
        Crashlytics.logException(exception)
    }
}