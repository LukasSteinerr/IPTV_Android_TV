package com.example.iptvsonic.view

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
fun ProcessingWave(
    modifier: Modifier = Modifier,
    yOffset: Float,
    color: Color
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val infiniteTransition = rememberInfiniteTransition(label = "infinite transition")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "animation progress"
    )

    val calculatedWavePoints by remember(size, animationProgress) {
        derivedStateOf {
            if (size.width > 0) {
                val waveSpeed = animationProgress * size.width
                val fullSphere = animationProgress * Math.PI * 2
                val normalizer = cos(fullSphere).toFloat()
                val waveWidth = (Math.PI / (size.width / 2)).toFloat() // Adjusted for a wider wave
                val waveHeight = 40.0f

                val points = mutableListOf<Offset>()
                for (i in 0..size.width) {
                    val calc = sin((waveSpeed + i) * waveWidth)
                    points.add(
                        Offset(
                            i.toFloat(),
                            calc * waveHeight * normalizer + yOffset
                        )
                    )
                }
                points
            } else {
                emptyList()
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                size = it
            }
    ) {
        if (calculatedWavePoints.isNotEmpty()) {
            val path = Path().apply {
                moveTo(calculatedWavePoints.first().x, calculatedWavePoints.first().y)
                for (point in calculatedWavePoints) {
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