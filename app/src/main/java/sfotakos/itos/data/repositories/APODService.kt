package sfotakos.itos.data.repositories

import sfotakos.itos.BuildConfig
import sfotakos.itos.data.entities.APOD
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Tag

interface APODService {

    @GET("planetary/apod")
    fun getTodayApod(@Query("api_key") apiKey: String): Call<APOD>

    @GET("planetary/apod")
    fun getApodByDate(
        @Query("api_key") apiKey: String,
        @Query("date") date: String
    ): Call<APOD>

    companion object {
        const val QUERY_DATE_FORMAT = "yyyy-MM-dd"

        private const val BASE_URL = "https://api.nasa.gov/"

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