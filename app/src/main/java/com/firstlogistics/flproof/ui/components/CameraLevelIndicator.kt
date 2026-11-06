package com.firstlogistics.flproof.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.firstlogistics.flproof.ui.theme.GreenAccent
import kotlin.math.abs

@Composable
fun CameraLevelIndicator(
    pitch: Float,
    roll: Float,
    isLevel: Boolean,
    modifier: Modifier = Modifier
) {
    val levelColor by animateColorAsState(
        targetValue = if (isLevel) GreenAccent else Color.White.copy(alpha = 0.5f),
        label = "levelColor"
    )

    Box(
        modifier = modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(80.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val lineLength = 28.dp.toPx()

            // Artificial horizon line tilted by roll angle
            val clampedRoll = roll.coerceIn(-45f, 45f)
            val rad = Math.toRadians(clampedRoll.toDouble())
            val dx = (lineLength * kotlin.math.cos(rad)).toFloat()
            val dy = (lineLength * kotlin.math.sin(rad)).toFloat()

            // Left reticle
            drawLine(
                color = levelColor,
                start = Offset(center.x - dx, center.y - dy),
                end = Offset(center.x - (8.dp.toPx() * kotlin.math.cos(rad)).toFloat(), center.y - (8.dp.toPx() * kotlin.math.sin(rad)).toFloat()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Right reticle
            drawLine(
                color = levelColor,
                start = Offset(center.x + (8.dp.toPx() * kotlin.math.cos(rad)).toFloat(), center.y + (8.dp.toPx() * kotlin.math.sin(rad)).toFloat()),
                end = Offset(center.x + dx, center.y + dy),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Center target dot
            drawCircle(
                color = levelColor,
                radius = 3.dp.toPx(),
                center = center
            )
        }
    }
}

