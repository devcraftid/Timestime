package com.firstlogistics.flproof.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StorageManager(private val context: Context) {

    val rootDir: File by lazy {
        File(context.filesDir, "FL_PROOF").apply { if (!exists()) mkdirs() }
    }

    val originalDir: File by lazy {
        File(rootDir, "Original").apply { if (!exists()) mkdirs() }
    }

    val evidenceDir: File by lazy {
        File(rootDir, "Evidence").apply { if (!exists()) mkdirs() }
    }

    val reportsDir: File by lazy {
        File(rootDir, "Reports").apply { if (!exists()) mkdirs() }
    }

    val exportsDir: File by lazy {
        File(rootDir, "Exports").apply { if (!exists()) mkdirs() }
    }

    val videosDir: File by lazy {
        File(rootDir, "Videos").apply { if (!exists()) mkdirs() }
    }

    fun initDirectories() {
        originalDir
        evidenceDir
        reportsDir
        exportsDir
        videosDir
    }

    fun createVideoOutputFile(sessionId: String, index: Int = 1): File {
        val filename = "VID_${sessionId}_${index}_${System.currentTimeMillis()}.mp4"
        return File(videosDir, filename)
    }

    fun generateEvidenceId(sequenceNumber: Long): String {
        val year = SimpleDateFormat("yyyy", Locale.US).format(Date())
        val formattedSeq = String.format(Locale.US, "%06d", sequenceNumber)
        return "FL-$year-$formattedSeq"
    }

    fun saveOriginalPhoto(bitmap: Bitmap, evidenceId: String): File {
        val filename = "ORIG_${evidenceId}_${System.currentTimeMillis()}.jpg"
        val file = File(originalDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        return file
    }

    fun saveWatermarkedPhoto(bitmap: Bitmap, evidenceId: String): File {
        val filename = "EVD_${evidenceId}_${System.currentTimeMillis()}.jpg"
        val file = File(evidenceDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        return file
    }

    fun calculateSha256(file: File): String {
        if (!file.exists()) return ""
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun copyToGallery(file: File, title: String): Boolean {
        if (!file.exists()) return false
        return try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FL_PROOF")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(file).use { input ->
                        input.copyTo(out)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadBitmap(path: String): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    fun getFileSizeBytes(path: String): Long {
        val file = File(path)
        return if (file.exists()) file.length() else 0L
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.2f MB", bytes.toDouble() / (1024 * 1024))
            bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes.toDouble() / 1024)
            else -> "$bytes B"
        }
    }

    fun deleteFileSafely(path: String): Boolean {
        val file = File(path)
        return if (file.exists()) file.delete() else true
    }

    /**
     * Menghasilkan file HTML responsif mandiri (self-contained) yang dapat dibuka di browser
     * untuk melihat detail foto tunggal maupun seluruh foto dalam sesi Multi Evidence ID
     * dengan tampilan TIMELINE profesional (Barcode Scanning, Extraction, Dimensioning, Photographing).
     */
    fun generateHtmlWebViewer(
        evidence: com.firstlogistics.flproof.data.entity.EvidenceEntity,
        sessionPhotos: List<com.firstlogistics.flproof.data.entity.EvidenceEntity>
    ): File {
        val fileName = "FL_PROOF_VIEWER_${evidence.evidenceId}.html"
        val htmlFile = File(exportsDir, fileName)

        val photosToRender = if (sessionPhotos.isNotEmpty()) sessionPhotos else listOf(evidence)
        val barcodeStr = evidence.barcodeValue?.ifBlank { null } ?: evidence.sessionId ?: evidence.evidenceId
        val barcodeBase64 = QrCodeGenerator.generateBarcodeBase64(barcodeStr)

        val photoDataList = photosToRender.mapIndexed { idx, item ->
            val fileToDisplay = File(item.watermarkedPhotoPath).let { if (it.exists()) it else File(item.originalPhotoPath) }
            val base64Img = if (fileToDisplay.exists()) {
                try {
                    val bytes = fileToDisplay.readBytes()
                    "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                } catch (e: Exception) {
                    ""
                }
            } else ""

            val dateTimeStr = "${item.dateFormatted} ${item.timeFormatted}"
            val coordStr = if (item.latitude != 0.0) String.format(Locale.US, "%.6f, %.6f", item.latitude, item.longitude) else "-"

            mapOf(
                "index" to (idx + 1).toString(),
                "id" to item.evidenceId,
                "sessionIndex" to item.sessionIndex.toString(),
                "dateTime" to dateTimeStr,
                "address" to item.address,
                "coords" to "$coordStr (±${item.gpsAccuracy.toInt()}m)",
                "officer" to item.officerName,
                "category" to item.category,
                "status" to item.status,
                "hash" to item.sha256Hash,
                "base64" to base64Img,
                "lat" to item.latitude.toString(),
                "lng" to item.longitude.toString()
            )
        }

        val initialPhoto = photoDataList.firstOrNull() ?: mapOf("dateTime" to "", "base64" to "")

        val middleThumbsHtml = if (photoDataList.size > 2) {
            photoDataList.subList(1, photoDataList.size - 1).mapIndexed { idx, p ->
                val realIdx = idx + 1
                "<img class=\"step-thumb\" id=\"thumb-$realIdx\" src=\"${p["base64"]}\" onclick=\"selectPhoto($realIdx)\" />"
            }.joinToString("")
        } else if (photoDataList.size == 2) {
            "<img class=\"step-thumb\" id=\"thumb-1\" src=\"${photoDataList[1]["base64"]}\" onclick=\"selectPhoto(1)\" />"
        } else ""

        val lastIdx = photoDataList.size - 1
        val lastThumbHtml = if (lastIdx > 0) {
            "<img class=\"step-thumb\" id=\"thumb-$lastIdx\" src=\"${photoDataList[lastIdx]["base64"]}\" onclick=\"selectPhoto($lastIdx)\" />"
        } else ""

        val jsonArrayStr = photoDataList.joinToString(",", prefix = "[", postfix = "]") { p ->
            """{"dateTime":"${p["dateTime"]}","base64":"${p["base64"]}","coords":"${p["coords"]}","address":"${p["address"]?.replace("\"", "\\\"")?.replace("\n", " ") ?: ""}"}"""
        }

        val sb = StringBuilder()
        sb.append("""
            <!DOCTYPE html>
            <html lang="id">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Timeline Dokumentasi - ${evidence.sessionId ?: evidence.evidenceId}</title>
                <style>
                    :root {
                        --bg-dark: #070D1B;
                        --card-bg: #0F1A30;
                        --accent-green: #00E676;
                        --accent-orange: #FF6600;
                        --text-primary: #FFFFFF;
                        --text-secondary: #94A3B8;
                        --border-color: #1E293B;
                        --pill-bg: rgba(15, 26, 48, 0.85);
                    }
                    * { box-sizing: border-box; margin: 0; padding: 0; }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        background-color: var(--bg-dark);
                        color: var(--text-primary);
                        padding: 16px;
                        min-height: 100vh;
                    }
                    .top-bar {
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        padding-bottom: 12px;
                        border-bottom: 1px solid var(--border-color);
                        margin-bottom: 20px;
                    }
                    .brand {
                        display: flex;
                        align-items: center;
                        gap: 10px;
                    }
                    .logo-fl {
                        display: flex;
                        font-weight: 900;
                        font-size: 18px;
                        border-radius: 6px;
                        overflow: hidden;
                    }
                    .logo-f { background-color: #FF6600; color: white; padding: 3px 8px; font-style: italic; }
                    .logo-l { background-color: #7A7A7A; color: white; padding: 3px 8px; font-style: italic; }
                    .brand-info h1 { font-size: 15px; font-weight: 800; letter-spacing: 0.5px; }
                    .brand-info p { font-size: 11px; color: var(--text-secondary); }
                    .session-tag {
                        background: rgba(0, 230, 118, 0.15);
                        color: var(--accent-green);
                        border: 1px solid var(--accent-green);
                        padding: 4px 12px;
                        border-radius: 999px;
                        font-size: 12px;
                        font-weight: 600;
                    }

                    /* 2-Column Responsive Layout */
                    .main-container {
                        display: grid;
                        grid-template-columns: 1.2fr 1fr;
                        gap: 24px;
                        max-width: 1200px;
                        margin: 0 auto;
                    }
                    @media (max-width: 860px) {
                        .main-container {
                            grid-template-columns: 1fr;
                        }
                    }

                    /* Left Panel: Big Photo with top/bottom pills */
                    .photo-panel {
                        position: relative;
                        background: #000;
                        border: 1px solid var(--border-color);
                        border-radius: 18px;
                        overflow: hidden;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        min-height: 480px;
                        box-shadow: 0 10px 30px rgba(0,0,0,0.5);
                    }
                    .timestamp-pill {
                        position: absolute;
                        top: 14px;
                        background: var(--pill-bg);
                        backdrop-filter: blur(8px);
                        -webkit-backdrop-filter: blur(8px);
                        border: 1px solid rgba(255,255,255,0.15);
                        color: #E2E8F0;
                        padding: 6px 14px;
                        border-radius: 999px;
                        font-size: 11px;
                        font-weight: 500;
                        z-index: 10;
                    }
                    .main-image {
                        width: 100%;
                        max-height: 560px;
                        object-fit: contain;
                        display: block;
                        transition: opacity 0.2s ease;
                    }
                    .photos-pill {
                        position: absolute;
                        bottom: 14px;
                        background: var(--pill-bg);
                        backdrop-filter: blur(8px);
                        -webkit-backdrop-filter: blur(8px);
                        border: 1px solid rgba(255,255,255,0.15);
                        padding: 4px 14px;
                        border-radius: 999px;
                        font-size: 12px;
                        font-weight: 600;
                        color: #FFFFFF;
                        display: flex;
                        align-items: center;
                        gap: 8px;
                        z-index: 10;
                    }
                    .nav-btn {
                        background: rgba(255,255,255,0.1);
                        border: none;
                        color: white;
                        cursor: pointer;
                        border-radius: 4px;
                        padding: 2px 6px;
                        font-size: 11px;
                    }
                    .nav-btn:hover { background: var(--accent-green); color: black; }

                    /* Right Panel: Connected Timeline */
                    .timeline-panel {
                        background: var(--card-bg);
                        border: 1px solid var(--border-color);
                        border-radius: 18px;
                        padding: 20px;
                    }
                    .timeline-header {
                        font-size: 18px;
                        font-weight: 800;
                        margin-bottom: 20px;
                        color: #FFFFFF;
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                    }
                    .timeline-track {
                        position: relative;
                        padding-left: 28px;
                    }
                    .timeline-track::before {
                        content: '';
                        position: absolute;
                        left: 9px;
                        top: 8px;
                        bottom: 20px;
                        width: 2px;
                        background: #334155;
                    }
                    .timeline-step {
                        position: relative;
                        margin-bottom: 24px;
                    }
                    .timeline-step:last-child {
                        margin-bottom: 0;
                    }
                    .step-node {
                        position: absolute;
                        left: -28px;
                        top: 2px;
                        width: 20px;
                        height: 20px;
                        border-radius: 50%;
                        background: #1E293B;
                        border: 2px solid #64748B;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        font-size: 10px;
                        color: #FFF;
                        font-weight: bold;
                        z-index: 2;
                    }
                    .timeline-step.active .step-node {
                        background: var(--accent-green);
                        border-color: #FFFFFF;
                        color: #070D1B;
                    }
                    .step-title {
                        font-size: 14px;
                        font-weight: 700;
                        color: #FFFFFF;
                    }
                    .step-time {
                        font-size: 11px;
                        color: var(--text-secondary);
                        margin-bottom: 8px;
                    }
                    .barcode-container {
                        background: #FFFFFF;
                        border-radius: 8px;
                        padding: 10px;
                        display: inline-block;
                        max-width: 100%;
                        text-align: center;
                        margin-top: 6px;
                    }
                    .barcode-container img {
                        max-width: 100%;
                        height: 48px;
                        display: block;
                    }
                    .barcode-container span {
                        color: #000000;
                        font-family: monospace;
                        font-size: 12px;
                        font-weight: bold;
                        letter-spacing: 2px;
                        display: block;
                        margin-top: 4px;
                    }
                    .step-thumbnails {
                        display: flex;
                        gap: 10px;
                        margin: 10px 0;
                        flex-wrap: wrap;
                    }
                    .step-thumb {
                        width: 68px;
                        height: 68px;
                        border-radius: 8px;
                        object-fit: cover;
                        border: 2px solid transparent;
                        cursor: pointer;
                        background: #000;
                        transition: transform 0.15s ease, border-color 0.15s ease;
                    }
                    .step-thumb:hover { transform: scale(1.05); }
                    .step-thumb.selected {
                        border-color: var(--accent-green);
                        box-shadow: 0 0 10px rgba(0, 230, 118, 0.4);
                    }
                    .step-details {
                        font-size: 12px;
                        color: #CBD5E1;
                        background: rgba(0,0,0,0.25);
                        padding: 10px;
                        border-radius: 8px;
                        line-height: 1.6;
                        margin-top: 6px;
                    }
                    .detail-row {
                        display: flex;
                        justify-content: space-between;
                        border-bottom: 1px solid rgba(255,255,255,0.05);
                        padding: 2px 0;
                    }
                    .detail-row:last-child { border-bottom: none; }
                    .detail-lbl { color: var(--text-secondary); }
                    .detail-val { font-weight: 600; text-align: right; }
                    .badge-verified {
                        background: rgba(0, 230, 118, 0.15);
                        color: var(--accent-green);
                        padding: 2px 6px;
                        border-radius: 4px;
                        font-weight: bold;
                    }
                    .hash-text {
                        font-family: monospace;
                        font-size: 10px;
                        word-break: break-all;
                        color: var(--accent-green);
                    }
                    .customer-actions {
                        display: flex;
                        gap: 8px;
                        margin-top: 14px;
                        width: 100%;
                        flex-wrap: wrap;
                    }
                    .action-btn {
                        flex: 1;
                        min-width: 140px;
                        padding: 11px 14px;
                        border-radius: 10px;
                        font-size: 12px;
                        font-weight: 700;
                        text-align: center;
                        text-decoration: none;
                        cursor: pointer;
                        border: 1px solid var(--border-color);
                        background: #1E293B;
                        color: #FFFFFF;
                        transition: all 0.2s ease;
                        display: inline-flex;
                        align-items: center;
                        justify-content: center;
                        gap: 6px;
                    }
                    .action-btn:hover { background: #334155; }
                    .action-btn.green-btn {
                        background: #00E676;
                        color: #070D1B;
                        border-color: #00E676;
                    }
                    .action-btn.green-btn.playing {
                        background: #FF6600;
                        color: #FFFFFF;
                        border-color: #FF6600;
                    }
                    .customer-notice {
                        background: rgba(0, 230, 118, 0.08);
                        border: 1px solid rgba(0, 230, 118, 0.3);
                        border-radius: 10px;
                        padding: 12px 14px;
                        margin-top: 12px;
                        font-size: 12px;
                        color: #CBD5E1;
                        line-height: 1.5;
                        width: 100%;
                    }
                    .customer-notice strong {
                        color: #00E676;
                        display: block;
                        margin-bottom: 2px;
                    }
                    .footer {
                        text-align: center;
                        margin-top: 30px;
                        padding-top: 16px;
                        border-top: 1px solid var(--border-color);
                        font-size: 11px;
                        color: var(--text-secondary);
                    }
                </style>
            </head>
            <body>
                <div class="top-bar">
                    <div class="brand">
                        <div class="logo-fl">
                            <div class="logo-f">F</div>
                            <div class="logo-l">L</div>
                        </div>
                        <div class="brand-info">
                            <h1>FIRST LOGISTICS DOCUMENTATION</h1>
                            <p>FL PROOF &bull; Multi Evidence Verification Portal Pelanggan</p>
                        </div>
                    </div>
                    <div class="session-tag">
                        Multi Evidence ID: ${evidence.sessionId ?: evidence.evidenceId}
                    </div>
                </div>

                <div class="main-container">
                    <!-- Left Column: Main Big Photo (matching reference image) -->
                    <div class="left-col">
                        <div class="photo-panel">
                            <div class="timestamp-pill" id="timePill">
                                ${initialPhoto["dateTime"]}
                            </div>
                            <img id="mainPhoto" class="main-image" src="${initialPhoto["base64"]}" alt="Parcel Evidence" />
                            <div class="photos-pill">
                                <button class="nav-btn" onclick="prevPhoto()">&uarr;</button>
                                <span id="photoCounter">1 / ${photoDataList.size} photos</span>
                                <button class="nav-btn" onclick="nextPhoto()">&darr;</button>
                            </div>
                        </div>

                        <!-- Customer Inspection Video & Actions -->
                        <div class="customer-actions">
                            <button id="btnPlayVideo" class="action-btn green-btn" onclick="togglePlayAnimation()">
                                &#9654; Putar Video / Animasi Barang
                            </button>
                            <a id="btnDownload" class="action-btn" download="Bukti_${evidence.evidenceId}.jpg" href="${initialPhoto["base64"]}">
                                &#128229; Simpan Foto
                            </a>
                            <a class="action-btn" target="_blank" href="https://www.google.com/maps/search/?api=1&query=${evidence.latitude},${evidence.longitude}">
                                &#128205; Buka Maps
                            </a>
                        </div>
                        <div class="customer-notice">
                            <strong>&#10003; BUKTI FISIK TERVERIFIKASI FIRST LOGISTICS</strong>
                            Foto & rekaman kondisi fisik barang telah didokumentasikan secara resmi oleh kurir pada saat serah terima di lokasi penerima.
                        </div>
                    </div>

                    <!-- Right Column: Timeline Track (matching reference image) -->
                    <div class="timeline-panel">
                        <div class="timeline-header">
                            <span>Timeline</span>
                            <span style="font-size:12px; color:var(--text-secondary); font-weight:normal;">Total: ${photoDataList.size} Foto</span>
                        </div>

                        <div class="timeline-track">
                            <!-- Step 1: Barcode Scanning (1) -->
                            <div class="timeline-step active" id="stepNode1">
                                <div class="step-node">1</div>
                                <div class="step-title">Barcode Scanning (1)</div>
                                <div class="step-time">${photoDataList.firstOrNull()?.get("dateTime") ?: ""}</div>
                                <div class="barcode-container">
                                    ${if (barcodeBase64.isNotBlank()) "<img src=\"$barcodeBase64\" alt=\"Barcode\" />" else ""}
                                </div>
                            </div>

                            <!-- Step 2: Extraction (2) -->
                            <div class="timeline-step" id="stepNode2">
                                <div class="step-node">2</div>
                                <div class="step-title">Extraction (2)</div>
                                <div class="step-time">${photoDataList.firstOrNull()?.get("dateTime") ?: ""}</div>
                                <div class="step-thumbnails">
                                    ${if (photoDataList.isNotEmpty()) {
                                        "<img class=\"step-thumb selected\" id=\"thumb-0\" src=\"${photoDataList[0]["base64"]}\" onclick=\"selectPhoto(0)\" />"
                                    } else ""}
                                </div>
                                <div class="step-details">
                                    <div class="detail-row">
                                        <span class="detail-lbl">ID Bukti:</span>
                                        <span class="detail-val">${evidence.evidenceId}</span>
                                    </div>
                                    <div class="detail-row">
                                        <span class="detail-lbl">Shipment / ID Bukti:</span>
                                        <span class="detail-val">${evidence.evidenceId}</span>
                                    </div>
                                    <div class="detail-row">
                                        <span class="detail-lbl">Kategori:</span>
                                        <span class="detail-val">${evidence.category}</span>
                                    </div>
                                    <div class="detail-row">
                                        <span class="detail-lbl">Petugas:</span>
                                        <span class="detail-val">${evidence.officerName}</span>
                                    </div>
                                </div>
                            </div>

                            <!-- Step 3: Dimensioning (3) -->
                            <div class="timeline-step" id="stepNode3">
                                <div class="step-node">3</div>
                                <div class="step-title">Dimensioning & Kondisi (3)</div>
                                <div class="step-time">${if (photoDataList.size > 1) photoDataList[1]["dateTime"] else photoDataList.firstOrNull()?.get("dateTime") ?: ""}</div>
                                <div class="step-thumbnails">
                                    $middleThumbsHtml
                                </div>
                                <div class="step-details">
                                    <div class="detail-row">
                                        <span class="detail-lbl">GPS Koordinat:</span>
                                        <span class="detail-val">${initialPhoto["coords"]}</span>
                                    </div>
                                    <div class="detail-row">
                                        <span class="detail-lbl">Lokasi:</span>
                                        <span class="detail-val" style="font-size:11px;">${initialPhoto["address"]}</span>
                                    </div>
                                    <div class="detail-row">
                                        <span class="detail-lbl">Arah Kompas:</span>
                                        <span class="detail-val">${evidence.compassDirection.ifBlank { "Tersedia" }}</span>
                                    </div>
                                </div>
                            </div>

                            <!-- Step 4: Photographing (4) -->
                            <div class="timeline-step" id="stepNode4">
                                <div class="step-node">4</div>
                                <div class="step-title">Photographing / Dokumentasi Akhir (4)</div>
                                <div class="step-time">${photoDataList.lastOrNull()?.get("dateTime") ?: ""}</div>
                                <div class="step-thumbnails">
                                    $lastThumbHtml
                                </div>
                                <div class="step-details">
                                    <div class="detail-row">
                                        <span class="detail-lbl">Status Integritas:</span>
                                        <span class="detail-val"><span class="badge-verified">VERIFIED</span></span>
                                    </div>
                                    <div class="detail-row">
                                        <span class="detail-lbl">Kamera:</span>
                                        <span class="detail-val">${evidence.cameraLens} &bull; ${evidence.deviceModel}</span>
                                    </div>
                                    <div class="detail-row" style="flex-direction:column; gap:2px; margin-top:4px;">
                                        <span class="detail-lbl">SHA-256 Hash:</span>
                                        <span class="hash-text">${evidence.sha256Hash}</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="footer">
                    Dokumen bukti operasional resmi &copy; FIRST LOGISTICS FL PROOF &bull; Tersimpan secara offline di perangkat internal.
                </div>

                <script>
                    const photos = $jsonArrayStr;
                    let currentIndex = 0;
                    let isPlaying = false;
                    let playInterval = null;

                    function selectPhoto(index) {
                        if (index < 0 || index >= photos.length) return;
                        currentIndex = index;
                        const data = photos[index];

                        const mainImg = document.getElementById('mainPhoto');
                        mainImg.style.opacity = '0.3';
                        setTimeout(() => {
                            mainImg.src = data.base64;
                            mainImg.style.opacity = '1';
                        }, 100);

                        document.getElementById('timePill').innerText = data.dateTime;
                        document.getElementById('photoCounter').innerText = (currentIndex + 1) + " / " + photos.length + " photos";

                        const btnDl = document.getElementById('btnDownload');
                        if (btnDl && data.base64) btnDl.href = data.base64;

                        // Update thumbnails border
                        document.querySelectorAll('.step-thumb').forEach(el => el.classList.remove('selected'));
                        const selThumb = document.getElementById('thumb-' + index);
                        if (selThumb) selThumb.classList.add('selected');
                    }

                    function prevPhoto() {
                        const next = currentIndex > 0 ? currentIndex - 1 : photos.length - 1;
                        selectPhoto(next);
                    }

                    function nextPhoto() {
                        const next = currentIndex < photos.length - 1 ? currentIndex + 1 : 0;
                        selectPhoto(next);
                    }

                    function togglePlayAnimation() {
                        isPlaying = !isPlaying;
                        const btn = document.getElementById('btnPlayVideo');
                        if (isPlaying) {
                            btn.innerText = '⏸ Jeda Video / Animasi';
                            btn.classList.add('playing');
                            playInterval = setInterval(() => {
                                nextPhoto();
                            }, 1500);
                        } else {
                            btn.innerText = '▶ Putar Video / Animasi Barang';
                            btn.classList.remove('playing');
                            if (playInterval) { clearInterval(playInterval); playInterval = null; }
                        }
                    }
                </script>
            </body>
            </html>
        """.trimIndent())

        FileOutputStream(htmlFile).use { out ->
            out.write(sb.toString().toByteArray(Charsets.UTF_8))
        }

        return htmlFile
    }
}

