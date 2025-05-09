package com.example.frontend_triptales

import com.google.android.gms.maps.model.LatLng

data class User(
    val id: String,
    val username: String,
    val email: String,
    val avatarUrl: String? = null,
    val badges: List<Badge> = emptyList(),
    val likesCount: Int = 0
)

data class Trip(
    val id: String,
    val name: String,
    val description: String,
    val creatorId: String,
    val members: List<String> = emptyList(),
    val posts: List<Post> = emptyList()
)

data class Post(
    val id: String,
    val userId: String,
    val username: String,
    val content: String,
    val imageUrl: String? = null,
    val location: LatLng? = null,
    val locationName: String? = null,
    val timestamp: Long,
    val likes: Int = 0,
    val comments: List<Comment> = emptyList()
)

data class Comment(
    val id: String,
    val userId: String,
    val username: String,
    val content: String,
    val timestamp: Long
)

data class Badge(
    val id: String,
    val name: String,
    val icon: String,
    val description: String
)