package com.example.tv_app.epg.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.tv_app.epg.model.EpgTimeSlot
import com.example.tv_app.epg.ui.EpgTheme
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpgTimeline(
    timeSlots: List<EpgTimeSlot>,
    scrollState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(EpgTheme.TimelineHeight)
            .background(EpgTheme.TimelineBackground)
    ) {
        LazyRow(
            state = scrollState,
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = false // Controlled by main grid scroll
        ) {
            // Channel column spacer
            item {
                Box(
                    modifier = Modifier
                        .width(EpgTheme.ChannelWidth)
                        .fillMaxHeight()
                )
            }
            
            // Time slots
            items(timeSlots) { timeSlot ->
                EpgTimeSlotItem(
                    timeSlot = timeSlot,
                    modifier = Modifier
                        .width(120.dp) // 30 minutes at 4dp per minute
                        .fillMaxHeight()
                )
            }
        }
        
        // Current time indicator
        CurrentTimeIndicator(
            currentTime = LocalDateTime.now(),
            startTime = timeSlots.firstOrNull()?.time ?: LocalDateTime.now(),
            scrollOffset = scrollState.firstVisibleItemScrollOffset,
            channelWidth = EpgTheme.ChannelWidth,
            modifier = Modifier.fillMaxHeight()
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpgTimeSlotItem(
    timeSlot: EpgTimeSlot,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = timeSlot.displayText,
            style = EpgTheme.TimelineStyle,
            textAlign = TextAlign.Center,
            color = EpgTheme.OnSurface
        )
    }
}

@Composable
private fun CurrentTimeIndicator(
    currentTime: LocalDateTime,
    startTime: LocalDateTime,
    scrollOffset: Int,
    channelWidth: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val minutesFromStart = ChronoUnit.MINUTES.between(startTime, currentTime)
    val pixelOffset = (minutesFromStart.toFloat() * 4f).dp // 4dp per minute
    val adjustedOffset = pixelOffset + channelWidth - scrollOffset.toFloat().dp
    
    // Only show if current time is visible in the viewport
    if (adjustedOffset.value > channelWidth.value && adjustedOffset.value < 1000) { // Rough viewport width check
        Canvas(
            modifier = modifier
                .fillMaxHeight()
                .width(2.dp)
                .padding(start = adjustedOffset)
        ) {
            drawLine(
                color = EpgTheme.CurrentTimeIndicator,
                start = Offset(0f, 0f),
                end = Offset(0f, size.height),
                strokeWidth = 4.dp.toPx()
            )
        }
    }
}

@Preview
@Composable
private fun EpgTimelinePreview() {
    val now = LocalDateTime.now()
    val timeSlots = EpgTimeSlot.generateTimeSlots(
        startTime = now.truncatedTo(ChronoUnit.HOURS),
        endTime = now.truncatedTo(ChronoUnit.HOURS).plusHours(6)
    )
    
    EpgTimeline(
        timeSlots = timeSlots,
        scrollState = rememberLazyListState()
    )
}
