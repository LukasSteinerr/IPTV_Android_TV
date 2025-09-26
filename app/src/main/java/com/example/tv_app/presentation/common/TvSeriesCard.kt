package com.example.tv_app.presentation.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.StandardCardContainer
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.tv_app.model.TvSeries
import com.example.tv_app.repository.TMDBImageProvider
import com.example.tv_app.ui.theme.JetStreamCardShape
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSeriesCard(
    tvSeries: TvSeries,
    tmdbImageProvider: TMDBImageProvider,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true
) {
    var posterUrl by remember { mutableStateOf<String?>(null) }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(tvSeries.tmdbId) {
        posterUrl = tmdbImageProvider.getPosterUrl(tvSeries.tmdbId, tvSeries.coverUrl)
    }

    // Animate the title alpha based on focus state
    val titleAlpha by animateFloatAsState(
        targetValue = if (isFocused && showTitle) 1f else 0f,
        label = "titleAlpha"
    )

    StandardCardContainer(
        modifier = modifier,
        title = {
            if (showTitle) {
                Text(
                    text = tvSeries.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(titleAlpha)
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
            }
        },
        imageCard = {
            Surface(
                onClick = onClick,
                shape = ClickableSurfaceDefaults.shape(JetStreamCardShape),
                border = ClickableSurfaceDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(
                            width = 3.dp,
                            color = Color(0xFFE3E2E6)
                        ),
                        shape = JetStreamCardShape
                    )
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
                modifier = Modifier.onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                }
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
        }
    )
}
