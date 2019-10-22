package io.github.sfotakos.itos.data.repositories

import android.annotation.SuppressLint
import io.github.sfotakos.itos.data.entities.APOD
import io.github.sfotakos.itos.network.ApiException
import io.github.sfotakos.itos.network.ResponseWrapper
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class APODRepository {

    @SuppressLint("SimpleDateFormat")
    fun fetchApods(offset: Int, calendar: Calendar = Calendar.getInstance()) : List<ResponseWrapper<APOD>>{
        val apods = mutableListOf<ResponseWrapper<APOD>>() as ArrayList<ResponseWrapper<APOD>>

        for (i in 0 .. offset) {
            try {
                val response = APODService.createService().getApodByDate("DEMO_KEY", getDateString(calendar)).execute()
                getPreviousDay(calendar)
                val apodResponse: ResponseWrapper<APOD> = if (response.isSuccessful) {
                    ResponseWrapper(response.body(), null)
                } else {
                    ResponseWrapper(null, ApiException(response.code(), response.message()))
                }
                ResponseWrapper(response.body(), null)
                apods.add(apodResponse)
            } catch (exception: Exception) {
                val apodResponse: ResponseWrapper<APOD> = when (exception) {
                    is IOException -> ResponseWrapper(
                        null,
                        ApiException(-1, "Network Error, please try again later")
                    )
                    else -> ResponseWrapper(null, ApiException(-1, exception.localizedMessage))
                }
                apods.add(apodResponse)
            }
        }

        return apods
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