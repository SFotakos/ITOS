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
import kotlin.concurrent.thread

class APODRepository {

    companion object {
        var BASEURL = "https://api.nasa.gov/"
    }

    fun getAPOD() : MediatorLiveData<ResponseWrapper<APOD>> {
        val apodLiveData = MediatorLiveData<ResponseWrapper<APOD>>()
        thread {
            val response = App.apodService.getAPOD("DEMO_KEY").execute()
            val apodResponse : ResponseWrapper<APOD>
            apodResponse = if (response.isSuccessful) {
                ResponseWrapper(response.body(), null)
            } else {
                ResponseWrapper(null, ApiException(response.code(), response.message()))
            }
            apodLiveData.postValue(apodResponse)
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