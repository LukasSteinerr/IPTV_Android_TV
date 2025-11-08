package com.example.tv_app.mobile_ui

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import com.example.tv_app.model.Playlist
import com.example.tv_app.model.PlaylistTypeConstants
import com.example.tv_app.repository.PlaylistService
import com.example.tv_app.viewmodel.PlaylistViewModel
import com.example.tv_app.viewmodel.PlaylistViewModelFactory

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AddPlaylistScreen(
    playlistService: PlaylistService,
    onPlaylistAdded: () -> Unit,
    modifier: Modifier = Modifier,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A1F2E),
                        Color(0xFF0F1419)
                    ),
                    radius = 1200f
                )
            )
            .padding(horizontal = 24.dp, vertical = 16.dp) // Mobile padding
    ) {
        val title = when (currentStep) {
            1 -> "Playlist Type"
            2 -> "Name Your Playlist"
            3 -> if (playlistType == PlaylistTypeConstants.xtream) "Xtream Codes Login" else "M3U Details"
            else -> "Processing..."
        }
        val description = when (currentStep) {
            1 -> "Select the type of playlist you want to add."
            2 -> "Give your playlist a memorable name."
            3 -> if (playlistType == PlaylistTypeConstants.xtream) "Enter your server address, username, and password." else "Enter the URL for your M3U playlist."
            else -> "Do not turn off your device during processing."
        }

        // Mobile Header/Indicator
        if (currentStep < 4) {
            MobileHeader(currentStep = currentStep, title = title, description = description)
        }

        AnimatedContent(
            targetState = currentStep,
            modifier = Modifier.fillMaxWidth().weight(1f),
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
                    onClearDatabase = { playlistViewModel.clearDatabase() },
                    focusRequester = initialFocusRequester // Keep focus requester for now, will remove later
                )
                2 -> Step2_NamePlaylist(
                    playlistName = name,
                    onPlaylistNameChange = { name = it },
                    onNext = { currentStep = 3 },
                    onBack = { currentStep = 1 },
                    focusRequester = initialFocusRequester
                )
                3 -> Step3_EnterDetails(
                    playlistType = playlistType,
                    url = url,
                    onUrlChange = { url = it },
                    username = username,
                    onUsernameChange = { username = it },
                    password = password,
                    onPasswordChange = { password = it },
                    onNext = { currentStep = 4 },
                    onBack = { currentStep = 2 },
                    focusRequester = initialFocusRequester
                )
                4 -> Step4_Processing(
                    isLoading = isLoading,
                    loadingMessage = loadingMessage,
                    errorMessage = errorMessage,
                    onAddPlaylist = {
                        val newPlaylist = Playlist(
                            name = name,
                            url = url,
                            username = if (playlistType == PlaylistTypeConstants.xtream) username else null,
                            password = if (playlistType == PlaylistTypeConstants.xtream) password else null,
                            typeInt = playlistType
                        )
                        playlistViewModel.addPlaylist(newPlaylist)
                    },
                    onDone = onPlaylistAdded,
                    onBack = { currentStep = 3 },
                    focusRequester = initialFocusRequester
                )
            }
        }
    }
}

@Composable
fun MobileHeader(currentStep: Int, title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step progress indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (index < currentStep) MaterialTheme.colorScheme.primary
                                   else Color.White.copy(alpha = 0.3f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                if (index < 2) {
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(2.dp)
                            .background(
                                color = if (index < currentStep - 1) MaterialTheme.colorScheme.primary
                                       else Color.White.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }

        Text(
            text = "Step $currentStep of 3",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = description,
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun Step1_ChoosePlaylistType(
    onTypeSelected: (Int) -> Unit,
    onCancel: () -> Unit,
    onClearDatabase: () -> Unit,
    focusRequester: FocusRequester // Keep for now, will remove later
) {
    var selectedType by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp)
    ) {
        // Content area that takes most of the space
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Primary options with descriptions
            PlaylistOptionCard(
                title = "M3U Playlist",
                description = "Standard M3U file format",
                isSelected = selectedType == PlaylistTypeConstants.m3u,
                onClick = {
                    selectedType = PlaylistTypeConstants.m3u
                    onTypeSelected(PlaylistTypeConstants.m3u)
                },
                modifier = Modifier.focusRequester(focusRequester)
            )

            Spacer(modifier = Modifier.height(16.dp))

            PlaylistOptionCard(
                title = "Xtream Codes",
                description = "Server with username & password",
                isSelected = selectedType == PlaylistTypeConstants.xtream,
                onClick = {
                    selectedType = PlaylistTypeConstants.xtream
                    onTypeSelected(PlaylistTypeConstants.xtream)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            PlaylistOptionCard(
                title = "Stalker Portal",
                description = "Coming soon",
                isSelected = false,
                onClick = { /* Handle Stalker Portal */ },
                enabled = false
            )
        }

        // Buttons fixed at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
           MobileButton("Cancel", onClick = onCancel, isSecondary = true, modifier = Modifier.weight(1f).height(48.dp))
           Spacer(modifier = Modifier.width(16.dp))
           MobileButton("Clear Database", onClick = onClearDatabase, isSecondary = true, modifier = Modifier.weight(1f).height(48.dp))
       }
    }
}

@Composable
fun PlaylistOptionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(0.8f) // Use fillMaxWidth for mobile
            .height(80.dp),
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                           else Color.Black.copy(alpha = 0.2f),
            contentColor = if (isSelected) Color.Black
                          else Color.White,
            disabledContainerColor = Color.Gray.copy(alpha = 0.1f),
            disabledContentColor = Color.Gray.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            width = 2.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
                   else if (enabled) Color.White.copy(alpha = 0.3f)
                   else Color.Gray.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isSelected) Color.Black else Color.White
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) Color.Black.copy(alpha = 0.8f)
                               else Color.White.copy(alpha = 0.7f)
                    )
                }
                if (isSelected) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun Step3_EnterDetails(
    playlistType: Int,
    url: String,
    onUrlChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    focusRequester: FocusRequester // Keep for now
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp)
    ) {
        // Content area that takes most of the space
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MobileTextField(
                value = url,
                onValueChange = onUrlChange,
                label = if (playlistType == PlaylistTypeConstants.xtream) "Server Address" else "Playlist URL",
                modifier = Modifier.focusRequester(focusRequester),
                isError = url.isNotBlank() && !isValidUrl(url, playlistType),
                errorMessage = if (url.isNotBlank() && !isValidUrl(url, playlistType)) {
                    if (playlistType == PlaylistTypeConstants.xtream) "Invalid server address format" else "Invalid URL format"
                } else null
            )

            if (playlistType == PlaylistTypeConstants.xtream) {
                Spacer(modifier = Modifier.height(16.dp))
                MobileTextField(value = username, onValueChange = onUsernameChange, label = "Username")
                Spacer(modifier = Modifier.height(16.dp))
                MobileTextField(value = password, onValueChange = onPasswordChange, label = "Password")
            }
        }

        // Buttons fixed at bottom with proper spacing
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MobileButton("Back", onClick = onBack, isSecondary = true, modifier = Modifier.weight(1f).height(48.dp))
            Spacer(modifier = Modifier.width(16.dp))
            MobileButton("Next", onClick = onNext, enabled = url.isNotBlank() && isValidUrl(url, playlistType), modifier = Modifier.weight(1f).height(48.dp))
        }
    }
}

