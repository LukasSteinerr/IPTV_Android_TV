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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.tv_app.epg.ui.EpgTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class DateOption(
    val date: LocalDate,
    val displayText: String,
    val isToday: Boolean = false
)

data class TimeOfDayOption(
    val label: String,
    val hour: Int
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpgControls(
    selectedDate: LocalDate,
    selectedTimeOfDay: TimeOfDayOption,
    onDateSelected: (LocalDate) -> Unit,
    onTimeOfDaySelected: (TimeOfDayOption) -> Unit,
    onJumpToLive: () -> Unit,
    canJumpToLive: Boolean,
    modifier: Modifier = Modifier
) {
    val dateOptions = remember {
        generateDateOptions()
    }
    
    val timeOfDayOptions = remember {
        listOf(
            TimeOfDayOption("Morning", 6),
            TimeOfDayOption("Afternoon", 12),
            TimeOfDayOption("Evening", 19)
        )
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(EpgTheme.Surface)
            .padding(EpgTheme.ContentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Date selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Date:",
                style = EpgTheme.ChannelNameStyle,
                color = EpgTheme.OnSurface
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dateOptions) { dateOption ->
                    DateOptionButton(
                        dateOption = dateOption,
                        isSelected = dateOption.date == selectedDate,
                        onDateSelected = onDateSelected
                    )
                }
            }
        }
        
        // Time of day selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Time:",
                style = EpgTheme.ChannelNameStyle,
                color = EpgTheme.OnSurface
            )
            
            timeOfDayOptions.forEach { timeOption ->
                TimeOfDayButton(
                    timeOption = timeOption,
                    isSelected = timeOption.hour == selectedTimeOfDay.hour,
                    onTimeSelected = onTimeOfDaySelected
                )
                
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Jump to live button
            if (canJumpToLive) {
                Button(
                    onClick = onJumpToLive,
                    colors = ButtonDefaults.colors(
                        containerColor = EpgTheme.Primary,
                        contentColor = EpgTheme.OnPrimary
                    )
                ) {
                    Text("Jump to Live")
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DateOptionButton(
    dateOption: DateOption,
    isSelected: Boolean,
    onDateSelected: (LocalDate) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val backgroundColor = when {
        isFocused -> EpgTheme.PrimaryVariant
        isSelected -> EpgTheme.Primary
        else -> EpgTheme.ChannelBackground
    }
    
    val textColor = when {
        isFocused || isSelected -> EpgTheme.OnPrimary
        else -> EpgTheme.OnSurface
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = EpgTheme.OnPrimary,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onDateSelected(dateOption.date) }
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = dateOption.displayText,
            style = EpgTheme.ProgramTitleStyle,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TimeOfDayButton(
    timeOption: TimeOfDayOption,
    isSelected: Boolean,
    onTimeSelected: (TimeOfDayOption) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val backgroundColor = when {
        isFocused -> EpgTheme.PrimaryVariant
        isSelected -> EpgTheme.Primary
        else -> EpgTheme.ChannelBackground
    }
    
    val textColor = when {
        isFocused || isSelected -> EpgTheme.OnPrimary
        else -> EpgTheme.OnSurface
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = EpgTheme.OnPrimary,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onTimeSelected(timeOption) }
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = timeOption.label,
            style = EpgTheme.ProgramTitleStyle,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

private fun generateDateOptions(): List<DateOption> {
    val today = LocalDate.now()
    val dateOptions = mutableListOf<DateOption>()
    
    // Add past days
    for (i in 7 downTo 1) {
        val date = today.minusDays(i.toLong())
        val formatter = if (i == 1) {
            DateTimeFormatter.ofPattern("'Yesterday'")
        } else {
            DateTimeFormatter.ofPattern("EEE, MMM d")
        }
        dateOptions.add(
            DateOption(
                date = date,
                displayText = date.format(formatter)
            )
        )
    }
    
    // Add today
    dateOptions.add(
        DateOption(
            date = today,
            displayText = "Today",
            isToday = true
        )
    )
    
    // Add future days
    for (i in 1..7) {
        val date = today.plusDays(i.toLong())
        val formatter = if (i == 1) {
            DateTimeFormatter.ofPattern("'Tomorrow'")
        } else {
            DateTimeFormatter.ofPattern("EEE, MMM d")
        }
        dateOptions.add(
            DateOption(
                date = date,
                displayText = date.format(formatter)
            )
        )
    }
    
    return dateOptions
}

@Preview
@Composable
private fun EpgControlsPreview() {
    EpgControls(
        selectedDate = LocalDate.now(),
        selectedTimeOfDay = TimeOfDayOption("Morning", 6),
        onDateSelected = {},
        onTimeOfDaySelected = {},
        onJumpToLive = {},
        canJumpToLive = true
    )
}
