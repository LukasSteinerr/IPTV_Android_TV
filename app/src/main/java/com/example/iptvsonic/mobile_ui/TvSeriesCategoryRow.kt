package com.example.iptvsonic.mobile_ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.example.iptvsonic.model.Category
import com.example.iptvsonic.model.TvSeries
import com.example.iptvsonic.presentation.common.TvSeriesCard
import com.example.iptvsonic.repository.TMDBImageProvider

@Composable
fun TvSeriesCategoryRow(
    category: Category,
    tvSeries: List<TvSeries>,
    tmdbImageProvider: TMDBImageProvider,
    onTvSeriesSelected: (TvSeries) -> Unit
) {
    // Removed TV-specific focus logic

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = category.name,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(
                start = 16.dp, // Use fixed padding for mobile
                bottom = 16.dp
            )
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp), // Reduced spacing for mobile
            contentPadding = PaddingValues(horizontal = 16.dp), // Use fixed padding for mobile
            modifier = Modifier.fillMaxWidth()
        ) {
            items(tvSeries) { series ->
                TvSeriesCard(
                    tvSeries = series,
                    tmdbImageProvider = tmdbImageProvider,
                    onClick = { onTvSeriesSelected(series) },
                    modifier = Modifier.width(120.dp) // Reduced card size for mobile
                )
            }
        }
    }
}
