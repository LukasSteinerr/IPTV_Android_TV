@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.tv_app.mobile_ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState // Added for ChannelListWithEpg
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity // Added for TimeSlider pixel/dp conversion
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tv_app.model.Channel
import com.example.tv_app.model.Category
import com.example.tv_app.model.Playlist
import com.example.tv_app.model.ContentType
import com.example.tv_app.model.TvProgram
import com.example.tv_app.repository.PlaylistService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor

@Composable
fun LiveTVScreen(
    playlist: Playlist,
    playlistService: PlaylistService,
    onBackPressed: () -> Unit,
    onChannelSelected: (Channel) -> Unit = {},
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onRefresh: () -> Unit = {}
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val liveTvCategories = remember(playlist) {
        playlist.categories.filter { it.contentType == ContentType.liveTV }.toList()
    }
    
    var selectedCategory by remember { mutableStateOf(liveTvCategories.firstOrNull()) }
    var selectedTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var epgData by remember { mutableStateOf<Map<String, List<TvProgram>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(false) }
    var isInteractingWithSlider by remember { mutableStateOf(false) }
    
    // State for positioning of bump-out indicator (removed states calculated locally in TimeSlider)
    var scrollOffset by remember { mutableFloatStateOf(0f) }
    
    val filteredChannels: List<Channel> = remember(playlist, selectedCategory) {
        if (selectedCategory == null) {
            playlist.channels.filter { it.category.target?.isLiveTV == true }.toList() // FIXED: Use .target to access Category properties
        } else {
            selectedCategory?.channels?.toList() ?: emptyList()
        }
    }
    
    LaunchedEffect(filteredChannels) {
        isLoading = true
        val epgMap = mutableMapOf<String, List<TvProgram>>()
        for (channel in filteredChannels) {
            if (!channel.epgId.isNullOrEmpty()) {
                val programs = playlistService.getEpgProgramsForChannel(channel)
                epgMap[channel.epgId!!] = programs
            }
        }
        epgData = epgMap
        isLoading = false
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            CategoryDrawer(
                categories = liveTvCategories,
                selectedCategory = selectedCategory,
                onCategorySelected = { category ->
                    selectedCategory = category
                    scope.launch { drawerState.close() }
                },
                onAllChannelsSelected = {
                    selectedCategory = null
                    scope.launch { drawerState.close() }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(contentPadding)
        ) {
            // Channel List
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 45.dp) // Make space for slider
            ) {
                Text(
                    text = selectedCategory?.name?.uppercase() ?: "ALL CHANNELS",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
                
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    ChannelListWithEpg(
                        channels = filteredChannels,
                        epgData = epgData,
                        currentTime = selectedTime,
                        onChannelSelected = onChannelSelected,
                        onScrollOffsetChanged = { offset ->
                            scrollOffset = offset
                        }
                    )
                }
            }
            
            // Time Slider - positioned from top (appbar) to bottom (before navbar)
            TimeSlider(
                modifier = Modifier.align(Alignment.TopEnd),
                selectedTime = selectedTime,
                currentTime = System.currentTimeMillis(),
                showBumpOut = isInteractingWithSlider,
                onTimeChange = { newTime -> selectedTime = newTime },
                onInteractionStart = { isInteractingWithSlider = true },
                onInteractionEnd = {
                    isInteractingWithSlider = false
                    onRefresh()
                }
            )
        }
    }
}

@Composable
fun ChannelListWithEpg(
    channels: List<Channel>,
    epgData: Map<String, List<TvProgram>>,
    currentTime: Long,
    onChannelSelected: (Channel) -> Unit,
    onScrollOffsetChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        items(channels) { channel ->
            val programs = epgData[channel.epgId] ?: emptyList()
            val nowAndNext = findNowAndNextPrograms(programs, currentTime)
            ChannelListItemWithEpg(
                channel = channel,
                nowProgram = nowAndNext.first,
                nextProgram = nowAndNext.second,
                onClick = { onChannelSelected(channel) }
            )
            Divider(color = Color.Gray.copy(alpha = 0.3f), thickness = 1.dp)
        }
    }
}

@Composable
fun ChannelListItemWithEpg(
    channel: Channel,
    nowProgram: TvProgram?,
    nextProgram: TvProgram?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = channel.logoUrl,
            contentDescription = channel.name,
            modifier = Modifier
                .size(48.dp)
                .padding(end = 12.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = channel.name, color = Color.White, fontWeight = FontWeight.Bold)
            Text(
                text = "Now: ${nowProgram?.title ?: "No information"}",
                color = Color.LightGray,
                fontSize = 14.sp
            )
            Text(
                text = "Next: ${nextProgram?.title ?: "No information"}",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "View Channel",
            tint = Color.Gray
        )
    }
}

