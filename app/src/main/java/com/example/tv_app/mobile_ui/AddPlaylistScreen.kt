package com.example.tv_app.mobile_ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tv_app.model.Playlist
import com.example.tv_app.model.PlaylistTypeConstants
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.viewmodel.PlaylistViewModel
import com.example.tv_app.viewmodel.PlaylistViewModelFactory
import kotlinx.coroutines.launch
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.OutlinedTextFieldDefaults

@Composable
fun AddPlaylistScreen(
    playlistService: PlaylistService,
    onPlaylistAdded: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    playlistViewModel: PlaylistViewModel = viewModel(factory = PlaylistViewModelFactory(playlistService))
) {
    var playlistType by remember { mutableStateOf(PlaylistTypeConstants.xtream) }
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    // Use mutableStateOf to ensure atomic updates
    val screenState = remember { mutableStateOf<AddPlaylistState>(AddPlaylistState.Form) }

    val isLoading by playlistViewModel.isLoading
    val loadingMessage by playlistViewModel.loadingMessage
    val errorMessage by playlistViewModel.errorMessage

    val coroutineScope = rememberCoroutineScope()

    // Handle state transitions
    LaunchedEffect(screenState.value, isLoading, errorMessage) {
        when (screenState.value) {
            is AddPlaylistState.Succeeding -> {
                // Already succeeded, should have navigated
                screenState.value = AddPlaylistState.Form
            }
            else -> { /* no action */ }
        }
    }

    // Show loading screen when adding playlist, form otherwise
    when (screenState.value) {
        is AddPlaylistState.Loading -> {
            PlaylistLoadingScreen(
                loadingMessage = loadingMessage,
                modifier = modifier
            )
        }
        is AddPlaylistState.Succeeding -> {
            // Keep showing loading until navigation happens
            PlaylistLoadingScreen(
                loadingMessage = "Playlist added successfully!",
                modifier = modifier
            )
        }
        AddPlaylistState.Form -> {
            AddPlaylistForm(
                playlistType = playlistType,
                name = name,
                url = url,
                username = username,
                password = password,
                errorMessage = errorMessage,
                onPlaylistTypeChange = { playlistType = it },
                onNameChange = { name = it },
                onUrlChange = { url = it },
                onUsernameChange = { username = it },
                onPasswordChange = { password = it },
                onAddPlaylist = {
                    coroutineScope.launch {
                        val newPlaylist = Playlist(
                            name = name,
                            url = url,
                            username = if (playlistType == PlaylistTypeConstants.xtream) username else null,
                            password = if (playlistType == PlaylistTypeConstants.xtream) password else null,
                            typeInt = playlistType
                        )
                        screenState.value = AddPlaylistState.Loading
                        playlistViewModel.addPlaylist(newPlaylist)
                    }
                },
                onNavigateUp = onNavigateUp,
                modifier = modifier
            )
        }
    }

    // Watch for loading completion to trigger navigation
    LaunchedEffect(isLoading, errorMessage, screenState.value) {
        val state = screenState.value
        if (state is AddPlaylistState.Loading && !isLoading && errorMessage.isEmpty()) {
            // Loading completed successfully - transition to succeeding and navigate
            screenState.value = AddPlaylistState.Succeeding
            onPlaylistAdded()
        } else if (state is AddPlaylistState.Loading && !isLoading && errorMessage.isNotEmpty()) {
            // Loading completed with error - go back to form
            screenState.value = AddPlaylistState.Form
        }
    }
}

sealed class AddPlaylistState {
    object Form : AddPlaylistState()
    object Loading : AddPlaylistState()
    object Succeeding : AddPlaylistState()
}

@Composable
fun PlaylistLoadingScreen(
    loadingMessage: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0A0A),
                        Color(0xFF121212),
                        Color(0xFF0A0A0A)
                    )
                )
            )
            .then(modifier),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            com.example.tv_app.presentation.components.LoadingIndicator(
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = loadingMessage,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun AddPlaylistForm(
    playlistType: Int,
    name: String,
    url: String,
    username: String,
    password: String,
    errorMessage: String,
    onPlaylistTypeChange: (Int) -> Unit,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onAddPlaylist: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0A0A),
                        Color(0xFF121212),
                        Color(0xFF0A0A0A)
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .then(modifier)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateUp,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Add New Playlist",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.5.sp
                )
            )
        }

        // Playlist Type Tabs
        TabRow(
            selectedTabIndex = if (playlistType == PlaylistTypeConstants.xtream) 0 else 1,
            containerColor = Color.White.copy(alpha = 0.05f),
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[if (playlistType == PlaylistTypeConstants.xtream) 0 else 1]),
                    color = Color.White
                )
            },
            divider = {}
        ) {
            Tab(
                selected = playlistType == PlaylistTypeConstants.xtream,
                onClick = { onPlaylistTypeChange(PlaylistTypeConstants.xtream) },
                text = { Text("Xtream Codes") }
            )
            Tab(
                selected = playlistType == PlaylistTypeConstants.m3u,
                onClick = { onPlaylistTypeChange(PlaylistTypeConstants.m3u) },
                text = { Text("M3U Playlist") }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Input Fields
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ModernTextField(value = name, onValueChange = onNameChange, label = "Playlist Name")
            ModernTextField(value = url, onValueChange = onUrlChange, label = if (playlistType == PlaylistTypeConstants.xtream) "Server URL" else "M3U URL")

            AnimatedVisibility(visible = playlistType == PlaylistTypeConstants.xtream) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ModernTextField(value = username, onValueChange = onUsernameChange, label = "Username")
                    ModernTextField(value = password, onValueChange = onPasswordChange, label = "Password")
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        }

        // Action Button
        Button(
            onClick = onAddPlaylist,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.1f),
                contentColor = Color.White
            )
        ) {
            Text(
                "Add Playlist",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
}

@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color.White,
            focusedBorderColor = Color.White.copy(alpha = 0.5f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedLabelColor = Color.White.copy(alpha = 0.7f),
            unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
            focusedContainerColor = Color.White.copy(alpha = 0.05f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
        ),
        singleLine = true
    )
}