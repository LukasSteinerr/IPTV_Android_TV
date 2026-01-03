package com.example.tv_app.mobile_ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.tv.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.example.tv_app.model.ObjectBox
import com.example.tv_app.model.Playlist
import com.example.tv_app.model.TvSeries
import com.example.tv_app.model.Channel
import com.example.tv_app.model.Movie
import com.example.tv_app.model.TvEpisode
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.ui.theme.TV_APPTheme
import com.example.tv_app.presentation.screens.videoPlayer.VideoPlayerScreen
import com.example.tv_app.presentation.screens.videoPlayer.VideoPlayerViewModel
import io.objectbox.Box
import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

class MainActivity : FragmentActivity() {

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
                    MobileAppNavigation()
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
fun MobileAppNavigation() {
    var currentScreen by remember { mutableStateOf<MobileScreen>(MobileScreen.MyPlaylists) }
    var playlistRefreshKey by remember { mutableStateOf(0) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var selectedMovie by remember { mutableStateOf<Movie?>(null) }
    var selectedTvSeries by remember { mutableStateOf<TvSeries?>(null) }
    var movieDetailsKey by remember { mutableStateOf(0) } // Key to force recomposition
    var tvSeriesDetailsKey by remember { mutableStateOf(0) } // Key to force recomposition
    val playlistService = remember { PlaylistService() }
    
    // Create a shared ViewModel for the video player
    val videoPlayerViewModel = remember { VideoPlayerViewModel() }

    val hazeState = remember { HazeState() }
    val context = LocalContext.current

    val showBottomBar = when (currentScreen) {
        MobileScreen.Home, MobileScreen.Downloads, MobileScreen.MyList, MobileScreen.Settings -> true
        else -> false
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                GlassmorphicBottomNavigationBar(
                    currentScreen = currentScreen,
                    onScreenSelected = { currentScreen = it },
                    hazeState = hazeState
                )
            }
        }
    ) { paddingValues ->
        when (currentScreen) {
            MobileScreen.MyPlaylists -> {
                MyPlaylistsScreen(
                    refreshKey = playlistRefreshKey,
                    playlistService = playlistService,
                    onNavigateToAddPlaylist = {
                        currentScreen = MobileScreen.AddPlaylist
                    },
                    onPlaylistSelected = { playlist ->
                        selectedPlaylist = playlist
                        currentScreen = MobileScreen.Home
                        Log.d("MainActivity", "Selected playlist: ${playlist.name}")
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            MobileScreen.AddPlaylist -> {
                AddPlaylistScreen(
                    playlistService = playlistService,
                    onPlaylistAdded = {
                        playlistRefreshKey++
                        currentScreen = MobileScreen.MyPlaylists
                    },
                    onNavigateUp = {
                        currentScreen = MobileScreen.MyPlaylists
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            MobileScreen.Home -> {
                selectedPlaylist?.let { playlist ->
                    HomeScreen(
                        playlist = playlist,
                        playlistService = playlistService,
                        onMovieSelected = { movie ->
                            selectedMovie = movie
                            currentScreen = MobileScreen.MovieDetails
                        },
                        onShowSelected = { show ->
                            selectedTvSeries = show
                            currentScreen = MobileScreen.TvSeriesDetails
                        },
                        onChannelSelected = { channel ->
                            // TODO: Navigate to channel player
                        },
                        onBackPressed = {
                            currentScreen = MobileScreen.MyPlaylists
                        },
                        hazeState = hazeState,
                        contentPadding = paddingValues
                    )
                }
            }
            MobileScreen.Downloads -> {
                DownloadsScreen()
            }
            MobileScreen.MyList -> {
                MyListScreen()
            }
            MobileScreen.Settings -> {
                SettingsScreen()
            }
            MobileScreen.MovieDetails -> {
                selectedMovie?.let { movie ->
                    MovieDetailsScreen(
                        key = movieDetailsKey, // Force recomposition when key changes
                        movie = movie,
                        playlistService = playlistService,
                        onBackPressed = {
                            currentScreen = MobileScreen.Home
                        },
                        onMovieSelected = { newMovie ->
                            selectedMovie = newMovie
                            movieDetailsKey++ // Increment key to force screen refresh
                        },
                        onPlayMovie = { movie ->
                            Log.d("MainActivity", "Playing movie: ${movie.name}")
                            videoPlayerViewModel.loadMovie(movie)
                            currentScreen = MobileScreen.VideoPlayer
                        },
                        onDownloadMovie = { movieToDownload ->
                            // Use a repository initialized in the activity or remember here
                            val repo = com.example.tv_app.repository.DownloadRepository(context)
                            repo.downloadMovie(movieToDownload)
                            // Optionally show a toast
                            android.widget.Toast.makeText(context, "Downloading ${movieToDownload.name}...", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
            MobileScreen.TvSeriesDetails -> {
                selectedTvSeries?.let { tvSeries ->
                    TvSeriesDetailsScreen(
                        key = tvSeriesDetailsKey, // Force recomposition when key changes
                        tvSeries = tvSeries,
                        playlistService = playlistService,
                        onBackPressed = {
                            currentScreen = MobileScreen.Home
                        },
                        onTvSeriesSelected = { newSeries ->
                            selectedTvSeries = newSeries
                            tvSeriesDetailsKey++ // Increment key to force screen refresh
                        },
                        onEpisodeSelected = { episode ->
                            Log.d("MainActivity", "Playing episode: ${episode.name}")
                            videoPlayerViewModel.loadEpisode(episode)
                            currentScreen = MobileScreen.VideoPlayer
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
            MobileScreen.VideoPlayer -> {
                VideoPlayerScreen(
                    onBackPressed = {
                        // Reset the video player state when navigating away
                        videoPlayerViewModel.reset()
                        // Check if we were playing a movie or episode to navigate back to the correct screen
                        // For now, we'll check if we have a selected TV series to determine the back navigation
                        currentScreen = if (selectedTvSeries != null) MobileScreen.TvSeriesDetails else MobileScreen.MovieDetails
                    },
                    viewModel = videoPlayerViewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {
                // Do nothing
            }
        }
    }
}


sealed class MobileScreen {
    object MyPlaylists : MobileScreen()
    object AddPlaylist : MobileScreen()
    object MovieDetails : MobileScreen()
    object TvSeriesDetails : MobileScreen()
    object VideoPlayer : MobileScreen()
    object Home : MobileScreen()
    object Downloads : MobileScreen()
    object MyList : MobileScreen()
    object Settings : MobileScreen()
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
