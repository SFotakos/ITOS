package io.github.sfotakos.itos.data.repositories

import android.util.Log
import androidx.paging.ItemKeyedDataSource
import io.github.sfotakos.itos.data.entities.APOD
import io.github.sfotakos.itos.network.ResponseWrapper
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class ApodDataSource : ItemKeyedDataSource<String, ResponseWrapper<APOD>>() {

    override fun loadInitial(
        params: LoadInitialParams<String>,
        callback: LoadInitialCallback<ResponseWrapper<APOD>>
    ) {
        callback.onResult(APODRepository().fetchApods(params.requestedLoadSize))
    }

    override fun loadAfter(
        params: LoadParams<String>,
        callback: LoadCallback<ResponseWrapper<APOD>>
    ) {
        val calendar = Calendar.getInstance()
        calendar.time =
            SimpleDateFormat(APODService.QUERY_DATE_FORMAT, Locale.ENGLISH)
            .parse(params.key)

        callback.onResult(
            APODRepository().fetchApods(
                params.requestedLoadSize, getPreviousDay(calendar)))
    }

    override fun loadBefore(
        params: LoadParams<String>,
        callback: LoadCallback<ResponseWrapper<APOD>>
    ) {
        //unused
    }

    override fun getKey(item: ResponseWrapper<APOD>): String {
        item.let {
            return when {
                item.data != null -> item.data.date
                item.apiException != null -> item.apiException.key
                else -> {
                    Log.wtf("ApodDataSource", "Should never happen")
                    throw(Exception("Should never happen"))
                }
            }
        }
    }

    private fun getPreviousDay(calendar: Calendar): Calendar {
        calendar.add(Calendar.DATE, -1)
        return calendar
    }
}