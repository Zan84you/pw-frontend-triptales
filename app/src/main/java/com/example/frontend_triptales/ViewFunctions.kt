package com.example.frontend_triptales

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TripTalesViewModel : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips: StateFlow<List<Trip>> = _trips.asStateFlow()

    private val _selectedTrip = MutableStateFlow<Trip?>(null)
    val selectedTrip: StateFlow<Trip?> = _selectedTrip.asStateFlow()

    private val _token = MutableStateFlow<String?>(null)

    suspend fun login(username: String, password: String): Boolean {
        return try {
            val response = ApiClient.apiService.login(LoginRequest(username, password))
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    _token.value = body.token
                    _isLoggedIn.value = true
                    _currentUser.value = User(
                        id = body.userId,
                        username = username,
                        email = "" // recuperabile da API utente
                    )
                    fetchGroups()
                    return true
                }
                // Body was null
                Log.e("Login", "Risposta senza body")
                false
            } else {
                Log.e("Login", "Errore ${response.code()}: ${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e("Login", "Eccezione: ${e.localizedMessage}")
            false
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentUser.value = null
        _token.value = null
        _selectedTrip.value = null
    }

    fun fetchGroups() {
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getGroups()
                if (response.isSuccessful) {
                    _trips.value = response.body() ?: emptyList()
                } else {
                    Log.e("Groups", "Errore ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("Groups", "Errore di rete: ${e.localizedMessage}")
            }
        }
    }

    suspend fun createTrip(name: String, description: String): Boolean {
        return try {
            val creatorId = currentUser.value?.id ?: return false
            val newTrip = Trip(
                id = 0,
                name = name,
                description = description,
                creatorId = creatorId
            )

            val response = ApiClient.apiService.createGroup(newTrip)
            if (response.isSuccessful) {
                fetchGroups()
                true  // Success
            } else {
                false // Failure
            }
        } catch (e: Exception) {
            false   // Failure
        }
    }

    fun selectTrip(trip: Trip) {
        _selectedTrip.value = trip
    }

    fun addComment(postId: Int, content: String) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            try {
                val newComment = Comment(
                    id = 0,
                    userId = user.id,
                    username = user.username,
                    content = content,
                    timestamp = System.currentTimeMillis()
                )
                val response = ApiClient.apiService.addComment(newComment)
                if (response.isSuccessful) {
                    fetchGroups()
                } else {
                    Log.e("AddComment", "Errore ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("AddComment", "Errore rete: ${e.localizedMessage}")
            }
        }
    }

    fun likePost(postId: Int) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            try {
                val like = Like(id = 0, user = user.id.toInt(), post = postId)
                val response = ApiClient.apiService.likePost(like)
                if (response.isSuccessful) {
                    fetchGroups()
                } else {
                    Log.e("LikePost", "Errore ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("LikePost", "Errore rete: ${e.localizedMessage}")
            }
        }
    }

    suspend fun addPost(
        content: String,
        imageUrl: String? = null,
        location: LatLng? = null,
        locationName: String? = null
    ): Boolean {
        val user = _currentUser.value ?: return false
        val trip = _selectedTrip.value ?: return false

        val newPost = Post(
            id = 0,
            userId = user.id,
            username = user.username,
            content = content,
            image = imageUrl,
            location = location,
            locationName = locationName,
            timestamp = System.currentTimeMillis()
        )

        return try {
            val response = ApiClient.apiService.addPost(newPost)
            if (response.isSuccessful) {
                fetchGroups()
                true
            } else {
                Log.e("AddPost", "Errore ${response.code()}: ${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e("AddPost", "Errore rete: ${e.localizedMessage}")
            false
        }
    }

    suspend fun register(username: String, email: String, password: String): Boolean {
        return try {
            val response = ApiClient.apiService.register(
                RegisterRequest(username, email, password)
            )
            if (response.isSuccessful) {
                response.body()?.let { loginResponse ->
                    _token.value = loginResponse.token
                    _isLoggedIn.value = true
                    _currentUser.value = User(
                        id = loginResponse.userId,
                        username = username,
                        email = email
                    )
                    fetchGroups()
                    return true
                }
                // Null body
                Log.e("Register", "Risposta senza body")
                false
            } else {
                Log.e("Register", "Error ${response.code()}: ${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e("Register", "Network error: ${e.localizedMessage}")
            false
        }
    }
}
