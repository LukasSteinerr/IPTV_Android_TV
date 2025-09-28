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
    // Parameters kept for compatibility but not used in UI
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(EpgTheme.Surface)
            .padding(EpgTheme.ContentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Jump to live button only
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
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

// Removed DateOptionButton, TimeOfDayButton, and generateDateOptions functions
// as they are no longer needed after removing date and time selectors

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
