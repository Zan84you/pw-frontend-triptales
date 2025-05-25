package com.example.frontend_triptales

import android.content.Context
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody

object TokenManager {
    private const val PREFS_NAME = "token_prefs"
    private const val TOKEN_KEY = "auth_token"

    fun saveToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(TOKEN_KEY, token).apply()
    }

    fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(TOKEN_KEY, null)
    }

    fun clearToken(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(TOKEN_KEY).apply()
    }
}


data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val access: String,
    val refresh: String
)

data class User(
    val id: String,
    val username: String,
    val email: String,
    val avatarUrl: String? = null,
    val badges: List<Badge> = emptyList(),
    val likesCount: Int = 0
)

data class CreateTripRequest(
    val name: String,
    val description: String
)

data class Trip(
    val id: Int,
    val name: String,
    val description: String,
    val creatorId: String,
    val memberIds: List<String> = emptyList(),  // Solo ID dei membri
    val postIds: List<String> = emptyList()     // Solo ID dei post
)

data class PostRequest(
    val group: Int,
    val title: String,
    val content: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class Post(
    val id: String,
    val userId: String,
    val username: String,
    val title: String,
    val content: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val timestamp: Long,
    val likes: Int = 0,
    val comments: List<Comment> = emptyList(),
    val group: Int
)

data class Comment(
    val id: Int,
    val userId: String,
    val username: String,
    val content: String,
    val timestamp: Long
)

data class Like(
    val id: Int,
    val user: Int,
    val post: Int
)

data class Badge(
    val id: Int,
    val name: String,
    val icon: String,
    val description: String
)

data class UserBadge(
    val id: Int,
    val user: Int,
    val badge: Int,
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)
