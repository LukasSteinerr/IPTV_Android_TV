package com.example.tv_app.epg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
import com.example.tv_app.epg.ui.EpgColors
import com.example.tv_app.epg.ui.EpgTheme
import com.example.tv_app.model.Channel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpgChannelItem(
    channel: EpgChannel,
    isSelected: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    onChannelClicked: (EpgChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    val isFocused = remember { mutableStateOf(false) }
    val isFocusedValue = isFocused.value
    
    val backgroundColor = EpgColors.channelBackgroundColor(isSelected)
    
    val borderColor = when {
        isSelected -> EpgTheme.Primary
        isFocusedValue -> EpgTheme.Secondary
        else -> EpgTheme.ProgramBorder
    }
    
    val borderWidth = when {
        isSelected || isFocusedValue -> 3.dp
        else -> 1.dp
    }
    
    Box(
        modifier = modifier
            .width(EpgTheme.ChannelWidth)
            .fillMaxHeight()
            .clip(EpgTheme.ChannelShape)
            .background(backgroundColor)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = EpgTheme.ChannelShape
            )
            .clickable {
                onChannelClicked(channel)
            }
            .focusable()
            .onFocusChanged { focusState ->
                isFocused.value = focusState.isFocused
                onFocusChanged(focusState.isFocused)
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Channel logo
            if (!channel.logoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(channel.logoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Channel logo for ${channel.name}",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            // Channel name
            Text(
                text = channel.name,
                style = EpgTheme.ChannelNameStyle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) EpgTheme.OnPrimary else EpgTheme.OnSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview
@Composable
private fun EpgChannelItemPreview() {
    val sampleChannel = EpgChannel(
        id = "1",
        name = "Sample Channel",
        logoUrl = null,
        originalChannel = Channel(
            id = 1,
            name = "Sample Channel",
            streamUrl = "https://example.com/stream",
            logoUrl = null,
            epgId = "sample"
        )
    )
    
    EpgChannelItem(
        channel = sampleChannel,
        isSelected = false,
        onFocusChanged = {},
        onChannelClicked = {}
    )
}
