package com.example.tv_app.epg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.tv_app.epg.model.EpgChannel
import com.example.tv_app.epg.model.EpgData
import com.example.tv_app.epg.model.EpgProgram
import com.example.tv_app.epg.ui.components.EpgControls
import com.example.tv_app.epg.ui.components.EpgGrid
import com.example.tv_app.epg.ui.components.EpgProgramDetails
import com.example.tv_app.epg.ui.components.TimeOfDayOption
import com.example.tv_app.model.Channel
import com.example.tv_app.model.Playlist
import com.example.tv_app.model.TvProgram
import com.example.tv_app.repository.PlaylistService
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

sealed class EpgUiState {
    object Loading : EpgUiState()
    data class Success(val epgData: EpgData) : EpgUiState()
    data class Error(val message: String) : EpgUiState()
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpgScreen(
    playlist: Playlist,
    playlistService: PlaylistService,
    onChannelSelected: (Channel) -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var uiState by remember { mutableStateOf<EpgUiState>(EpgUiState.Loading) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTimeOfDay by remember { mutableStateOf(TimeOfDayOption("Morning", 6)) }
    var selectedChannel by remember { mutableStateOf<EpgChannel?>(null) }
    var selectedProgram by remember { mutableStateOf<EpgProgram?>(null) }
    
    val scope = rememberCoroutineScope()
    
    // Load EPG data when date changes
    LaunchedEffect(selectedDate) {
        scope.launch {
            loadEpgData(
                playlist = playlist,
                playlistService = playlistService,
                date = selectedDate,
                onResult = { result ->
                    uiState = result
                    // Auto-select first channel if available
                    if (result is EpgUiState.Success && selectedChannel == null) {
                        selectedChannel = result.epgData.channels.firstOrNull()
                    }
                }
            )
        }
    }
    
    // Jump to current time based on time of day selection
    LaunchedEffect(selectedTimeOfDay) {
        if (uiState is EpgUiState.Success) {
            // Scroll to selected time of day would be implemented here
            // This is a placeholder for the scroll-to-time functionality
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        EpgTheme.Background,
                        Color(0xFF0A0E14)
                    ),
                    radius = 1200f
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Controls
            EpgControls(
                selectedDate = selectedDate,
                selectedTimeOfDay = selectedTimeOfDay,
                onDateSelected = { date ->
                    selectedDate = date
                    selectedChannel = null
                    selectedProgram = null
                },
                onTimeOfDaySelected = { timeOption ->
                    selectedTimeOfDay = timeOption
                },
                onJumpToLive = {
                    val now = LocalDateTime.now()
                    if (selectedDate == LocalDate.now()) {
                        // Jump to current time - implementation would scroll grid
                        selectedTimeOfDay = when (now.hour) {
                            in 6..11 -> TimeOfDayOption("Morning", 6)
                            in 12..18 -> TimeOfDayOption("Afternoon", 12)
                            else -> TimeOfDayOption("Evening", 19)
                        }
                    }
                },
                canJumpToLive = selectedDate == LocalDate.now(),
                modifier = Modifier.fillMaxWidth()
            )
            
            when (val currentState = uiState) {
                is EpgUiState.Loading -> {
                    LoadingContent()
                }
                is EpgUiState.Success -> {
                    Row(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Main EPG Grid
                        EpgGrid(
                            epgData = currentState.epgData,
                            selectedChannel = selectedChannel,
                            selectedProgram = selectedProgram,
                            onChannelSelected = { channel ->
                                selectedChannel = channel
                                selectedProgram = null
                            },
                            onProgramSelected = { program ->
                                selectedProgram = program
                            },
                            onChannelClicked = { channel ->
                                onChannelSelected(channel.originalChannel)
                            },
                            onProgramClicked = { program ->
                                if (program.isCurrentProgram && program.originalProgram != null) {
                                    // Play current program
                                    selectedChannel?.let { channel ->
                                        onChannelSelected(channel.originalChannel)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Program Details Panel
                        EpgProgramDetails(
                            selectedChannel = selectedChannel,
                            selectedProgram = selectedProgram,
                            modifier = Modifier
                                .width(300.dp)
                                .fillMaxSize()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        )
                    }
                }
                is EpgUiState.Error -> {
                    ErrorContent(
                        message = currentState.message,
                        onRetry = {
                            scope.launch {
                                loadEpgData(
                                    playlist = playlist,
                                    playlistService = playlistService,
                                    date = selectedDate,
                                    onResult = { result -> uiState = result }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = EpgTheme.Primary
            )
            Text(
                text = "Loading EPG data...",
                style = EpgTheme.DetailsTitleStyle,
                color = EpgTheme.OnSurface
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Error Loading EPG",
                style = EpgTheme.DetailsTitleStyle,
                color = EpgTheme.OnSurface
            )
            
            Text(
                text = message,
                style = EpgTheme.DetailsDescriptionStyle,
                color = EpgTheme.OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            androidx.tv.material3.Button(
                onClick = onRetry,
                colors = androidx.tv.material3.ButtonDefaults.colors(
                    containerColor = EpgTheme.Primary,
                    contentColor = EpgTheme.OnPrimary
                )
            ) {
                Text("Retry")
            }
        }
    }
}

private suspend fun loadEpgData(
    playlist: Playlist,
    playlistService: PlaylistService,
    date: LocalDate,
    onResult: (EpgUiState) -> Unit
) {
    try {
        onResult(EpgUiState.Loading)
        
        // Load channels
        val channels = playlistService.getLiveTVChannelsForPlaylist(playlist)
        if (channels.isEmpty()) {
            onResult(EpgUiState.Error("No channels found in this playlist"))
            return
        }
        
        // Load programs for each channel
        val programsByChannel = mutableMapOf<String, List<TvProgram>>()
        
        for (channel in channels) {
            try {
                val programs = playlistService.getEpgProgramsForChannel(channel)
                    .filter { program ->
                        program.startTime != null && program.stopTime != null &&
                        program.startTime!!.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() == date
                    }
                programsByChannel[channel.id.toString()] = programs
            } catch (e: Exception) {
                // Continue with empty programs for this channel
                programsByChannel[channel.id.toString()] = emptyList()
            }
        }
        
        // Create EPG data
        val epgData = EpgData.createFromChannelsAndPrograms(
            channels = channels,
            programsByChannel = programsByChannel,
            date = date
        )
        
        onResult(EpgUiState.Success(epgData))
        
    } catch (e: Exception) {
        onResult(EpgUiState.Error("Failed to load EPG data: ${e.message}"))
    }
}

@Preview
@Composable
private fun EpgScreenPreview() {
    // This would need mock data for preview
    LoadingContent()
}
