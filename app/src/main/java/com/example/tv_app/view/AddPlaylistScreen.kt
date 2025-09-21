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
            .background(
                androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A1F2E),
                        Color(0xFF0F1419)
                    ),
                    radius = 1200f
                )
            )
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

        AnimatedVisibility(
            visible = true,
            modifier = Modifier.weight(0.8f),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SidePanel(
                step = currentStep,
                title = title,
                description = description
            )
        }

        AnimatedContent(
            targetState = currentStep,
            modifier = Modifier.weight(1.2f),
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
                    focusRequester = initialFocusRequester
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
                    onClearError = { playlistViewModel.clearError() },
                    focusRequester = initialFocusRequester
                )
            }
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
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.4f)
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F1419),
                        Color(0xFF1A1F2E)
                    )
                )
            )
    ) {
        // Subtle accent line
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .background(TvMaterialTheme.colorScheme.primary)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 48.dp, vertical = 64.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // Step progress indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = if (index < step) TvMaterialTheme.colorScheme.primary 
                                       else Color.White.copy(alpha = 0.3f),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    if (index < 2) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(2.dp)
                                .background(
                                    color = if (index < step - 1) TvMaterialTheme.colorScheme.primary 
                                           else Color.White.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
            }

            StepIndicator(currentStep = step, totalSteps = 3)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = title,
                color = Color.White,
                style = TvMaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = TvMaterialTheme.typography.displaySmall.lineHeight * 1.1
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.8f),
                style = TvMaterialTheme.typography.bodyLarge.copy(
                    lineHeight = TvMaterialTheme.typography.bodyLarge.lineHeight * 1.4
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            // Step hints for better UX
            when (step) {
                1 -> Text(
                    text = "💡 Use D-pad to navigate between options",
                    color = TvMaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    style = TvMaterialTheme.typography.bodySmall
                )
                2 -> {
                    // This previously showed a hint about credentials.
                }
                3 -> {
                    // This previously showed a hint about credentials.
                }
                4 -> Text(
                    text = "💡 You can rename your playlist anytime",
                    color = TvMaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    style = TvMaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun Step1_ChoosePlaylistType(
    onTypeSelected: (Int) -> Unit,
    onCancel: () -> Unit,
    onClearDatabase: () -> Unit,
    focusRequester: FocusRequester
) {
    var selectedType by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 64.dp, vertical = 40.dp)
    ) {
        // Content area that takes most of the space
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title for this step
            Text(
                text = "Choose Your Playlist Type",
                color = Color.White,
                style = TvMaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(bottom = 48.dp)
            )

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

            Spacer(modifier = Modifier.height(24.dp))

            PlaylistOptionCard(
                title = "Xtream Codes",
                description = "Server with username & password",
                isSelected = selectedType == PlaylistTypeConstants.xtream,
                onClick = {
                    selectedType = PlaylistTypeConstants.xtream
                    onTypeSelected(PlaylistTypeConstants.xtream)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

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
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
           TvButton("Cancel", onClick = onCancel, isSecondary = true)
           Spacer(modifier = Modifier.width(24.dp))
           TvButton("Clear Database", onClick = onClearDatabase, isSecondary = true)
       }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlaylistOptionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    androidx.tv.material3.Card(
        onClick = onClick,
        modifier = modifier
            .width(400.dp)
            .height(80.dp),
        colors = androidx.tv.material3.CardDefaults.colors(
            containerColor = if (isSelected) TvMaterialTheme.colorScheme.primary 
                           else Color.Transparent,
            contentColor = if (isSelected) Color.Black 
                          else Color.White,
            focusedContainerColor = if (isSelected) TvMaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                   else TvMaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            focusedContentColor = if (isSelected) Color.Black else Color.White
        ),
        border = androidx.tv.material3.CardDefaults.border(
            border = androidx.tv.material3.Border(
                border = BorderStroke(
                    width = 2.dp,
                    color = if (enabled) Color.White.copy(alpha = 0.3f) 
                           else Color.Gray.copy(alpha = 0.2f)
                )
            ),
            focusedBorder = androidx.tv.material3.Border(
                border = BorderStroke(
                    width = 3.dp,
                    color = TvMaterialTheme.colorScheme.primary
                )
            )
        ),
        shape = androidx.tv.material3.CardDefaults.shape(RoundedCornerShape(12.dp))
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
                        style = TvMaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isSelected) Color.Black else Color.White
                    )
                    Text(
                        text = description,
                        style = TvMaterialTheme.typography.bodyMedium,
                        color = if (isSelected) Color.Black.copy(alpha = 0.8f)
                               else Color.White.copy(alpha = 0.7f)
                    )
                }
                if (isSelected) {
                    Text(
                        text = "✓",
                        style = TvMaterialTheme.typography.headlineSmall,
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
    focusRequester: FocusRequester
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 64.dp, vertical = 40.dp)
    ) {
        // Content area that takes most of the space
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (playlistType == PlaylistTypeConstants.xtream) "Enter Server Details" else "Enter Playlist URL",
                color = Color.White,
                style = TvMaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(bottom = 48.dp)
            )

            TvTextField(
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
                Spacer(modifier = Modifier.height(32.dp))
                TvTextField(value = username, onValueChange = onUsernameChange, label = "Username")
                Spacer(modifier = Modifier.height(32.dp))
                TvTextField(value = password, onValueChange = onPasswordChange, label = "Password")
            }
        }

        // Buttons fixed at bottom with proper spacing
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvButton("Back", onClick = onBack, isSecondary = true)
            Spacer(modifier = Modifier.width(24.dp))
            TvButton("Next", onClick = onNext, enabled = url.isNotBlank() && isValidUrl(url, playlistType))
        }
    }
}

