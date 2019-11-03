package io.github.sfotakos.itos.data.repositories

import android.util.Log
import androidx.paging.ItemKeyedDataSource
import io.github.sfotakos.itos.data.entities.APOD
import io.github.sfotakos.itos.network.ResponseWrapper
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class ApodDataSource : ItemKeyedDataSource<String, APOD>() {

    //Unused
    override fun loadBefore(params: LoadParams<String>,callback: LoadCallback<APOD>) {}

    override fun loadInitial(
        params: LoadInitialParams<String>,
        callback: LoadInitialCallback<APOD>
    ) {
        processWrappedApods(APODRepository().fetchApods(params.requestedLoadSize), callback)
    }

    override fun loadAfter(
        params: LoadParams<String>,
        callback: LoadCallback<APOD>
    ) {
        val calendar = Calendar.getInstance()
        calendar.time =
            SimpleDateFormat(APODService.QUERY_DATE_FORMAT, Locale.ENGLISH)
            .parse(params.key)

        processWrappedApods(APODRepository().fetchApods(
            params.requestedLoadSize, getPreviousDay(calendar)), callback)
    }

    private fun processWrappedApods(wrappedApods: ResponseWrapper<List<APOD>>, callback: LoadCallback<APOD>) {
        when {
            wrappedApods.data != null ->
                callback.onResult(wrappedApods.data)
            wrappedApods.apiException != null ->
                Log.d("ApodDataSource", wrappedApods.apiException.getErrorMessage())
            else -> {
                Log.wtf("ApodDataSource", "Should never happen")
                throw(Exception("Should never happen"))
            }
        }
    }

    override fun getKey(item: APOD): String { return item.date }

    private fun getPreviousDay(calendar: Calendar): Calendar {
        calendar.add(Calendar.DATE, -1)
        return calendar
    }
}