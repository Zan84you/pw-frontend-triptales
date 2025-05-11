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

    private val _trips = MutableStateFlow<List<TripGroup>>(emptyList())
    val trips: StateFlow<List<TripGroup>> = _trips.asStateFlow()

    private val _selectedTrip = MutableStateFlow<TripGroup?>(null)
    val selectedTrip: StateFlow<TripGroup?> = _selectedTrip.asStateFlow()

    private val _token = MutableStateFlow<String?>(null)

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.login(LoginRequest(username, password))
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        _token.value = body.token
                        _isLoggedIn.value = true
                        _currentUser.value = User(
                            id = body.userId,
                            username = username,
                            email = "", // recuperabile da API utente
                        )
                        fetchGroups()
                    }
                } else {
                    Log.e("Login", "Errore ${response.code()}: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("Login", "Eccezione: ${e.localizedMessage}")
            }
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

    fun createGroup(name: String, description: String) {
        viewModelScope.launch {
            try {
                val creatorId = currentUser.value?.id ?: return@launch
                val newGroup = TripGroup(
                    id = 0, // backend lo imposta
                    name = name,
                    description = description,
                    creatorId = creatorId
                )
                val response = ApiClient.apiService.createGroup(newGroup)
                if (response.isSuccessful) {
                    fetchGroups()
                } else {
                    Log.e("CreateGroup", "Errore ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("CreateGroup", "Errore di rete: ${e.localizedMessage}")
            }
        }
    }

    fun selectTrip(trip: TripGroup) {
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

    fun addPost(content: String, imageUrl: String?, location: LatLng?, locationName: String?) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val trip = _selectedTrip.value ?: return@launch
            try {
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
                val response = ApiClient.apiService.addPost(newPost)

                if (response.isSuccessful) {
                    fetchGroups()
                } else {
                    Log.e("AddPost", "Errore ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("AddPost", "Errore rete: ${e.localizedMessage}")
            }
        }
    }
}
