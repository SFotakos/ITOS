package io.github.sfotakos.itos.data.repositories

import android.annotation.SuppressLint
import android.util.Log
import androidx.paging.PagedList
import androidx.paging.PagingRequestHelper
import io.github.sfotakos.itos.data.entities.APOD
import io.github.sfotakos.itos.data.repositories.db.ApodDb
import io.github.sfotakos.itos.network.ResponseWrapper
import io.github.sfotakos.itos.network.createStatusLiveData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class ApodBoundaryCallback (private val db: ApodDb) :  PagedList.BoundaryCallback<APOD>() {

    val helper = PagingRequestHelper(Executors.newSingleThreadExecutor())
    val networkState = helper.createStatusLiveData()

    private val pageSize = 4

    override fun onZeroItemsLoaded() {
        super.onZeroItemsLoaded()
        helper.runIfNotRunning(PagingRequestHelper.RequestType.INITIAL) {
            APODService.createService()
                .getApodByDate("DEMO_KEY", getDateString(Calendar.getInstance()))
                .enqueue(createWebserviceCallback(it))
        }
    }

    override fun onItemAtEndLoaded(itemAtEnd: APOD) {
        super.onItemAtEndLoaded(itemAtEnd)
        val calendar = Calendar.getInstance()
        calendar.time =
            SimpleDateFormat(APODService.QUERY_DATE_FORMAT, Locale.ENGLISH)
                .parse(itemAtEnd.date)


            fetchApods(getPreviousDay(calendar))
    }

    private fun fetchApods(calendar: Calendar = Calendar.getInstance()) {
        for (i in 0..pageSize) {
            helper.runIfNotRunning(PagingRequestHelper.RequestType.AFTER) {
                APODService.createService()
                    .getApodByDate("DEMO_KEY", getDateString(calendar))
                    .enqueue(createWebserviceCallback(it))
            }
        }
    }

    private fun createWebserviceCallback(it: PagingRequestHelper.Request.Callback)
            : Callback<APOD> {
        return object : Callback<APOD> {
            override fun onFailure(
                call: Call<APOD>,
                t: Throwable) {
                it.recordFailure(t)
            }

            override fun onResponse(
                call: Call<APOD>,
                response: Response<APOD>
            ) {
                val apod: APOD? = response.body()
                val error = response.errorBody()
                when {
                    apod != null -> {
                        Executors.newSingleThreadExecutor().execute{ db.apodDao().insertApod(apod) }
                        it.recordSuccess()
                    }
                    error != null -> {
                        Log.d("ApodDataSource", error.string())
                    }
                    else -> {
                        Log.wtf("ApodDataSource", "Should never happen")
                        throw(Exception("Should never happen"))
                    }
                }
            }
        }
    }

    private fun getPreviousDay(calendar: Calendar): Calendar {
        calendar.add(Calendar.DATE, -1)
        return calendar
    }

    @SuppressLint("SimpleDateFormat")
    private fun getDateString(calendar: Calendar): String {
        val dateFormat = SimpleDateFormat(APODService.QUERY_DATE_FORMAT)
        return dateFormat.format(calendar.time)
    }

}