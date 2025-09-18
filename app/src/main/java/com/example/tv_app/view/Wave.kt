package com.example.tv_app.view

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WaveWidget(
    modifier: Modifier = Modifier,
    yOffset: Float,
    color: Color
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val wavePoints = remember { mutableStateListOf<Offset>() }

    val animationController = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animationController.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    val waveSpeed = animationController.value * 1080
    val fullSphere = animationController.value * Math.PI * 2
    val normalizer = cos(fullSphere).toFloat()
    val waveWidth = (Math.PI / 540).toFloat() // Decreased frequency
    val waveHeight = 20.0f

    wavePoints.clear()
    for (i in 0..size.width) {
        val calc = sin((waveSpeed - i) * waveWidth)
        wavePoints.add(
            Offset(
                i.toFloat(),
                calc * waveHeight * normalizer + yOffset
            )
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                size = it
            }
    ) {
        if (wavePoints.isNotEmpty()) {
            val path = Path().apply {
                moveTo(wavePoints.first().x, wavePoints.first().y)
                for (point in wavePoints) {
                    lineTo(point.x, point.y)
                }
                lineTo(size.width.toFloat(), size.height.toFloat())
                lineTo(0f, size.height.toFloat())
                close()
            }
            drawPath(
                path = path,
                color = color
            )
        }
    }
}