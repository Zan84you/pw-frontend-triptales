package com.example.frontend_triptales

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface ApiService {

    @POST("register/")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>

    @POST("login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("me/")
    suspend fun getUserMe(): Response<User>

    @GET("groups/mine/")
    suspend fun getMyGroups(): Response<List<Trip>>

    @POST("groups/")
    suspend fun createGroup(@Body group: CreateTripRequest): Response<Trip>

    // --- Post ---
    @GET("posts/")
    suspend fun getPosts(): Response<List<Post>>

    @Multipart
    @POST("posts/")
    suspend fun addPost(
        @Part("title") title: RequestBody,
        @Part("content") content: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude : RequestBody,
        @Part("group") groupId: RequestBody,
    ): Response<Post>

    @GET("groups/{group_id}/posts/")
    suspend fun getPostsForGroup(@Path("group_id") groupId: Int): Response<List<Post>>

    @GET("groups/{group_id}/members/")
    suspend fun getGroupMembers(@Path("group_id") groupId: Int): Response<List<User>>

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

class AuthInterceptor(private val token: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val newRequest = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        Log.d("AuthInterceptor", "Aggiunto header Authorization con token: Bearer $token")

        return chain.proceed(newRequest)
    }
}

object ApiClient {
    private const val BASE_URL = "http://costaalberto.duckdns.org:8019/api/"

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
