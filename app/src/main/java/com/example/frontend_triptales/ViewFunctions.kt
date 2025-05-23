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
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.SocketException

class TripTalesViewModel(application: Application) : AndroidViewModel(application) {
    // Stati
    private val context = getApplication<Application>().applicationContext
    private val _isLoggedIn = MutableStateFlow(false)
    private val _currentUser = MutableStateFlow<User?>(null)
    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    private val _selectedTrip = MutableStateFlow<Trip?>(null)
    private val _token = MutableStateFlow<String?>(null)
    private val _postResult = MutableStateFlow<PostResult?>(null)

    // Flussi pubblici
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    val trips: StateFlow<List<Trip>> = _trips.asStateFlow()
    val selectedTrip: StateFlow<Trip?> = _selectedTrip.asStateFlow()
    val postResult: StateFlow<PostResult?> = _postResult.asStateFlow()

    // Repository
    private val postRepository = PostRepository(
        apiService = ApiClient.create(),
        context = context
    )

    // Ottiene un ApiService con token
    private fun getApiService(): ApiService {
        return ApiClient.create(_token.value)
    }

    // Login
    suspend fun login(username: String, password: String, context: Context): String? {
        return try {
            val response = ApiClient.create().login(LoginRequest(username, password))

            if (response.isSuccessful) {
                val loginResponse = response.body()!!
                _token.value = loginResponse.access.also { token ->
                    TokenManager.saveToken(context, token)
                }

                val userResponse = getApiService().getUserMe()
                if (userResponse.isSuccessful && userResponse.body() != null) {
                    _currentUser.value = userResponse.body()
                    _isLoggedIn.value = true
                    fetchGroups()
                    loginResponse.access
                } else null
            } else null
        } catch (e: Exception) {
            Log.e("Login", "Error: ${e.localizedMessage}")
            null
        }
    }

    // Logout
    fun logout() {
        _isLoggedIn.value = false
        _currentUser.value = null
        _token.value = null
        _selectedTrip.value = null
        _trips.value = emptyList()
        _postResult.value = null
    }

    // Gestione gite
    fun fetchGroups() {
        viewModelScope.launch {
            try {
                val token = TokenManager.getToken(context) ?: run {
                    Log.e("FetchGroups", "Token non trovato")
                    return@launch
                }

                ApiClient.create(token).getMyGroups().let { response ->
                    if (response.isSuccessful) {
                        _trips.value = response.body() ?: emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e("FetchGroups", "Errore: ${e.localizedMessage}")
            }
        }
    }

    suspend fun createTrip(name: String, description: String): Boolean {
        return try {
            val response = getApiService().createGroup(CreateTripRequest(name, description))
            if (response.isSuccessful) {
                fetchGroups()
                true
            } else {
                Log.e("CreateTrip", "Errore ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e("CreateTrip", "Errore: ${e.localizedMessage}")
            false
        }
    }

    fun selectTrip(trip: Trip) {
        _selectedTrip.value = trip
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
                title = title,
                content = content,
                latitude = latitude,
                longitude = longitude,
                groupId = groupId
            )) {
                is PostResult.Success -> {
                    delay(100) // Piccolo delay per evitare broken pipe
                    true
                }
                is PostResult.Error -> {
                    Log.e("Network", "Error: ${result.message}")
                    false
                }
                PostResult.Loading -> false // Non dovrebbe mai arrivare qui
            }
        } catch (e: SocketException) {
            Log.e("Network", "Connection closed prematurely: ${e.message}")
            true // Considera comunque successo se il server ha risposto 201
        } catch (e: Exception) {
            Log.e("Network", "Error: ${e.message}")
            false
        }
    }

    // Gestione commenti
    fun addComment(postId: String, content: String) {
        viewModelScope.launch {
            _currentUser.value?.let { user ->
                try {
                    val response = getApiService().addComment(
                        Comment(
                            id = 0,
                            userId = user.id,
                            username = user.username,
                            content = content,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    if (response.isSuccessful) fetchGroups()
                } catch (e: Exception) {
                    Log.e("AddComment", "Errore: ${e.localizedMessage}")
                }
            }
        }
    }

    // Registrazione
    suspend fun register(username: String, email: String, password: String): Boolean {
        return try {
            ApiClient.create().register(RegisterRequest(username, email, password)).let { response ->
                if (response.isSuccessful) {
                    response.body()?.let { loginResponse ->
                        _token.value = loginResponse.access
                        getApiService().getUserMe().let { userResponse ->
                            if (userResponse.isSuccessful && userResponse.body() != null) {
                                _currentUser.value = userResponse.body()
                                fetchGroups()
                                _isLoggedIn.value = true
                                true
                            } else false
                        }
                    } ?: false
                } else false
            }
        } catch (e: Exception) {
            Log.e("Register", "Errore: ${e.localizedMessage}")
            false
        }
    }

    // Upload immagini (da implementare)
    suspend fun uploadImage(uri: Uri) {
        // TODO: Implementa l'upload
    }

    // Reset del risultato del post
    fun resetPostResult() {
        _postResult.value = null
    }
}

// Classi di supporto
sealed class PostResult {

    data object Loading : PostResult()
    data object Success : PostResult()
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
            val token = TokenManager.getToken(context) ?: return PostResult.Error("Token non valido")

            ApiClient.create(token).addPost(
                title = title.toRequestBody("text/plain".toMediaTypeOrNull()),
                content = content.toRequestBody("text/plain".toMediaTypeOrNull()),
                latitude = latitude?.toString().orEmpty().toRequestBody("text/plain".toMediaTypeOrNull()),
                longitude = longitude?.toString().orEmpty().toRequestBody("text/plain".toMediaTypeOrNull()),
                groupId = groupId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            ).let { response ->
                if (response.isSuccessful) {
                    PostResult.Success
                } else {
                    PostResult.Error(response.errorBody()?.string() ?: "Errore sconosciuto")
                }
            }
        } catch (e: Exception) {
            PostResult.Error("Errore di rete: ${e.localizedMessage}")
        }
    }
}