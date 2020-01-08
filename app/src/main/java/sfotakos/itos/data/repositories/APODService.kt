package sfotakos.itos.data.repositories

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import sfotakos.itos.BuildConfig
import sfotakos.itos.data.entities.APOD

interface APODService {

    @GET("planetary/apod")
    fun getTodayApod(@Query("api_key") apiKey: String): Call<APOD>

    @GET("planetary/apod")
    fun getApodByDate(
        @Query(API_QUERY_PARAM) apiKey: String,
        @Query(DATE_QUERY_PARAM) date: String
    ): Call<APOD>

    companion object {
        private const val BASE_URL = "https://api.nasa.gov/"
        const val API_QUERY_PARAM = "api_key"
        const val DATE_QUERY_PARAM = "date"

        fun createService(): APODService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(
                    OkHttpClient().newBuilder()
                        .addInterceptor(HttpLoggingInterceptor().apply {
                            level = if (BuildConfig.DEBUG)
                                HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
                        })
                        .build()
                )
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(APODService::class.java)
        }
    }
}