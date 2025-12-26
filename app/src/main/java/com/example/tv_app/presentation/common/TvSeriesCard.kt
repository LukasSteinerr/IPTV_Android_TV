package com.example.tv_app.presentation.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import com.example.tv_app.model.TvSeries
import com.example.tv_app.repository.TMDBImageProvider
import com.example.tv_app.ui.theme.JetStreamCardShape
import androidx.compose.ui.graphics.Color

@Composable
fun TvSeriesCard(
    tvSeries: TvSeries,
    tmdbImageProvider: TMDBImageProvider,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true
) {
    var posterUrl by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(tvSeries.tmdbId) {
        posterUrl = tmdbImageProvider.getPosterUrl(tvSeries.tmdbId, tvSeries.coverUrl)
    }

    Column(modifier = modifier.clickable(onClick = onClick)) {
        Card(
            shape = JetStreamCardShape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                model = posterUrl ?: tvSeries.coverUrl,
                contentDescription = tvSeries.name,
                modifier = Modifier
                    .aspectRatio(10.5f / 16f)
                    .clip(JetStreamCardShape),
                contentScale = ContentScale.Crop
            )
        }

        if (showTitle) {
            Text(
                text = tvSeries.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White
            )
        }
    }
}
