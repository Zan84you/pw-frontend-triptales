package com.example.frontend_triptales

import android.app.Application
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.frontend_triptales.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TripTalesApp()
            }
        }
    }
}

@Composable
fun TripTalesApp() {
    val context = LocalContext.current
    val viewModel: TripTalesViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
    )
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val navController = rememberNavController()

    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) "trips" else "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate("trips") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate("register")
                    }
                )
            }
            composable("register") {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate("trips") {
                            popUpTo("register") { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }
            composable("trips") {
                TripsScreen(
                    onTripSelected = { trip ->
                        viewModel.selectTrip(trip) // seleziono il trip prima di navigare
                        navController.navigate("trip_detail")
                    },
                    onCreateTrip = {
                        navController.navigate("create_trip")
                    },
                    onLogout = {
                        viewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable("create_trip") {
                CreateTripScreen(
                    onTripCreated = {
                        navController.popBackStack()
                    },
                    onCancel = {
                        navController.popBackStack()
                    }
                )
            }
            composable("trip_detail") {
                TripDetailScreen(
                    viewModel = viewModel,
                    navController = navController,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable("profile") {
                ProfileScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable("create_post") {
                CreatePostScreen(
                    onPostCreated = {
                        navController.popBackStack()
                    },
                    onCancel = {
                        navController.popBackStack()
                    },
                    viewModel = viewModel // sempre passare il viewModel per accedere al trip selezionato
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,  // Passiamo il token alla schermata successiva
    onNavigateToRegister: () -> Unit,
    viewModel: TripTalesViewModel = viewModel()
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Text(
                text = "TripTales",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Documenta le tue gite in modo collaborativo",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }

            Button(
                onClick = {
                    errorMessage = null
                    if (username.isEmpty() || password.isEmpty()) {
                        errorMessage = "Inserisci username e password"
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        val token = viewModel.login(username, password)
                        isLoading = false
                        if (!token.isNullOrEmpty()) {
                            onLoginSuccess(token)
                        } else {
                            errorMessage = "Credenziali non valide"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Accedi")
                }
            }

            TextButton(onClick = onNavigateToRegister) {
                Text("Non hai un account? Registrati")
            }
        }
    }
}

@Composable
fun RegisterScreen(
    onRegisterSuccess: (String) -> Unit,  // Ora passa il token
    onNavigateToLogin: () -> Unit,
    viewModel: TripTalesViewModel = viewModel()
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Text(
                text = "Crea Account",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }

            Button(
                onClick = {
                    if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                        errorMessage = "Compila tutti i campi"
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        // Simulate network delay
                        delay(1000)
                        val token = viewModel.register(username, email, password)
                        isLoading = false
                        if (token) {
                            onRegisterSuccess(token.toString())
                        } else {
                            errorMessage = "Errore durante la registrazione"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Registrati")
                }
            }

            TextButton(onClick = onNavigateToLogin) {
                Text("Hai già un account? Accedi")
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(
    onTripSelected: (Trip) -> Unit,
    onCreateTrip: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: TripTalesViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
    )

    LaunchedEffect(Unit) {
        viewModel.fetchGroups()
    }

    val trips by viewModel.trips.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Le tue gite") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateTrip) {
                Icon(Icons.Default.Add, contentDescription = "Crea nuova gita")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                currentUser?.let { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.username.first().toString(),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Ciao, ${user.username}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Badge: ${user.badges.size} | Like: ${user.likesCount}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (trips.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nessuna gita disponibile.\nCreane una nuova!",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(trips) { trip ->
                    TripCard(trip = trip, onClick = { onTripSelected(trip) })
                }
            }
        }
    }
}
//@Composable
//fun TripCard(trip: Trip, onClick: () -> Unit) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable(onClick = onClick),
//        shape = RoundedCornerShape(12.dp)
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp)
//        ) {
//            Text(
//                text = trip.name,
//                fontWeight = FontWeight.Bold,
//                fontSize = 18.sp
//            )
//            Spacer(modifier = Modifier.height(4.dp))
//            Text(
//                text = trip.description,
//                fontSize = 14.sp,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//            Row(
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Icon(
//                    Icons.Default.Group,
//                    contentDescription = null,
//                    tint = MaterialTheme.colorScheme.primary,
//                    modifier = Modifier.size(16.dp)
//                )
//                Spacer(modifier = Modifier.width(4.dp))
//                Text(
//                    text = "${trip.members?.size} partecipanti",
//                    fontSize = 12.sp,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//                Spacer(modifier = Modifier.width(16.dp))
//                Icon(
//                    Icons.Default.Image,
//                    contentDescription = null,
//                    tint = MaterialTheme.colorScheme.primary,
//                    modifier = Modifier.size(16.dp)
//                )
//                Spacer(modifier = Modifier.width(4.dp))
//                Text(
//                    text = "${trip.posts?.size} post",
//                    fontSize = 12.sp,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun CreateTripScreen(
//    onTripCreated: () -> Unit,
//    onCancel: () -> Unit,
//    viewModel: TripTalesViewModel = viewModel()
//) {
//    var name by remember { mutableStateOf("") }
//    var description by remember { mutableStateOf("") }
//    var isLoading by remember { mutableStateOf(false) }
//    val scope = rememberCoroutineScope()
//    val context = LocalContext.current
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Crea nuova gita") },
//                navigationIcon = {
//                    IconButton(onClick = onCancel) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
//                    }
//                }
//            )
//        }
//    ) { innerPadding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(innerPadding)
//                .padding(horizontal = 16.dp),
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            Spacer(modifier = Modifier.height(16.dp))
//
//            OutlinedTextField(
//                value = name,
//                onValueChange = { name = it },
//                label = { Text("Nome della gita") },
//                modifier = Modifier.fillMaxWidth(),
//                singleLine = true
//            )
//
//            OutlinedTextField(
//                value = description,
//                onValueChange = { description = it },
//                label = { Text("Descrizione") },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(120.dp),
//                singleLine = false
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            Button(
//                onClick = {
//                    if (name.isEmpty()) {
//                        Toast.makeText(context, "Inserisci un nome per la gita", Toast.LENGTH_SHORT).show()
//                        return@Button
//                    }
//                    isLoading = true
//                    scope.launch {
//                        delay(800)
//                        val success = viewModel.createTrip(name, description)
//                        isLoading = false
//                        if (success) {
//                            Toast.makeText(context, "Gita creata con successo", Toast.LENGTH_SHORT).show()
//                            onTripCreated()
//                        } else {
//                            Toast.makeText(context, "Errore nella creazione", Toast.LENGTH_SHORT).show()
//                        }
//                    }
//                },
//                modifier = Modifier.fillMaxWidth(),
//                enabled = !isLoading
//            ) {
//                if (isLoading) {
//                    CircularProgressIndicator(
//                        modifier = Modifier.size(24.dp),
//                        color = MaterialTheme.colorScheme.onPrimary,
//                        strokeWidth = 2.dp
//                    )
//                } else {
//                    Text("Crea gita")
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun PostsTab(viewModel: TripTalesViewModel = viewModel()) {
//    val posts by viewModel.posts.collectAsState()
//
//    if (posts.isEmpty()) {
//        EmptyState(message = "Nessun post. Aggiungi il primo post!")
//    } else {
//        LazyColumn {
//            items(posts) { post ->
//                PostCard(post = post)
//            }
//        }
//    }
//}
//
//@Composable
//fun MembersTab(viewModel: TripTalesViewModel = viewModel()) {
//    val members by viewModel.members.collectAsState()
//
//    if (members.isEmpty()) {
//        EmptyState(message = "Nessun membro nel gruppo")
//    } else {
//        LazyColumn {
//            items(members) { member ->
//                MemberItem(member = member)
//            }
//        }
//    }
//}
//
//@Composable
//fun EmptyState(message: String) {
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        Text(text = message)
//    }
//}
//
//@Composable
//fun PostCard(post: Post, viewModel: TripTalesViewModel = viewModel()) {
//    val scope = rememberCoroutineScope()
//    var commentText by remember { mutableStateOf("") }
//    var showComments by remember { mutableStateOf(false) }
//    val context = LocalContext.current
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(12.dp)
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp)
//        ) {
//            // User info
//            Row(
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Box(
//                    modifier = Modifier
//                        .size(32.dp)
//                        .clip(CircleShape)
//                        .background(MaterialTheme.colorScheme.primaryContainer),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text(
//                        text = post.username.first().toString(),
//                        color = MaterialTheme.colorScheme.onPrimaryContainer,
//                        fontWeight = FontWeight.Bold
//                    )
//                }
//                Spacer(modifier = Modifier.width(8.dp))
//                Column {
//                    Text(
//                        text = post.username,
//                        fontWeight = FontWeight.Bold,
//                        fontSize = 14.sp
//                    )
//                    if (post.locationName != null) {
//                        Text(
//                            text = post.locationName,
//                            fontSize = 12.sp,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                    }
//                }
//            }
//
//            Spacer(modifier = Modifier.height(12.dp))
//
//            // Post content
//            Text(text = post.content)
//
//            Spacer(modifier = Modifier.height(12.dp))
//
//            // Post image if available
//            //post.image?.let {
//            //    Box(
//            //        modifier = Modifier
//            //            .fillMaxWidth()
//            //            .height(200.dp)
//            //            .clip(RoundedCornerShape(8.dp))
//            //            .background(MaterialTheme.colorScheme.surfaceVariant)
//            //    ) {
//            //        // In a real app, use Coil or other image loading library
//            //        Image(
//            //            painter = rememberAsyncImagePainter(it),
//            //            contentDescription = null,
//            //            modifier = Modifier.fillMaxSize(),
//            //            contentScale = ContentScale.Crop
//            //        )
//            //    }
//            //    Spacer(modifier = Modifier.height(12.dp))
//            //}
//
//            // Actions
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    IconButton(
//                        onClick = {
//                            scope.launch {
//                                //viewModel.likePost(post.id)
//                                Toast.makeText(context, "Post apprezzato", Toast.LENGTH_SHORT).show()
//                            }
//                        }
//                    ) {
//                        Icon(
//                            Icons.Default.ThumbUp,
//                            contentDescription = "Mi piace",
//                            tint = MaterialTheme.colorScheme.primary
//                        )
//                    }
//                    Text(text = post.likes.toString())
//
//                    Spacer(modifier = Modifier.width(16.dp))
//
//                    IconButton(onClick = { showComments = !showComments }) {
//                        Icon(
//                            Icons.Default.Comment,
//                            contentDescription = "Commenti",
//                            tint = MaterialTheme.colorScheme.primary
//                        )
//                    }
//                    Text(text = post.comments.size.toString())
//                }
//            }
//
//            // Comments section
//            if (showComments) {
//                Spacer(modifier = Modifier.height(16.dp))
//                Divider()
//                Spacer(modifier = Modifier.height(8.dp))
//
//                Text(
//                    text = "Commenti",
//                    fontWeight = FontWeight.Bold,
//                    fontSize = 16.sp
//                )
//
//                Spacer(modifier = Modifier.height(8.dp))
//
//                // Comments list
//                if (post.comments.isNotEmpty()) {
//                    Column(
//                        verticalArrangement = Arrangement.spacedBy(8.dp)
//                    ) {
//                        post.comments.forEach { comment ->
//                            Row(
//                                modifier = Modifier.fillMaxWidth(),
//                                verticalAlignment = Alignment.Top
//                            ) {
//                                Box(
//                                    modifier = Modifier
//                                        .size(24.dp)
//                                        .clip(CircleShape)
//                                        .background(MaterialTheme.colorScheme.primaryContainer),
//                                    contentAlignment = Alignment.Center
//                                ) {
//                                    Text(
//                                        text = comment.username.first().toString(),
//                                        fontSize = 12.sp,
//                                        color = MaterialTheme.colorScheme.onPrimaryContainer
//                                    )
//                                }
//                                Spacer(modifier = Modifier.width(8.dp))
//                                Column {
//                                    Text(
//                                        text = comment.username,
//                                        fontWeight = FontWeight.Bold,
//                                        fontSize = 12.sp
//                                    )
//                                    Text(
//                                        text = comment.content,
//                                        fontSize = 14.sp
//                                    )
//                                }
//                            }
//                        }
//                    }
//                } else {
//                    Text(
//                        text = "Nessun commento",
//                        fontSize = 14.sp,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Add comment
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    OutlinedTextField(
//                        value = commentText,
//                        onValueChange = { commentText = it },
//                        placeholder = { Text("Aggiungi un commento...") },
//                        modifier = Modifier.weight(1f),
//                        singleLine = true
//                    )
//                    IconButton(
//                        onClick = {
//                            if (commentText.isNotEmpty()) {
//                                scope.launch {
//                                    viewModel.addComment(post.id, commentText)
//                                    commentText = ""
//                                }
//                            }
//                        }
//                    ) {
//                        Icon(
//                            Icons.Default.Send,
//                            contentDescription = "Invia",
//                            tint = MaterialTheme.colorScheme.primary
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
////@Composable
////fun MapTab(trip: Trip) {
////    val posts = trip.posts?.filter { it.location != null }
////    var mapProperties by remember {
////        mutableStateOf(
////            MapProperties(
////                mapType = MapType.NORMAL,
////                isMyLocationEnabled = false
////            )
////        )
////    }
////    val context = LocalContext.current
////    val launcher = rememberLauncherForActivityResult(
////        contract = ActivityResultContracts.RequestPermission(),
////        onResult = { isGranted ->
////            mapProperties = mapProperties.copy(isMyLocationEnabled = isGranted)
////        }
////    )
////
////    // Request location permission
////    LaunchedEffect(Unit) {
////        when (PackageManager.PERMISSION_GRANTED) {
////            ContextCompat.checkSelfPermission(
////                context,
////                Manifest.permission.ACCESS_FINE_LOCATION
////            ) -> {
////                mapProperties = mapProperties.copy(isMyLocationEnabled = true)
////            }
////            else -> {
////                launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
////            }
////        }
////    }
////
////    Box(modifier = Modifier.fillMaxSize()) {
////        if (posts != null) {
////            if (posts.isEmpty()) {
////                Box(
////                    modifier = Modifier.fillMaxSize(),
////                    contentAlignment = Alignment.Center
////                ) {
////                    Text(
////                        text = "Nessun post con posizione",
////                        textAlign = TextAlign.Center,
////                        color = MaterialTheme.colorScheme.onSurfaceVariant
////                    )
////                }
////            } else {
////                val rome = LatLng(41.9028, 12.4964)
////                var cameraPositionState = rememberCameraPositionState {
////                    position = CameraPosition.fromLatLngZoom(
////                        posts.firstOrNull()?.location ?: rome, 12f
////                    )
////                }
////
////                GoogleMap(
////                    modifier = Modifier.fillMaxSize(),
////                    cameraPositionState = cameraPositionState,
////                    properties = mapProperties
////                ) {
////                    posts.forEach { post ->
////                        post.location?.let { location ->
////                            Marker(
////                                state = MarkerState(position = location),
////                                title = post.username,
////                                snippet = post.locationName ?: "Post"
////                            )
////                        }
////                    }
////                }
////            }
////        }
////    }
////}
//
////@Composable
////fun MembersTab(trip: Trip, viewModel: TripTalesViewModel = viewModel()) {
////    val members by viewModel.members.collectAsState()
////
////    if (members.isEmpty()) {
////        Box(
////            modifier = Modifier
////                .fillMaxSize()
////                .padding(16.dp),
////            contentAlignment = Alignment.Center
////        ) {
////            Text("Nessun membro nel gruppo")
////        }
////    } else {
////        LazyColumn(
////            modifier = Modifier
////                .fillMaxSize()
////                .padding(16.dp),
////            verticalArrangement = Arrangement.spacedBy(8.dp)
////        ) {
////            items(members) { member ->
////                MemberItem(member = member)
////            }
////        }
////    }
////}
//
//@Composable
//fun MemberItem(member: User) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(8.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Box(
//            modifier = Modifier
//                .size(40.dp)
//                .clip(CircleShape)
//                .background(MaterialTheme.colorScheme.primaryContainer),
//            contentAlignment = Alignment.Center
//        ) {
//            Text(
//                text = member.username.first().toString(),
//                color = MaterialTheme.colorScheme.onPrimaryContainer,
//                fontWeight = FontWeight.Bold
//            )
//        }
//        Spacer(modifier = Modifier.width(16.dp))
//        Text(
//            text = member.username,
//            style = MaterialTheme.typography.bodyLarge
//        )
//    }
//}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onPostCreated: () -> Unit,
    onCancel: () -> Unit,
    viewModel: TripTalesViewModel = viewModel()
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val trip = viewModel.selectedTrip.collectAsState().value

    LaunchedEffect(trip) {
        Log.d("CreatePostScreen", "Trip selezionato: $trip")
    }

    if (trip == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Errore: nessuna gita selezionata.")
            Button(onClick = onCancel) {
                Text("Torna indietro")
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuovo post") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titolo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Cosa vuoi condividere?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                singleLine = false
            )

            OutlinedTextField(
                value = locationName,
                onValueChange = { locationName = it },
                label = { Text("Nome del luogo (opzionale)") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (title.isEmpty() || content.isEmpty()) {
                        Toast.makeText(
                            context,
                            "Titolo e contenuto sono obbligatori",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    isLoading = true
                    scope.launch {
                        val latitude = if (locationName.isNotEmpty()) 41.9028 else null
                        val longitude = if (locationName.isNotEmpty()) 12.4964 else null

                        Log.d(
                            "CreatePostScreen",
                            "Creazione post. Titolo=$title, Content=$content, TripId=${trip.id}"
                        )

                        val success = try {
                            viewModel.addPost(
                                title = title,
                                content = content,
                                latitude = latitude,
                                longitude = longitude,
                                groupId = trip.id
                            )
                        } catch (e: Exception) {
                            Log.e("CreatePostScreen", "Errore nella creazione del post", e)
                            Toast.makeText(
                                context,
                                "Errore nella creazione del post: ${e.localizedMessage}",
                                Toast.LENGTH_SHORT
                            ).show()
                            false
                        } finally {
                            isLoading = false
                        }

                        if (success as Boolean) {
                            Log.d("CreatePostScreen", "Post creato con successo")
                            Toast.makeText(context, "Post creato con successo", Toast.LENGTH_SHORT)
                                .show()
                            onPostCreated()
                        } else {
                            Log.e("CreatePostScreen", "Errore nella creazione del post")
                            Toast.makeText(
                                context,
                                "Errore nella creazione del post",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Pubblica")
                }
            }
        }
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: TripTalesViewModel = viewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()

    if (currentUser == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Utente non trovato")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profilo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            currentUser?.let { user ->
                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.username.first().toString(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = user.username,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )

                Text(
                    text = user.email,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Badge",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (user.badges.isEmpty()) {
                            Text(
                                text = "Nessun badge ottenuto",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(user.badges) { badge ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Explore,
                                                contentDescription = null,
                                                modifier = Modifier.size(32.dp),
                                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = badge.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Attività",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "12",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Post",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "3",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Gite",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${user.likesCount}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Mi piace",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TripCard(
    trip: Trip,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = trip.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = trip.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            TripStats(
                membersCount = trip.memberIds?.size ?: 0,
                postsCount = trip.postIds?.size ?: 0
            )
        }
    }
}

@Composable
private fun TripStats(
    membersCount: Int,
    postsCount: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        //IconWithText(
        //    icon = Icons.Default.Group,
        //    text = "$membersCount partecipanti"
        //)
//
        //Spacer(modifier = Modifier.width(16.dp))
//
        //IconWithText(
        //    icon = Icons.Default.Image,
        //    text = "$postsCount post"
        //)
    }
}

//@Composable
//private fun IconWithText(
//    icon: ImageVector,
//    text: String
//) {
//    Row(
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Icon(
//            imageVector = icon,
//            contentDescription = null,
//            tint = MaterialTheme.colorScheme.primary,
//            modifier = Modifier.size(16.dp)
//        )
//
//        Spacer(modifier = Modifier.width(4.dp))
//
//        Text(
//            text = text,
//            style = MaterialTheme.typography.labelSmall,
//            color = MaterialTheme.colorScheme.onSurfaceVariant
//        )
//    }
//}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripScreen(
    onTripCreated: () -> Unit,
    onCancel: () -> Unit,
    viewModel: TripTalesViewModel = viewModel()
) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crea nuova gita") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { innerPadding ->
        CreateTripForm(
            name = name,
            onNameChange = { name = it },
            description = description,
            onDescriptionChange = { description = it },
            isLoading = isLoading,
            onSubmit = {
                if (name.isBlank()) {
                    Toast.makeText(context, "Inserisci un nome per la gita", Toast.LENGTH_SHORT).show()
                    return@CreateTripForm
                }

                isLoading = true
                scope.launch {
                    val success = viewModel.createTrip(name, description)
                    isLoading = false

                    if (success) {
                        Toast.makeText(context, "Gita creata con successo", Toast.LENGTH_SHORT).show()
                        onTripCreated()
                    } else {
                        Toast.makeText(context, "Errore nella creazione", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun CreateTripForm(
    name: String,
    onNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    isLoading: Boolean,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Nome della gita") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = name.isBlank()
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Descrizione") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            singleLine = false
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && name.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Crea gita")
            }
        }
    }
}

@Composable
fun PostsTab(
    viewModel: TripTalesViewModel,
    modifier: Modifier = Modifier
) {
    val posts by viewModel.posts.collectAsState()

    if (posts.isEmpty()) {
        EmptyState(
            message = "Nessun post. Aggiungi il primo post!",
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(posts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
fun MembersTab(
    viewModel: TripTalesViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val members by viewModel.members.collectAsState()

    if (members.isEmpty()) {
        EmptyState(
            message = "Nessun membro nel gruppo",
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(members, key = { it.id }) { member ->
                MemberItem(
                    member = member,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PostCard(
    post: Post,
    viewModel: TripTalesViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var showComments by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            PostHeader(post)

            Spacer(modifier = Modifier.height(12.dp))

            PostContent(post)

            Spacer(modifier = Modifier.height(12.dp))

            PostActions(
                post = post,
                onLikeClick = {
                    scope.launch {
                        // viewModel.likePost(post.id)
                        Toast.makeText(context, "Post apprezzato", Toast.LENGTH_SHORT).show()
                    }
                },
                onCommentClick = { showComments = !showComments }
            )

            if (showComments) {
                PostCommentsSection(
                    comments = post.comments,
                    commentText = commentText,
                    onCommentTextChange = { commentText = it },
                    onAddComment = {
                        if (commentText.isNotEmpty()) {
                            scope.launch {
                                viewModel.addComment(post.id, commentText)
                                commentText = ""
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PostHeader(post: Post) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Column {
            //Text(
            //    text = if (!post.username.isNullOrEmpty()) post.username else "Anonimo",
            //    style = MaterialTheme.typography.labelLarge,
            //    fontWeight = FontWeight.Bold
            //)

            post.title?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                )
            }

            post.locationName?.let { location ->
                Text(
                    text = location,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}




@Composable
private fun UserAvatar(
    initial: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PostContent(post: Post) {
    Text(
        text = post.content,
        style = MaterialTheme.typography.bodyMedium
    )

    // Uncomment when implementing images
    /*
    post.image?.let { imageUrl ->
        Spacer(modifier = Modifier.height(12.dp))
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
    }
    */
}

@Composable
private fun PostActions(
    post: Post,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onLikeClick) {
                Icon(
                    imageVector = Icons.Default.ThumbUp,
                    contentDescription = "Mi piace",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = post.likes.toString(),
                style = MaterialTheme.typography.labelMedium
            )

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(onClick = onCommentClick) {
                Icon(
                    imageVector = Icons.Default.Comment,
                    contentDescription = "Commenti",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = post.comments?.size?.toString() ?: "0",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun PostCommentsSection(
    comments: List<Comment>,
    commentText: String,
    onCommentTextChange: (String) -> Unit,
    onAddComment: () -> Unit
) {
    Column {
        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Commenti",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (comments.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                comments.forEach { comment ->
                    CommentItem(comment)
                }
            }
        } else {
            Text(
                text = "Nessun commento",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AddCommentRow(
            commentText = commentText,
            onCommentTextChange = onCommentTextChange,
            onAddComment = onAddComment
        )
    }
}

@Composable
private fun CommentItem(comment: Comment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        UserAvatar(
            initial = comment.username.first().toString(),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = comment.username,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun AddCommentRow(
    commentText: String,
    onCommentTextChange: (String) -> Unit,
    onAddComment: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = commentText,
            onValueChange = onCommentTextChange,
            placeholder = { Text("Aggiungi un commento...") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )

        IconButton(
            onClick = onAddComment,
            enabled = commentText.isNotEmpty()
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Invia",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun MemberItem(
    member: User,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            initial = member.username.first().toString(),
            modifier = Modifier.size(40.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = member.username,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    viewModel: TripTalesViewModel = viewModel(),
    navController: NavController,
    onBack: () -> Unit
) {
    val selectedTrip by viewModel.selectedTrip.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Post", "Mappa", "Membri")

    LaunchedEffect(selectedTrip?.id) {
        selectedTrip?.id?.let { groupId ->
            viewModel.fetchPostsForGroup(groupId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dettaglio Gita") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (selectedTrip != null) {
                            navController.navigate("create_post")
                        } else {
                            Log.e("TripDetailScreen", "Nessuna gita selezionata")
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Aggiungi post")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            selectedTrip?.let { trip ->
                // Tab layout
                TabRow(selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }

                // Tab content
                when (selectedTabIndex) {
                    0 -> PostsTab(viewModel = viewModel)
                    1 -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Mappa (da implementare)")
                    }
                    2 -> MembersTab()
                }
            } ?: run {
                Text("Seleziona una gita per continuare", modifier = Modifier.padding(16.dp))
            }
        }
    }
}
