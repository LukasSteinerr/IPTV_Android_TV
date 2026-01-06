package com.example.iptvsonic.mobile_ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import coil.compose.rememberAsyncImagePainter
import com.example.iptvsonic.model.TvSeries
import com.example.iptvsonic.repository.TMDBImageProvider
import kotlinx.coroutines.launch

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FeaturedTvSeriesContent(
    tvSeries: List<TvSeries>,
    onDetailsTapped: (TvSeries) -> Unit,
    modifier: Modifier = Modifier
) {
    // Calculate the total height of the top area: status bar + standard AppBar height (56.dp).
    val statusBarsHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val totalExtension = statusBarsHeight + 56.dp
    
    val layoutModifier = Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)

        // Reduce the reported height by totalExtension to prevent pushing down siblings
        layout(placeable.width, placeable.height - totalExtension.roundToPx()) {
            placeable.placeRelative(0, 0)
        }
    }

    val pagerState = rememberPagerState(pageCount = { tvSeries.size })
    val coroutineScope = rememberCoroutineScope()
    
    val currentTvSeries = tvSeries.getOrNull(pagerState.currentPage)
    var currentImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentTvSeries) {
        if (currentTvSeries != null) {
            currentImageUrl = TMDBImageProvider.getInstance().getBackdropUrl(currentTvSeries.featuredPosterUrl)
                ?: currentTvSeries.coverUrl
        }
    }

    PaletteBackedContent(
        imageUrl = currentImageUrl,
        // Apply the layout modifier first to shrink the perceived height of this composable in parent layout
        // Then pull the PaletteBackedContent up to occupy the space behind the app bar and status bar.
        // This makes the outer box taller, covering the new area, without affecting sibling layout positions.
        modifier = modifier.then(layoutModifier).offset(y = -totalExtension)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            // Push the inner content down to maintain its original visual position relative to the screen content area.
            // We use padding to push down by the total extension amount.
            modifier = Modifier.padding(top = totalExtension)
        ) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 32.dp),
                pageSpacing = 16.dp
            ) { page ->
                var imageUrl by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(tvSeries[page].featuredPosterUrl) {
                    coroutineScope.launch {
                        imageUrl = TMDBImageProvider.getInstance().getBackdropUrl(tvSeries[page].featuredPosterUrl) ?: tvSeries[page].coverUrl
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = if (pagerState.currentPage == page) BorderStroke(2.dp, Color.White) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.75f) // Make it longer vertically (taller than wide)
                        .clickable { onDetailsTapped(tvSeries[page]) }
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUrl),
                        contentDescription = "Featured TV Series",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            PageIndicator(
                numberOfPages = tvSeries.size,
                selectedPage = pagerState.currentPage
            )
        }
    }
}
