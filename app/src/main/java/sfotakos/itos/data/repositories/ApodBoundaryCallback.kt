package sfotakos.itos.data.repositories

import android.annotation.SuppressLint
import android.util.Log
import androidx.paging.PagedList
import androidx.paging.PagingRequestHelper
import sfotakos.itos.data.entities.APOD
import sfotakos.itos.data.repositories.db.ApodDb
import sfotakos.itos.network.createStatusLiveData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import sfotakos.itos.BuildConfig
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class ApodBoundaryCallback(private val db: ApodDb) : PagedList.BoundaryCallback<APOD>() {

    companion object {
        const val YESTERDAY = -1
        const val TOMORROW = 1
    }

    //TODO add multi thread executor
    val helper = PagingRequestHelper(Executors.newSingleThreadExecutor())
    val networkState = helper.createStatusLiveData()

    override fun onZeroItemsLoaded() {
        super.onZeroItemsLoaded()
        fetchApods(Calendar.getInstance(), PagingRequestHelper.RequestType.INITIAL)
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
        val calendar = Calendar.getInstance()
        calendar.time =
            SimpleDateFormat(APODService.QUERY_DATE_FORMAT, Locale.ENGLISH)
                .parse(itemAtEnd.date)
        fetchApods(getPreviousDay(calendar), PagingRequestHelper.RequestType.AFTER)
    }

    private fun fetchApods(calendar: Calendar, requestType: PagingRequestHelper.RequestType) {
        helper.runIfNotRunning(requestType) {
            APODService.createService()
                .getApodByDate(BuildConfig.NASA_KEY, getDateString(calendar))
                .enqueue(createWebserviceCallback(it))
        }
    }

    private fun createWebserviceCallback(it: PagingRequestHelper.Request.Callback)
            : Callback<APOD> {
        return object : Callback<APOD> {
            override fun onFailure(
                call: Call<APOD>,
                t: Throwable
            ) {
                //TODO treat error to differ between network and parsing errors.
                it.recordFailure(t)
            }

            override fun onResponse(
                call: Call<APOD>,
                response: Response<APOD>
            ) {
                if (response.isSuccessful) {
                    val apod: APOD? = response.body()
                    if (apod != null) {
                        Executors.newSingleThreadExecutor().execute {
                            db.apodDao().insertApod(apod)
                            it.recordSuccess()
                        }

                    } else {
                        recordFailure(it, "APOD must not be null")
                    }
                } else {
                    //TODO proper error treatment
                    recordFailure(it, response.errorBody()!!.string())
                }
            }
        }
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