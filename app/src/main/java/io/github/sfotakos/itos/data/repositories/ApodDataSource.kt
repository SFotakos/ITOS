package io.github.sfotakos.itos.data.repositories

import androidx.paging.ItemKeyedDataSource
import io.github.sfotakos.itos.data.entities.APOD
import io.github.sfotakos.itos.network.ResponseWrapper
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
        return item.data!!.date
    }

    private fun getPreviousDay(calendar: Calendar): Calendar {
        calendar.add(Calendar.DATE, -1)
        return calendar
    }
}