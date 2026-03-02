package com.roosterplanner.app.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("wp-json/roosterplanner/v1/me")
    suspend fun me(): MeResponse

    @GET("wp-json/roosterplanner/v1/schedules")
    suspend fun schedules(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): List<ScheduleItem>

    @GET("wp-json/roosterplanner/v1/notifications")
    suspend fun notifications(
        @Query("limit") limit: Int = 50,
        @Query("unread_only") unreadOnly: Int = 0
    ): List<NotificationItem>

    @POST("wp-json/roosterplanner/v1/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Long)

    @GET("wp-json/roosterplanner/v1/availability")
    suspend fun availability(
        @Query("month") month: String,
        @Query("location_id") locationId: Long
    ): AvailabilityResponse

    @POST("wp-json/roosterplanner/v1/availability")
    suspend fun upsertAvailability(@Body body: AvailabilityUpsertRequest)

    @POST("wp-json/roosterplanner/v1/mobile/push-token")
    suspend fun registerPushToken(@Body body: PushTokenRequest)

    @POST("wp-json/roosterplanner/v1/sick")
    suspend fun reportSick(@Body body: SickReportRequest)

    @GET("wp-json/roosterplanner/v1/shifts")
    suspend fun shifts(@Query("location_id") locationId: Long): List<Shift>
}

object ApiClient {
    fun build(config: AuthConfig, enableLogging: Boolean = false): ApiService {
        val authHeader = Credentials.basic(config.username, config.appPassword)
        
        val client = OkHttpClient.Builder().apply {
            addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Authorization", authHeader)
                    .build()
                chain.proceed(request)
            }
            if (enableLogging) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
            }
        }.build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(config.baseUrl.ensureTrailingSlash())
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(ApiService::class.java)
    }
}

private fun String.ensureTrailingSlash(): String = if (this.endsWith('/')) this else "$this/"
