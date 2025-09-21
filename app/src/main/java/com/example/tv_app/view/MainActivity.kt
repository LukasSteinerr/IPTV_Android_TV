package com.example.tv_app.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.tv.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.example.tv_app.model.ObjectBox
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.ui.theme.TV_APPTheme
import io.objectbox.Box
import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Test Firebase connection
        testFirebaseConnection()

        setContent {
            TV_APPTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    AppNavigation()
                }
            }
        }
    }

    // Define a simple function to test the connection
    fun testFirebaseConnection() {
        val db = Firebase.firestore
        val user = hashMapOf(
            "first" to "Ada",
            "last" to "Lovelace",
            "born" to 1815
        )

        // Add a new document with a generated ID
        db.collection("testUsers")
            .add(user)
            .addOnSuccessListener { documentReference ->
                Log.d("FirebaseTest", "DocumentSnapshot added with ID: ${documentReference.id}")
                // Connection successful! You can show a Toast or update UI here
            }
            .addOnFailureListener { e ->
                Log.w("FirebaseTest", "Error adding document", e)
                // Connection failed. Handle the error (e.g., check for network issues, rules)
            }
    }
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.MyPlaylists) }
    val playlistService = remember { PlaylistService() }

    when (currentScreen) {
        Screen.MyPlaylists -> {
            MyPlaylistsScreen(
                playlistService = playlistService,
                onNavigateToAddPlaylist = {
                    currentScreen = Screen.AddPlaylist
                },
                onPlaylistSelected = { playlist ->
                    // Handle playlist selection - could navigate to channels screen
                    Log.d("MainActivity", "Selected playlist: ${playlist.name}")
                }
            )
        }
        Screen.AddPlaylist -> {
            AddPlaylistScreen(
                playlistService = playlistService,
                onPlaylistAdded = {
                    currentScreen = Screen.MyPlaylists
                }
            )
        }
    }
}

sealed class Screen {
    object MyPlaylists : Screen()
    object AddPlaylist : Screen()
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TV_APPTheme {
        Greeting("Android")
    }
}