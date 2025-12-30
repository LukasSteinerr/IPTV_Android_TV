package com.example.tv_app.mobile_ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.geometry.toRect

val FixedAmbientColor = Color(0xFFFFA574)

sealed class BottomBarTab(val screen: MobileScreen, val title: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    object Home : BottomBarTab(MobileScreen.Home, "Home", Icons.Outlined.Home, Icons.Filled.Home)
    object Downloads : BottomBarTab(MobileScreen.Downloads, "Downloads", Icons.Outlined.Download, Icons.Filled.Download)
    object MyList : BottomBarTab(MobileScreen.MyList, "My List", Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite)
    object Settings : BottomBarTab(MobileScreen.Settings, "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
}

@Composable
fun GlassmorphicBottomNavigationBar(
    currentScreen: MobileScreen,
    onScreenSelected: (MobileScreen) -> Unit,
    hazeState: HazeState
) {
    val tabs = listOf(
        BottomBarTab.Home,
        BottomBarTab.Downloads,
        BottomBarTab.MyList,
        BottomBarTab.Settings,
    )
    val selectedTabIndex = remember(currentScreen) {
        tabs.indexOfFirst { it.screen == currentScreen }.coerceAtLeast(0)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black) // Solid black background
            .navigationBarsPadding() // Apply navigation bar padding
            .height(64.dp)
    ) {
        val animatedSelectedTabIndex by animateFloatAsState(
            targetValue = selectedTabIndex.toFloat(),
            label = "animatedSelectedTabIndex",
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioLowBouncy,
            )
        )

        val ambientColor = FixedAmbientColor

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RectangleShape)
                .blur(50.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        ) {
            val tabWidth = size.width / tabs.size
            drawCircle(
                color = ambientColor.copy(alpha = .6f),
                radius = size.height / 2,
                center = Offset(
                    (tabWidth * animatedSelectedTabIndex) + tabWidth / 2,
                    size.height / 2
                )
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RectangleShape)
        ) {
            val path = Path().apply {
                // Change to rectangular shape for the gleam base path
                addRect(size.toRect())
            }
            val length = PathMeasure().apply { setPath(path, false) }.length

            val tabWidth = size.width / tabs.size
            drawPath(
                path,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        ambientColor.copy(alpha = 0f),
                        ambientColor.copy(alpha = 1f),
                        ambientColor.copy(alpha = 1f),
                        ambientColor.copy(alpha = 0f),
                    ),
                    startX = tabWidth * animatedSelectedTabIndex,
                    endX = tabWidth * (animatedSelectedTabIndex + 1),
                ),
                style = Stroke(
                    width = 6f,
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(length / 2, length)
                    )
                )
            )
        }

        BottomBarTabs(
            tabs = tabs,
            selectedTab = selectedTabIndex,
            onTabSelected = { tab ->
                onScreenSelected(tab.screen)
            }
        )
    }
}

@Composable
fun BottomBarTabs(
    tabs: List<BottomBarTab>,
    selectedTab: Int,
    onTabSelected: (BottomBarTab) -> Unit,
) {
    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.copy(
            fontSize = 12.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        ),
        LocalContentColor provides Color.White
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            for (tab in tabs) {
                val isSelected = selectedTab == tabs.indexOf(tab)
                val alpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else .35f,
                    label = "alpha"
                )
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else .98f,
                    visibilityThreshold = .000001f,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessLow,
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                    ),
                    label = "scale"
                )
                Column(
                    modifier = Modifier
                        .scale(scale)
                        .alpha(alpha)
                        .fillMaxHeight()
                        .weight(1f)
                        .clickable { onTabSelected(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.icon,
                        contentDescription = "tab ${tab.title}"
                    )
                    Text(text = tab.title)
                }
            }
        }
    }
}