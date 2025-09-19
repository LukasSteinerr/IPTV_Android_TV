package com.example.tv_app.view

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.tv_app.model.Playlist
import com.example.tv_app.model.PlaylistTypeConstants
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.viewmodel.PlaylistViewModel
import com.example.tv_app.viewmodel.PlaylistViewModelFactory
import androidx.tv.material3.MaterialTheme as TvMaterialTheme

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AddPlaylistScreen(
    playlistService: PlaylistService,
    onPlaylistAdded: () -> Unit,
    playlistViewModel: PlaylistViewModel = viewModel(factory = PlaylistViewModelFactory(playlistService))
) {
    var currentStep by remember { mutableStateOf(1) }
    var playlistType by remember { mutableStateOf(PlaylistTypeConstants.xtream) }
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isLoading by playlistViewModel.isLoading
    val loadingMessage by playlistViewModel.loadingMessage
    val errorMessage by playlistViewModel.errorMessage

    val initialFocusRequester = remember { FocusRequester() }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)) // Darker background
    ) {
        val title = when (currentStep) {
            1 -> "Playlist Type"
            2 -> if (playlistType == PlaylistTypeConstants.xtream) "Xtream Codes Login" else "M3U Details"
            else -> "Processing..."
        }
        val description = when (currentStep) {
            1 -> "Select the type of playlist you want to add."
            2 -> if (playlistType == PlaylistTypeConstants.xtream) "Enter your server address, username, and password." else "Enter the URL for your M3U playlist."
            else -> "We are checking your playlist details. This may take a moment."
        }

        SidePanel(
            step = currentStep,
            title = title,
            description = description
        )

        AnimatedContent(
            targetState = currentStep,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith
                            slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                } else {
                    slideInHorizontally(initialOffsetX = { -it }) + fadeIn() togetherWith
                            slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                }
            }
        ) { step ->
            when (step) {
                1 -> Step1_ChoosePlaylistType(
                    onTypeSelected = { type ->
                        playlistType = type
                        currentStep = 2
                    },
                    onCancel = onPlaylistAdded,
                    focusRequester = initialFocusRequester
                )
                2 -> Step2_EnterDetails(
                    playlistType = playlistType,
                    url = url,
                    onUrlChange = { url = it },
                    username = username,
                    onUsernameChange = { username = it },
                    password = password,
                    onPasswordChange = { password = it },
                    onNext = { currentStep = 3 },
                    onBack = { currentStep = 1 },
                    focusRequester = initialFocusRequester
                )
                3 -> Step3_Processing(
                    playlistName = name,
                    onPlaylistNameChange = { name = it },
                    isLoading = isLoading,
                    loadingMessage = loadingMessage,
                    errorMessage = errorMessage,
                    onAddPlaylist = {
                        val newPlaylist = Playlist(
                            name = name.ifBlank { "My Playlist" },
                            url = url,
                            username = if (playlistType == PlaylistTypeConstants.xtream) username else null,
                            password = if (playlistType == PlaylistTypeConstants.xtream) password else null,
                            typeInt = playlistType
                        )
                        playlistViewModel.addPlaylist(newPlaylist)
                    },
                    onDone = onPlaylistAdded,
                    onBack = { currentStep = 2 },
                    onCancelOperation = { playlistViewModel.cancelOperation() },
                    onClearError = { playlistViewModel.clearError() },
                    focusRequester = initialFocusRequester
                )
            }
        }

        LaunchedEffect(currentStep) {
            initialFocusRequester.requestFocus()
        }
    }
}

@Composable
fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Text(
        text = "Step $currentStep of $totalSteps",
        color = Color.White.copy(alpha = 0.7f),
        style = TvMaterialTheme.typography.labelMedium
    )
}

@Composable
fun SidePanel(
    step: Int,
    title: String,
    description: String
) {
    Box(modifier = Modifier
        .fillMaxHeight()
        .fillMaxWidth(0.4f)
        .background(Color(0xFF1E1E1E))
    ) {
        WaveWidget(yOffset = 300f, color = Color.DarkGray.copy(alpha = 0.3f))
        WaveWidget(yOffset = 350f, color = TvMaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            verticalArrangement = Arrangement.Center
        ) {
            StepIndicator(currentStep = step, totalSteps = 3)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                color = Color.White,
                style = TvMaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.8f),
                style = TvMaterialTheme.typography.bodyLarge
            )
        }
    }
}


