package com.example.frontend_triptales

import com.google.android.gms.maps.model.LatLng

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String, 
    val userId: String
)

data class User(
    val id: String,
    val username: String,
    val email: String,
    val avatarUrl: String? = null,
    val badges: List<Badge> = emptyList(),
    val likesCount: Int = 0
)

data class Trip(
    val id: Int,
    val name: String,
    val description: String,
    val creatorId: String,
    val members: List<String> = emptyList(),
    val posts: List<Post> = emptyList()
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
