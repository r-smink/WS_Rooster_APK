package com.roosterplanner.app.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface ApiService {
    @POST("wp-json/roosterplanner/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("wp-json/roosterplanner/v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): LoginResponse

    @POST("wp-json/roosterplanner/v1/auth/logout")
    suspend fun logout(@Body body: LogoutRequest)

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
        @Query("location_id") locationId: Long?
    ): AvailabilityResponse

    @POST("wp-json/roosterplanner/v1/availability")
    suspend fun upsertAvailability(@Body body: AvailabilityUpsertRequest)

    @POST("wp-json/roosterplanner/v1/mobile/push-token")
    suspend fun registerPushToken(@Body body: PushTokenRequest)

    @POST("wp-json/roosterplanner/v1/sick")
    suspend fun reportSick(@Body body: SickReportRequest)

    @GET("wp-json/roosterplanner/v1/shifts")
    suspend fun shifts(@Query("location_id") locationId: Long): List<Shift>

    @GET("wp-json/roosterplanner/v1/chat")
    suspend fun getChatHistory(
        @Query("last_id") lastId: Long? = null,
        @Query("limit") limit: Int = 50
    ): List<ChatMessage>

    @POST("wp-json/roosterplanner/v1/chat")
    suspend fun sendChatMessage(@Body body: SendMessageRequest): ChatMessage

    @GET("wp-json/roosterplanner/v1/team-phone")
    suspend fun teamPhone(): TeamPhoneResponse

    // ---- Shift swaps ----

    @GET("wp-json/roosterplanner/v1/swaps/my-shifts")
    suspend fun swapsMyShifts(): MyShiftsResponse

    @GET("wp-json/roosterplanner/v1/swaps/available")
    suspend fun swapsAvailable(): AvailableShiftsResponse

    @GET("wp-json/roosterplanner/v1/swaps/requests")
    suspend fun swapsRequests(): SwapsRequestsResponse

    @GET("wp-json/roosterplanner/v1/swaps/colleagues")
    suspend fun swapsColleagues(): ColleaguesResponse

    @POST("wp-json/roosterplanner/v1/swaps/request")
    suspend fun requestSwap(@Body body: RequestSwapBody)

    @POST("wp-json/roosterplanner/v1/swaps/{id}/respond")
    suspend fun respondSwap(
        @Path("id") id: Long,
        @Body body: RespondSwapBody
    )

    @POST("wp-json/roosterplanner/v1/swaps/claim")
    suspend fun claimSwap(@Body body: ClaimSwapBody)

    @POST("wp-json/roosterplanner/v1/schedules/{id}/toggle-swappable")
    suspend fun toggleSwappable(@Path("id") id: Long): ToggleSwappableResponse

    // ---- Time off (verlof) ----

    @GET("wp-json/roosterplanner/v1/timeoff")
    suspend fun timeoff(): TimeoffResponse

    @POST("wp-json/roosterplanner/v1/timeoff")
    suspend fun submitTimeoff(@Body body: TimeoffRequest)
}

object ApiClient {
    private var authToken: String? = null

    fun setToken(token: String?) {
        authToken = token
    }

    private fun createBaseBuilder(): OkHttpClient.Builder {
        return OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                authToken?.let {
                    requestBuilder.header("Authorization", "Bearer $it")
                }
                requestBuilder.header("User-Agent", "WerkRooster-App/1.0")
                chain.proceed(requestBuilder.build())
            }
    }

    fun build(baseUrl: String, enableLogging: Boolean = false, onTokenExpired: (() -> Unit)? = null): ApiService {
        val client = createBaseBuilder().apply {
            readTimeout(30, TimeUnit.SECONDS)
            if (onTokenExpired != null) {
                addInterceptor { chain ->
                    val response = chain.proceed(chain.request())
                    if (response.code == 401) {
                        onTokenExpired()
                    }
                    response
                }
            }
            if (enableLogging) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                })
            }
        }.build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        
        return Retrofit.Builder()
            .baseUrl(baseUrl.ensureTrailingSlash())
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }

    fun getChatClient(): OkHttpClient {
        return createBaseBuilder()
            .readTimeout(0, TimeUnit.SECONDS) // Belangrijk voor SSE
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Connection", "keep-alive")
                    .header("Cache-Control", "no-cache")
                    .header("Accept", "text/event-stream")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }
}

private fun String.ensureTrailingSlash(): String = if (this.endsWith('/')) this else "$this/"
