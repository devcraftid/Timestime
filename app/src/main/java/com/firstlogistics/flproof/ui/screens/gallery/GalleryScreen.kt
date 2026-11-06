package com.firstlogistics.flproof.ui.screens.gallery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.firstlogistics.flproof.data.entity.EvidenceEntity
import com.firstlogistics.flproof.data.model.DocumentationCategory
import com.firstlogistics.flproof.sharing.ShareHelper
import com.firstlogistics.flproof.ui.components.ConfirmationDialog
import com.firstlogistics.flproof.ui.components.FLProofTopBar
import com.firstlogistics.flproof.ui.theme.*
import java.io.File

@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onOpenEvidenceDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val activeRecords by viewModel.activeRecords.collectAsState()
    val trashedRecords by viewModel.trashedRecords.collectAsState()

    val currentList = if (uiState.showTrashTab) trashedRecords else activeRecords

    var itemToDelete by remember { mutableStateOf<EvidenceEntity?>(null) }
    var itemToHardDelete by remember { mutableStateOf<EvidenceEntity?>(null) }

    Scaffold(
        topBar = {
            FLProofTopBar(
                title = if (uiState.showTrashTab) "Kotak Sampah" else "Galeri Bukti",
                showSettingsButton = true,
                onSettingsClick = onNavigateToSettings,
                actions = {
                    IconButton(onClick = { viewModel.toggleTrashTab() }) {
                        Icon(
                            imageVector = if (uiState.showTrashTab) Icons.Default.Collections else Icons.Default.Delete,
                            contentDescription = "Trash",
                            tint = if (uiState.showTrashTab) GreenAccent else TextSecondary
                        )
                    }
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = if (uiState.isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle View",
                            tint = TextSecondary
                        )
                    }
                }
            )
        },
        containerColor = NavyBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Cari ID, alamat, kategori, barcode...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = GreenAccent,
                    unfocusedBorderColor = NavyBorder,
                    focusedContainerColor = NavySurface,
                    unfocusedContainerColor = NavySurface,
                    cursorColor = GreenAccent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Category Filter Chips Carousel
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedCategory.isEmpty(),
                        onClick = { viewModel.setSelectedCategory("") },
                        label = { Text("SEMUA KATEGORI") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenAccent,
                            selectedLabelColor = PureWhite,
                            containerColor = NavyCard,
                            labelColor = TextWhite
                        )
                    )
                }
                items(DocumentationCategory.entries) { cat ->
                    FilterChip(
                        selected = uiState.selectedCategory == cat.title,
                        onClick = { viewModel.setSelectedCategory(cat.title) },
                        label = { Text(cat.title) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenAccent,
                            selectedLabelColor = PureWhite,
                            containerColor = NavyCard,
                            labelColor = TextWhite
                        )
                    )
                }
            }

            // Results count and batch action bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentList.size} Dokumen Ditemukan",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )

                if (uiState.isBatchMode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${uiState.selectedItemsForBatch.size} dipilih",
                            fontSize = 12.sp,
                            color = GreenAccent,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                val selectedRecords = activeRecords.filter { uiState.selectedItemsForBatch.contains(it.id) }
                                ShareHelper.shareMultiple(context, selectedRecords)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Batch",
                                tint = GreenAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = { viewModel.clearBatchSelection() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Batch",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Trash Tips Banner
            if (uiState.showTrashTab && currentList.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StatusWarning.copy(alpha = 0.15f))
                        .border(1.dp, StatusWarning.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = StatusWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Tekan lama foto untuk opsi Hapus Permanen.",
                            color = TextWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // List or Grid Content
            if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.showTrashTab) Icons.Default.DeleteOutline else Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (uiState.showTrashTab) "Kotak Sampah Kosong" else "Tidak Ada Bukti Dokumentasi",
                            color = TextSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (uiState.showTrashTab) "Belum ada dokumen yang dihapus." else "Ambil foto bukti dokumentasi dari menu Kamera.",
                            color = TextTertiary,
                            fontSize = 12.sp
                        )
                    }
                }
            } else if (uiState.isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(currentList, key = { it.id }) { record ->
                        EvidenceGridCard(
                            record = record,
                            isSelected = uiState.selectedItemsForBatch.contains(record.id),
                            isTrash = uiState.showTrashTab,
                            onClick = {
                                if (uiState.isBatchMode) {
                                    viewModel.toggleBatchSelection(record.id)
                                } else {
                                    onOpenEvidenceDetail(record.evidenceId)
                                }
                            },
                            onLongClick = {
                                if (uiState.showTrashTab) {
                                    itemToHardDelete = record
                                } else {
                                    viewModel.toggleBatchSelection(record.id)
                                }
                            },
                            onDelete = { itemToDelete = record },
                            onRestore = { viewModel.restore(record.id) },
                            onPermanentDelete = { itemToHardDelete = record }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(currentList, key = { it.id }) { record ->
                        EvidenceListCard(
                            record = record,
                            isSelected = uiState.selectedItemsForBatch.contains(record.id),
                            isTrash = uiState.showTrashTab,
                            onClick = {
                                if (uiState.isBatchMode) {
                                    viewModel.toggleBatchSelection(record.id)
                                } else {
                                    onOpenEvidenceDetail(record.evidenceId)
                                }
                            },
                            onLongClick = {
                                if (uiState.showTrashTab) {
                                    itemToHardDelete = record
                                } else {
                                    viewModel.toggleBatchSelection(record.id)
                                }
                            },
                            onDelete = { itemToDelete = record },
                            onRestore = { viewModel.restore(record.id) },
                            onPermanentDelete = { itemToHardDelete = record }
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialogs
    if (itemToDelete != null) {
        ConfirmationDialog(
            title = "Pindahkan ke Sampah?",
            message = "Dokumen ${itemToDelete!!.evidenceId} akan dipindahkan ke kotak sampah dan dapat dipulihkan kembali.",
            confirmButtonText = "Hapus",
            isDestructive = true,
            onConfirm = {
                viewModel.softDelete(itemToDelete!!.id)
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null }
        )
    }

    if (itemToHardDelete != null) {
        ConfirmationDialog(
            title = "Hapus Permanen?",
            message = "PERINGATAN: File foto asli dan watermark dokumen ${itemToHardDelete!!.evidenceId} akan dihapus permanen dari perangkat dan tidak dapat dipulihkan.",
            confirmButtonText = "Hapus Permanen",
            isDestructive = true,
            onConfirm = {
                viewModel.hardDelete(itemToHardDelete!!.id)
                itemToHardDelete = null
            },
            onDismiss = { itemToHardDelete = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EvidenceGridCard(
    record: EvidenceEntity,
    isSelected: Boolean,
    isTrash: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    val photoPath = if (File(record.watermarkedPhotoPath).exists()) record.watermarkedPhotoPath else record.originalPhotoPath

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(NavySurface)
            .border(
                1.5.dp,
                if (isSelected) GreenAccent else NavyBorder,
                RoundedCornerShape(12.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = File(photoPath),
            contentDescription = record.evidenceId,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dark gradient overlay at bottom for readable text
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
                .padding(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = record.evidenceId,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Text(
                    text = record.category,
                    color = GreenAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = record.dateFormatted,
                    color = PureWhite.copy(alpha = 0.8f),
                    fontSize = 9.sp
                )
            }
        }

        // Top badges: Category and Delete/Restore button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = GreenAccent,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Spacer(modifier = Modifier.size(1.dp))
            }

            if (isTrash) {
                IconButton(
                    onClick = onRestore,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PureWhite.copy(alpha = 0.9f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "Restore",
                        tint = GreenAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PureWhite.copy(alpha = 0.9f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = StatusError,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EvidenceListCard(
    record: EvidenceEntity,
    isSelected: Boolean,
    isTrash: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    val photoPath = if (File(record.watermarkedPhotoPath).exists()) record.watermarkedPhotoPath else record.originalPhotoPath

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NavySurface)
            .border(
                1.dp,
                if (isSelected) GreenAccent else NavyBorder,
                RoundedCornerShape(12.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = File(photoPath),
            contentDescription = record.evidenceId,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(76.dp)
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
                    color = if (record.category.contains("RUSAK") || record.category.contains("HILANG")) StatusError else GreenAccent
                )
            }

            Text(
                text = "${record.dateFormatted} • ${record.timeFormatted}",
                fontSize = 11.sp,
                color = TextSecondary
            )

            Text(
                text = record.address.ifBlank { "GPS: ${record.latitude}, ${record.longitude}" },
                fontSize = 11.sp,
                color = TextTertiary,
                maxLines = 1
            )
        }

        if (isTrash) {
            Row {
                IconButton(onClick = onRestore) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "Restore",
                        tint = GreenAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onPermanentDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Hard Delete",
                        tint = StatusError,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

