package com.firstlogistics.flproof.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firstlogistics.flproof.location.GpsData
import com.firstlogistics.flproof.ui.theme.*

@Composable
fun GpsAccuracyBadge(
    gpsData: GpsData,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusLabel) = when {
        !gpsData.isAvailable -> Pair(StatusError, "GPS Nonaktif / Offline")
        gpsData.accuracy <= 10f -> Pair(GreenAccent, "Akurasi Tinggi (±${gpsData.accuracy.toInt()}m)")
        gpsData.accuracy <= 25f -> Pair(StatusWarning, "Akurasi Sedang (±${gpsData.accuracy.toInt()}m)")
        else -> Pair(StatusError, "Akurasi Rendah (±${gpsData.accuracy.toInt()}m)")
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(NavySurface.copy(alpha = 0.85f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(statusColor)
        )

        Icon(
            imageVector = if (gpsData.isAvailable) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
            contentDescription = "GPS Status",
            tint = statusColor,
            modifier = Modifier.size(14.dp)
        )

        Text(
            text = statusLabel,
            fontSize = 11.sp,
            color = TextWhite,
            fontWeight = FontWeight.Medium
        )
    }
}