@Composable
fun Step1_ChoosePlaylistType(
    onTypeSelected: (Int) -> Unit,
    onCancel: () -> Unit,
    focusRequester: FocusRequester
) {
    var selectedType by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 64.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TvButton(
            text = "M3U Playlist",
            onClick = {
                selectedType = PlaylistTypeConstants.m3u
                onTypeSelected(PlaylistTypeConstants.m3u)
            },
            modifier = Modifier.focusRequester(focusRequester),
            isSelected = selectedType == PlaylistTypeConstants.m3u
        )
        Spacer(modifier = Modifier.height(24.dp))
        TvButton(
            text = "Xtream Codes",
            onClick = {
                selectedType = PlaylistTypeConstants.xtream
                onTypeSelected(PlaylistTypeConstants.xtream)
            },
            isSelected = selectedType == PlaylistTypeConstants.xtream
        )
        Spacer(modifier = Modifier.height(24.dp))
        TvButton(
            text = "Stalker Portal",
            onClick = { /* Handle Stalker Portal */ },
            enabled = false,
            isSelected = false
        )
        Spacer(modifier = Modifier.height(48.dp))
        TvButton("Cancel", onClick = onCancel, isSecondary = true)
    }
}

@Composable
fun Step2_EnterDetails(
    playlistType: Int,
    url: String,
    onUrlChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    focusRequester: FocusRequester
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 64.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TvTextField(value = url, onValueChange = onUrlChange, label = "Server Address (URL)", modifier = Modifier.focusRequester(focusRequester))
        if (playlistType == PlaylistTypeConstants.xtream) {
            Spacer(modifier = Modifier.height(24.dp))
            TvTextField(value = username, onValueChange = onUsernameChange, label = "Username")
            Spacer(modifier = Modifier.height(24.dp))
            TvTextField(value = password, onValueChange = onPasswordChange, label = "Password")
        }
        Spacer(modifier = Modifier.height(48.dp))
        Row {
            TvButton("Back", onClick = onBack, isSecondary = true)
            Spacer(modifier = Modifier.width(24.dp))
            TvButton("Next", onClick = onNext)
        }
    }
}

@Composable
fun Step3_Processing(
    playlistName: String,
    onPlaylistNameChange: (String) -> Unit,
    isLoading: Boolean,
    loadingMessage: String,
    errorMessage: String,
    onAddPlaylist: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
    onCancelOperation: () -> Unit,
    onClearError: () -> Unit,
    focusRequester: FocusRequester
) {
    LaunchedEffect(Unit) {
        onAddPlaylist()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 64.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TvTextField(
            value = playlistName,
            onValueChange = onPlaylistNameChange,
            label = "Playlist Name (Optional)",
            modifier = Modifier.focusRequester(focusRequester)
        )
        Spacer(modifier = Modifier.height(32.dp))

        Box(modifier = Modifier.height(120.dp), contentAlignment = Alignment.Center) {
            if (isLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = TvMaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(loadingMessage, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    TvButton("Cancel", onClick = onCancelOperation, isSecondary = true)
                }
            } else if (errorMessage.isNotEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: $errorMessage", color = TvMaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    TvButton("Dismiss", onClick = onClearError)
                }
            } else {
                Text("Processing Complete!", color = Color.Green, style = TvMaterialTheme.typography.titleLarge)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Row {
            TvButton("Back", onClick = onBack, enabled = !isLoading, isSecondary = true)
            Spacer(modifier = Modifier.width(24.dp))
            TvButton("Done", onClick = onDone, enabled = !isLoading)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSecondary: Boolean = false,
    isSelected: Boolean = false
) {
    val buttonColors = when {
        isSelected -> ButtonDefaults.colors(
            containerColor = Color.White,
            contentColor = Color.Black,
            focusedContainerColor = Color.White.copy(alpha = 0.8f),
            focusedContentColor = Color.Black
        )
        isSecondary -> ButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = TvMaterialTheme.colorScheme.onSurface,
            focusedContainerColor = TvMaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            focusedContentColor = TvMaterialTheme.colorScheme.onSurface
        )
        else -> ButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = TvMaterialTheme.colorScheme.onSurface,
            focusedContainerColor = TvMaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            focusedContentColor = TvMaterialTheme.colorScheme.onSurface
        )
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .width(280.dp)
            .height(56.dp),
        enabled = enabled,
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
        colors = buttonColors,
        border = if (isSecondary) ButtonDefaults.border(
            border = Border(
                border = BorderStroke(
                    width = 1.dp,
                    color = TvMaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            ),
            focusedBorder = Border(
                border = BorderStroke(
                    width = 1.dp,
                    color = TvMaterialTheme.colorScheme.onSurface
                )
            )
        ) else ButtonDefaults.border()
    ) {
        Text(text = text, style = TvMaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.width(400.dp),
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = TvMaterialTheme.colorScheme.primary,
            focusedIndicatorColor = TvMaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = Color.Gray,
            focusedLabelColor = TvMaterialTheme.colorScheme.primary,
            unfocusedLabelColor = Color.Gray
        ),
        singleLine = true
    )
}

@Preview(device = "id:tv_1080p")
@Composable
fun PreviewAddPlaylistScreen() {
    val context = LocalContext.current
    TvMaterialTheme {
        AddPlaylistScreen(playlistService = PlaylistService(context), onPlaylistAdded = {})
    }
}