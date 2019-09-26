package io.github.sfotakos.itos.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import io.github.sfotakos.itos.data.entities.APOD
import io.github.sfotakos.itos.data.repositories.APODRepository

class APODViewModel (app: Application) : AndroidViewModel(app) {

    private val apodLiveData = MediatorLiveData<APOD>()

    fun getAPOD()  : LiveData<APOD> {
        APODRepository().getAPOD(apodLiveData)
        return apodLiveData
    }
}

