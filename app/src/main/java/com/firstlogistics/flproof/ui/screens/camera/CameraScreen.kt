package com.firstlogistics.flproof.ui.screens.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.firstlogistics.flproof.data.model.DocumentationCategory
import com.firstlogistics.flproof.ui.components.CameraLevelIndicator
import com.firstlogistics.flproof.ui.components.CategoryChip
import com.firstlogistics.flproof.ui.components.GpsAccuracyBadge
import com.firstlogistics.flproof.ui.theme.*
import java.util.Locale

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onEvidenceSaved: (String) -> Unit,
    onOpenEvidenceDetail: (String) -> Unit = {},
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val gpsData by viewModel.locationTracker.gpsData.collectAsState()
    val compassData by viewModel.compassSensorManager.compassData.collectAsState()

    var previewView: PreviewView? by remember { mutableStateOf(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: hasCameraPermission
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: hasLocationPermission
        if (hasLocationPermission) {
            viewModel.locationTracker.startTracking()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    var showPreviewModal by remember { mutableStateOf(false) }

    // If a photo was captured OR a video was recorded, show CameraPreviewScreen
    if (showPreviewModal && (uiState.previewWatermarkedBitmap != null || uiState.recordedVideoFile != null)) {
        CameraPreviewScreen(
            previewBitmap = uiState.previewWatermarkedBitmap,
            videoFile = uiState.recordedVideoFile,
            videoDurationSec = uiState.recordingDurationSec,
            sessionId = uiState.activeSessionId,
            sessionIndex = uiState.sessionIndex,
            category = uiState.selectedCategory,
            qualityCheck = uiState.qualityCheck,
            onCategoryChange = { viewModel.setCategory(it) },
            onRetake = {
                if (uiState.previewWatermarkedBitmap != null) {
                    viewModel.retakePhoto()
                } else {
                    viewModel.retakeVideo()
                }
                showPreviewModal = false
            },
            onSaveAndContinue = { notes, nextMode ->
                viewModel.saveCurrentEvidence(
                    notes = notes,
                    addMoreMedia = true,
                    nextMode = nextMode
                ) {
                    showPreviewModal = false
                    previewView?.let { pv ->
                        viewModel.bindCamera(lifecycleOwner, pv)
                    }
                }
            },
            onSaveAndFinish = { notes ->
                viewModel.saveCurrentEvidence(
                    notes = notes,
                    addMoreMedia = false
                ) { targetSessionId ->
                    showPreviewModal = false
                    onEvidenceSaved(targetSessionId)
                }
            }
        )
        return
    }

    if (!hasCameraPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NavyBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "Izin Kamera Diperlukan",
                    style = MaterialTheme.typography.titleLarge,
                    color = BrandNavy,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Aplikasi FL PROOF membutuhkan izin kamera dan mikrofon untuk mengambil foto dan merekam video dokumentasi operasional First Logistics.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = PureWhite),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Berikan Izin Kamera, Audio & Lokasi", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // CameraX Surface View
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    this.scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView = this
                    viewModel.bindCamera(lifecycleOwner, this)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        previewView?.let { pv ->
                            viewModel.focusOnPoint(offset.x, offset.y, pv.width, pv.height)
                        }
                    }
                }
        )

        // 3x3 Grid Overlay
        if (uiState.showGrid) {
            CameraGridOverlay()
        }

        // Camera Level Horizon
        if (uiState.showLevel) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CameraLevelIndicator(
                    pitch = compassData.pitch,
                    roll = compassData.roll,
                    isLevel = compassData.isLevel
                )
            }
        }

        // Top Control Bar Overlay (Clean, non-obtrusive)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash Mode
            IconButton(
                onClick = { viewModel.toggleFlash() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = when (uiState.flashMode) {
                        FlashMode.OFF -> Icons.Default.FlashOff
                        FlashMode.ON -> Icons.Default.FlashOn
                        FlashMode.AUTO -> Icons.Default.FlashAuto
                    },
                    contentDescription = "Flash Mode",
                    tint = if (uiState.flashMode != FlashMode.OFF) Color(0xFFFFD54F) else PureWhite
                )
            }

            // Grid Toggle
            IconButton(
                onClick = { viewModel.toggleGrid() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.GridOn,
                    contentDescription = "Grid",
                    tint = if (uiState.showGrid) GreenAccent else PureWhite
                )
            }

            // Camera Level Toggle
            IconButton(
                onClick = { viewModel.toggleLevel() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Straighten,
                    contentDescription = "Level",
                    tint = if (uiState.showLevel) GreenAccent else PureWhite
                )
            }

            // Flip Lens Front/Back
            IconButton(
                onClick = {
                    previewView?.let { pv ->
                        viewModel.toggleCameraLens(lifecycleOwner, pv)
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.FlipCameraAndroid,
                    contentDescription = "Flip Lens",
                    tint = PureWhite
                )
            }
        }

        // Active Multi-Media Session Banner (When in active multi-media session)
        if (!uiState.activeSessionId.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 54.dp, start = 12.dp, end = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BrandNavy.copy(alpha = 0.92f))
                    .border(1.dp, GreenAccent, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesomeMotion,
                    contentDescription = null,
                    tint = GreenAccent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Sesi: ${uiState.activeSessionId} (Media #${uiState.sessionIndex})",
                    color = PureWhite,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )

                if (uiState.sessionMediaCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(GreenAccent)
                            .clickable {
                                onEvidenceSaved(uiState.activeSessionId!!)
                            }
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Selesai",
                            color = BrandNavy,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // Top Status Badges Row (Normal mode)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 54.dp, start = 14.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GpsAccuracyBadge(gpsData = gpsData)

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Compass",
                        tint = GreenAccent,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = compassData.formattedCompass,
                        color = PureWhite,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // GPS Quality Indicator Alert (jika akurasi GPS buruk)
        if (uiState.isGpsWeak) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 96.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xD9B83B35))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "GPS Lemah",
                    tint = PureWhite,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "Akurasi GPS Lemah (${if (gpsData.accuracy > 0f) "±" + gpsData.accuracy.toInt() + "m" else "Mencari satelit"}). Bawa HP ke area terbuka.",
                    color = PureWhite,
                    fontSize = 9.5.sp
                )
            }
        }

        // Bottom Controls Container
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                    )
                )
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Mode Switcher: [ FOTO ] vs [ VIDEO ]
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, NavyBorder, RoundedCornerShape(20.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isPhotoMode = uiState.cameraMode == CameraMode.PHOTO
                val isVideoMode = uiState.cameraMode == CameraMode.VIDEO

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isPhotoMode) GreenAccent else Color.Transparent)
                        .clickable(enabled = !uiState.isRecordingVideo) {
                            previewView?.let { pv ->
                                viewModel.setCameraMode(CameraMode.PHOTO, lifecycleOwner, pv)
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📷 FOTO",
                        color = if (isPhotoMode) BrandNavy else PureWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isVideoMode) Color(0xFFFF3B30) else Color.Transparent)
                        .clickable(enabled = !uiState.isRecordingVideo) {
                            previewView?.let { pv ->
                                viewModel.setCameraMode(CameraMode.VIDEO, lifecycleOwner, pv)
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎥 VIDEO",
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Zoom Ratio Selector (1x, 2x, 3x)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ZoomPill(text = "1x", isSelected = uiState.zoomRatio == 1.0f) {
                    viewModel.setZoom(1.0f)
                }
                ZoomPill(text = "2x", isSelected = uiState.zoomRatio == 2.0f) {
                    viewModel.setZoom(2.0f)
                }
                ZoomPill(text = "3x", isSelected = uiState.zoomRatio == 3.0f) {
                    viewModel.setZoom(3.0f)
                }
            }

            // Category Chips Carousel
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(DocumentationCategory.entries) { category ->
                    CategoryChip(
                        category = category,
                        isSelected = uiState.selectedCategory == category,
                        onSelected = { viewModel.setCategory(it) }
                    )
                }
            }

            // Live Recording Timer Badge (when recording video)
            if (uiState.isRecordingVideo) {
                val durationStr = String.format(Locale.US, "%02d:%02d", uiState.recordingDurationSec / 60, uiState.recordingDurationSec % 60)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xD9FF3B30))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(PureWhite)
                    )
                    Text(
                        text = "REC $durationStr",
                        color = PureWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Shutter Button Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Exposure button
                IconButton(
                    onClick = {
                        val next = if (uiState.exposureIndex < uiState.exposureRange.second) uiState.exposureIndex + 1 else uiState.exposureRange.first
                        viewModel.setExposure(next)
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Brightness6,
                        contentDescription = "Exposure",
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Shutter / Record Button
                if (uiState.cameraMode == CameraMode.PHOTO) {
                    // Shutter Photo
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(4.dp, PureWhite, CircleShape)
                            .clickable(enabled = !uiState.isCapturing) {
                                viewModel.takePhoto {
                                    showPreviewModal = true
                                }
                            }
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isCapturing) {
                            CircularProgressIndicator(
                                color = PureWhite,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(GreenAccent)
                            )
                        }
                    }
                } else {
                    // Record Video Button
                    val isRec = uiState.isRecordingVideo
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(4.dp, if (isRec) Color(0xFFFF3B30) else PureWhite, CircleShape)
                            .clickable {
                                if (isRec) {
                                    viewModel.stopVideoRecording {
                                        showPreviewModal = true
                                    }
                                } else {
                                    viewModel.startVideoRecording()
                                }
                            }
                            .padding(if (isRec) 18.dp else 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRec) {
                            // Red square stop icon
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFF3B30))
                            )
                        } else {
                            // Red circle record icon
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3B30))
                            )
                        }
                    }
                }

                // Back / Close camera button
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup Kamera",
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CameraGridOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val gridColor = Color.White.copy(alpha = 0.35f)
        val stroke = 1.dp.toPx()

        // Vertical lines (3x3)
        drawLine(gridColor, Offset(width / 3f, 0f), Offset(width / 3f, height), stroke)
        drawLine(gridColor, Offset(2f * width / 3f, 0f), Offset(2f * width / 3f, height), stroke)

        // Horizontal lines (3x3)
        drawLine(gridColor, Offset(0f, height / 3f), Offset(width, height / 3f), stroke)
        drawLine(gridColor, Offset(0f, 2f * height / 3f), Offset(width, 2f * height / 3f), stroke)
    }
}

@Composable
fun ZoomPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) GreenAccent else Color.Black.copy(alpha = 0.6f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = PureWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
