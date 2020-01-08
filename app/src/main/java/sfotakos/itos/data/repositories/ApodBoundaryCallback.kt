package sfotakos.itos.data.repositories

import androidx.paging.PagedList
import androidx.paging.PagingRequestHelper
import com.crashlytics.android.Crashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
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
import sfotakos.itos.data.entities.APOD
import sfotakos.itos.data.repositories.APODService.Companion.DATE_QUERY_PARAM
import sfotakos.itos.data.repositories.db.ApodDb
import sfotakos.itos.data.repositories.db.ContinuityDb
import sfotakos.itos.network.createStatusLiveData
import java.io.IOException
import java.lang.Exception
import java.lang.IllegalStateException
import java.util.*
import java.util.concurrent.Executors

class ApodBoundaryCallback(private val apodDb: ApodDb, private val continuityDb: ContinuityDb) :
    PagedList.BoundaryCallback<APOD>() {

    //TODO get context into this class to get from strings.xml
    companion object {
        const val INTERNAL_SERVER_ERROR = 500
        const val CONNECTION_ERROR_CODE = -1
        const val PARSING_ERROR_CODE = -2
        const val INVALID_REQUEST_TYPE_ERROR_CODE = -3
        const val INVALID_DATE_STATE_ERROR_CODE = -4
        const val SUCCESS_NULL_BODY_ERROR_CODE = -200
        private const val ERROR_MSG = "There was a rip in time space, we'll try mending it."
        private const val CODE_FORMAT = " (%d)"
        const val CONNECTION_ERROR_MSG = "Planet tracking was lost, retrieving connection..."
        const val FORMATTED_ERROR_MSG = ERROR_MSG + CODE_FORMAT
        const val CRASHLYTICS_ERROR_MSG = "Error fetching APOD" +
                "\nDate: %s" +
                "\nErrorMessage: %s" +
                "\nHttpCode:$CODE_FORMAT"
    }

    //TODO proper remoteconfig handling
    //TODO add custom key to crashlytics
    init {
        val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)
        firebaseRemoteConfig.fetch().addOnCompleteListener {
            if (it.isSuccessful) {
                shouldLogHttp = firebaseRemoteConfig.getBoolean("log_http_connection")
            }
        }
    }

    private var initialKey: String? = null
    private var shouldLogHttp = false

    //This is so we can recycle this class instead of creating a new one every time the initial key changes
    fun setInitialKey(key: String?) {
        initialKey = key
    }

    //TODO add multi thread executor?
    val helper = PagingRequestHelper(Executors.newSingleThreadExecutor())
    val networkState = helper.createStatusLiveData()

    override fun onZeroItemsLoaded() {
        super.onZeroItemsLoaded()
        val calendar = gmtCalendar()
        initialKey?.let {
            calendar.time = stringToDate(initialKey!!)
        }
        fetchApods(calendar, PagingRequestHelper.RequestType.INITIAL)
    }

    override fun onItemAtFrontLoaded(itemAtFront: APOD) {
        super.onItemAtFrontLoaded(itemAtFront)
        val calendar = gmtCalendar()
        calendar.time = stringToDate(itemAtFront.date)
        nextDay(calendar)
        if (calendar < gmtCalendar()) {
            fetchApods(calendar, PagingRequestHelper.RequestType.BEFORE)
        }
    }

    override fun onItemAtEndLoaded(itemAtEnd: APOD) {
        super.onItemAtEndLoaded(itemAtEnd)
        val earliestDateCalendar = earliestApiDateCalendar()
        val calendar = gmtCalendar()
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
                    onSuccessfulFetch(apod, it)
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
                when (t) {
                    is IOException -> {
                        recordFailure(
                            it,
                            CONNECTION_ERROR_MSG
                        )
                        logToCrashlytics(
                            IOException(
                                CRASHLYTICS_ERROR_MSG.format(
                                    getDateParam(call),
                                    t.cause,
                                    CONNECTION_ERROR_CODE
                                )
                            )
                        )
                    }
                    else -> {
                        recordFailure(
                            it,
                            FORMATTED_ERROR_MSG.format(PARSING_ERROR_CODE)
                        )
                        logToCrashlytics(
                            JsonIOException(
                                CRASHLYTICS_ERROR_MSG.format(
                                    getDateParam(call),
                                    t.cause,
                                    PARSING_ERROR_CODE
                                )
                            )
                        )
                    }
                }
            }

            override fun onResponse(
                call: Call<APOD>,
                response: Response<APOD>
            ) {
                if (response.isSuccessful) {
                    val apod: APOD? = response.body()
                    if (apod != null) {
                        Executors.newSingleThreadExecutor().execute {
                            onSuccessfulFetch(apod, it)
                        }
                    } else {
                        recordFailure(
                            it,
                            FORMATTED_ERROR_MSG.format(SUCCESS_NULL_BODY_ERROR_CODE)
                        )
                        logToCrashlytics(
                            IllegalStateException(
                                CRASHLYTICS_ERROR_MSG.format(
                                    getDateParam(call),
                                    "Successful response but null APOD",
                                    SUCCESS_NULL_BODY_ERROR_CODE
                                )
                            )
                        )
                    }
                } else {
                    when (response.code()) {
                        INTERNAL_SERVER_ERROR -> {
                            val date = getDateParam(call)
                            if (date != null) {
                                var calendar = gmtCalendar()
                                calendar.time = stringToDate(date)
                                calendar = when (requestType) {
                                    PagingRequestHelper.RequestType.BEFORE -> nextDay(calendar)
                                    PagingRequestHelper.RequestType.AFTER -> previousDay(calendar)
                                    else -> {
                                        recordFailure(
                                            it,
                                            FORMATTED_ERROR_MSG.format(INVALID_REQUEST_TYPE_ERROR_CODE)
                                        )
                                        logToCrashlytics(
                                            IllegalStateException(
                                                CRASHLYTICS_ERROR_MSG.format(
                                                    getDateParam(call),
                                                    "RequestType wasn't BEFORE nor AFTER",
                                                    INVALID_REQUEST_TYPE_ERROR_CODE
                                                )
                                            )
                                        )
                                        return
                                    }
                                }
                                // Need to record failure before retrying due to runIfNotRunning
                                recordFailure(
                                    it,
                                    FORMATTED_ERROR_MSG.format(response.code())
                                )
                                if (shouldLogHttp) {
                                    logToCrashlytics(
                                        IOException(
                                            CRASHLYTICS_ERROR_MSG.format(
                                                getDateParam(call),
                                                response.errorBody(),
                                                response.code()
                                            )
                                        )
                                    )
                                }
                                fetchApods(calendar, requestType)
                            } else {
                                recordFailure(
                                    it,
                                    FORMATTED_ERROR_MSG.format(INVALID_DATE_STATE_ERROR_CODE)
                                )
                                logToCrashlytics(
                                    IllegalStateException(
                                        CRASHLYTICS_ERROR_MSG.format(
                                            getDateParam(call),
                                            "Date shouldn't ever be null here",
                                            INVALID_DATE_STATE_ERROR_CODE
                                        )
                                    )
                                )
                            }
                        }
                        else -> {
                            recordFailure(
                                it,
                                FORMATTED_ERROR_MSG.format(response.code())
                            )
                            logToCrashlytics(
                                IOException(
                                    CRASHLYTICS_ERROR_MSG.format(
                                        getDateParam(call),
                                        response.errorBody(),
                                        response.code()
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun onSuccessfulFetch(
        apod: APOD,
        requestCallback: PagingRequestHelper.Request.Callback
    ) {
        continuityDb.apodDao().insertApod(apod)
        apodDb.apodDao().insertApod(apod)
        requestCallback.recordSuccess()
    }

    private fun recordFailure(
        it: PagingRequestHelper.Request.Callback,
        error: String
    ) {
        it.recordFailure(Throwable(error))
    }

    private fun getDateParam(call: Call<APOD>): String? {
        return call.request().url().queryParameter(DATE_QUERY_PARAM)
    }

    private fun logToCrashlytics(exception: Exception){
        Crashlytics.logException(exception)
    }
}