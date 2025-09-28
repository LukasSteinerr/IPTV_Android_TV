package com.example.tv_app.epg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tv_app.epg.model.EpgChannel
import com.example.tv_app.epg.model.EpgData
import com.example.tv_app.epg.model.EpgProgram
import com.example.tv_app.epg.ui.EpgTheme
import com.example.tv_app.model.Channel
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun EpgGrid(
    epgData: EpgData,
    selectedChannel: EpgChannel?,
    selectedProgram: EpgProgram?,
    onChannelSelected: (EpgChannel) -> Unit,
    onProgramSelected: (EpgProgram) -> Unit,
    onChannelClicked: (EpgChannel) -> Unit,
    onProgramClicked: (EpgProgram) -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberLazyListState()
    val verticalScrollState = rememberLazyListState()
    var focusedChannelIndex by remember { mutableStateOf(0) }
    var focusedProgramIndex by remember { mutableStateOf(0) }
    
    val timelineScrollState = rememberLazyListState()
    
    // Sync timeline scroll with horizontal scroll
    LaunchedEffect(horizontalScrollState.firstVisibleItemIndex, horizontalScrollState.firstVisibleItemScrollOffset) {
        if (!timelineScrollState.isScrollInProgress) {
            timelineScrollState.scrollToItem(
                horizontalScrollState.firstVisibleItemIndex,
                horizontalScrollState.firstVisibleItemScrollOffset
            )
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EpgTheme.Background)
    ) {
        // Timeline
        EpgTimeline(
            timeSlots = epgData.timeSlots,
            scrollState = timelineScrollState,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Main grid content
        LazyColumn(
            state = verticalScrollState,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(epgData.channels) { channelIndex, channel ->
                EpgChannelRow(
                    channel = channel,
                    programs = epgData.programsByChannel[channel.id] ?: emptyList(),
                    isChannelSelected = selectedChannel?.id == channel.id,
                    selectedProgram = selectedProgram,
                    horizontalScrollState = horizontalScrollState,
                    onChannelFocused = { 
                        focusedChannelIndex = channelIndex
                        onChannelSelected(channel)
                    },
                    onChannelClicked = onChannelClicked,
                    onProgramFocused = { program, programIndex ->
                        focusedProgramIndex = programIndex
                        onProgramSelected(program)
                    },
                    onProgramClicked = onProgramClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(EpgTheme.ProgramHeight)
                )
            }
        }
    }
}

@Composable
private fun EpgChannelRow(
    channel: EpgChannel,
    programs: List<EpgProgram>,
    isChannelSelected: Boolean,
    selectedProgram: EpgProgram?,
    horizontalScrollState: androidx.compose.foundation.lazy.LazyListState,
    onChannelFocused: () -> Unit,
    onChannelClicked: (EpgChannel) -> Unit,
    onProgramFocused: (EpgProgram, Int) -> Unit,
    onProgramClicked: (EpgProgram) -> Unit,
    modifier: Modifier = Modifier
) {
    var isChannelFocused by remember { mutableStateOf(false) }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Channel column
        EpgChannelItem(
            channel = channel,
            isSelected = isChannelFocused,
            onFocusChanged = { focused ->
                isChannelFocused = focused
                if (focused) {
                    onChannelFocused()
                }
            },
            onChannelClicked = onChannelClicked,
            modifier = Modifier.height(EpgTheme.ProgramHeight)
        )
        
        // Programs row
        LazyRow(
            state = horizontalScrollState,
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = true // Allow horizontal scrolling
        ) {
            itemsIndexed(programs) { programIndex, program ->
                val isSelected = selectedProgram?.id == program.id
                var isProgramFocused by remember { mutableStateOf(false) }
                
                EpgProgramItem(
                    program = program,
                    isSelected = isProgramFocused,
                    onFocusChanged = { focused ->
                        isProgramFocused = focused
                        if (focused) {
                            onProgramFocused(program, programIndex)
                        }
                    },
                    onProgramClicked = onProgramClicked,
                    modifier = Modifier.height(EpgTheme.ProgramHeight)
                )
            }
        }
    }
}

@Preview
@Composable
private fun EpgGridPreview() {
    val sampleChannels = listOf(
        EpgChannel(
            id = "1",
            name = "Channel 1",
            logoUrl = null,
            originalChannel = Channel(1, "Channel 1", "url1", null, "epg1")
        ),
        EpgChannel(
            id = "2", 
            name = "Channel 2",
            logoUrl = null,
            originalChannel = Channel(2, "Channel 2", "url2", null, "epg2")
        )
    )
    
    val now = LocalDateTime.now()
    val samplePrograms = mapOf(
        "1" to listOf(
            EpgProgram(
                id = "p1",
                title = "Program 1",
                description = "Description 1",
                startTime = now,
                endTime = now.plusMinutes(60),
                isCurrentProgram = true,
                originalProgram = null
            )
        ),
        "2" to listOf(
            EpgProgram(
                id = "p2",
                title = "Program 2", 
                description = "Description 2",
                startTime = now,
                endTime = now.plusMinutes(30),
                originalProgram = null
            )
        )
    )
    
    val epgData = EpgData(
        channels = sampleChannels,
        programsByChannel = samplePrograms,
        timeSlots = emptyList(),
        date = LocalDate.now(),
        startTime = now,
        endTime = now.plusHours(6)
    )
    
    EpgGrid(
        epgData = epgData,
        selectedChannel = null,
        selectedProgram = null,
        onChannelSelected = {},
        onProgramSelected = {},
        onChannelClicked = {},
        onProgramClicked = {}
    )
}
