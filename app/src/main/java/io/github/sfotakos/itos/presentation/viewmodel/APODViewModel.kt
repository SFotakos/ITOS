package io.github.sfotakos.itos.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import io.github.sfotakos.itos.network.NetworkUtils
import io.github.sfotakos.itos.data.entities.APOD
import io.github.sfotakos.itos.data.repositories.APODRepository
import io.github.sfotakos.itos.network.ResponseWrapper

class APODViewModel (app: Application) : AndroidViewModel(app) {

    fun getApodObservable()  : LiveData<ResponseWrapper<APOD>> {
        return APODRepository().getAPOD()
    }
}

