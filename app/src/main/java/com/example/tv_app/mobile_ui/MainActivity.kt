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
import com.example.tv_app.repository.DownloadRepository
import com.example.tv_app.utils.NotificationPermissionHelper
import com.example.tv_app.ui.theme.TV_APPTheme
import com.example.tv_app.presentation.screens.videoPlayer.VideoPlayerScreen
import com.example.tv_app.presentation.screens.videoPlayer.VideoPlayerViewModel
import com.example.tv_app.utils.PermissionHelper
import io.objectbox.Box
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import androidx.core.view.WindowCompat

class MainActivity : FragmentActivity() {

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable drawing behind the system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Request necessary permissions
        requestPermissionsIfNeeded()

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
    
    private fun requestPermissionsIfNeeded() {
        if (!PermissionHelper.hasStoragePermission(this)) {
            PermissionHelper.requestStoragePermission(this)
        }
        if (!PermissionHelper.hasNotificationPermission(this)) {
            PermissionHelper.requestNotificationPermission(this)
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PermissionHelper.STORAGE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Storage permission denied. Downloads may not work.", Toast.LENGTH_LONG).show()
                }
            }
            PermissionHelper.NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
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
                android.util.Log.d("FirebaseTest", "DocumentSnapshot added with ID: ${documentReference.id}")
                // Connection successful! You can show a Toast or update UI here
            }
            .addOnFailureListener { e ->
                android.util.Log.w("FirebaseTest", "Error adding document", e)
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
    var lastMainScreen by remember { mutableStateOf<MobileScreen>(MobileScreen.Home) }
    var homeScreenSelectedTab by remember { mutableStateOf(0) } // Track selected tab within HomeScreen (0=Movie, 1=Shows, 2=LiveTV)
    var videoPlayerSourceScreen by remember { mutableStateOf<MobileScreen?>(null) } // Track where the video player was launched from
    val playlistService = remember { PlaylistService() }
    
    // Create a shared ViewModel for the video player
    val videoPlayerViewModel = remember { VideoPlayerViewModel() }
    val hazeState = remember { HazeState() }

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
                        android.util.Log.d("MainActivity", "Selected playlist: ${playlist.name}")
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
                        selectedTab = homeScreenSelectedTab, // Pass current state
                        onTabSelected = { homeScreenSelectedTab = it }, // Update state when tab changes
                        onMovieSelected = { movie ->
                            selectedMovie = movie
                            selectedTvSeries = null // Clear TV series state
                            lastMainScreen = MobileScreen.Home
                            currentScreen = MobileScreen.MovieDetails
                        },
                        onShowSelected = { show ->
                            selectedTvSeries = show
                            selectedMovie = null // Clear movie state
                            lastMainScreen = MobileScreen.Home
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
                val context = androidx.compose.ui.platform.LocalContext.current
                val downloadRepository = remember { com.example.tv_app.repository.DownloadRepository(context) }
                DownloadsScreen(
                    downloadRepository = downloadRepository,
                    onPlayMovie = { downloadedMovie ->
                        // TODO: Implement local playback
                        // For now we can maybe map it back to a Movie object or use a separate player launcher
                        android.util.Log.d("MainActivity", "Play local: ${downloadedMovie.localPath}")
                    }
                )
            }
            MobileScreen.MyList -> {
                MyListScreen(
                    onMovieSelected = { movie ->
                        selectedMovie = movie
                        selectedTvSeries = null // Clear TV series state
                        lastMainScreen = MobileScreen.MyList
                        currentScreen = MobileScreen.MovieDetails
                    },
                    onTvSeriesSelected = { series ->
                        selectedTvSeries = series
                        selectedMovie = null // Clear movie state
                        lastMainScreen = MobileScreen.MyList
                        currentScreen = MobileScreen.TvSeriesDetails
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            MobileScreen.Settings -> {
                SettingsScreen()
            }
            MobileScreen.MovieDetails -> {
                val context = androidx.compose.ui.platform.LocalContext.current
                val downloadRepository = remember { com.example.tv_app.repository.DownloadRepository(context) }
                
                selectedMovie?.let { movie ->
                    MovieDetailsScreen(
                        key = movieDetailsKey, // Force recomposition when key changes
                        movie = movie,
                        playlistService = playlistService,
                        onBackPressed = {
                            currentScreen = lastMainScreen
                            selectedMovie = null // Clear selected movie on back
                        },
                        onMovieSelected = { newMovie ->
                            selectedMovie = newMovie
                            movieDetailsKey++ // Increment key to force screen refresh
                        },
                        onPlayMovie = { movie ->
                            android.util.Log.d("MainActivity", "Playing movie: ${movie.name}")
                            videoPlayerViewModel.loadMovie(movie)
                            videoPlayerSourceScreen = MobileScreen.MovieDetails
                            currentScreen = MobileScreen.VideoPlayer
                        },
                        onDownloadMovie = { movie ->
                            android.util.Log.d("MainActivity", "Download button clicked for: ${movie.name}")
                            android.util.Log.d("MainActivity", "Stream URL: ${movie.streamUrl}")
                            android.util.Log.d("MainActivity", "Stream ID: ${movie.streamId}")
                            downloadRepository.downloadMovie(movie, object : DownloadRepository.NotificationPermissionCallback {
                                override fun onPermissionRequired() {
                                    android.util.Log.d("MainActivity", "Notification permission required, opening settings")
                                    // Ensure we open notification settings from UI thread
                                    try {
                                        NotificationPermissionHelper.openNotificationSettings(context)
                                    } catch (e: Exception) {
                                        android.util.Log.e("MainActivity", "Failed to open notification settings: ${e.message}")
                                    }
                                }
                                
                                override fun onPermissionGranted() {
                                    android.util.Log.d("MainActivity", "Download started successfully")
                                }
                            })
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
                            currentScreen = lastMainScreen
                            selectedTvSeries = null // Clear selected TV series on back
                        },
                        onTvSeriesSelected = { newSeries ->
                            selectedTvSeries = newSeries
                            tvSeriesDetailsKey++ // Increment key to force screen refresh
                        },
                        onEpisodeSelected = { episode ->
                            android.util.Log.d("MainActivity", "Playing episode: ${episode.name}")
                            videoPlayerViewModel.loadEpisode(episode)
                            videoPlayerSourceScreen = MobileScreen.TvSeriesDetails
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
                        currentScreen = videoPlayerSourceScreen ?: lastMainScreen // Fallback to lastMainScreen if source is null
                        videoPlayerSourceScreen = null // Clear source after navigating back
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
