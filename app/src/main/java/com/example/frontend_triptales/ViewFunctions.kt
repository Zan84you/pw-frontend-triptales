package com.example.frontend_triptales

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.SocketException

class TripTalesViewModel(application: Application) : AndroidViewModel(application) {
    // Stati
    private val _isLoggedIn = MutableStateFlow(false)
    private val _currentUser = MutableStateFlow<User?>(null)
    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    private val _selectedTrip = MutableStateFlow<Trip?>(null)
    private val _token = MutableStateFlow<String?>(null)
    private val _postResult = MutableStateFlow<PostResult?>(null)
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    private val _members = MutableStateFlow<List<User>>(emptyList())

    // Flussi pubblici
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    val trips: StateFlow<List<Trip>> = _trips.asStateFlow()
    val selectedTrip: StateFlow<Trip?> = _selectedTrip.asStateFlow()
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()
    val members: StateFlow<List<User>> = _members.asStateFlow()

    // Dipendenze
    private val context = getApplication<Application>().applicationContext
    private val apiService: ApiService get() = ApiClient.create(_token.value)
    private val postRepository = PostRepository(apiService, context)

    // Metodi di autenticazione
    suspend fun login(username: String, password: String): String? {
        return try {
            val response = ApiClient.create().login(LoginRequest(username, password))

            if (response.isSuccessful) {
                response.body()?.let { loginResponse ->
                    _token.value = loginResponse.access.also {
                        TokenManager.saveToken(context, it)
                    }

                    val userResponse = apiService.getUserMe()
                    if (userResponse.isSuccessful) {
                        _currentUser.value = userResponse.body()
                        _isLoggedIn.value = true
                        fetchGroups()
                        loginResponse.access
                    } else null
                } ?: null
            } else null
        } catch (e: Exception) {
            Log.e("Login", "Error: ${e.localizedMessage}")
            null
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentUser.value = null
        _token.value = null
        _selectedTrip.value = null
        _trips.value = emptyList()
        _postResult.value = null
        _posts.value = emptyList()
        _members.value = emptyList()
    }

    suspend fun register(username: String, email: String, password: String): Boolean {
        return try {
            val response = ApiClient.create().register(RegisterRequest(username, email, password))
            if (response.isSuccessful) {
                response.body()?.let { loginResponse ->
                    _token.value = loginResponse.access
                    apiService.getUserMe().let { userResponse ->
                        if (userResponse.isSuccessful) {
                            _currentUser.value = userResponse.body()
                            _isLoggedIn.value = true
                            fetchGroups()
                            true
                        } else false
                    }
                } ?: false
            } else false
        } catch (e: Exception) {
            Log.e("Register", "Error: ${e.localizedMessage}")
            false
        }
    }

    // Gestione gruppi
    fun fetchGroups() {
        viewModelScope.launch {
            try {
                val token = TokenManager.getToken(context) ?: run {
                    Log.e("FetchGroups", "Token non trovato")
                    return@launch
                }

                val response = ApiClient.create(token).getMyGroups()
                if (response.isSuccessful) {
                    _trips.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("FetchGroups", "Error: ${e.localizedMessage}")
            }
        }
    }

    suspend fun createTrip(name: String, description: String): Boolean {
        return try {
            val response = apiService.createGroup(CreateTripRequest(name, description))
            if (response.isSuccessful) {
                fetchGroups()
                true
            } else {
                Log.e("CreateTrip", "Error: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e("CreateTrip", "Error: ${e.localizedMessage}")
            false
        }
    }

    fun selectTrip(trip: Trip) {
        _selectedTrip.value = trip
        trip.id?.let {
            fetchPostsForGroup(it)
            fetchGroupMembers(it)
        }
    }

    // Gestione post
    suspend fun addPost(
        title: String,
        content: String,
        latitude: Double?,
        longitude: Double?,
        groupId: Int
    ): Boolean {
        return try {
            when (val result = postRepository.addPost(
                title, content, latitude, longitude, groupId
            )) {
                is PostResult.Success -> {
                    delay(100)
                    fetchPostsForGroup(groupId)
                    true
                }
                is PostResult.Error -> {
                    Log.e("AddPost", result.message)
                    false
                }
                else -> false
            }
        } catch (e: SocketException) {
            Log.e("AddPost", "Socket error: ${e.message}")
            true
        } catch (e: Exception) {
            Log.e("AddPost", "Error: ${e.message}")
            false
        }
    }

    fun fetchPostsForGroup(groupId: Int) {
        viewModelScope.launch {
            try {
                val token = TokenManager.getToken(context)
                if (token == null) {
                    Log.e("FetchPosts", "Token mancante")
                    return@launch
                }
                val apiWithToken = ApiClient.create(token)
                val response = apiWithToken.getPostsForGroup(groupId)
                if (response.isSuccessful) {
                    val postsList = response.body() ?: emptyList()
                    Log.d("FetchPosts", "Posts ricevuti: ${postsList.size}")
                    _posts.value = postsList
                } else {
                    Log.e("FetchPosts", "Errore risposta: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("FetchPosts", "Error: ${e.localizedMessage}")
            }
        }
    }


    fun fetchGroupMembers(groupId: Int) {
        viewModelScope.launch {
            try {
                val token = TokenManager.getToken(context)
                if (token == null) {
                    Log.e("FetchMembers", "Token mancante")
                    return@launch
                }
                val apiWithToken = ApiClient.create(token)
                val response = apiWithToken.getGroupMembers(groupId)
                if (response.isSuccessful) {
                    _members.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("FetchMembers", "Error: ${e.localizedMessage}")
            }
        }
    }


    // Gestione commenti
    fun addComment(postId: String, content: String) {
        viewModelScope.launch {
            _currentUser.value?.let { user ->
                try {
                    val response = apiService.addComment(
                        Comment(
                            id = 0,
                            userId = user.id,
                            username = user.username,
                            content = content,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    if (response.isSuccessful) {
                        _selectedTrip.value?.id?.let { fetchPostsForGroup(it) }
                    }
                } catch (e: Exception) {
                    Log.e("AddComment", "Error: ${e.localizedMessage}")
                }
            }
        }
    }

    // Altri metodi
    fun resetPostResult() {
        _postResult.value = null
    }

    suspend fun uploadImage(uri: Uri) {
        // TODO: Implement
    }
}

sealed class PostResult {
    object Loading : PostResult()
    object Success : PostResult()
    data class Error(val message: String) : PostResult()
}

class PostRepository(
    private val apiService: ApiService,
    private val context: Context
) {
    suspend fun addPost(
        title: String,
        content: String,
        latitude: Double?,
        longitude: Double?,
        groupId: Int
    ): PostResult {
        return try {
            val token = TokenManager.getToken(context) ?:
            return PostResult.Error("Token non valido")

            val response = ApiClient.create(token).addPost(
                title = title.toRequestBody("text/plain".toMediaTypeOrNull()),
                content = content.toRequestBody("text/plain".toMediaTypeOrNull()),
                latitude = latitude?.toString().orEmpty()
                    .toRequestBody("text/plain".toMediaTypeOrNull()),
                longitude = longitude?.toString().orEmpty()
                    .toRequestBody("text/plain".toMediaTypeOrNull()),
                groupId = groupId.toString()
                    .toRequestBody("text/plain".toMediaTypeOrNull())
            )

            if (response.isSuccessful) {
                PostResult.Success
            } else {
                PostResult.Error(response.errorBody()?.string() ?: "Errore sconosciuto")
            }
        } catch (e: Exception) {
            PostResult.Error("Errore di rete: ${e.localizedMessage}")
        }
    }
}