package io.github.sfotakos.itos

import android.app.Application
import io.github.sfotakos.itos.data.repositories.APODRepository
import io.github.sfotakos.itos.data.repositories.APODService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class App : Application() {

    companion object {

        var BASEURL = "https://api.nasa.gov/"

        private val retrofit = Retrofit.Builder()
            .baseUrl(BASEURL)
            .client(
                OkHttpClient().newBuilder()
                    .addInterceptor(HttpLoggingInterceptor().apply {
                        level = if (BuildConfig.DEBUG)
                            HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
                    })
                .build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apodService: APODService = retrofit.create(APODService::class.java)
    }
}