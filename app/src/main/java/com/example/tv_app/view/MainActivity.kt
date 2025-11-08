package com.example.tv_app.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.tv.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.tv_app.presentation.utils.DeviceType
import com.example.tv_app.mobile_ui.MobileAppNavigation
import com.example.tv_app.mobile_ui.MobileScreen
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
                    AppNavigation() // This will handle the device check
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
    val context = LocalContext.current
    val isTv = remember { DeviceType.isTv(context) }

    if (isTv) {
        TvAppNavigation()
    } else {
        MobileAppNavigation()
    }
}

@Composable
fun TvAppNavigation() {
    var currentScreen by remember { mutableStateOf<TvScreen>(TvScreen.MyPlaylists) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var selectedMovie by remember { mutableStateOf<Movie?>(null) }
    var selectedTvSeries by remember { mutableStateOf<TvSeries?>(null) }
    var movieDetailsKey by remember { mutableStateOf(0) } // Key to force recomposition
    var tvSeriesDetailsKey by remember { mutableStateOf(0) } // Key to force recomposition
    var selectedTab by remember { mutableStateOf(0) } // Centralized tab state
    val playlistService = remember { PlaylistService() }
    
    // Create a shared ViewModel for the video player
    val videoPlayerViewModel = remember { VideoPlayerViewModel() }

    when (currentScreen) {
        TvScreen.MyPlaylists -> {
            MyPlaylistsScreen(
                playlistService = playlistService,
                onNavigateToAddPlaylist = {
                    currentScreen = TvScreen.AddPlaylist
                },
                onPlaylistSelected = { playlist ->
                    selectedPlaylist = playlist
                    currentScreen = TvScreen.MoviePage
                    Log.d("MainActivity", "Selected playlist: ${playlist.name}")
                }
            )
        }
        TvScreen.AddPlaylist -> {
            AddPlaylistScreen(
                playlistService = playlistService,
                onPlaylistAdded = {
                    currentScreen = TvScreen.MyPlaylists
                }
            )
        }
        TvScreen.MoviePage -> {
            selectedPlaylist?.let { playlist ->
                MoviePageScreen(
                    playlist = playlist,
                    playlistService = playlistService,
                    selectedTab = selectedTab,
                    onTabSelected = { newTab ->
                        selectedTab = newTab
                        // Handle tab navigation here
                        when (newTab) {
                            0 -> { /* Movies - current screen */ }
                            1 -> currentScreen = TvScreen.ShowsPage
                            2 -> currentScreen = TvScreen.LiveTVPage
                            3 -> currentScreen = TvScreen.FavoritesPage
                        }
                    },
                    onBackPressed = {
                        currentScreen = TvScreen.MyPlaylists
                    },
                    onMovieSelected = { movie ->
                        selectedMovie = movie
                        currentScreen = TvScreen.MovieDetails
                        Log.d("MainActivity", "Selected movie: ${movie.name}")
                    },
                    onNavigateToSearch = {
                        currentScreen = TvScreen.SearchPage
                    }
                )
            }
        }
        is TvScreen.ShowsPage -> {
            selectedPlaylist?.let { playlist ->
                ShowsScreen(
                    playlist = playlist,
                    playlistService = playlistService,
                    selectedTab = selectedTab,
                    onTabSelected = { newTab ->
                        selectedTab = newTab
                        // Handle tab navigation here
                        when (newTab) {
                            0 -> currentScreen = TvScreen.MoviePage
                            1 -> { /* Shows - current screen */ }
                            2 -> currentScreen = TvScreen.LiveTVPage
                            3 -> currentScreen = TvScreen.FavoritesPage
                        }
                    },
                    onBackPressed = {
                        currentScreen = TvScreen.MoviePage
                    },
                    onShowSelected = { show ->
                        selectedTvSeries = show
                        currentScreen = TvScreen.TvSeriesDetails
                        Log.d("MainActivity", "Selected show: ${show.name}")
                    },
                    onNavigateToSearch = {
                        currentScreen = TvScreen.SearchPage
                    }
                )
            }
        }
        is TvScreen.LiveTVPage -> {
            selectedPlaylist?.let { playlist ->
                LiveTVScreen(
                    playlist = playlist,
                    playlistService = playlistService,
                    selectedTab = selectedTab,
                    onTabSelected = { newTab ->
                        selectedTab = newTab
                        // Handle tab navigation here
                        when (newTab) {
                            0 -> currentScreen = TvScreen.MoviePage
                            1 -> currentScreen = TvScreen.ShowsPage
                            2 -> { /* Live TV - current screen */ }
                            3 -> currentScreen = TvScreen.FavoritesPage
                        }
                    },
                    onBackPressed = {
                        currentScreen = TvScreen.MoviePage
                    },
                    onChannelSelected = { channel ->
                        Log.d("MainActivity", "Selected channel: ${channel.name}")
                        // TODO: Navigate to channel player
                    }
                )
            }
        }
        is TvScreen.FavoritesPage -> {
            selectedPlaylist?.let { playlist ->
                FavoritesScreen(
                    playlist = playlist,
                    selectedTab = selectedTab,
                    onTabSelected = { newTab ->
                        selectedTab = newTab
                        // Handle tab navigation here
                        when (newTab) {
                            0 -> currentScreen = TvScreen.MoviePage
                            1 -> currentScreen = TvScreen.ShowsPage
                            2 -> currentScreen = TvScreen.LiveTVPage
                            3 -> { /* Favorites - current screen */ }
                        }
                    },
                    onBackPressed = {
                        currentScreen = TvScreen.MoviePage
                    }
                )
            }
        }
        is TvScreen.SearchPage -> {
            selectedPlaylist?.let { playlist ->
                SearchScreen(
                    playlist = playlist,
                    selectedTab = selectedTab,
                    onTabSelected = { newTab ->
                        selectedTab = newTab
                        // Handle tab navigation here
                        when (newTab) {
                            0 -> currentScreen = TvScreen.MoviePage
                            1 -> currentScreen = TvScreen.ShowsPage
                            2 -> currentScreen = TvScreen.LiveTVPage
                            3 -> currentScreen = TvScreen.FavoritesPage
                        }
                    },
                    onBackPressed = {
                        currentScreen = TvScreen.MoviePage
                    }
                )
            }
        }
        is TvScreen.MovieDetails -> {
            selectedMovie?.let { movie ->
                MovieDetailsScreen(
                    key = movieDetailsKey, // Force recomposition when key changes
                    movie = movie,
                    playlistService = playlistService,
                    onBackPressed = {
                        currentScreen = TvScreen.MoviePage
                    },
                    onMovieSelected = { newMovie ->
                        selectedMovie = newMovie
                        movieDetailsKey++ // Increment key to force screen refresh
                    },
                    onPlayMovie = { movie ->
                        Log.d("MainActivity", "Playing movie: ${movie.name}")
                        videoPlayerViewModel.loadMovie(movie)
                        currentScreen = TvScreen.VideoPlayer
                    }
                )
            }
        }
        is TvScreen.TvSeriesDetails -> {
            selectedTvSeries?.let { tvSeries ->
                TvSeriesDetailsScreen(
                    key = tvSeriesDetailsKey, // Force recomposition when key changes
                    tvSeries = tvSeries,
                    playlistService = playlistService,
                    onBackPressed = {
                        currentScreen = TvScreen.ShowsPage
                    },
                    onTvSeriesSelected = { newSeries ->
                        selectedTvSeries = newSeries
                        tvSeriesDetailsKey++ // Increment key to force screen refresh
                    },
                    onEpisodeSelected = { episode ->
                        Log.d("MainActivity", "Playing episode: ${episode.name}")
                        videoPlayerViewModel.loadEpisode(episode)
                        currentScreen = TvScreen.VideoPlayer
                    }
                )
            }
        }
        is TvScreen.VideoPlayer -> {
            VideoPlayerScreen(
                onBackPressed = {
                    // Reset the video player state when navigating away
                    videoPlayerViewModel.reset()
                    // Check if we were playing a movie or episode to navigate back to the correct screen
                    // For now, we'll check if we have a selected TV series to determine the back navigation
                    currentScreen = if (selectedTvSeries != null) TvScreen.TvSeriesDetails else TvScreen.MovieDetails
                },
                viewModel = videoPlayerViewModel
            )
        }
    }
}

sealed class TvScreen {
    object MyPlaylists : TvScreen()
    object AddPlaylist : TvScreen()
    object MoviePage : TvScreen()
    object ShowsPage : TvScreen()
    object LiveTVPage : TvScreen()
    object FavoritesPage : TvScreen()
    object SearchPage : TvScreen()
    object MovieDetails : TvScreen()
    object TvSeriesDetails : TvScreen()
    object VideoPlayer : TvScreen()
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