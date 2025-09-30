package com.example.tv_app.presentation.screens.videoPlayer.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.tv_app.model.Track
import com.example.tv_app.ui.theme.JetStreamBackground
import com.example.tv_app.ui.theme.JetStreamOnBackground
import com.example.tv_app.ui.theme.JetStreamOnSurface
import com.example.tv_app.ui.theme.JetStreamPrimary
import com.example.tv_app.ui.theme.JetStreamSurface

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SubtitleMenu(
    subtitleTracks: List<Track>,
    onSubtitleSelected: (Track) -> Unit,
    onDismiss: () -> Unit,
    isVisible: Boolean,
    currentSelectedTrack: Track? = null
) {
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable { onDismiss() }
                .focusable()
        ) {
            // Main content container - clean single layer
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(500.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(JetStreamSurface)
                    .border(
                        width = 1.dp,
                        color = JetStreamBorder,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Header
                    Text(
                        text = "Audio & Subtitles",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = JetStreamOnSurface
                        ),
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                    
                    // Subtitle list
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Add "Off" option
                        item {
                            SubtitleMenuItem(
                                track = Track(
                                    trackId = "off",
                                    language = "off",
                                    label = "Off"
                                ),
                                onClick = {
                                    onSubtitleSelected(
                                        Track(
                                            trackId = "off",
                                            language = "off",
                                            label = "Off"
                                        )
                                    )
                                    onDismiss()
                                },
                                isSelected = currentSelectedTrack?.trackId == "off" || currentSelectedTrack == null,
                                focusRequester = focusRequester
                            )
                        }
                        
                        // Add subtitle tracks
                        itemsIndexed(subtitleTracks) { index, track ->
                            SubtitleMenuItem(
                                track = track,
                                onClick = {
                                    onSubtitleSelected(track)
                                    onDismiss()
                                },
                                isSelected = currentSelectedTrack?.trackId == track.trackId,
                                focusRequester = if (index == 0 && currentSelectedTrack == null) focusRequester else null
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Request focus when menu becomes visible
    LaunchedEffect(isVisible) {
        if (isVisible) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
fun SubtitleMenuItem(
    track: Track,
    onClick: () -> Unit,
    isSelected: Boolean,
    focusRequester: FocusRequester? = null
) {
    val (isFocused, setFocused) = remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .onFocusChanged { setFocused(it.isFocused) }
            .clickable { onClick() }
            .background(
                color = when {
                    isSelected -> JetStreamPrimary.copy(alpha = 0.2f)
                    isFocused -> JetStreamOnSurface.copy(alpha = 0.1f)
                    else -> Color.Transparent
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection indicator
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(24.dp)
                .padding(end = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(16.dp)
                        .background(
                            color = JetStreamPrimary,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            } else if (isFocused) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(16.dp)
                        .border(
                            width = 2.dp,
                            color = JetStreamOnSurface,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(16.dp)
                        .border(
                            width = 1.dp,
                            color = JetStreamOnSurface.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
        
        // Text content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = JetStreamOnSurface,
                    fontSize = 16.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (track.language != "off") {
                Text(
                    text = track.language.uppercase(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = JetStreamOnSurface.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Add a border color for the container
val JetStreamBorder = Color(0xFF404040)
