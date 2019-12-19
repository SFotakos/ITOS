package sfotakos.itos.data.repositories

import android.annotation.SuppressLint
import androidx.paging.PagedList
import androidx.paging.PagingRequestHelper
import sfotakos.itos.data.entities.APOD
import sfotakos.itos.data.repositories.db.ApodDb
import sfotakos.itos.network.createStatusLiveData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import sfotakos.itos.BuildConfig
import sfotakos.itos.data.repositories.db.ContinuityDb
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class ApodBoundaryCallback(private val apodDb: ApodDb, private val continuityDb: ContinuityDb) : PagedList.BoundaryCallback<APOD>() {

    //TODO get context into this class to get from strings.xml
    companion object {
        const val YESTERDAY = -1
        const val TOMORROW = 1
        const val INTERNAL_SERVER_ERROR = 500
        const val TEMP_ERROR_MSG = "There was a rip in time space, we'll try mending it."
        const val ERROR_CODE_FORMAT = " (%d)"
    }

    private var initialKey: String? = null

    //This is so we can recycle this class instead of creating a new one every time the initial key changes
    fun setInitialKey(key: String?){
        initialKey = key
    }

    //TODO add multi thread executor?
    val helper = PagingRequestHelper(Executors.newSingleThreadExecutor())
    val networkState = helper.createStatusLiveData()

    override fun onZeroItemsLoaded() {
        super.onZeroItemsLoaded()
        val calendar = Calendar.getInstance()
        initialKey?.let {
            calendar.time =
            SimpleDateFormat(APODService.QUERY_DATE_FORMAT, Locale.ENGLISH)
                .parse(initialKey)
        }
        fetchApods(calendar, PagingRequestHelper.RequestType.INITIAL)
    }

    override fun onItemAtFrontLoaded(itemAtFront: APOD) {
        super.onItemAtFrontLoaded(itemAtFront)
        val calendar = Calendar.getInstance()
        calendar.time =
            SimpleDateFormat(APODService.QUERY_DATE_FORMAT, Locale.ENGLISH)
                .parse(itemAtFront.date)
        getNextDay(calendar)
        if (calendar < Calendar.getInstance()) {
            fetchApods(calendar, PagingRequestHelper.RequestType.BEFORE)
        }
    }

    override fun onItemAtEndLoaded(itemAtEnd: APOD) {
        super.onItemAtEndLoaded(itemAtEnd)
        val minDateCalendar = Calendar.getInstance()
        minDateCalendar.set(Calendar.YEAR, 1995)
        minDateCalendar.set(Calendar.MONTH, Calendar.JUNE)
        minDateCalendar.set(Calendar.DAY_OF_MONTH, 16)
        val calendar = Calendar.getInstance()
        calendar.time =
            SimpleDateFormat(APODService.QUERY_DATE_FORMAT, Locale.ENGLISH)
                .parse(itemAtEnd.date)
        if (calendar >= minDateCalendar) {
            fetchApods(getPreviousDay(calendar), PagingRequestHelper.RequestType.AFTER)
        }
    }

    //Check if the APOD exists in the apod.db, if it doesn't fetch it from NASA API
    private fun fetchApods(calendar: Calendar, requestType: PagingRequestHelper.RequestType) {
        helper.runIfNotRunning(requestType) {
            Executors.newSingleThreadExecutor().execute {
                val apod: APOD? = apodDb.apodDao().queryByDate(getDateString(calendar))
                if (apod != null) {
                    onSuccessfulFetch(apod, it)
                } else {
                    APODService.createService()
                        .getApodByDate(BuildConfig.NASA_KEY, getDateString(calendar))
                        .enqueue(createWebserviceCallback(it, requestType))
                }
            }
        }
    }

    private fun createWebserviceCallback(it: PagingRequestHelper.Request.Callback, requestType: PagingRequestHelper.RequestType)
            : Callback<APOD> {
        return object : Callback<APOD> {
            override fun onFailure(
                call: Call<APOD>,
                t: Throwable
            ) {
                when (t) {
                    is IOException -> recordFailure(it,
                        "Planet tracking was lost, retrieving connection...")
                    else -> recordFailure(it,
                        TEMP_ERROR_MSG + ERROR_CODE_FORMAT.format(-1))
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
                        recordFailure(it,
                            TEMP_ERROR_MSG + ERROR_CODE_FORMAT.format(-200))
                    }
                } else {
                    //TODO gracefully deal with INTERNAL_SERVER_ERROR
                    when(response.code()) {
                        INTERNAL_SERVER_ERROR -> {
                            val date = call.request().url().queryParameter("date")
                            if (date != null) {
                                var calendar = Calendar.getInstance()
                                calendar.time =
                                    SimpleDateFormat(APODService.QUERY_DATE_FORMAT, Locale.ENGLISH)
                                        .parse(date)
                                calendar = when (requestType) {
                                    PagingRequestHelper.RequestType.BEFORE -> getNextDay(calendar)
                                    PagingRequestHelper.RequestType.AFTER -> getPreviousDay(calendar)
                                    else -> {
                                        recordFailure(
                                            it,
                                            TEMP_ERROR_MSG + ERROR_CODE_FORMAT.format(response.code())
                                        )
                                        return
                                    }
                                }
                                recordFailure(
                                    it,
                                    TEMP_ERROR_MSG + ERROR_CODE_FORMAT.format(response.code())
                                )
                                fetchApods(calendar, requestType)
                            } else {
                                recordFailure(
                                    it,
                                    TEMP_ERROR_MSG + ERROR_CODE_FORMAT.format(response.code())
                                )
                            }
                        }
                        else -> recordFailure(it,
                            TEMP_ERROR_MSG + ERROR_CODE_FORMAT.format(response.code()))
                    }
                }
            }
        }
    }

    private fun onSuccessfulFetch(apod: APOD, requestCallback: PagingRequestHelper.Request.Callback){
        continuityDb.apodDao().insertApod(apod)
        apodDb.apodDao().insertApod(apod)
        requestCallback.recordSuccess()
    }

    private fun recordFailure(it: PagingRequestHelper.Request.Callback, error: String) {
        it.recordFailure(Throwable(error))
    }

    private fun getPreviousDay(calendar: Calendar): Calendar {
        calendar.add(Calendar.DATE, YESTERDAY)
        return calendar
    }

    private fun getNextDay(calendar: Calendar): Calendar {
        calendar.add(Calendar.DATE, TOMORROW)
        return calendar
    }

    @SuppressLint("SimpleDateFormat")
    private fun getDateString(calendar: Calendar): String {
        val dateFormat = SimpleDateFormat(APODService.QUERY_DATE_FORMAT)
        return dateFormat.format(calendar.time)
    }
}