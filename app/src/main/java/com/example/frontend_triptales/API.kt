package com.example.frontend_triptales

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface ApiService {

    @POST("auth/login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("groups/")
    suspend fun getGroups(): Response<List<TripGroup>>

    @POST("groups/")
    suspend fun createGroup(@Body group: TripGroup): Response<TripGroup>

    // --- Post ---
    @GET("posts/")
    suspend fun getPosts(): Response<List<Post>>

    @Multipart
    @POST("posts/")
    suspend fun createPostWithImage(
        @Part image: MultipartBody.Part,
        @Part("content") content: RequestBody,
        @Part("userId") userId: RequestBody,
        @Part("username") username: RequestBody,
        @Part("timestamp") timestamp: RequestBody,
        @Part("group") groupId: RequestBody,
        @Part("locationName") locationName: RequestBody,
        @Part("location") location: RequestBody
    ): Response<Post>

    @POST("posts/")
    suspend fun addPost(@Body post: Post): Response<Post>

    // --- Commenti ---
    @GET("comments/")
    suspend fun getComments(): Response<List<Comment>>

    @POST("comments/")
    suspend fun addComment(@Body comment: Comment): Response<Comment>

    // --- Like ---
    @GET("likes/")
    suspend fun getLikes(): Response<List<Like>>

    @POST("likes/")
    suspend fun likePost(@Body like: Like): Response<Like>

    // --- Badge ---
    @GET("badges/")
    suspend fun getBadges(): Response<List<Badge>>

    @GET("userbadges/")
    suspend fun getUserBadges(): Response<List<UserBadge>>
}

object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:8000/" // Emulator loopback

    private val retrofit: Retrofit by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
