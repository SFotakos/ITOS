package io.github.sfotakos.itos

import android.app.Application
import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class App : Application() {

    companion object {

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.nasa.gov/")
            .client(
                OkHttpClient().newBuilder()
                    .addInterceptor(HttpLoggingInterceptor().apply {
                        level = if (BuildConfig.DEBUG)
                            HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
                    })
                .build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apodService = retrofit.create(APODService::class.java)
    }
}