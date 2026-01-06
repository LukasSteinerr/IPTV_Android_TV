package com.example.iptvsonic.view

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.Text
import com.example.iptvsonic.model.Category
import com.example.iptvsonic.model.TvSeries
import com.example.iptvsonic.presentation.common.TvSeriesCard
import com.example.iptvsonic.repository.TMDBImageProvider

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TvSeriesCategoryRow(
    category: Category,
    tvSeries: List<TvSeries>,
    tmdbImageProvider: TMDBImageProvider,
    onTvSeriesSelected: (TvSeries) -> Unit
) {
    val (lazyRow, firstItem) = remember { FocusRequester.createRefs() }

    Column(
        modifier = Modifier.focusGroup()
    ) {
        Text(
            text = category.name,
            color = Color.White,
            style = TvMaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(
                start = rememberChildPadding().start,
                bottom = 16.dp
            )
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(
                start = rememberChildPadding().start,
                end = rememberChildPadding().end,
            ),
            modifier = Modifier
                .focusRequester(lazyRow)
                .focusRestorer {
                    firstItem
                }
        ) {
            itemsIndexed(tvSeries) { index, series ->
                val itemModifier = if (index == 0) {
                    Modifier
                        .focusRequester(firstItem)
                        .width(150.dp)
                } else {
                    Modifier.width(150.dp)
                }
                
                TvSeriesCard(
                    tvSeries = series,
                    tmdbImageProvider = tmdbImageProvider,
                    onClick = { onTvSeriesSelected(series) },
                    modifier = itemModifier
                )
            }
        }
    }
}
