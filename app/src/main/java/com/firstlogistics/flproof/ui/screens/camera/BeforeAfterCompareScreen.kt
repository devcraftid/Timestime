package com.firstlogistics.flproof.ui.screens.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.firstlogistics.flproof.data.entity.EvidenceEntity
import com.firstlogistics.flproof.data.repository.EvidenceRepository
import com.firstlogistics.flproof.ui.components.FLProofTopBar
import com.firstlogistics.flproof.ui.theme.*
import java.io.File

@Composable
fun BeforeAfterCompareScreen(
    evidenceRepository: EvidenceRepository,
    onNavigateBack: () -> Unit
) {
    val allRecords by evidenceRepository.getAllEvidence().collectAsState(initial = emptyList())

    var selectedBefore by remember { mutableStateOf<EvidenceEntity?>(null) }
    var selectedAfter by remember { mutableStateOf<EvidenceEntity?>(null) }
    var isSelectingBefore by remember { mutableStateOf(true) }
    var showPickerModal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            FLProofTopBar(
                title = "Perbandingan Before & After",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                showSettingsButton = false
            )
        },
        containerColor = NavyBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Pilih dua foto untuk membandingkan kondisi barang sebelum dan sesudah aktivitas operasional.",
                color = TextSecondary,
                fontSize = 13.sp
            )

            // Comparison View
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // BEFORE Card
                CompareSlotCard(
                    title = "BEFORE (SEBELUM)",
                    evidence = selectedBefore,
                    modifier = Modifier.weight(1f),
                    onPickClick = {
                        isSelectingBefore = true
                        showPickerModal = true
                    }
                )

                // AFTER Card
                CompareSlotCard(
                    title = "AFTER (SESUDAH)",
                    evidence = selectedAfter,
                    modifier = Modifier.weight(1f),
                    onPickClick = {
                        isSelectingBefore = false
                        showPickerModal = true
                    }
                )
            }

            // Summary of difference if both selected
            if (selectedBefore != null && selectedAfter != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NavySurface)
                        .border(1.dp, GreenAccentBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Compare,
                                contentDescription = null,
                                tint = GreenAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Analisis Perbandingan",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        }

                        Text(
                            text = "• Before: ${selectedBefore!!.evidenceId} (${selectedBefore!!.dateFormatted} ${selectedBefore!!.timeFormatted})",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Text(
                            text = "• After: ${selectedAfter!!.evidenceId} (${selectedAfter!!.dateFormatted} ${selectedAfter!!.timeFormatted})",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        val statusDiff = if (selectedBefore!!.status != selectedAfter!!.status) {
                            "Status berubah dari ${selectedBefore!!.status} menjadi ${selectedAfter!!.status}"
                        } else {
                            "Status konsisten: ${selectedBefore!!.status}"
                        }
                        Text(
                            text = "• $statusDiff",
                            fontSize = 12.sp,
                            color = GreenAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // Modal to pick evidence photo
    if (showPickerModal) {
        AlertDialog(
            onDismissRequest = { showPickerModal = false },
            title = {
                Text(
                    text = if (isSelectingBefore) "Pilih Foto BEFORE" else "Pilih Foto AFTER",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allRecords) { rec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(NavyCard)
                                .clickable {
                                    if (isSelectingBefore) selectedBefore = rec else selectedAfter = rec
                                    showPickerModal = false
                                }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val path = if (File(rec.watermarkedPhotoPath).exists()) rec.watermarkedPhotoPath else rec.originalPhotoPath
                            AsyncImage(
                                model = File(path),
                                contentDescription = rec.evidenceId,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Column {
                                Text(
                                    text = "${rec.evidenceId} - ${rec.category}",
                                    color = TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${rec.dateFormatted} • ${rec.timeFormatted}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPickerModal = false }) {
                    Text("Tutup", color = GreenAccent)
                }
            },
            containerColor = NavySurface
        )
    }
}

@Composable
fun CompareSlotCard(
    title: String,
    evidence: EvidenceEntity?,
    modifier: Modifier = Modifier,
    onPickClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(NavySurface)
            .border(1.dp, NavyBorder, RoundedCornerShape(12.dp))
            .clickable { onPickClick() }
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (evidence != null) {
            val path = if (File(evidence.watermarkedPhotoPath).exists()) evidence.watermarkedPhotoPath else evidence.originalPhotoPath
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenAccent
                )
                AsyncImage(
                    model = File(path),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                )
                Text(
                    text = evidence.evidenceId,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = evidence.category,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Pilih",
                    tint = GreenAccent,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "Ketuk untuk memilih foto",
                    fontSize = 11.sp,
                    color = TextTertiary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

