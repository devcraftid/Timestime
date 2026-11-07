package com.firstlogistics.flproof.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.firstlogistics.flproof.data.entity.EvidenceEntity
import com.firstlogistics.flproof.ui.components.FLProofTopBar
import com.firstlogistics.flproof.ui.components.StatCard
import com.firstlogistics.flproof.ui.theme.*
import java.io.File

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onStartCamera: () -> Unit,
    onOpenEvidenceDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToBeforeAfter: () -> Unit,
    onNavigateToSeaBank: () -> Unit,
    onNavigateToWorkMate: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            FLProofTopBar(
                title = "FL PROOF",
                showSettingsButton = true,
                onSettingsClick = onNavigateToSettings
            )
        },
        containerColor = NavyBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Enterprise Clock & Officer Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(NavySurface)
                        .border(1.dp, NavyBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = com.firstlogistics.flproof.R.drawable.logo_fl),
                                    contentDescription = "First Logistics Logo",
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Column {
                                    Text(
                                        text = uiState.companyName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GreenAccent,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Halo, ${uiState.officerName}",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NavyCard)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "OFFLINE ACTIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenAccent
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 1.dp,
                            color = NavyBorder
                        )

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
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Date",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = uiState.currentDateString,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "Time",
                                    tint = GreenAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = uiState.currentTimeString,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            }
                        }
                    }
                }
            }

            // Big Action Button: MULAI DOKUMENTASI
            item {
                Button(
                    onClick = onStartCamera,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenAccent,
                        contentColor = PureWhite
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PureWhite.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Camera",
                                modifier = Modifier.size(22.dp),
                                tint = PureWhite
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MULAI DOKUMENTASI",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PureWhite,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Ambil foto bukti dengan watermark GPS & waktu",
                                fontSize = 11.sp,
                                color = PureWhite.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Alokasi SeaBank Module Card (Menu Bulat & Logo Resmi SeaBank)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSeaBank() },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = NavySurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, BrandOrange.copy(alpha = 0.6f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Menu Bulat SeaBank dengan Logo Resmi
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(3.dp, BrandOrange, CircleShape)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.firstlogistics.flproof.R.drawable.logo_seabank_circle),
                                contentDescription = "Logo SeaBank",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ALOKASI SEABANK",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Scan barcode resi & OCR alamat paket SeaBank otomatis (Offline)",
                                fontSize = 11.5.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BrandOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "Buka SeaBank",
                                tint = PureWhite,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // Fitur Volume & Berat (First Logistics WorkMate)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToWorkMate() },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = NavySurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, BrandBlue.copy(alpha = 0.6f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Logo Bulat Volume & Kilo
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .border(2.5.dp, BrandOrange, CircleShape)
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.firstlogistics.flproof.R.drawable.logo_volume_kilo),
                                contentDescription = "Volume & Kilo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "VOLUME & BERAT",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(BrandBlue.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "WORKMATE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandBlue
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Scan AWB, input dimensi suara (Voice P×L×T), kalkulator volume & berat volumetrik",
                                fontSize = 11.5.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BrandBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "Buka WorkMate",
                                tint = PureWhite,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // Statistics Grid (2 rows)
            item {
                Text(
                    text = "Ringkasan Operasional",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "TOTAL BUKTI",
                            value = uiState.totalCount.toString(),
                            icon = Icons.Default.Inventory2,
                            iconColor = GreenAccent,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToGallery
                        )
                        StatCard(
                            title = "HARI INI",
                            value = uiState.totalToday.toString(),
                            icon = Icons.Default.Today,
                            iconColor = StatusInfo,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToGallery
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "BARANG MASUK",
                            value = uiState.totalIncoming.toString(),
                            icon = Icons.AutoMirrored.Filled.CallReceived,
                            iconColor = GreenAccent,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "BARANG KELUAR",
                            value = uiState.totalOutgoing.toString(),
                            icon = Icons.AutoMirrored.Filled.CallMade,
                            iconColor = StatusWarning,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "BERMASALAH",
                            value = uiState.totalProblematic.toString(),
                            icon = Icons.Default.WarningAmber,
                            iconColor = StatusError,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Quick Operations Row
            item {
                Text(
                    text = "Aksi Cepat",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        title = "SeaBank",
                        imageRes = com.firstlogistics.flproof.R.drawable.logo_seabank_circle,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSeaBank
                    )
                    QuickActionButton(
                        title = "Before/After",
                        icon = Icons.Default.Compare,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToBeforeAfter
                    )
                    QuickActionButton(
                        title = "Semua Bukti",
                        icon = Icons.Default.PhotoLibrary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToGallery
                    )
                    QuickActionButton(
                        title = "Buat PDF",
                        icon = Icons.Default.PictureAsPdf,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToReports
                    )
                }
            }

            // Recent Documentation Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dokumentasi Terbaru",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    TextButton(onClick = onNavigateToGallery) {
                        Text(
                            text = "Lihat Semua",
                            fontSize = 12.sp,
                            color = GreenAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Recent Documentation Items
            if (uiState.recentRecords.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NavySurface)
                            .border(1.dp, NavyBorder, RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Belum Ada Dokumentasi",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Tekan 'MULAI DOKUMENTASI' untuk memotret bukti pertama Anda.",
                                color = TextTertiary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                items(uiState.recentRecords) { record ->
                    RecentEvidenceCard(
                        record = record,
                        onClick = { onOpenEvidenceDetail(record.evidenceId) }
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
fun QuickActionButton(
    title: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    imageRes: Int? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(NavySurface)
            .border(1.dp, NavyBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (imageRes != null) Color.White else NavyCard)
                    .border(
                        1.5.dp,
                        if (imageRes != null) BrandOrange else NavyBorder,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (imageRes != null) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = imageRes),
                        contentDescription = title,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Fit
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = GreenAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextWhite,
                maxLines = 1
            )
        }
    }
}

@Composable
fun RecentEvidenceCard(
    record: EvidenceEntity,
    onClick: () -> Unit
) {
    val photoPath = if (File(record.watermarkedPhotoPath).exists()) {
        record.watermarkedPhotoPath
    } else record.originalPhotoPath

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NavySurface)
            .border(1.dp, NavyBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(10.dp),
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
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (record.category.contains("RUSAK") || record.category.contains("HILANG"))
                                StatusError.copy(alpha = 0.2f)
                            else GreenAccentMuted
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = record.category,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (record.category.contains("RUSAK") || record.category.contains("HILANG"))
                            StatusError
                        else GreenAccent
                    )
                }
            }

            Text(
                text = "${record.dateFormatted} • ${record.timeFormatted}",
                fontSize = 11.sp,
                color = TextSecondary
            )

            Text(
                text = record.address.ifBlank { "Koordinat: ${record.latitude}, ${record.longitude}" },
                fontSize = 11.sp,
                color = TextTertiary,
                maxLines = 1
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}

