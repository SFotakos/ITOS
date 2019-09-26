package io.github.sfotakos.itos

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import io.github.sfotakos.itos.data.entities.APOD
import kotlin.concurrent.thread

class APODViewModel (app: Application) : AndroidViewModel(app) {

    private val apodLiveData = MediatorLiveData<APOD>()

    fun getAPOD()  : LiveData<APOD> {
        APODRepository().getAPOD(apodLiveData)
        return apodLiveData
    }
}

