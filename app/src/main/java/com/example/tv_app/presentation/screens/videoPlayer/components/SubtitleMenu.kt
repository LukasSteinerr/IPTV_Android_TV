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
import com.example.tv_app.ui.theme.DarkBackground
import com.example.tv_app.ui.theme.DarkOnBackground
import com.example.tv_app.ui.theme.DarkOnSurface
import com.example.tv_app.ui.theme.DarkPrimary
import com.example.tv_app.ui.theme.DarkSurface
import com.example.tv_app.ui.theme.DarkBorder

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
        // Single layer - just the menu box, no background overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusable()
        ) {
            // Main content container - clean single layer
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(500.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface)
                    .border(
                        width = 1.dp,
                        color = DarkBorder,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Header
                    Text(
                        text = "Audio & Subtitles",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkOnSurface
                        ),
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                    
                    // Subtitle list
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
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
            .height(56.dp) // Increased height to accommodate all text
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .onFocusChanged { setFocused(it.isFocused) }
            .clickable { onClick() }
            .background(
                color = when {
                    isSelected -> DarkPrimary.copy(alpha = 0.2f)
                    isFocused -> DarkOnSurface.copy(alpha = 0.1f)
                    else -> Color.Transparent
                }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp), // Adjusted padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection indicator - properly sized checkbox
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(24.dp) // Fixed size for proper square checkbox
                .padding(end = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                // Filled checkbox when selected
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(18.dp) // Slightly smaller than container
                        .background(
                            color = DarkPrimary,
                            shape = RoundedCornerShape(4.dp) // More rounded corners
                        )
                )
            } else {
                // Empty checkbox border when not selected
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(18.dp)
                        .border(
                            width = if (isFocused) 2.dp else 1.dp,
                            color = if (isFocused) DarkOnSurface else DarkOnSurface.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            }
        }
        
        // Text content - aligned with checkbox, proper spacing
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = track.label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = DarkOnSurface,
                    fontSize = 16.sp,
                    lineHeight = 18.sp // Reduced line height for better fit
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Only show language if it's not "Off" with proper spacing
            if (track.language != "off") {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.language.uppercase(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = DarkOnSurface.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        lineHeight = 14.sp // Reduced line height for better fit
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
