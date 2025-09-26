package com.example.tv_app.view

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun rememberChildPadding(): PaddingValues {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val childPadding = (screenWidth - 180.dp * 5) / 5
    return PaddingValues(horizontal = childPadding / 2)
}