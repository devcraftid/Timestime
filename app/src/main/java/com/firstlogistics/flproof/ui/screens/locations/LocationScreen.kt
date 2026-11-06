package com.firstlogistics.flproof.ui.screens.locations

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.firstlogistics.flproof.data.entity.EvidenceEntity
import com.firstlogistics.flproof.ui.components.FLProofTopBar
import com.firstlogistics.flproof.ui.components.GpsAccuracyBadge
import com.firstlogistics.flproof.ui.theme.*
import java.io.File
import java.util.Locale

@Composable
fun LocationScreen(
    viewModel: LocationViewModel,
    onOpenEvidenceDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val gpsData by viewModel.locationTracker.gpsData.collectAsState()
    val compassData by viewModel.compassSensorManager.compassData.collectAsState()

    Scaffold(
        topBar = {
            FLProofTopBar(
                title = "Riwayat Lokasi",
                showSettingsButton = true,
                onSettingsClick = onNavigateToSettings
            )
        },
        containerColor = NavyBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Device GPS Status Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(NavySurface)
                        .border(1.dp, NavyBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "GPS",
                                    tint = GreenAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Status GPS Saat Ini",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            }
                            GpsAccuracyBadge(gpsData = gpsData)
                        }

                        HorizontalDivider(thickness = 1.dp, color = NavyBorder)

                        if (gpsData.isAvailable && gpsData.latitude != 0.0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Latitude", fontSize = 11.sp, color = TextSecondary)
                                    Text(String.format(Locale.US, "%.6f", gpsData.latitude), fontSize = 13.sp, color = TextWhite, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Longitude", fontSize = 11.sp, color = TextSecondary)
                                    Text(String.format(Locale.US, "%.6f", gpsData.longitude), fontSize = 13.sp, color = TextWhite, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Arah Kompas", fontSize = 11.sp, color = TextSecondary)
                                    Text(compassData.formattedCompass, fontSize = 13.sp, color = GreenAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Text(
                                text = "Menunggu sinyal GPS satelit...",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Search Filter
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Cari riwayat lokasi atau ID...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = GreenAccent,
                        unfocusedBorderColor = NavyBorder,
                        focusedContainerColor = NavySurface,
                        unfocusedContainerColor = NavySurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Titik Koordinat Dokumentasi (${uiState.filteredRecords.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }
            }

            if (uiState.filteredRecords.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.LocationOff, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(44.dp))
                            Text("Belum Ada Titik Lokasi", fontSize = 14.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                            Text("Ambil foto bukti dokumentasi dengan GPS aktif.", fontSize = 12.sp, color = TextTertiary)
                        }
                    }
                }
            } else {
                items(uiState.filteredRecords, key = { it.id }) { record ->
                    LocationRecordItem(
                        record = record,
                        onClick = { onOpenEvidenceDetail(record.evidenceId) },
                        onOpenMap = {
                            val uri = Uri.parse("geo:${record.latitude},${record.longitude}?q=${record.latitude},${record.longitude}(${Uri.encode(record.evidenceId)})")
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(mapIntent)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun LocationRecordItem(
    record: EvidenceEntity,
    onClick: () -> Unit,
    onOpenMap: () -> Unit
) {
    val photoPath = if (File(record.watermarkedPhotoPath).exists()) record.watermarkedPhotoPath else record.originalPhotoPath

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NavySurface)
            .border(1.dp, NavyBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = File(photoPath),
                contentDescription = record.evidenceId,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NavyCard)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = record.evidenceId,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = record.category,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenAccent
                    )
                }

                Text(
                    text = record.address.ifBlank { "Lokasi Lapangan" },
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 2
                )

                Text(
                    text = String.format(Locale.US, "GPS: %.6f, %.6f (±%dm)", record.latitude, record.longitude, record.gpsAccuracy.toInt()),
                    fontSize = 11.sp,
                    color = GreenAccent
                )
            }

            IconButton(
                onClick = onOpenMap,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NavyCard)
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Buka Peta",
                    tint = GreenAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

