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
    fun fetchApods(offset: Int, calendar: Calendar = Calendar.getInstance()) : ResponseWrapper<List<APOD>>{
        val apods = mutableListOf<APOD>() as ArrayList<APOD>

        for (i in 0 .. offset) {
            val dateString = getDateString(calendar)
            try {
                val response = APODService.createService().getApodByDate("DEMO_KEY", dateString).execute()
                if (response.isSuccessful) {
                    apods.add(response.body()!!)
                } else {
                    return ResponseWrapper(null, ApiException(response.code(), response.message(), dateString))
                }
            } catch (exception: Exception) {
                return when (exception) {
                    is IOException -> ResponseWrapper(
                        null,
                        ApiException(-1, "Network Error, please try again later", dateString))
                    else -> ResponseWrapper(
                        null, ApiException(-1, exception.localizedMessage, dateString))
                }
            } finally {
                getPreviousDay(calendar)
            }
        }

        return ResponseWrapper(apods, null)
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