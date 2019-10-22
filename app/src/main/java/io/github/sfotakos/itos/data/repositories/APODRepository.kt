package io.github.sfotakos.itos.data.repositories

import android.content.Context
import androidx.lifecycle.MediatorLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.sfotakos.itos.App
import io.github.sfotakos.itos.data.entities.APOD
import io.github.sfotakos.itos.network.ApiException
import io.github.sfotakos.itos.network.ResponseWrapper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.lang.Thread.sleep
import kotlin.concurrent.thread

class APODRepository {

    companion object {
        var BASEURL = "https://api.nasa.gov/"
    }

    fun fetchApodByDate(liveData: MediatorLiveData<ResponseWrapper<APOD>>, date: String) {
        thread {
            App.apodService.getApodByDate("DEMO_KEY", date).enqueue(object: Callback<APOD> {
                override fun onResponse(call: Call<APOD>, response: Response<APOD>) {
                    val apodResponse : ResponseWrapper<APOD> = if (response.isSuccessful) {
                        ResponseWrapper(response.body(), null)
                    } else {
                        ResponseWrapper(null, ApiException(response.code(), response.message()))
                    }
                    ResponseWrapper(response.body(), null)
                    liveData.postValue(apodResponse)
                }

                override fun onFailure(call: Call<APOD>, t: Throwable) {
                    val apodResponse : ResponseWrapper<APOD> = when (t){
                        is IOException -> ResponseWrapper(null, ApiException(-1, "Network Error, please try again later"))
                        else -> ResponseWrapper(null, ApiException(-1, t.localizedMessage))
                    }
                    liveData.postValue(apodResponse)
                }

            })
        }
    }

    fun getMockAPOD(context: Context, liveData: MediatorLiveData<ResponseWrapper<APOD>>){
        thread {
            sleep(3000)
            val jsonfile: String =
                context.assets.open("APOD_MOCK").bufferedReader().use { it.readText() }
            liveData.postValue(ResponseWrapper(
                Gson().fromJson<APOD>(jsonfile, object : TypeToken<APOD>() {}.type), null))
        }
    }
}