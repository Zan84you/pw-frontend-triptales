package com.example.frontend_triptales

import android.content.Context
import com.google.android.gms.maps.model.LatLng

object TokenManager {
    private const val PREF_NAME = "triptales_prefs"
    private const val TOKEN_KEY = "auth_token"

    fun saveToken(context: Context, token: String) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(TOKEN_KEY, token).apply()
    }

    fun getToken(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(TOKEN_KEY, null)
    }

    fun clearToken(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().remove(TOKEN_KEY).apply()
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
    val members: List<String>? = emptyList(),
    val posts: List<Post>? = emptyList()
)

data class Post(
    val id: Int,
    val userId: String,
    val username: String,
    val content: String,
    val image: String? = null,
    val location: LatLng? = null,
    val locationName: String? = null,
    val timestamp: Long,
    val likes: Int = 0,
    val comments: List<Comment> = emptyList()
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
