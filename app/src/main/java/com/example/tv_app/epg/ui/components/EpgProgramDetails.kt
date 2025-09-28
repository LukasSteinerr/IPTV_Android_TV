package com.example.tv_app.epg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.tv_app.epg.model.EpgChannel
import com.example.tv_app.epg.model.EpgProgram
import com.example.tv_app.epg.ui.EpgTheme
import com.example.tv_app.model.Channel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpgProgramDetails(
    selectedChannel: EpgChannel?,
    selectedProgram: EpgProgram?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(EpgTheme.Surface, EpgTheme.DetailsShape)
            .padding(EpgTheme.ContentPadding)
    ) {
        if (selectedProgram != null && selectedChannel != null) {
            ProgramDetailsContent(
                channel = selectedChannel,
                program = selectedProgram
            )
        } else if (selectedChannel != null) {
            ChannelDetailsContent(channel = selectedChannel)
        } else {
            EmptyDetailsContent()
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProgramDetailsContent(
    channel: EpgChannel,
    program: EpgProgram
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMM d") }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Channel info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            if (!channel.logoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(channel.logoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Channel logo",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Column {
                Text(
                    text = channel.name,
                    style = EpgTheme.ChannelNameStyle,
                    color = EpgTheme.OnSurface
                )
                Text(
                    text = program.startTime.format(dateFormatter),
                    style = EpgTheme.ProgramTimeStyle,
                    color = EpgTheme.OnSurfaceVariant
                )
            }
        }
        
        // Program title
        Text(
            text = program.title,
            style = EpgTheme.DetailsTitleStyle,
            color = EpgTheme.OnSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        // Program time and duration
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "${program.startTime.format(timeFormatter)} - ${program.endTime.format(timeFormatter)}",
                style = EpgTheme.ProgramTimeStyle,
                color = EpgTheme.OnSurfaceVariant
            )
            
            Text(
                text = "${program.durationMinutes} min",
                style = EpgTheme.ProgramTimeStyle,
                color = EpgTheme.OnSurfaceVariant
            )
            
            if (program.isCurrentProgram) {
                Text(
                    text = "LIVE",
                    style = EpgTheme.ProgramTimeStyle,
                    color = EpgTheme.ProgramBackgroundCurrent
                )
            }
        }
        
        // Progress indicator for current program
        if (program.isCurrentProgram) {
            val now = LocalDateTime.now()
            val totalDuration = java.time.Duration.between(program.startTime, program.endTime).toMinutes()
            val elapsed = java.time.Duration.between(program.startTime, now).toMinutes()
            val progress = if (totalDuration > 0) (elapsed.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f) else 0f
            
            Column {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = EpgTheme.Secondary,
                    trackColor = EpgTheme.ProgramBorder
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${elapsed}m elapsed",
                        style = EpgTheme.ProgramTimeStyle,
                        color = EpgTheme.OnSurfaceVariant
                    )
                    Text(
                        text = "${totalDuration - elapsed}m remaining",
                        style = EpgTheme.ProgramTimeStyle,
                        color = EpgTheme.OnSurfaceVariant
                    )
                }
            }
        }
        
        // Program description
        if (program.description.isNotEmpty()) {
            Text(
                text = program.description,
                style = EpgTheme.DetailsDescriptionStyle,
                color = EpgTheme.OnSurfaceVariant,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChannelDetailsContent(
    channel: EpgChannel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Channel info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            if (!channel.logoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(channel.logoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Channel logo",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Text(
                text = channel.name,
                style = EpgTheme.DetailsTitleStyle,
                color = EpgTheme.OnSurface
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EmptyDetailsContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Select a channel or program",
            style = EpgTheme.DetailsTitleStyle,
            color = EpgTheme.OnSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Navigate through the EPG to see program information",
            style = EpgTheme.DetailsDescriptionStyle,
            color = EpgTheme.OnSurfaceVariant
        )
    }
}

@Preview
@Composable
private fun EpgProgramDetailsPreview() {
    val sampleChannel = EpgChannel(
        id = "1",
        name = "Sample Channel",
        logoUrl = null,
        originalChannel = Channel(1, "Sample Channel", "url", null, "epg")
    )
    
    val sampleProgram = EpgProgram(
        id = "1",
        title = "Sample TV Show",
        description = "A sample TV show with a longer description to showcase how the details panel handles longer text content.",
        startTime = LocalDateTime.now(),
        endTime = LocalDateTime.now().plusMinutes(60),
        isCurrentProgram = true,
        originalProgram = null
    )
    
    EpgProgramDetails(
        selectedChannel = sampleChannel,
        selectedProgram = sampleProgram
    )
}
