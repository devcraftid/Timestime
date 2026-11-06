package com.firstlogistics.flproof.ui.screens.seabank

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firstlogistics.flproof.R
import com.firstlogistics.flproof.ui.components.FLProofTopBar
import com.firstlogistics.flproof.ui.theme.*

@Composable
fun SeabankScreen(
    viewModel: SeabankViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var showBarcodeScannerDialog by remember { mutableStateOf(false) }

    if (showBarcodeScannerDialog) {
        AwbBarcodeScannerDialog(
            onBarcodeDetected = { scannedCode ->
                viewModel.setAwbInput(scannedCode)
                viewModel.confirmAwb()
                showBarcodeScannerDialog = false
            },
            onDismiss = { showBarcodeScannerDialog = false }
        )
    }

    // Launcher for taking photo from camera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            viewModel.setLabelBitmap(bmp)
        }
    }

    // Launcher for selecting image from gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bmp = loadBitmapWithExif(context, uri)
            if (bmp != null) {
                viewModel.setLabelBitmap(bmp)
            }
        }
    }

    Scaffold(
        topBar = {
            FLProofTopBar(
                title = "Alokasi SeaBank",
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
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Header Stats & Title with Official SeaBank Circular Logo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(2.dp, BrandOrange, CircleShape)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_seabank_circle),
                            contentDescription = "SeaBank Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                    Column {
                        Text(
                            text = "Alokasi SeaBank",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandNavy
                        )
                        Text(
                            text = "Scan AWB & OCR Alamat Paket",
                            fontSize = 11.5.sp,
                            color = TextSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(BrandOrange.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "SEABANK OCR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2 Stat Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatPill(
                    title = "Total paket tersimpan",
                    value = uiState.totalCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    title = "Paket hari ini",
                    value = uiState.todayCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Navigation (Work vs Archive)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TabButton(
                    text = "＋ Proses Paket",
                    isActive = uiState.activeTab == SeabankTab.WORK,
                    modifier = Modifier.weight(1f)
                ) {
                    viewModel.setTab(SeabankTab.WORK)
                }

                TabButton(
                    text = "▤ Arsip & Download",
                    isActive = uiState.activeTab == SeabankTab.ARCHIVE,
                    modifier = Modifier.weight(1f)
                ) {
                    viewModel.setTab(SeabankTab.ARCHIVE)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Tab Content
            when (uiState.activeTab) {
                SeabankTab.WORK -> {
                    SeabankWorkTab(
                        uiState = uiState,
                        onScanBarcodeClick = { showBarcodeScannerDialog = true },
                        onAwbChange = { viewModel.setAwbInput(it) },
                        onConfirmAwb = { viewModel.confirmAwb() },
                        onOpenCamera = { cameraLauncher.launch(null) },
                        onOpenGallery = { galleryLauncher.launch("image/*") },
                        onRotatePhoto = { viewModel.rotateLabelBitmap() },
                        onRunOcr = { viewModel.runOcr() },
                        onRecipientChange = { viewModel.setRecipient(it) },
                        onAddressChange = { viewModel.setAddress(it) },
                        onSave = { viewModel.savePackage(context) },
                        onReset = { viewModel.resetPackageForm() }
                    )
                }
                SeabankTab.ARCHIVE -> {
                    SeabankArchiveTab(
                        uiState = uiState,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onToggleSelect = { viewModel.toggleSelect(it) },
                        onSelectAll = { viewModel.selectAll() },
                        onDeleteSelected = { viewModel.deleteSelected(context) },
                        onEdit = { viewModel.editPackage(it) },
                        onDelete = { viewModel.deletePackage(it) },
                        onExportExcel = { viewModel.exportExcel(context) },
                        onExportDoc = { viewModel.exportDocument(context) },
                        onExportPdf = { viewModel.exportPdf(context) }
                    )
                }
            }
        }
    }
}

@Composable
fun SeabankWorkTab(
    uiState: SeabankUiState,
    onScanBarcodeClick: () -> Unit,
    onAwbChange: (String) -> Unit,
    onConfirmAwb: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    onRotatePhoto: () -> Unit,
    onRunOcr: () -> Unit,
    onRecipientChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // STEP 1 CARD: Scan / Input Barcode AWB
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PaleBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("1", color = BrandBlue, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                    Column {
                        Text("Nomor Barcode / Resi AWB", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
                        Text("Bisa scan langsung dengan kamera atau ketik manual.", fontSize = 11.sp, color = TextSecondary)
                    }
                }

                // Opsi 1: Scan Barcode dengan Kamera
                Button(
                    onClick = onScanBarcodeClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = PureWhite)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode AWB", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("📷 Scan Barcode / QR AWB (Kamera)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), thickness = 0.8.dp, color = NavyBorder)
                    Text("  ATAU KETIK MANUAL  ", fontSize = 10.sp, color = TextTertiary, fontWeight = FontWeight.Bold)
                    HorizontalDivider(modifier = Modifier.weight(1f), thickness = 0.8.dp, color = NavyBorder)
                }

                // Opsi 2: Ketik Manual
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.awbInput,
                        onValueChange = onAwbChange,
                        placeholder = { Text("Ketik No. AWB resi...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = NavyBorder,
                            cursorColor = BrandBlue
                        )
                    )

                    Button(
                        onClick = onConfirmAwb,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandNavy, contentColor = PureWhite),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("Konfirmasi", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (uiState.isAwbConfirmed && uiState.confirmedAwb.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(GreenAccentMuted)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "✓ AWB Terkonfirmasi: ${uiState.confirmedAwb}. Tahap foto label sudah aktif.",
                            color = GreenAccent,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // STEP 2 CARD: Foto / Upload Label Alamat
        val isStep2Active = uiState.isAwbConfirmed && uiState.confirmedAwb.isNotBlank()

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isStep2Active) NavySurface else NavySurface.copy(alpha = 0.6f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isStep2Active) NavyBorder else NavyBorder.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isStep2Active) PaleBlue else PaleBlue.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("2", color = if (isStep2Active) BrandBlue else TextSecondary, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                    Column {
                        Text("Foto / Upload Label Alamat", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isStep2Active) BrandNavy else TextSecondary)
                        Text(
                            if (isStep2Active) "Ambil foto label paket, lalu jalankan OCR." else "Tahap ini aktif setelah AWB dikonfirmasi.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                if (isStep2Active) {
                    // Photo Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenCamera,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = PureWhite)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buka Kamera", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onOpenGallery,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandNavy),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pilih Galeri", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Label Photo Preview
                    if (uiState.labelPhotoBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF0F3F6))
                                .border(1.dp, NavyBorder, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = uiState.labelPhotoBitmap.asImageBitmap(),
                                contentDescription = "Foto Label",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Quick Rotate Button on Preview
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                Button(
                                    onClick = onRotatePhoto,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BrandNavy.copy(alpha = 0.85f),
                                        contentColor = PureWhite
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Putar 90°",
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Putar 90°", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // OCR Trigger Button & Progress
                    Button(
                        onClick = onRunOcr,
                        enabled = uiState.labelPhotoBitmap != null && !uiState.isOcrRunning,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = PureWhite)
                    ) {
                        if (uiState.isOcrRunning) {
                            CircularProgressIndicator(color = PureWhite, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sedang Membaca Label...", fontSize = 12.5.sp)
                        } else {
                            Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("✦ Ubah Foto Menjadi Teks (OCR)", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = uiState.ocrStatusText,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    // Editable Fields: Kepada & Alamat
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Penerima :", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
                        OutlinedTextField(
                            value = uiState.recipient,
                            onValueChange = onRecipientChange,
                            placeholder = { Text("Contoh: Nama penerima") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = NavyBorder,
                                cursorColor = BrandBlue
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Alamat :", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
                            if (uiState.rawOcrText.isNotBlank() && uiState.address != uiState.rawOcrText) {
                                TextButton(
                                    onClick = { onAddressChange(uiState.rawOcrText) },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Gunakan Teks Penuh", fontSize = 11.sp, color = BrandBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = uiState.address,
                            onValueChange = onAddressChange,
                            placeholder = { Text("Alamat lengkap penerima sampai RT/RW / Dusun / Kelurahan") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 8,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = NavyBorder,
                                cursorColor = BrandBlue
                            )
                        )
                    }

                    // Notice box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(PaleBlue)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "PENTING: Jangan simpan sebelum mencocokkan nama dan alamat dengan foto label asli.",
                            fontSize = 11.sp,
                            color = BrandNavy,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Save and Reset buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSave,
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandNavy, contentColor = PureWhite)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan Paket", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onReset,
                            modifier = Modifier.weight(0.9f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
                        ) {
                            Text("Ulangi", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun SeabankArchiveTab(
    uiState: SeabankUiState,
    onSearchChange: (String) -> Unit,
    onToggleSelect: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onEdit: (com.firstlogistics.flproof.data.entity.SeabankPackageEntity) -> Unit,
    onDelete: (com.firstlogistics.flproof.data.entity.SeabankPackageEntity) -> Unit,
    onExportExcel: () -> Unit,
    onExportDoc: () -> Unit,
    onExportPdf: () -> Unit
) {
    val filteredList = remember(uiState.packages, uiState.searchQuery) {
        val q = uiState.searchQuery.trim().lowercase()
        if (q.isBlank()) uiState.packages
        else uiState.packages.filter {
            it.awb.lowercase().contains(q) || it.recipient.lowercase().contains(q) || it.address.lowercase().contains(q)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search & Export Actions Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Cari AWB / Penerima / Alamat...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedBorderColor = BrandBlue,
                unfocusedBorderColor = NavyBorder,
                cursorColor = BrandBlue
            )
        )

        // Action Buttons Row (Excel, Word, PDF)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onExportExcel,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenAccent, contentColor = PureWhite),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Excel (.csv)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onExportDoc,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = PureWhite),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Dokumen", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onExportPdf,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandNavy, contentColor = PureWhite),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Select All & Delete Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onSelectAll) {
                Text(
                    text = if (uiState.selectedIds.size == uiState.packages.size && uiState.packages.isNotEmpty()) "Batal Pilih Semua" else "Pilih Semua",
                    fontSize = 11.5.sp,
                    color = BrandBlue,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.selectedIds.isNotEmpty()) {
                Button(
                    onClick = onDeleteSelected,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError, contentColor = PureWhite),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Hapus (${uiState.selectedIds.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Package List
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada paket SeaBank tersimpan.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList) { item ->
                    val isSelected = uiState.selectedIds.contains(item.id)

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = NavySurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) BrandBlue else NavyBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onToggleSelect(item.id) },
                                colors = CheckboxDefaults.colors(checkedColor = BrandBlue)
                            )

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.awb,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandNavy
                                    )
                                    Text(
                                        text = item.createdDateFormatted,
                                        fontSize = 10.5.sp,
                                        color = TextSecondary
                                    )
                                }

                                Text(
                                    text = "Penerima : ${item.recipient}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextWhite
                                )

                                Text(
                                    text = "Alamat : ${item.address.replace("\n", ", ")}",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    maxLines = 2
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { onEdit(item) }) {
                                        Text("Edit", fontSize = 11.sp, color = BrandBlue)
                                    }
                                    TextButton(onClick = { onDelete(item) }) {
                                        Text("Hapus", fontSize = 11.sp, color = StatusError)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatPill(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 11.sp, color = TextSecondary)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = BrandNavy)
        }
    }
}

@Composable
fun TabButton(
    text: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) BrandBlue else NavySurface)
            .border(1.dp, if (isActive) BrandBlue else NavyBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isActive) PureWhite else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

fun loadBitmapWithExif(context: android.content.Context, uri: android.net.Uri): android.graphics.Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (bitmap == null) return null

        val exifStream = context.contentResolver.openInputStream(uri) ?: return bitmap
        val exif = androidx.exifinterface.media.ExifInterface(exifStream)
        val orientation = exif.getAttributeInt(
            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
            androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
        )
        exifStream.close()

        val rotationDegrees = when (orientation) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rotationDegrees != 0f) {
            val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees) }
            android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


