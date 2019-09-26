package io.github.sfotakos.itos.data.repositories

import io.github.sfotakos.itos.data.entities.APOD
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface APODService {

    @GET("planetary/apod")
    fun getAPOD(@Query("api_key") apiKey: String): Call<APOD>

}