@Composable
fun Step2_NamePlaylist(
    playlistName: String,
    onPlaylistNameChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    focusRequester: FocusRequester
) {
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 64.dp, vertical = 40.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Name Your Playlist",
                color = Color.White,
                style = TvMaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(bottom = 48.dp)
            )
            TvTextField(
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
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvButton("Back", onClick = onBack, isSecondary = true)
            Spacer(modifier = Modifier.width(24.dp))
            TvButton("Next", onClick = onNext, enabled = playlistName.isNotBlank())
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
    onClearError: () -> Unit,
    focusRequester: FocusRequester
) {
    LaunchedEffect(Unit) {
        onAddPlaylist()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 64.dp, vertical = 40.dp)
    ) {
        // Content area that takes most of the space
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.height(160.dp), contentAlignment = Alignment.Center) {
                if (isLoading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = TvMaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = loadingMessage,
                            color = Color.White,
                            style = TvMaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                } else if (errorMessage.isNotEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "❌ Error",
                            color = TvMaterialTheme.colorScheme.error,
                            style = TvMaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = errorMessage,
                            color = TvMaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            style = TvMaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "✅ Success!",
                            color = Color(0xFF4CAF50),
                            style = TvMaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "Your playlist has been added successfully",
                            color = Color.White.copy(alpha = 0.8f),
                            style = TvMaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Buttons fixed at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvButton("Back", onClick = onBack, enabled = !isLoading, isSecondary = true)
            Spacer(modifier = Modifier.width(24.dp))
            TvButton("Done", onClick = onDone, enabled = !isLoading && errorMessage.isEmpty())
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
    isSecondary: Boolean = false
) {
    val buttonColors = if (isSecondary) {
        ButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White.copy(alpha = 0.7f),
            focusedContainerColor = Color.White.copy(alpha = 0.1f),
            focusedContentColor = Color.White
        )
    } else {
        ButtonDefaults.colors(
            containerColor = TvMaterialTheme.colorScheme.primary,
            contentColor = Color.Black,
            focusedContainerColor = TvMaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            focusedContentColor = Color.Black
        )
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .width(180.dp)
            .height(50.dp),
        enabled = enabled,
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = buttonColors,
        border = ButtonDefaults.border(
            border = Border(BorderStroke(0.dp, Color.Transparent)),
            focusedBorder = Border(BorderStroke(0.dp, Color.Transparent))
        ),
        scale = ButtonDefaults.scale(focusedScale = 1.05f)
    ) {
        Text(
            text = text,
            style = TvMaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { 
                Text(
                    text = label,
                    style = TvMaterialTheme.typography.bodyLarge
                ) 
            },
            modifier = modifier.width(480.dp),
            shape = RoundedCornerShape(12.dp),
            textStyle = TvMaterialTheme.typography.bodyLarge,
            isError = isError,
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = TvMaterialTheme.colorScheme.primary,
                focusedIndicatorColor = if (isError) TvMaterialTheme.colorScheme.error else TvMaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = if (isError) TvMaterialTheme.colorScheme.error else Color.White.copy(alpha = 0.5f),
                focusedLabelColor = if (isError) TvMaterialTheme.colorScheme.error else TvMaterialTheme.colorScheme.primary,
                unfocusedLabelColor = if (isError) TvMaterialTheme.colorScheme.error else Color.White.copy(alpha = 0.7f),
                focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                errorIndicatorColor = TvMaterialTheme.colorScheme.error,
                errorLabelColor = TvMaterialTheme.colorScheme.error,
                errorTextColor = Color.White
            ),
            singleLine = true
        )
        
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = TvMaterialTheme.colorScheme.error,
                style = TvMaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
fun PreviewAddPlaylistScreen() {
    val context = LocalContext.current
    TvMaterialTheme {
        AddPlaylistScreen(playlistService = PlaylistService(), onPlaylistAdded = {})
    }
}