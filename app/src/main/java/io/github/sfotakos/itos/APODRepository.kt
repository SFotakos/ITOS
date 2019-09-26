package io.github.sfotakos.itos

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.sfotakos.itos.data.entities.APOD
import kotlin.concurrent.thread

class APODRepository {

    fun getAPOD(apodLiveData: MutableLiveData<APOD>) {
        thread {
            val apod = App.apodService.getAPOD("DEMO_KEY").execute().body()
            apodLiveData.postValue(apod!!)
        }
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