@Composable
fun Step2_NamePlaylist(
    playlistName: String,
    onPlaylistNameChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    focusRequester: FocusRequester // Keep for now
) {
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MobileTextField(
                value = playlistName,
                onValueChange = onPlaylistNameChange,
                label = "Playlist Name",
                modifier = Modifier.focusRequester(focusRequester),
                isError = playlistName.isBlank(),
                errorMessage = if (playlistName.isBlank()) "Playlist name cannot be empty" else null
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MobileButton("Back", onClick = onBack, isSecondary = true, modifier = Modifier.weight(1f).height(48.dp))
            Spacer(modifier = Modifier.width(16.dp))
            MobileButton("Next", onClick = onNext, enabled = playlistName.isNotBlank(), modifier = Modifier.weight(1f).height(48.dp))
        }
    }
}

@Composable
fun Step4_Processing(
    isLoading: Boolean,
    loadingMessage: String,
    errorMessage: String,
    onAddPlaylist: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
    focusRequester: FocusRequester // Keep for now
) {
    LaunchedEffect(Unit) {
        onAddPlaylist()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Assuming ProcessingWave is defined elsewhere or will be adapted/removed
        // For now, commenting out TV-specific wave components
        /*
        ProcessingWave(
            yOffset = 500f,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        ProcessingWave(
            yOffset = 520f,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        */

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = loadingMessage,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
            } else if (errorMessage.isNotEmpty()) {
                Text(
                    text = "❌",
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(32.dp))
                MobileButton("Back", onClick = onBack, modifier = Modifier.width(200.dp).height(48.dp))
            } else {
                Text(
                    text = "✅",
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Success!",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(32.dp))
                MobileButton("Done", onClick = onDone, modifier = Modifier.width(200.dp).height(48.dp).focusRequester(focusRequester))

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }
        }
    }
}

@Composable
fun MobileButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSecondary: Boolean = false
) {
    val buttonColors = if (isSecondary) {
        ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White.copy(alpha = 0.7f),
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.White.copy(alpha = 0.3f)
        )
    } else {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
        )
    }

    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = buttonColors,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

// URL validation function
private fun isValidUrl(url: String, playlistType: Int): Boolean {
    if (url.isBlank()) return false
    
    return try {
        val uri = java.net.URI(url)
        when (playlistType) {
            PlaylistTypeConstants.m3u -> {
                // For M3U, accept http/https URLs ending with .m3u or .m3u8
                (uri.scheme == "http" || uri.scheme == "https") &&
                uri.host != null &&
                (url.endsWith(".m3u", ignoreCase = true) || url.endsWith(".m3u8", ignoreCase = true) || 
                 url.contains("m3u", ignoreCase = true))
            }
            PlaylistTypeConstants.xtream -> {
                // For Xtream, accept http/https URLs with valid host
                (uri.scheme == "http" || uri.scheme == "https") && uri.host != null
            }
            else -> false
        }
    } catch (e: Exception) {
        false
    }
}

@Composable
fun MobileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            modifier = modifier.fillMaxWidth(0.8f),
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            isError = isError,
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedIndicatorColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = if (isError) MaterialTheme.colorScheme.error else Color.White.copy(alpha = 0.5f),
                focusedLabelColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = if (isError) MaterialTheme.colorScheme.error else Color.White.copy(alpha = 0.7f),
                focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                errorIndicatorColor = MaterialTheme.colorScheme.error,
                errorLabelColor = MaterialTheme.colorScheme.error,
                errorTextColor = Color.White
            ),
            singleLine = true
        )
        
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .width(480.dp) // Keep width for error message alignment
                    .padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAddPlaylistScreen() {
    MaterialTheme {
        AddPlaylistScreen(playlistService = PlaylistService(), onPlaylistAdded = {})
    }
}