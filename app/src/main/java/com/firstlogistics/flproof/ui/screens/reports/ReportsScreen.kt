package com.firstlogistics.flproof.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.firstlogistics.flproof.data.model.DocumentationCategory
import com.firstlogistics.flproof.ui.components.FLProofTopBar
import com.firstlogistics.flproof.ui.components.StatCard
import com.firstlogistics.flproof.ui.theme.*
import java.io.File

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onOpenEvidenceDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            FLProofTopBar(
                title = "Laporan Operasional",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period Tabs: Harian, Mingguan, Bulanan, Semua
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NavySurface)
                        .border(1.dp, NavyBorder, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ReportPeriod.entries.forEach { period ->
                        val isSelected = uiState.selectedPeriod == period
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) GreenAccent else Color.Transparent)
                                .clickable { viewModel.setPeriod(period) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = period.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PureWhite else TextWhite
                            )
                        }
                    }
                }
            }

            // Summary Statistics Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "TOTAL PERIODE",
                            value = uiState.totalCount.toString(),
                            icon = Icons.Default.Assessment,
                            iconColor = GreenAccent,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "BERMASALAH",
                            value = uiState.problematicCount.toString(),
                            icon = Icons.Default.WarningAmber,
                            iconColor = StatusError,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "BARANG MASUK",
                            value = uiState.incomingCount.toString(),
                            icon = Icons.AutoMirrored.Filled.CallReceived,
                            iconColor = GreenAccent,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "BARANG KELUAR",
                            value = uiState.outgoingCount.toString(),
                            icon = Icons.AutoMirrored.Filled.CallMade,
                            iconColor = StatusWarning,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Export Actions: PDF & CSV
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.exportPdf(context) },
                        enabled = !uiState.isGeneratingPdf && uiState.records.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenAccent,
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isGeneratingPdf) {
                            CircularProgressIndicator(
                                color = PureWhite,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Export PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.exportCsv(context) },
                        enabled = !uiState.isExportingCsv && uiState.records.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isExportingCsv) {
                            CircularProgressIndicator(
                                color = GreenAccent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Export CSV", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Section Header: Included Records
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rincian Dokumen Laporan (${uiState.records.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }
            }

            // Records List
            if (uiState.records.isEmpty()) {
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
                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(40.dp))
                            Text("Tidak Ada Data Pada Periode Ini", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(uiState.records, key = { it.id }) { record ->
                    ReportItemCard(
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
fun ReportItemCard(
    record: EvidenceEntity,
    onClick: () -> Unit
) {
    val photoPath = if (File(record.watermarkedPhotoPath).exists()) record.watermarkedPhotoPath else record.originalPhotoPath

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(NavySurface)
            .border(1.dp, NavyBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = File(photoPath),
            contentDescription = record.evidenceId,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(NavyCard)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = record.evidenceId,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = record.category,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (record.category.contains("RUSAK") || record.category.contains("HILANG")) StatusError else GreenAccent
                )
            }
            Text(
                text = "${record.dateFormatted} • ${record.timeFormatted}",
                fontSize = 11.sp,
                color = TextSecondary
            )
            Text(
                text = record.address.ifBlank { "Lokasi: ${record.latitude}, ${record.longitude}" },
                fontSize = 10.sp,
                color = TextTertiary,
                maxLines = 1
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(16.dp)
        )
    }
}