@Composable
fun TimeSlider(
    selectedTime: Long,
    currentTime: Long,
    showBumpOut: Boolean,
    onTimeChange: (Long) -> Unit,
    onInteractionStart: () -> Unit,
    onInteractionEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionLastHour = remember { mutableIntStateOf(-1) }

    BoxWithConstraints(modifier = modifier) {
        val actualAvailableHeight = constraints.maxHeight.toFloat()
        // Account for navbar height (64.dp) to ensure slider ends before navbar
        val navbarHeightPx = with(LocalDensity.current) { 64.dp.toPx() }
        val availableSliderHeight = actualAvailableHeight - navbarHeightPx
        val dynamicHourHeight = availableSliderHeight / 24f

        // This Column holds the visible slider bar and refresh button
        // Height is constrained to availableSliderHeight to ensure it ends before navbar
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .height(with(LocalDensity.current) { availableSliderHeight.toDp() })
        ) {
            IconButton(onClick = onInteractionEnd) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh EPG", tint = Color.White)
            }

            Box(
                modifier = Modifier
                    .weight(1f) // Takes remaining height
                    .width(45.dp)
                    .background(Color.Black.copy(alpha = 0.3f))
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                onInteractionStart()
                                val calendar = Calendar.getInstance()
                                calendar.timeInMillis = selectedTime
                                interactionLastHour.intValue = calendar.get(Calendar.HOUR_OF_DAY)
                            },
                            onDragEnd = { onInteractionEnd() },
                            onDragCancel = { onInteractionEnd() },
                            onVerticalDrag = { change, _ ->
                                val hourHeight = size.height / 24f
                                val offset = change.position.y
                                val touchedIndex = floor(offset / hourHeight).toInt().coerceIn(0, 23)
                                val newSelectedHour = touchedIndex

                                if (interactionLastHour.intValue != newSelectedHour) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val calendar = Calendar.getInstance()
                                    calendar.timeInMillis = selectedTime
                                    calendar.set(Calendar.HOUR_OF_DAY, newSelectedHour)
                                    calendar.set(Calendar.MINUTE, 0)
                                    calendar.set(Calendar.SECOND, 0)
                                    onTimeChange(calendar.timeInMillis)
                                    interactionLastHour.intValue = newSelectedHour
                                }
                            }
                        )
                    }
            ) {
                val calendar = Calendar.getInstance().apply { timeInMillis = selectedTime }
                val currentCalendar = Calendar.getInstance().apply { timeInMillis = currentTime }
                val selectedHour = calendar.get(Calendar.HOUR_OF_DAY)
                val currentHour = currentCalendar.get(Calendar.HOUR_OF_DAY)

                Column(modifier = Modifier.fillMaxHeight()) {
                    for (hourValue in 0..23) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(
                                    when {
                                        hourValue == selectedHour -> Color.Blue.copy(alpha = 0.3f)
                                        hourValue == currentHour -> Color.Blue.copy(alpha = 0.3f)
                                        else -> Color.Transparent
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = hourValue.toString().padStart(2, '0'),
                                color = when {
                                    hourValue == currentHour -> Color.Blue
                                    hourValue == selectedHour -> Color.White
                                    else -> Color.White.copy(alpha = 0.7f)
                                },
                                fontWeight = if (hourValue == selectedHour || hourValue == currentHour) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 16.sp,
                            )
                        }
                    }
                }
            }
        }

        // Bump-out Indicator overlay, now a sibling and unconstrained by width
        if (showBumpOut) {
            val calendar = Calendar.getInstance().apply { timeInMillis = selectedTime }
            val selectedHour = calendar.get(Calendar.HOUR_OF_DAY)

            // We need to account for the refresh button's height in our offset calculation.
            // Assuming the IconButton has a default size of 48.dp.
            val refreshButtonHeightPx = with(LocalDensity.current) { 48.dp.toPx() }
            val sliderAreaHeight = availableSliderHeight - refreshButtonHeightPx
            val hourHeightInSlider = sliderAreaHeight / 24f

            val selectedHourTopY = hourHeightInSlider * selectedHour + refreshButtonHeightPx
            val indicatorHeightPx = with(LocalDensity.current) { 36.dp.toPx() }
            val topOffsetPx = selectedHourTopY + (hourHeightInSlider / 2f) - (indicatorHeightPx / 2f)

            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-45).dp - 10.dp, y = with(LocalDensity.current) { topOffsetPx.toDp() })
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(4.dp)),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2))
            ) {
                Row(
                    modifier = Modifier
                        .size(width = 100.dp, height = 36.dp) // User's desired width
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${selectedHour.toString().padStart(2, '0')}:00",
                        color = Color.White,
                        fontWeight = FontWeight.W500,
                        fontSize = 15.sp,
                    )
                    Icon(
                        Icons.Default.MoreHoriz,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

fun findNowAndNextPrograms(programs: List<TvProgram>, currentTime: Long): Pair<TvProgram?, TvProgram?> {
    val now = Date(currentTime)
    val sortedPrograms = programs.sortedBy { it.startTime }
    val nowProgram = sortedPrograms.find { program ->
        val start = program.startTime
        val stop = program.stopTime
        start != null && stop != null && now.after(start) && now.before(stop)
    }
    val nextProgram = sortedPrograms.find { program ->
        val start = program.startTime
        start != null && nowProgram?.stopTime != null && start.after(nowProgram.stopTime)
    }
    return Pair(nowProgram, nextProgram)
}

@Composable
fun CategoryDrawer(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit,
    onAllChannelsSelected: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = Color.Black.copy(alpha = 0.95f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "LIVE TV CATEGORIES",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            NavigationDrawerItem(
                label = { Text("All Channels", color = if (selectedCategory == null) Color.Yellow else Color.White) },
                selected = selectedCategory == null,
                onClick = onAllChannelsSelected,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color.Transparent,
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            )
            Divider(color = Color.Gray)
            LazyColumn {
                items(categories) { category ->
                    NavigationDrawerItem(
                        label = { Text(category.name, color = if (category == selectedCategory) Color.Yellow else Color.White) },
                        selected = category == selectedCategory,
                        onClick = { onCategorySelected(category) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    }
}
