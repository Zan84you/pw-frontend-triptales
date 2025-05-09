package com.example.frontend_triptales

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ViewModel to handle app state
class TripTalesViewModel : ViewModel() {
    // Authentication
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Trips
    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips: StateFlow<List<Trip>> = _trips.asStateFlow()

    private val _selectedTrip = MutableStateFlow<Trip?>(null)
    val selectedTrip: StateFlow<Trip?> = _selectedTrip.asStateFlow()

    // Mock data for demonstration
    init {
        _trips.value = listOf(
            Trip(
                id = "1",
                name = "Gita a Roma",
                description = "Visita dei principali monumenti romani",
                creatorId = "user1",
                members = listOf("user1", "user2", "user3"),
                posts = listOf(
                    Post(
                        id = "post1",
                        userId = "user1",
                        username = "Marco",
                        content = "Colosseo magnifico!",
                        imageUrl = "https://example.com/colosseo.jpg",
                        location = LatLng(41.8902, 12.4922),
                        locationName = "Colosseo",
                        timestamp = System.currentTimeMillis() - 86400000,
                        likes = 5,
                        comments = listOf(
                            Comment(
                                id = "comment1",
                                userId = "user2",
                                username = "Laura",
                                content = "Bellissimo!",
                                timestamp = System.currentTimeMillis() - 82800000
                            )
                        )
                    )
                )
            ),
            Trip(
                id = "2",
                name = "Venezia in Gondola",
                description = "Esplorazione dei canali veneziani",
                creatorId = "user2",
                members = listOf("user1", "user2")
            )
        )
    }

    fun login(email: String, password: String): Boolean {
        // Simulate login with mock data
        if (email.isNotEmpty() && password.isNotEmpty()) {
            _currentUser.value = User(
                id = "user1",
                username = "Marco",
                email = email,
                badges = listOf(
                    Badge(
                        id = "badge1",
                        name = "Esploratore",
                        icon = "explore",
                        description = "Hai visitato 5 luoghi diversi"
                    )
                ),
                likesCount = 12
            )
            _isLoggedIn.value = true
            return true
        }
        return false
    }

    fun register(username: String, email: String, password: String): Boolean {
        // Simulate registration
        if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
            _currentUser.value = User(
                id = "new_user",
                username = username,
                email = email
            )
            _isLoggedIn.value = true
            return true
        }
        return false
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentUser.value = null
        _selectedTrip.value = null
    }

    fun selectTrip(trip: Trip) {
        _selectedTrip.value = trip
    }

    fun createTrip(name: String, description: String): Boolean {
        if (name.isNotEmpty() && currentUser.value != null) {
            val newTrip = Trip(
                id = "trip_${System.currentTimeMillis()}",
                name = name,
                description = description,
                creatorId = currentUser.value!!.id,
                members = listOf(currentUser.value!!.id)
            )
            _trips.value += newTrip
            return true
        }
        return false
    }

    fun addPost(content: String, imageUrl: String? = null, location: LatLng? = null, locationName: String? = null): Boolean {
        val currentTripValue = _selectedTrip.value ?: return false
        val currentUserValue = _currentUser.value ?: return false

        val newPost = Post(
            id = "post_${System.currentTimeMillis()}",
            userId = currentUserValue.id,
            username = currentUserValue.username,
            content = content,
            imageUrl = imageUrl,
            location = location,
            locationName = locationName,
            timestamp = System.currentTimeMillis()
        )

        val updatedPosts = currentTripValue.posts + newPost
        val updatedTrip = currentTripValue.copy(posts = updatedPosts)

        // Update the selected trip and the trip in the list
        _selectedTrip.value = updatedTrip
        _trips.value = _trips.value.map { if (it.id == updatedTrip.id) updatedTrip else it }

        return true
    }

    fun addComment(postId: String, content: String): Boolean {
        if (content.isEmpty() || _currentUser.value == null || _selectedTrip.value == null) return false

        val currentUser = _currentUser.value!!
        val selectedTrip = _selectedTrip.value!!

        val updatedPosts = selectedTrip.posts.map { post ->
            if (post.id == postId) {
                val newComment = Comment(
                    id = "comment_${System.currentTimeMillis()}",
                    userId = currentUser.id,
                    username = currentUser.username,
                    content = content,
                    timestamp = System.currentTimeMillis()
                )
                post.copy(comments = post.comments + newComment)
            } else {
                post
            }
        }

        val updatedTrip = selectedTrip.copy(posts = updatedPosts)
        _selectedTrip.value = updatedTrip
        _trips.value = _trips.value.map { if (it.id == updatedTrip.id) updatedTrip else it }

        return true
    }

    fun likePost(postId: String): Boolean {
        if (_currentUser.value == null || _selectedTrip.value == null) return false

        val selectedTrip = _selectedTrip.value!!

        val updatedPosts = selectedTrip.posts.map { post ->
            if (post.id == postId) {
                post.copy(likes = post.likes + 1)
            } else {
                post
            }
        }

        val updatedTrip = selectedTrip.copy(posts = updatedPosts)
        _selectedTrip.value = updatedTrip
        _trips.value = _trips.value.map { if (it.id == updatedTrip.id) updatedTrip else it }

        return true
    }
}
