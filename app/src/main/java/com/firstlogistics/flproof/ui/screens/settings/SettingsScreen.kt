package com.firstlogistics.flproof.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firstlogistics.flproof.data.model.WatermarkPosition
import com.firstlogistics.flproof.data.model.WatermarkSize
import com.firstlogistics.flproof.ui.components.ConfirmationDialog
import com.firstlogistics.flproof.ui.components.FLProofTopBar
import com.firstlogistics.flproof.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var tempPin by remember { mutableStateOf("") }
    var showBackupConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            FLProofTopBar(
                title = "Pengaturan",
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Section 1: Profil & Identitas
            SettingsSectionHeader(title = "Identitas Petugas & Perusahaan", icon = Icons.Default.Badge)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NavySurface)
                    .border(1.dp, NavyBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = uiState.companyName,
                        onValueChange = { viewModel.updateCompanyName(it) },
                        label = { Text("Nama Perusahaan") },
                        singleLine = true,
                        colors = outlinedFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = uiState.officerName,
                        onValueChange = { viewModel.updateOfficerName(it) },
                        label = { Text("Nama Petugas / Operator") },
                        singleLine = true,
                        colors = outlinedFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = uiState.officerId,
                        onValueChange = { viewModel.updateOfficerId(it) },
                        label = { Text("Nomor ID Petugas (Opsional)") },
                        singleLine = true,
                        colors = outlinedFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Section 2: Watermark Kustom
            SettingsSectionHeader(title = "Pengaturan Smart Watermark", icon = Icons.AutoMirrored.Filled.BrandingWatermark)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NavySurface)
                    .border(1.dp, NavyBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Position Selector
                    Text("Posisi Panel Watermark", fontSize = 12.sp, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WatermarkPosition.entries.forEach { pos ->
                            val isSel = uiState.watermarkPosition == pos
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) GreenAccent else NavyCard)
                                    .clickable { viewModel.updateWatermarkPosition(pos) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pos.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) NavyBackground else TextWhite
                                )
                            }
                        }
                    }

                    // Size Selector
                    Text("Ukuran Watermark", fontSize = 12.sp, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WatermarkSize.entries.forEach { sz ->
                            val isSel = uiState.watermarkSize == sz
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) GreenAccent else NavyCard)
                                    .clickable { viewModel.updateWatermarkSize(sz) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = sz.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) NavyBackground else TextWhite
                                )
                            }
                        }
                    }

                    HorizontalDivider(thickness = 1.dp, color = NavyBorder)

                    // Switches
                    SettingsSwitchRow(
                        title = "Tampilkan Alamat Lengkap",
                        subtitle = "Sertakan reverse-geocoding alamat pada watermark",
                        checked = uiState.showAddress,
                        onCheckedChange = { viewModel.toggleShowAddress(it) }
                    )

                    SettingsSwitchRow(
                        title = "Tampilkan Akurasi GPS",
                        subtitle = "Sertakan toleransi meter akurasi koordinat",
                        checked = uiState.showAccuracy,
                        onCheckedChange = { viewModel.toggleShowAccuracy(it) }
                    )

                    SettingsSwitchRow(
                        title = "Tampilkan Arah Kompas",
                        subtitle = "Sertakan azimuth dan arah mata angin perangkat",
                        checked = uiState.showCompass,
                        onCheckedChange = { viewModel.toggleShowCompass(it) }
                    )

                    HorizontalDivider(thickness = 1.dp, color = NavyBorder)

                    // Integrasi Vercel & Supabase
                    HorizontalDivider(thickness = 1.dp, color = NavyBorder)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Hosting Vercel Web Portal", fontSize = 12.sp, color = TextSecondary)
                        OutlinedTextField(
                            value = uiState.vercelPortalUrl,
                            onValueChange = { viewModel.updateVercelPortalUrl(it) },
                            placeholder = { Text("https://flproof-evidence.vercel.app/evidence", fontSize = 11.sp, color = TextTertiary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreenAccent,
                                unfocusedBorderColor = NavyBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedContainerColor = NavyCard,
                                unfocusedContainerColor = NavyCard
                            )
                        )
                        Text(
                            "Link Vercel yang akan dibuka saat customer men-scan QR code foto.",
                            fontSize = 10.sp,
                            color = TextTertiary
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Supabase Project URL", fontSize = 12.sp, color = TextSecondary)
                        OutlinedTextField(
                            value = uiState.supabaseUrl,
                            onValueChange = { viewModel.updateSupabaseUrl(it) },
                            placeholder = { Text("https://your-project.supabase.co", fontSize = 11.sp, color = TextTertiary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreenAccent,
                                unfocusedBorderColor = NavyBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedContainerColor = NavyCard,
                                unfocusedContainerColor = NavyCard
                            )
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Supabase Anon Key", fontSize = 12.sp, color = TextSecondary)
                        OutlinedTextField(
                            value = uiState.supabaseAnonKey,
                            onValueChange = { viewModel.updateSupabaseAnonKey(it) },
                            placeholder = { Text("eyJhbGciOiJIUzI1NiIsInR5cCI6...", fontSize = 11.sp, color = TextTertiary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreenAccent,
                                unfocusedBorderColor = NavyBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedContainerColor = NavyCard,
                                unfocusedContainerColor = NavyCard
                            )
                        )
                        Text(
                            "Kunci API Anon publik Supabase untuk mengunggah foto/video & sync database.",
                            fontSize = 10.sp,
                            color = TextTertiary
                        )
                    }
                }
            }

            // Section 3: Kamera & Audio
            SettingsSectionHeader(title = "Kamera & Penyimpanan", icon = Icons.Default.CameraAlt)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NavySurface)
                    .border(1.dp, NavyBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingsSwitchRow(
                        title = "Suara Shutter Kamera",
                        subtitle = "Bunyikan efek suara klik saat memotret",
                        checked = uiState.isShutterSoundEnabled,
                        onCheckedChange = { viewModel.toggleShutterSound(it) }
                    )

                    HorizontalDivider(thickness = 1.dp, color = NavyBorder)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Lokasi Penyimpanan Lokal", fontSize = 11.sp, color = TextSecondary)
                        Text(uiState.storagePath, fontSize = 12.sp, color = GreenAccent)
                        Text("Semua foto asli dan watermark tersimpan aman di internal storage.", fontSize = 10.sp, color = TextTertiary)
                    }
                }
            }

            // Section 4: Keamanan & PIN
            SettingsSectionHeader(title = "Keamanan & PIN Aplikasi", icon = Icons.Default.Security)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NavySurface)
                    .border(1.dp, NavyBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingsSwitchRow(
                        title = "Kunci PIN Aplikasi",
                        subtitle = "Meminta kode PIN setiap aplikasi dibuka",
                        checked = uiState.isPinLockEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && uiState.appPin.isEmpty()) {
                                showPinDialog = true
                            } else {
                                viewModel.togglePinLock(enabled)
                            }
                        }
                    )

                    if (uiState.isPinLockEnabled) {
                        Button(
                            onClick = { showPinDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyCard, contentColor = GreenAccent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Ubah Kode PIN Keamanan")
                        }
                    }
                }
            }

            // Section 5: Backup & Restore
            SettingsSectionHeader(title = "Backup & Pemulihan Lokal", icon = Icons.Default.Backup)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NavySurface)
                    .border(1.dp, NavyBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Buat cadangan lengkap database dan seluruh foto bukti dokumentasi ke dalam file ZIP tunggal.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Button(
                        onClick = { viewModel.createLocalBackup(context) },
                        enabled = !uiState.isBackingUp,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenAccent, contentColor = NavyBackground),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isBackingUp) {
                            CircularProgressIndicator(color = NavyBackground, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Buat File Backup Lengkap (.zip)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Section 6: Tentang Aplikasi
            SettingsSectionHeader(title = "Tentang Aplikasi", icon = Icons.Default.Info)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NavySurface)
                    .border(1.dp, NavyBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.firstlogistics.flproof.R.drawable.logo_fl),
                        contentDescription = "First Logistics Logo",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("FL PROOF - Evidence Camera", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("FIRST LOGISTICS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenAccent)
                        Text("Versi: 1.0.0 Enterprise Native", fontSize = 11.sp, color = TextSecondary)
                        Text("Tagline: Capture. Verify. Document.", fontSize = 10.sp, color = TextTertiary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // PIN Change Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Atur PIN Keamanan (4-6 Angka)", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempPin,
                    onValueChange = { if (it.length <= 6) tempPin = it },
                    label = { Text("Kode PIN Baru") },
                    singleLine = true,
                    colors = outlinedFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempPin.length >= 4) {
                            viewModel.setAppPin(tempPin)
                            viewModel.togglePinLock(true)
                            showPinDialog = false
                            tempPin = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenAccent, contentColor = NavyBackground)
                ) {
                    Text("Simpan PIN", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Batal", color = TextWhite)
                }
            },
            containerColor = NavySurface
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = GreenAccent, modifier = Modifier.size(18.dp))
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, color = TextWhite, fontWeight = FontWeight.Medium)
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PureWhite,
                checkedTrackColor = GreenAccent,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = NavyBorder
            )
        )
    }
}

@Composable
fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextWhite,
    unfocusedTextColor = TextWhite,
    focusedBorderColor = GreenAccent,
    unfocusedBorderColor = NavyBorder,
    focusedLabelColor = GreenAccent,
    unfocusedLabelColor = TextSecondary,
    cursorColor = GreenAccent
)

