package io.github.sfotakos.itos.data.repositories

import android.util.Log
import androidx.paging.PagedList
import io.github.sfotakos.itos.data.entities.APOD
import io.github.sfotakos.itos.data.repositories.db.ApodDb
import io.github.sfotakos.itos.network.ResponseWrapper
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class ApodBoundaryCallback (private val db: ApodDb) :  PagedList.BoundaryCallback<APOD>() {

    private val pageSize = 4

    override fun onZeroItemsLoaded() {
        super.onZeroItemsLoaded()
        thread {
            processWrappedApods(APODRepository().fetchApods(pageSize))
        }
    }

    override fun onItemAtEndLoaded(itemAtEnd: APOD) {
        super.onItemAtEndLoaded(itemAtEnd)
        val calendar = Calendar.getInstance()
        calendar.time =
            SimpleDateFormat(APODService.QUERY_DATE_FORMAT, Locale.ENGLISH)
                .parse(itemAtEnd.date)

        thread {
            processWrappedApods(
                APODRepository().fetchApods(
                    6, getPreviousDay(calendar)
                )
            )
        }
    }

    private fun processWrappedApods(wrappedApods: ResponseWrapper<List<APOD>>) {
        when {
            wrappedApods.data != null ->
                db.apodDao().insertApod(wrappedApods.data)
            wrappedApods.apiException != null ->
                Log.d("ApodDataSource", wrappedApods.apiException.getErrorMessage())
            else -> {
                Log.wtf("ApodDataSource", "Should never happen")
                throw(Exception("Should never happen"))
            }
        }
    }

    private fun getPreviousDay(calendar: Calendar): Calendar {
        calendar.add(Calendar.DATE, -1)
        return calendar
    }

}