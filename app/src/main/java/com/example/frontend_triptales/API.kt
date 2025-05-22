package com.example.frontend_triptales

import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("user/me/")
    suspend fun getUserMe(): Response<User>

    @GET("groups/")
    suspend fun getGroups(): Response<List<Trip>>

    @POST("groups/")
    suspend fun createGroup(@Body group: Trip): Response<Trip>

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

    @POST("auth/register/")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>
}

class AuthInterceptor(private val token: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val newRequest = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        return chain.proceed(newRequest)
    }
}

object ApiClient {
    private const val BASE_URL = "http://192.168.1.12:8000/api/" // Emulator loopback

    fun create(token: String? = null): ApiService{
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(logging)

        if(!token.isNullOrEmpty()){
            clientBuilder.addInterceptor(AuthInterceptor(token))
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }

}
