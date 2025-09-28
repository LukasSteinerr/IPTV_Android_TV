package com.example.tv_app.epg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.tv_app.epg.model.EpgProgram
import com.example.tv_app.epg.ui.EpgColors
import com.example.tv_app.epg.ui.EpgTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpgProgramItem(
    program: EpgProgram,
    isSelected: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    onProgramClicked: (EpgProgram) -> Unit,
    modifier: Modifier = Modifier
) {
    val width = remember(program.durationMinutes) {
        maxOf(program.durationMinutes.toFloat() * 4f, 120f).dp // 4dp per minute, min 120dp
    }
    
    val isFocused = remember { mutableStateOf(false) }
    val isFocusedValue = isFocused.value
    
    val backgroundColor = EpgColors.programBackgroundColor(
        isSelected = isSelected,
        isCurrent = program.isCurrentProgram,
        isGap = !program.isClickable
    )
    
    val borderColor = when {
        isSelected -> EpgTheme.Primary
        isFocusedValue -> EpgTheme.Secondary
        else -> EpgTheme.ProgramBorder
    }
    
    val borderWidth = when {
        isSelected || isFocusedValue -> 3.dp
        else -> 1.dp
    }
    
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    
    Box(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .clip(EpgTheme.ProgramShape)
            .background(backgroundColor)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = EpgTheme.ProgramShape
            )
            .clickable(enabled = program.isClickable) {
                onProgramClicked(program)
            }
            .focusable(enabled = program.isClickable)
            .onFocusChanged { focusState ->
                isFocused.value = focusState.isFocused
                onFocusChanged(focusState.isFocused)
            }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .fillMaxHeight()
        ) {
            // Program title
            Text(
                text = program.title,
                style = EpgTheme.ProgramTitleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) EpgTheme.OnPrimary else EpgTheme.OnSurface
            )
            
            // Program time
            if (program.isClickable) {
                Text(
                    text = "${program.startTime.format(timeFormatter)} - ${program.endTime.format(timeFormatter)}",
                    style = EpgTheme.ProgramTimeStyle,
                    maxLines = 1,
                    color = if (isSelected) EpgTheme.OnPrimary else EpgTheme.OnSurfaceVariant
                )
            }
            
            // Progress indicator for current program
            if (program.isCurrentProgram && program.isClickable) {
                val now = LocalDateTime.now()
                val totalDuration = java.time.Duration.between(program.startTime, program.endTime).toMinutes()
                val elapsed = java.time.Duration.between(program.startTime, now).toMinutes()
                val progress = if (totalDuration > 0) (elapsed.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f) else 0f
                
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .padding(top = 4.dp),
                    color = if (isSelected) EpgTheme.OnPrimary else EpgTheme.Secondary,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

@Preview
@Composable
private fun EpgProgramItemPreview() {
    val sampleProgram = EpgProgram(
        id = "1",
        title = "Sample TV Show",
        description = "A sample TV show for preview",
        startTime = LocalDateTime.now(),
        endTime = LocalDateTime.now().plusMinutes(60),
        isCurrentProgram = true,
        originalProgram = null
    )
    
    EpgProgramItem(
        program = sampleProgram,
        isSelected = false,
        onFocusChanged = {},
        onProgramClicked = {}
    )
}
