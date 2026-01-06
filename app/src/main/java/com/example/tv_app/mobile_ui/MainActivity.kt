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
import androidx.compose.ui.zIndex
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
    var myListRefreshKey by remember { mutableStateOf(0) } // Key to force MyList screen refresh
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
        val context = androidx.compose.ui.platform.LocalContext.current
        val downloadRepository = remember { com.example.tv_app.repository.DownloadRepository(context) }
        
        // Screens that are part of the main persistent navigation (Home, Downloads, MyList, Settings)
        // These screens are kept in the composition to preserve their state (e.g., scroll position).
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
            
            // Home Screen (Conditional composition for selectedPlaylist is necessary)
            selectedPlaylist?.let { playlist ->
                HomeScreen(
                    playlist = playlist,
                    playlistService = playlistService,
                    selectedTab = homeScreenSelectedTab,
                    onTabSelected = { homeScreenSelectedTab = it },
                    onMovieSelected = { movie ->
                        selectedMovie = movie
                        selectedTvSeries = null
                        lastMainScreen = MobileScreen.Home
                        currentScreen = MobileScreen.MovieDetails
                    },
                    onShowSelected = { show ->
                        selectedTvSeries = show
                        selectedMovie = null
                        lastMainScreen = MobileScreen.Home
                        currentScreen = MobileScreen.TvSeriesDetails
                    },
                    onChannelSelected = { channel ->
                        android.util.Log.d("MainActivity", "Playing channel: ${channel.name}")
                        videoPlayerViewModel.loadChannel(channel)
                        videoPlayerSourceScreen = MobileScreen.Home
                        currentScreen = MobileScreen.VideoPlayer
                    },
                    onBackPressed = { currentScreen = MobileScreen.MyPlaylists },
                    hazeState = hazeState,
                    contentPadding = paddingValues,
                    modifier = Modifier.zIndex(if (currentScreen == MobileScreen.Home) 1f else 0f)
                )
            }

            // Downloads Screen
            DownloadsScreen(
                downloadRepository = downloadRepository,
                onPlayMovie = { downloadedMovie ->
                    if (downloadedMovie.localPath != null) {
                        android.util.Log.d("MainActivity", "Playing local file: ${downloadedMovie.localPath}")
                        videoPlayerViewModel.loadDownloadedMedia(downloadedMovie)
                        videoPlayerSourceScreen = MobileScreen.Downloads
                        currentScreen = MobileScreen.VideoPlayer
                    } else {
                        android.util.Log.w("MainActivity", "Cannot play downloaded content: localPath is null or missing.")
                        Toast.makeText(context, "File path not found. Download may be incomplete.", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .padding(paddingValues)
                    .zIndex(if (currentScreen == MobileScreen.Downloads) 1f else 0f)
            )

            // Settings Screen
            SettingsScreen(
                onNavigateToMyPlaylists = { currentScreen = MobileScreen.MyPlaylists },
                modifier = Modifier
                    .padding(paddingValues)
                    .zIndex(if (currentScreen == MobileScreen.Settings) 1f else 0f)
            )

            // MyList Screen
            MyListScreen(
                refreshKey = myListRefreshKey, // Pass the new refresh key
                onMovieSelected = { movie ->
                    selectedMovie = movie
                    selectedTvSeries = null
                    lastMainScreen = MobileScreen.MyList
                    currentScreen = MobileScreen.MovieDetails
                },
                onTvSeriesSelected = { series ->
                    selectedTvSeries = series
                    selectedMovie = null
                    lastMainScreen = MobileScreen.MyList
                    currentScreen = MobileScreen.TvSeriesDetails
                },
                modifier = Modifier
                    .padding(paddingValues)
                    .zIndex(if (currentScreen == MobileScreen.MyList) 1f else 0f)
            )
        }


        // Non-persistent screens (MyPlaylists, AddPlaylist, Details, Player)
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
                        // Navigate based on playlist type
                        if (playlist.isM3u) {
                            currentScreen = MobileScreen.M3uPlaylist
                        } else {
                            currentScreen = MobileScreen.Home
                        }
                        android.util.Log.d("MainActivity", "Selected playlist: ${playlist.name} (Type: ${if(playlist.isM3u) "M3U" else "Xtream"})")
                    },
                    modifier = Modifier
                        .padding(paddingValues)
                        .zIndex(2f) // Ensure it is above the persistent main screens
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
                    modifier = Modifier
                        .padding(paddingValues)
                        .zIndex(2f)
                )
            }
            MobileScreen.MovieDetails -> {
                selectedMovie?.let { movie ->
                    MovieDetailsScreen(
                        key = movieDetailsKey,
                        movie = movie,
                        playlistService = playlistService,
                        onBackPressed = {
                            currentScreen = lastMainScreen
                            selectedMovie = null
                        },
                        onMovieSelected = { newMovie ->
                            selectedMovie = newMovie
                            movieDetailsKey++
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
                        onMyListToggled = {
                            myListRefreshKey++
                        },
                        modifier = Modifier
                            .padding(paddingValues)
                            .zIndex(2f)
                    )
                }
            }
            MobileScreen.TvSeriesDetails -> {
                selectedTvSeries?.let { tvSeries ->
                    TvSeriesDetailsScreen(
                        key = tvSeriesDetailsKey,
                        tvSeries = tvSeries,
                        playlistService = playlistService,
                        onBackPressed = {
                            currentScreen = lastMainScreen
                            selectedTvSeries = null
                        },
                        onTvSeriesSelected = { newSeries ->
                            selectedTvSeries = newSeries
                            tvSeriesDetailsKey++
                        },
                        onEpisodeSelected = { episode ->
                            android.util.Log.d("MainActivity", "Playing episode: ${episode.name}")
                            videoPlayerViewModel.loadEpisode(episode)
                            videoPlayerSourceScreen = MobileScreen.TvSeriesDetails
                            currentScreen = MobileScreen.VideoPlayer
                        },
                        onMyListToggled = {
                            myListRefreshKey++
                        },
                        modifier = Modifier
                            .padding(paddingValues)
                            .zIndex(2f)
                    )
                }
            }
            MobileScreen.VideoPlayer -> {
                VideoPlayerScreen(
                    onBackPressed = {
                        videoPlayerViewModel.reset()
                        currentScreen = videoPlayerSourceScreen ?: lastMainScreen
                        videoPlayerSourceScreen = null
                    },
                    viewModel = videoPlayerViewModel,
                    modifier = Modifier
                        .padding(paddingValues)
                        .zIndex(3f) // Highest Z-index for the player
                )
            }
            MobileScreen.M3uPlaylist -> {
                selectedPlaylist?.let { playlist ->
                    M3uPlaylistScreen(
                        playlist = playlist,
                        playlistService = playlistService,
                        onBackPressed = {
                            currentScreen = MobileScreen.MyPlaylists
                            selectedPlaylist = null
                        },
                        onChannelSelected = { channel ->
                            android.util.Log.d("MainActivity", "Playing M3U channel: ${channel.name}")
                            videoPlayerViewModel.loadChannel(channel)
                            videoPlayerSourceScreen = MobileScreen.M3uPlaylist
                            currentScreen = MobileScreen.VideoPlayer
                        },
                        modifier = Modifier
                            .padding(paddingValues)
                            .zIndex(2f)
                    )
                }
            }
            // All main screens handled in the persistent Box above
            MobileScreen.Home, MobileScreen.Downloads, MobileScreen.MyList, MobileScreen.Settings -> {
                // Do nothing, screens are already in the composition layer below
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
    object M3uPlaylist : MobileScreen()
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
