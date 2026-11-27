package bg.uni_plovdiv.fmi.mobileapps.di

import bg.uni_plovdiv.fmi.mobileapps.data.remote.OpenLibraryApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/** Tells Hilt how to build the HTTP client and the Open Library API. */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TIMEOUT_SECONDS = 15L

    /** Open Library asks apps to identify themselves with a User-Agent. */
    private const val USER_AGENT = "ReadingDiary/1.0 (Android course project)"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder().header("User-Agent", USER_AGENT).build()
                )
            }
            .build()

    @Provides
    @Singleton
    fun provideOpenLibraryApi(client: OkHttpClient): OpenLibraryApi =
        Retrofit.Builder()
            .baseUrl(OpenLibraryApi.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenLibraryApi::class.java)
}
