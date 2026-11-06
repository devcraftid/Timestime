package com.firstlogistics.flproof.ui.screens.camera

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.firstlogistics.flproof.camera.QualityCheckResult
import com.firstlogistics.flproof.data.model.DocumentationCategory
import com.firstlogistics.flproof.ui.components.CategoryChip
import com.firstlogistics.flproof.ui.theme.*
import java.io.File
import java.util.Locale

@Composable
fun CameraPreviewScreen(
    previewBitmap: Bitmap? = null,
    videoFile: File? = null,
    videoDurationSec: Int = 0,
    sessionId: String? = null,
    sessionIndex: Int = 1,
    category: DocumentationCategory,
    qualityCheck: QualityCheckResult?,
    onCategoryChange: (DocumentationCategory) -> Unit,
    onRetake: () -> Unit,
    onSaveAndContinue: (notes: String, nextMode: CameraMode) -> Unit,
    onSaveAndFinish: (notes: String) -> Unit
) {
    val context = LocalContext.current
    var notesText by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val isVideo = videoFile != null && previewBitmap == null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Session indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isVideo) "Verifikasi Rekaman Video" else "Verifikasi Bukti Foto",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy
                    )
                    if (!sessionId.isNullOrBlank()) {
                        Text(
                            text = "Sesi: $sessionId (Media #$sessionIndex)",
                            fontSize = 12.sp,
                            color = GreenAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isVideo) Color(0x33FF6600) else GreenAccentMuted)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isVideo) "VIDEO MP4" else "WATERMARK APPLIED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isVideo) Color(0xFFFF6600) else GreenAccent
                    )
                }
            }

            // Media Preview Container
            if (isVideo && videoFile != null) {
                // Video Review Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(NavySurface)
                        .border(1.dp, NavyBorder, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30).copy(alpha = 0.2f))
                                .border(2.dp, Color(0xFFFF3B30), CircleShape)
                                .clickable {
                                    try {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            videoFile
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "video/mp4")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Tidak dapat memutar video: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Video",
                                tint = Color(0xFFFF3B30),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Text(
                            text = "Rekaman Video Siap",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandNavy
                        )

                        val durationStr = String.format(Locale.US, "%02d:%02d", videoDurationSec / 60, videoDurationSec % 60)
                        val sizeMb = String.format(Locale.US, "%.1f MB", videoFile.length().toDouble() / (1024 * 1024))
                        Text(
                            text = "Durasi: $durationStr • Ukuran: $sizeMb",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Button(
                            onClick = {
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        videoFile
                                    )
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "video/mp4")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = PureWhite),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Putar Preview Video", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (previewBitmap != null) {
                // Photo Preview with Watermark applied
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(NavySurface)
                        .border(1.dp, NavyBorder, RoundedCornerShape(14.dp))
                ) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Preview Watermark",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Quality Check Card (for photos)
            if (qualityCheck != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NavySurface)
                        .border(
                            1.dp,
                            if (qualityCheck.isGoodQuality) GreenAccentBorder else StatusWarning.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (qualityCheck.isGoodQuality) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (qualityCheck.isGoodQuality) GreenAccent else StatusWarning,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Pemeriksaan Kualitas Foto:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandNavy
                            )
                        }

                        Text(
                            text = "• ${qualityCheck.lightingStatus} • ${qualityCheck.sharpnessStatus}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        Text(
                            text = qualityCheck.recommendation,
                            fontSize = 11.sp,
                            color = if (qualityCheck.isGoodQuality) GreenAccent else StatusWarning
                        )
                    }
                }
            }

            // Category Selection
            Text(
                text = "Kategori Dokumentasi",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = BrandNavy
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(DocumentationCategory.entries) { cat ->
                    CategoryChip(
                        category = cat,
                        isSelected = category == cat,
                        onSelected = { onCategoryChange(it) }
                    )
                }
            }

            // Optional Notes Field
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("Catatan Tambahan (Opsional)") },
                placeholder = { Text("Contoh: Dus atas sedikit robek saat serah terima") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = NavyBorder,
                    focusedLabelColor = BrandBlue,
                    unfocusedLabelColor = TextSecondary,
                    cursorColor = BrandBlue
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Multi-Media Action Options (1 QR = Banyak Foto & Video)
            Text(
                text = "Pilih Aksi Dokumentasi (Satu Sesi ID):",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = BrandNavy
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button + Tambah Foto
                OutlinedButton(
                    onClick = {
                        isSaving = true
                        onSaveAndContinue(notesText, CameraMode.PHOTO)
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenAccent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GreenAccent)
                ) {
                    Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "+ Foto Lagi", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                // Button + Tambah Video
                OutlinedButton(
                    onClick = {
                        isSaving = true
                        onSaveAndContinue(notesText, CameraMode.VIDEO)
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6600)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6600))
                ) {
                    Icon(imageVector = Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "+ Video", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                // Retake button
                OutlinedButton(
                    onClick = onRetake,
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(0.9f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Ulang", fontSize = 11.5.sp)
                }
            }

            // Selesai & Simpan Button (Main action to finalize session)
            Button(
                onClick = {
                    isSaving = true
                    onSaveAndFinish(notesText)
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandNavy,
                    contentColor = PureWhite
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = PureWhite,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Menyimpan & Menyinkronkan...", color = PureWhite, fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp), tint = PureWhite)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Selesai & Simpan Bukti Sesi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PureWhite
                    )
                }
            }
        }
    }
}
