package com.example.iptvsonic.mobile_ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import coil.compose.rememberAsyncImagePainter
import com.example.iptvsonic.model.Movie
import com.example.iptvsonic.repository.TMDBImageProvider
import kotlinx.coroutines.launch

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FeaturedContent(
    movies: List<Movie>,
    onDetailsTapped: (Movie) -> Unit,
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

    val pagerState = rememberPagerState(pageCount = { movies.size })

    val currentMovie = movies.getOrNull(pagerState.currentPage)
    var currentImageUrl by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentMovie) {
        if (currentMovie != null) {
            currentImageUrl = TMDBImageProvider.getInstance().getBackdropUrl(currentMovie.featuredPosterUrl)
                ?: currentMovie.coverUrl
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

                LaunchedEffect(movies[page].featuredPosterUrl) {
                    coroutineScope.launch {
                        imageUrl = TMDBImageProvider.getInstance().getBackdropUrl(movies[page].featuredPosterUrl) ?: movies[page].coverUrl
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = if (pagerState.currentPage == page) BorderStroke(2.dp, Color.White) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.75f) // Make it longer vertically (taller than wide)
                        .clickable { onDetailsTapped(movies[page]) }
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUrl),
                        contentDescription = "Featured Movie",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            PageIndicator(
                numberOfPages = movies.size,
                selectedPage = pagerState.currentPage
            )
        }
    }
}

@Composable
fun PageIndicator(numberOfPages: Int, selectedPage: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until numberOfPages) {
            val width by animateDpAsState(
                targetValue = if (i == selectedPage) 24.dp else 8.dp,
                label = "Indicator Width"
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (i == selectedPage) Color.White else Color.Gray)
            )
        }
    }
}
