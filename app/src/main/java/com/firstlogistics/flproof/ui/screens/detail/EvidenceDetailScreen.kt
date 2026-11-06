package com.firstlogistics.flproof.ui.screens.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.firstlogistics.flproof.R
import com.firstlogistics.flproof.data.entity.EvidenceEntity
import com.firstlogistics.flproof.ui.components.FLProofTopBar
import com.firstlogistics.flproof.ui.theme.*
import java.io.File
import java.util.Locale

@Composable
fun EvidenceDetailScreen(
    evidenceId: String,
    viewModel: EvidenceDetailViewModel,
    onNavigateBack: () -> Unit,
    onAddMediaToSession: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(evidenceId) {
        viewModel.loadEvidence(evidenceId)
    }

    val evidence = uiState.evidence
    val selectedItem = uiState.selectedMedia ?: evidence
    val sessionId = evidence?.sessionId ?: evidence?.evidenceId ?: evidenceId

    Scaffold(
        topBar = {
            FLProofTopBar(
                title = evidence?.evidenceId ?: "Detail Bukti",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                showSettingsButton = false,
                actions = {
                    IconButton(onClick = { viewModel.shareEvidence(context) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = BrandBlue
                        )
                    }
                }
            )
        },
        containerColor = NavyBackground
    ) { padding ->
        if (evidence == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandBlue)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Multi-Media Session Carousel (1 Sesi = Banyak Foto & Video)
            if (uiState.sessionMediaList.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Media Sesi (${uiState.sessionMediaList.size} item):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandNavy
                        )

                        TextButton(
                            onClick = { onAddMediaToSession(sessionId) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = GreenAccent)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Tambah Foto/Video", fontSize = 12.sp, color = GreenAccent, fontWeight = FontWeight.Bold)
                        }
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.sessionMediaList) { item ->
                            val isSelected = selectedItem?.id == item.id
                            val isItemVideo = item.mediaType == "video"

                            Box(
                                modifier = Modifier
                                    .size(width = 86.dp, height = 86.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NavySurface)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) GreenAccent else NavyBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.selectMedia(item) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isItemVideo) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayCircle,
                                            contentDescription = "Video",
                                            tint = Color(0xFFFF3B30),
                                            modifier = Modifier.size(30.dp)
                                        )
                                        Text(
                                            text = "VIDEO #${item.sessionIndex}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PureWhite
                                        )
                                    }
                                } else {
                                    AsyncImage(
                                        model = File(item.watermarkedPhotoPath),
                                        contentDescription = "Foto #${item.sessionIndex}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.65f))
                                            .padding(vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "FOTO #${item.sessionIndex}",
                                            fontSize = 9.sp,
                                            color = PureWhite,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Main Media Display
            if (selectedItem?.mediaType == "video") {
                // Video Player Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NavySurface)
                        .border(1.dp, NavyBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30).copy(alpha = 0.2f))
                                .border(2.dp, Color(0xFFFF3B30), CircleShape)
                                .clickable {
                                    viewModel.playVideo(context, selectedItem.videoPath)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Video",
                                tint = Color(0xFFFF3B30),
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Text(
                            text = "Rekaman Video Bukti (MP4)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandNavy
                        )

                        val sizeMb = String.format(Locale.US, "%.1f MB", selectedItem.fileSizeBytes.toDouble() / (1024 * 1024))
                        Text(
                            text = "${selectedItem.dateFormatted} • ${selectedItem.timeFormatted} • $sizeMb",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Button(
                            onClick = { viewModel.playVideo(context, selectedItem.videoPath) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30), contentColor = PureWhite),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Putar Video", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Photo Display with Watermark vs Original Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NavySurface)
                        .border(1.dp, NavyBorder, RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isWatermarked = !uiState.showOriginalPhoto

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isWatermarked) BrandBlue else Color.Transparent)
                            .clickable { if (!isWatermarked) viewModel.togglePhotoView() }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Foto Watermark (Ada QR)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isWatermarked) PureWhite else TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isWatermarked) BrandBlue else Color.Transparent)
                            .clickable { if (isWatermarked) viewModel.togglePhotoView() }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Foto Asli (Original)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isWatermarked) PureWhite else TextSecondary
                        )
                    }
                }

                val currentPhotoPath = if (uiState.showOriginalPhoto) {
                    selectedItem?.originalPhotoPath ?: evidence.originalPhotoPath
                } else {
                    selectedItem?.watermarkedPhotoPath ?: evidence.watermarkedPhotoPath
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NavySurface)
                        .border(1.dp, NavyBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = File(currentPhotoPath),
                        contentDescription = selectedItem?.evidenceId ?: evidence.evidenceId,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(NavySurface.copy(alpha = 0.9f))
                            .border(1.dp, NavyBorder, RoundedCornerShape(999.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${selectedItem?.dateFormatted ?: evidence.dateFormatted} • ${selectedItem?.timeFormatted ?: evidence.timeFormatted}",
                            color = BrandNavy,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Quick Actions: Share, PDF, Gallery
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.shareEvidence(context) },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandNavy, contentColor = PureWhite),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bagikan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.exportPdfReport(context) },
                    enabled = !uiState.isExportingPdf,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = PureWhite),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (uiState.isExportingPdf) {
                        CircularProgressIndicator(color = PureWhite, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.saveToGallery() },
                    modifier = Modifier
                        .weight(0.9f)
                        .height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Galeri", fontSize = 11.5.sp)
                }
            }

            // Cloud Sync & Vercel Web Portal Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = GreenAccent, modifier = Modifier.size(20.dp))
                            Text("Database Supabase & Portal Vercel", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
                        }

                        val syncedCount = uiState.sessionMediaList.count { it.isSynced }
                        val totalCount = uiState.sessionMediaList.size
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (syncedCount == totalCount && totalCount > 0) GreenAccentMuted else Color(0x33FF6600))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (syncedCount == totalCount && totalCount > 0) "TERHUBUNG ($syncedCount/$totalCount)" else "$syncedCount/$totalCount TERUPLOAD",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (syncedCount == totalCount && totalCount > 0) GreenAccent else Color(0xFFFF6600)
                            )
                        }
                    }

                    Text(
                        text = "Foto & video dapat diakses oleh customer melalui scan QR atau link portal web Vercel.",
                        fontSize = 11.5.sp,
                        color = TextSecondary
                    )

                    // Buttons: Upload to Supabase & Open Vercel Portal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.syncSessionToSupabase() },
                            enabled = !uiState.isUploadingCloud,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandNavy, contentColor = PureWhite),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (uiState.isUploadingCloud) {
                                CircularProgressIndicator(color = PureWhite, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Uploading...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = PureWhite)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload ke Supabase", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = { viewModel.openVercelPortal(context) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenAccent, contentColor = PureWhite),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp), tint = PureWhite)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buka Web", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = { viewModel.copyVercelUrl() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.3f))
                                .border(1.dp, NavyBorder, RoundedCornerShape(10.dp))
                        ) {
                            Icon(Icons.Default.Link, contentDescription = "Copy Link", tint = BrandBlue)
                        }
                    }
                }
            }

            // Information Detail Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_fl),
                            contentDescription = "First Logistics Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Column {
                            Text("FIRST LOGISTICS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                            Text("Informasi Bukti Dokumentasi", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
                        }
                    }

                    HorizontalDivider(color = NavyBorder, thickness = 1.dp)

                    DetailRowItem(label = "ID Dokumentasi", value = selectedItem?.evidenceId ?: evidence.evidenceId, isHighlight = true)
                    DetailRowItem(label = "ID Sesi Multi-Media", value = sessionId)
                    DetailRowItem(label = "Tipe Media", value = if (selectedItem?.mediaType == "video") "Rekaman Video (MP4)" else "Foto Dokumentasi (JPEG)")
                    DetailRowItem(label = "Petugas Lapangan", value = selectedItem?.officerName ?: evidence.officerName)
                    DetailRowItem(label = "Kategori", value = selectedItem?.category ?: evidence.category)
                    DetailRowItem(label = "Status Integritas", value = selectedItem?.status ?: evidence.status)
                    DetailRowItem(label = "Waktu Pengambilan", value = "${selectedItem?.dateFormatted ?: evidence.dateFormatted} • ${selectedItem?.timeFormatted ?: evidence.timeFormatted}")
                    DetailRowItem(label = "Kamera / Lensa", value = "${selectedItem?.cameraLens ?: evidence.cameraLens} • ${selectedItem?.deviceModel ?: evidence.deviceModel}")

                    // Alamat with Copy
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Alamat Lokasi", fontSize = 11.sp, color = TextSecondary)
                            Text(selectedItem?.address ?: evidence.address, fontSize = 12.5.sp, color = TextWhite)
                        }
                        IconButton(onClick = { viewModel.copyAddress() }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Alamat", tint = BrandBlue, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Koordinat GPS with Copy
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Koordinat GPS", fontSize = 11.sp, color = TextSecondary)
                            val lat = selectedItem?.latitude ?: evidence.latitude
                            val lng = selectedItem?.longitude ?: evidence.longitude
                            val acc = selectedItem?.gpsAccuracy ?: evidence.gpsAccuracy
                            val coordStr = if (lat != 0.0) {
                                "$lat, $lng (±${acc.toInt()}m)"
                            } else "GPS tidak tersedia"
                            Text(coordStr, fontSize = 12.5.sp, color = TextWhite)
                        }
                        IconButton(onClick = { viewModel.copyCoordinates() }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Koordinat", tint = BrandBlue, modifier = Modifier.size(16.dp))
                        }
                    }

                    if (!selectedItem?.compassDirection.isNullOrBlank()) {
                        DetailRowItem(label = "Arah Kompas", value = selectedItem!!.compassDirection)
                    }

                    DetailRowItem(label = "Resolusi & Ukuran", value = "${selectedItem?.resolution ?: evidence.resolution} (${(selectedItem?.fileSizeBytes ?: evidence.fileSizeBytes) / 1024} KB)")

                    // SHA-256 Hash
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("File Integrity Hash (SHA-256)", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = selectedItem?.sha256Hash ?: evidence.sha256Hash,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = BrandNavy
                        )
                    }

                    if (!selectedItem?.notes.isNullOrBlank()) {
                        DetailRowItem(label = "Catatan Tambahan", value = selectedItem!!.notes!!)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DetailRowItem(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, fontSize = 11.sp, color = TextSecondary)
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlight) BrandNavy else TextWhite
        )
    }
}
