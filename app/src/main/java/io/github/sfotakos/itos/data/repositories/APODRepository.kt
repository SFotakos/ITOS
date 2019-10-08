package io.github.sfotakos.itos.data.repositories

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
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
import kotlin.concurrent.thread

class APODRepository {

    companion object {
        var BASEURL = "https://api.nasa.gov/"
    }

    fun getAPOD() : MediatorLiveData<ResponseWrapper<APOD>> {
        val apodLiveData = MediatorLiveData<ResponseWrapper<APOD>>()
        thread {
            App.apodService.getAPOD("DEMO_KEY").enqueue(object: Callback<APOD> {
                override fun onResponse(call: Call<APOD>, response: Response<APOD>) {
                    val apodResponse : ResponseWrapper<APOD> = if (response.isSuccessful) {
                        ResponseWrapper(response.body(), null)
                    } else {
                        ResponseWrapper(null, ApiException(response.code(), response.message()))
                    }
                    ResponseWrapper(response.body(), null)
                    apodLiveData.postValue(apodResponse)
                }

                override fun onFailure(call: Call<APOD>, t: Throwable) {
                    val apodResponse : ResponseWrapper<APOD> = when (t){
                        is IOException -> ResponseWrapper(null, ApiException(-1, "Network Error, please try again later"))
                        else -> ResponseWrapper(null, ApiException(-1, t.localizedMessage))
                    }
                    apodLiveData.postValue(apodResponse)
                }

            })
        }
        return apodLiveData
    }

    fun getMockAPOD(context: Context) : LiveData<APOD> {
        val jsonfile: String =
            context.assets.open("APOD_MOCK").bufferedReader().use {it.readText()}
        // Create a LiveData with a String
        val apod : MutableLiveData<APOD> = MutableLiveData()
        apod.value = Gson().fromJson<APOD>(jsonfile, object: TypeToken<APOD>(){}.type)
        return apod
    }
}