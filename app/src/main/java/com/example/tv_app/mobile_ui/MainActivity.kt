package com.example.tv_app.mobile_ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.tv.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var selectedMovie by remember { mutableStateOf<Movie?>(null) }
    var selectedTvSeries by remember { mutableStateOf<TvSeries?>(null) }
    var movieDetailsKey by remember { mutableStateOf(0) } // Key to force recomposition
    var tvSeriesDetailsKey by remember { mutableStateOf(0) } // Key to force recomposition
    var selectedTab by remember { mutableStateOf(0) } // Centralized tab state
    val playlistService = remember { PlaylistService() }
    
    // Create a shared ViewModel for the video player
    val videoPlayerViewModel = remember { VideoPlayerViewModel() }

    val mainNavScreens = listOf(
        MobileScreen.MoviePage,
        MobileScreen.ShowsPage,
        MobileScreen.LiveTVPage,
        MobileScreen.FavoritesPage,
    )

    val showBottomBar = when (currentScreen) {
        MobileScreen.MoviePage, MobileScreen.ShowsPage, MobileScreen.LiveTVPage, MobileScreen.FavoritesPage, MobileScreen.SearchPage -> true
        else -> false
    }

    val currentNavScreen = when (currentScreen) {
        is MobileScreen.MoviePage -> MobileScreen.MoviePage
        is MobileScreen.ShowsPage -> MobileScreen.ShowsPage
        is MobileScreen.LiveTVPage -> MobileScreen.LiveTVPage
        is MobileScreen.FavoritesPage -> MobileScreen.FavoritesPage
        is MobileScreen.SearchPage -> MobileScreen.MoviePage // Search is not a main tab, default to MoviePage for selection logic
        else -> MobileScreen.MoviePage // Default for screens without a tab
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    mainNavScreens.forEach { screen ->
                        val isSelected = currentNavScreen == screen
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = when (screen) {
                                        MobileScreen.MoviePage -> Icons.Filled.Movie
                                        MobileScreen.ShowsPage -> Icons.Filled.Tv
                                        MobileScreen.LiveTVPage -> Icons.Filled.Home
                                        MobileScreen.FavoritesPage -> Icons.Filled.Favorite
                                        else -> Icons.Filled.Home
                                    },
                                    contentDescription = screen.javaClass.simpleName
                                )
                            },
                            label = { androidx.compose.material3.Text(screen.javaClass.simpleName.replace("Page", "")) },
                            selected = isSelected,
                            onClick = {
                                currentScreen = screen
                                selectedTab = mainNavScreens.indexOf(screen)
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        when (currentScreen) {
            MobileScreen.MyPlaylists -> {
                MyPlaylistsScreen(
                    playlistService = playlistService,
                    onNavigateToAddPlaylist = {
                        currentScreen = MobileScreen.AddPlaylist
                    },
                    onPlaylistSelected = { playlist ->
                        selectedPlaylist = playlist
                        currentScreen = MobileScreen.MoviePage
                        Log.d("MainActivity", "Selected playlist: ${playlist.name}")
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            MobileScreen.AddPlaylist -> {
                AddPlaylistScreen(
                    playlistService = playlistService,
                    onPlaylistAdded = {
                        currentScreen = MobileScreen.MyPlaylists
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            MobileScreen.MoviePage -> {
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
                                1 -> currentScreen = MobileScreen.ShowsPage
                                2 -> currentScreen = MobileScreen.LiveTVPage
                                3 -> currentScreen = MobileScreen.FavoritesPage
                            }
                        },
                        onBackPressed = {
                            currentScreen = MobileScreen.MyPlaylists
                        },
                        onMovieSelected = { movie ->
                            selectedMovie = movie
                            currentScreen = MobileScreen.MovieDetails
                            Log.d("MainActivity", "Selected movie: ${movie.name}")
                        },
                        onNavigateToSearch = {
                            currentScreen = MobileScreen.SearchPage
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
            is MobileScreen.ShowsPage -> {
                selectedPlaylist?.let { playlist ->
                    ShowsScreen(
                        playlist = playlist,
                        playlistService = playlistService,
                        selectedTab = selectedTab,
                        onTabSelected = { newTab ->
                            selectedTab = newTab
                            // Handle tab navigation here
                            when (newTab) {
                                0 -> currentScreen = MobileScreen.MoviePage
                                1 -> { /* Shows - current screen */ }
                                2 -> currentScreen = MobileScreen.LiveTVPage
                                3 -> currentScreen = MobileScreen.FavoritesPage
                            }
                        },
                        onBackPressed = {
                            currentScreen = MobileScreen.MoviePage
                        },
                        onShowSelected = { show ->
                            selectedTvSeries = show
                            currentScreen = MobileScreen.TvSeriesDetails
                            Log.d("MainActivity", "Selected show: ${show.name}")
                        },
                        onNavigateToSearch = {
                            currentScreen = MobileScreen.SearchPage
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
            is MobileScreen.LiveTVPage -> {
                selectedPlaylist?.let { playlist ->
                    LiveTVScreen(
                        playlist = playlist,
                        playlistService = playlistService,
                        selectedTab = selectedTab,
                        onTabSelected = { newTab ->
                            selectedTab = newTab
                            // Handle tab navigation here
                            when (newTab) {
                                0 -> currentScreen = MobileScreen.MoviePage
                                1 -> currentScreen = MobileScreen.ShowsPage
                                2 -> { /* Live TV - current screen */ }
                                3 -> currentScreen = MobileScreen.FavoritesPage
                            }
                        },
                        onBackPressed = {
                            currentScreen = MobileScreen.MoviePage
                        },
                        onChannelSelected = { channel ->
                            Log.d("MainActivity", "Selected channel: ${channel.name}")
                            // TODO: Navigate to channel player
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
            is MobileScreen.FavoritesPage -> {
                selectedPlaylist?.let { playlist ->
                    FavoritesScreen(
                        playlist = playlist,
                        selectedTab = selectedTab,
                        onTabSelected = { newTab ->
                            selectedTab = newTab
                            // Handle tab navigation here
                            when (newTab) {
                                0 -> currentScreen = MobileScreen.MoviePage
                                1 -> currentScreen = MobileScreen.ShowsPage
                                2 -> currentScreen = MobileScreen.LiveTVPage
                                3 -> { /* Favorites - current screen */ }
                            }
                        },
                        onBackPressed = {
                            currentScreen = MobileScreen.MoviePage
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
            is MobileScreen.SearchPage -> {
                selectedPlaylist?.let { playlist ->
                    SearchScreen(
                        playlist = playlist,
                        selectedTab = selectedTab,
                        onTabSelected = { newTab ->
                            selectedTab = newTab
                            // Handle tab navigation here
                            when (newTab) {
                                0 -> currentScreen = MobileScreen.MoviePage
                                1 -> currentScreen = MobileScreen.ShowsPage
                                2 -> currentScreen = MobileScreen.LiveTVPage
                                3 -> currentScreen = MobileScreen.FavoritesPage
                            }
                        },
                        onBackPressed = {
                            currentScreen = MobileScreen.MoviePage
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
            is MobileScreen.MovieDetails -> {
                selectedMovie?.let { movie ->
                    MovieDetailsScreen(
                        key = movieDetailsKey, // Force recomposition when key changes
                        movie = movie,
                        playlistService = playlistService,
                        onBackPressed = {
                            currentScreen = MobileScreen.MoviePage
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
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
            is MobileScreen.TvSeriesDetails -> {
                selectedTvSeries?.let { tvSeries ->
                    TvSeriesDetailsScreen(
                        key = tvSeriesDetailsKey, // Force recomposition when key changes
                        tvSeries = tvSeries,
                        playlistService = playlistService,
                        onBackPressed = {
                            currentScreen = MobileScreen.ShowsPage
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
            is MobileScreen.VideoPlayer -> {
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
        }
    }
}

sealed class MobileScreen {
    object MyPlaylists : MobileScreen()
    object AddPlaylist : MobileScreen()
    object MoviePage : MobileScreen()
    object ShowsPage : MobileScreen()
    object LiveTVPage : MobileScreen()
    object FavoritesPage : MobileScreen()
    object SearchPage : MobileScreen()
    object MovieDetails : MobileScreen()
    object TvSeriesDetails : MobileScreen()
    object VideoPlayer : MobileScreen()
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