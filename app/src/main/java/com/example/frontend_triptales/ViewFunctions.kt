package com.example.frontend_triptales

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TripTalesViewModel (application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>().applicationContext

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips: StateFlow<List<Trip>> = _trips.asStateFlow()

    private val _selectedTrip = MutableStateFlow<Trip?>(null)
    val selectedTrip: StateFlow<Trip?> = _selectedTrip.asStateFlow()

    private val _token = MutableStateFlow<String?>(null)

    // Ottiene un ApiService con token se disponibile
    private fun getApiService(): ApiService {
        return ApiClient.create(_token.value)
    }

    suspend fun login(username: String, password: String, context: Context): String? {
        return try {
            val response = ApiClient.create().login(LoginRequest(username, password))

            if (response.isSuccessful) {
                val loginResponse = response.body()!!
                val token = loginResponse.access
                _token.value = token
                Log.d("LoginToken", "Token ricevuto: ${loginResponse.access}")
                if(_token.value != null)
                    TokenManager.saveToken(context, token)
                Log.d("TokenSaved", "Toke salvato ${_token.value}")
                val apiServiceWithToken = ApiClient.create(token)
                val userResponse = apiServiceWithToken.getUserMe()

                return if (userResponse.isSuccessful && userResponse.body() != null) {
                    _currentUser.value = userResponse.body()
                    _isLoggedIn.value = true
                    fetchGroups()
                    loginResponse.access // ritorna il token
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }



    fun logout() {
        _isLoggedIn.value = false
        _currentUser.value = null
        _token.value = null
        _selectedTrip.value = null
        _trips.value = emptyList()
    }

    fun fetchGroups() {
        viewModelScope.launch {
            try {
                val token = TokenManager.getToken(context)
                if (token == null) {
                    Log.e("FetchGroups", "Token non trovato")
                    return@launch
                }

                val response = ApiClient.create(token).getMyGroups()
                if (response.isSuccessful) {
                    val tripsList = response.body() ?: emptyList()
                    Log.d("FetchGroups", "Ricevute ${tripsList.size} gite")
                    tripsList.forEach {
                        Log.d("Trip", "Nome: ${it.name}, Descrizione: ${it.description}")
                    }
                    _trips.value = tripsList
                }

            } catch (e: Exception) {
                Log.e("FetchGroups", "Errore di rete: ${e.localizedMessage}")
            }
        }
    }


    suspend fun createTrip(name: String, description: String, context: Context): Boolean {
        return try {
            val newTrip = CreateTripRequest(name, description)
            val token = TokenManager.getToken(context)
            Log.d("CreateTrip", "Token utilizzato ${token}")
            val response = ApiClient.create(token).createGroup(newTrip)
            if (response.isSuccessful) {
                fetchGroups()
                true
            } else {
                Log.e("CreateTrip", "Errore ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e("CreateTrip", "Errore rete: ${e.localizedMessage}")
            false
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
                val response = getApiService().addComment(newComment)
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

    //fun likePost(postId: Int) {
    //    viewModelScope.launch {
    //        val user = _currentUser.value ?: return@launch
    //        try {
    //            val like = Like(id = 0, user = user.id, post = postId)
    //            val response = getApiService().likePost(like)
    //            if (response.isSuccessful) {
    //                fetchGroups()
    //            } else {
    //                Log.e("LikePost", "Errore ${response.code()}")
    //            }
    //        } catch (e: Exception) {
    //            Log.e("LikePost", "Errore rete: ${e.localizedMessage}")
    //        }
    //    }
    //}

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
            timestamp = System.currentTimeMillis(),
            //group = trip.id
        )

        return try {
            val response = getApiService().addPost(newPost)
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
            val response = ApiClient.create().register(
                RegisterRequest(username, email, password)
            )
            if (response.isSuccessful) {
                val loginResponse = response.body()
                if (loginResponse != null) {
                    _token.value = loginResponse.access
                    val apiServiceWithToken = ApiClient.create(_token.value)
                    val userResponse = apiServiceWithToken.getUserMe()
                    if (userResponse.isSuccessful && userResponse.body() != null) {
                        _currentUser.value = userResponse.body()
                        fetchGroups()
                        _isLoggedIn.value = true
                        return true
                    } else {
                        Log.e("Register", "Errore nel recupero utente")
                        return false
                    }
                }
                Log.e("Register", "Risposta senza body")
                false
            } else {
                Log.e("Register", "Errore ${response.code()}: ${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e("Register", "Errore rete: ${e.localizedMessage}")
            false
        }
    }
}
