package com.example.tv_app.epg.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object EpgTheme {
    // Colors
    val Background = Color(0xFF0F1419)
    val Surface = Color(0xFF1A1F2E)
    val Primary = Color(0xFF2196F3)
    val PrimaryVariant = Color(0xFF1976D2)
    val Secondary = Color(0xFF03DAC6)
    val OnSurface = Color(0xFFE1E2E4)
    val OnSurfaceVariant = Color(0xFF9AA0A6)
    val OnPrimary = Color.White
    
    // EPG specific colors
    val ChannelBackground = Color(0xFF252B36)
    val ChannelBackgroundFocused = Color(0xFF2196F3)
    val ProgramBackground = Color(0xFF1E242E)
    val ProgramBackgroundFocused = Color(0xFF2196F3)
    val ProgramBackgroundCurrent = Color(0xFF4CAF50)
    val ProgramBorder = Color(0xFF3A4048)
    val TimelineBackground = Color(0xFF1A1F2E)
    val CurrentTimeIndicator = Color(0xFFFF5722)
    val GapBackground = Color(0xFF151B23)
    
    // Typography
    val ChannelNameStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = OnSurface
    )
    
    val ProgramTitleStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = OnSurface
    )
    
    val ProgramTimeStyle = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        color = OnSurfaceVariant
    )
    
    val TimelineStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = OnSurface
    )
    
    val DetailsTitleStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = OnSurface
    )
    
    val DetailsDescriptionStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = OnSurfaceVariant
    )
    
    // Shapes
    val ChannelShape = RoundedCornerShape(4.dp)
    val ProgramShape = RoundedCornerShape(4.dp)
    val DetailsShape = RoundedCornerShape(8.dp)
    
    // Dimensions
    val ChannelWidth = 160.dp
    val ProgramMinWidth = 120.dp
    val ProgramHeight = 60.dp
    val TimelineHeight = 40.dp
    val PixelsPerMinute = 4.dp
    val GridSpacing = 1.dp
    val ContentPadding = 16.dp
}

object EpgColors {
    @Composable
    fun programBackgroundColor(
        isSelected: Boolean,
        isCurrent: Boolean,
        isGap: Boolean
    ): Color {
        return when {
            isSelected -> EpgTheme.ProgramBackgroundFocused
            isCurrent -> EpgTheme.ProgramBackgroundCurrent
            isGap -> EpgTheme.GapBackground
            else -> EpgTheme.ProgramBackground
        }
    }
    
    @Composable
    fun channelBackgroundColor(isSelected: Boolean): Color {
        return if (isSelected) EpgTheme.ChannelBackgroundFocused else EpgTheme.ChannelBackground
    }
}
