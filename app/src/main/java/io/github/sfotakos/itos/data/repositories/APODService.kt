package io.github.sfotakos.itos.data.repositories

import io.github.sfotakos.itos.data.entities.APOD
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface APODService {

    @GET("planetary/apod")
    fun getTodayApod(@Query("api_key") apiKey: String): Call<APOD>

    @GET("planetary/apod")
    fun getApodByDate(
        @Query("api_key") apiKey: String,
        @Query("date") date: String
    ): Call<APOD>
}