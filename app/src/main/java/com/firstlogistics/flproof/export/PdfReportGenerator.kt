package com.firstlogistics.flproof.export

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.firstlogistics.flproof.data.entity.EvidenceEntity
import com.firstlogistics.flproof.utils.DateFormatter
import com.firstlogistics.flproof.utils.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfReportGenerator(
    private val context: Context,
    private val storageManager: StorageManager
) {

    suspend fun generateReport(
        title: String,
        records: List<EvidenceEntity>,
        companyName: String = "FIRST LOGISTICS",
        officerName: String = "Petugas Lapangan"
    ): File? = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // Standard A4 points (8.27 x 11.69 inches)
            val pageHeight = 842
            val margin = 36f

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#070D1B")
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#10B981")
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#334155")
                textSize = 10f
            }

            val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#475569")
                textSize = 9f
            }

            val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E2E8F0")
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            val accentBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#00E676")
                style = Paint.Style.FILL
            }

            // Estimate pages: 3-4 evidence items per page
            val itemsPerPage = 3
            val totalPages = ((records.size + itemsPerPage - 1) / itemsPerPage).coerceAtLeast(1)

            var itemIndex = 0

            val logoBmp = BitmapFactory.decodeResource(context.resources, com.firstlogistics.flproof.R.drawable.logo_fl)

            if (records.size == 1) {
                // Dedicated Single Evidence Report with Original & Watermarked Photos
                generateSingleEvidenceReport(
                    pdfDocument = pdfDocument,
                    record = records[0],
                    companyName = companyName,
                    officerName = officerName,
                    pageWidth = pageWidth,
                    pageHeight = pageHeight,
                    margin = margin,
                    logoBmp = logoBmp
                )
            } else {
                // Batch Evidence Report (multiple cards per page)
                generateBatchEvidenceReport(
                    pdfDocument = pdfDocument,
                    records = records,
                    title = title,
                    companyName = companyName,
                    officerName = officerName,
                    pageWidth = pageWidth,
                    pageHeight = pageHeight,
                    margin = margin,
                    logoBmp = logoBmp,
                    itemsPerPage = itemsPerPage,
                    totalPages = totalPages
                )
            }

            val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val pdfFile = File(storageManager.reportsDir, "LAPORAN_FLPROOF_$timestampStr.pdf")
            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun generateSingleEvidenceReport(
        pdfDocument: PdfDocument,
        record: EvidenceEntity,
        companyName: String,
        officerName: String,
        pageWidth: Int,
        pageHeight: Int,
        margin: Float,
        logoBmp: Bitmap?
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#070D1B")
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E676")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val sectionHeadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF6600")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 9f
        }

        val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 8f
        }

        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        // Header
        if (logoBmp != null) {
            canvas.drawBitmap(logoBmp, null, RectF(margin, margin, margin + 34f, margin + 34f), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        } else {
            val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF6600"); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(margin, margin, margin + 34f, margin + 34f), 6f, 6f, logoPaint)
        }

        canvas.drawText(companyName, margin + 44f, margin + 17f, titlePaint)
        canvas.drawText("LAPORAN BUKTI DOKUMENTASI RESMI • ID: ${record.evidenceId}", margin + 44f, margin + 31f, subtitlePaint)

        var yPos = margin + 44f
        canvas.drawLine(margin, yPos, pageWidth - margin, yPos, borderPaint)

        // Side-by-side Photo Comparison: FOTO ASLI vs FOTO WATERMARK
        yPos += 12f
        val contentWidth = pageWidth - 2 * margin
        val photoGap = 12f
        val photoWidth = (contentWidth - photoGap) / 2f
        val photoHeight = 185f

        // Photo 1: Foto Asli
        val origRect = RectF(margin, yPos, margin + photoWidth, yPos + photoHeight)
        canvas.drawRoundRect(origRect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(origRect, 8f, 8f, borderPaint)

        // Photo 1 Label Tag
        val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1E293B"); style = Paint.Style.FILL }
        val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 8.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        canvas.drawRoundRect(RectF(origRect.left, origRect.top, origRect.left + 100f, origRect.top + 18f), 6f, 6f, labelBgPaint)
        canvas.drawText("FOTO ASLI (ORIGINAL)", origRect.left + 8f, origRect.top + 12f, labelTextPaint)

        val origBmp = loadResizedBitmap(record.originalPhotoPath, (photoWidth - 8f).toInt(), (photoHeight - 26f).toInt())
        if (origBmp != null) {
            canvas.drawBitmap(origBmp, origRect.left + 4f, origRect.top + 22f, null)
            origBmp.recycle()
        }

        // Photo 2: Foto Watermark
        val wmLeft = origRect.right + photoGap
        val wmRect = RectF(wmLeft, yPos, wmLeft + photoWidth, yPos + photoHeight)
        canvas.drawRoundRect(wmRect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(wmRect, 8f, 8f, borderPaint)

        val wmLabelBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#00E676"); style = Paint.Style.FILL }
        val wmLabelText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#070D1B"); textSize = 8.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        canvas.drawRoundRect(RectF(wmRect.left, wmRect.top, wmRect.left + 115f, wmRect.top + 18f), 6f, 6f, wmLabelBg)
        canvas.drawText("BUKTI WATERMARK (EVD)", wmRect.left + 6f, wmRect.top + 12f, wmLabelText)

        val wmBmp = loadResizedBitmap(record.watermarkedPhotoPath, (photoWidth - 8f).toInt(), (photoHeight - 26f).toInt())
        if (wmBmp != null) {
            canvas.drawBitmap(wmBmp, wmRect.left + 4f, wmRect.top + 22f, null)
            wmBmp.recycle()
        }

        // Detailed Metadata Section
        yPos += photoHeight + 16f
        val metaBoxHeight = 440f
        val metaRect = RectF(margin, yPos, pageWidth - margin, yPos + metaBoxHeight)
        canvas.drawRoundRect(metaRect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(metaRect, 8f, 8f, borderPaint)

        // Accent stripe
        val accentStripe = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#00E676"); style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(margin, yPos, margin + 4f, yPos + metaBoxHeight), 8f, 8f, accentStripe)

        var curY = yPos + 18f
        val textLeft = margin + 14f

        // QR Code Box on the right side
        val qrBoxSize = 130f
        val qrLeft = pageWidth - margin - qrBoxSize - 14f
        val qrTop = yPos + 14f
        val qrRect = RectF(qrLeft, qrTop, qrLeft + qrBoxSize, qrTop + qrBoxSize)
        canvas.drawRoundRect(qrRect, 6f, 6f, Paint().apply { color = Color.WHITE; style = Paint.Style.FILL })
        canvas.drawRoundRect(qrRect, 6f, 6f, borderPaint)

        val qrPayload = "FLPROOF:${record.evidenceId}"
        val qrCodeBitmap = com.firstlogistics.flproof.utils.QrCodeGenerator.generateQrBitmap(
            qrPayload,
            120
        )
        if (qrCodeBitmap != null) {
            canvas.drawBitmap(qrCodeBitmap, null, RectF(qrLeft + 5f, qrTop + 5f, qrRightCalc(qrLeft, qrBoxSize) - 5f, qrTop + qrBoxSize - 5f), null)
            qrCodeBitmap.recycle()
        }

        val qrLabelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10B981")
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("SCAN VERIFIKASI RESMI", qrLeft + (qrBoxSize / 2f), qrTop + qrBoxSize + 12f, qrLabelTextPaint)

        // Left Content Column
        canvas.drawText("IDENTITAS DOKUMENTASI", textLeft, curY, sectionHeadPaint)
        curY += 14f
        canvas.drawText("Nomor Evidence ID:", textLeft, curY, boldPaint)
        canvas.drawText(record.evidenceId, textLeft + 110f, curY, boldPaint)
        curY += 13f

        if (!record.sessionId.isNullOrBlank()) {
            canvas.drawText("ID Sesi Multi-Foto:", textLeft, curY, boldPaint)
            canvas.drawText("${record.sessionId} (Foto #${record.sessionIndex})", textLeft + 110f, curY, normalPaint)
            curY += 13f
        }

        canvas.drawText("Kategori Dokumen:", textLeft, curY, boldPaint)
        canvas.drawText(record.category, textLeft + 110f, curY, normalPaint)
        curY += 13f

        canvas.drawText("Petugas Lapangan:", textLeft, curY, boldPaint)
        canvas.drawText(record.officerName.ifBlank { officerName }, textLeft + 110f, curY, normalPaint)
        curY += 13f

        canvas.drawText("Status Bukti:", textLeft, curY, boldPaint)
        canvas.drawText(record.status, textLeft + 110f, curY, boldPaint)
        curY += 18f

        // WAKTU
        canvas.drawText("WAKTU PENGAMBILAN", textLeft, curY, sectionHeadPaint)
        curY += 14f
        canvas.drawText("Tanggal & Hari:", textLeft, curY, boldPaint)
        canvas.drawText(record.dateFormatted, textLeft + 110f, curY, normalPaint)
        curY += 13f
        canvas.drawText("Jam Pengambilan:", textLeft, curY, boldPaint)
        canvas.drawText(record.timeFormatted, textLeft + 110f, curY, normalPaint)
        curY += 18f

        // LOKASI
        canvas.drawText("LOKASI & GEO-TAGGING", textLeft, curY, sectionHeadPaint)
        curY += 14f
        canvas.drawText("Koordinat GPS:", textLeft, curY, boldPaint)
        val gpsStr = if (record.latitude != 0.0) String.format(Locale.US, "%.6f, %.6f (Akurasi: ±%dm)", record.latitude, record.longitude, record.gpsAccuracy.toInt()) else "Tidak tersedia"
        canvas.drawText(gpsStr, textLeft + 110f, curY, normalPaint)
        curY += 13f

        canvas.drawText("Arah Kompas:", textLeft, curY, boldPaint)
        canvas.drawText(record.compassDirection.ifBlank { "Tidak tersedia" }, textLeft + 110f, curY, normalPaint)
        curY += 13f

        canvas.drawText("Alamat Lengkap:", textLeft, curY, boldPaint)
        curY += 12f
        val addrLines = wrapReportText(record.address, 360f, normalPaint)
        for (line in addrLines) {
            canvas.drawText(line, textLeft + 10f, curY, normalPaint)
            curY += 11f
        }
        curY += 8f

        // INFORMASI PERANGKAT & INTEGRITAS
        canvas.drawText("PERANGKAT, KAMERA & INTEGRITAS DATA", textLeft, curY, sectionHeadPaint)
        curY += 14f
        canvas.drawText("Kamera & Model:", textLeft, curY, boldPaint)
        canvas.drawText("${record.cameraLens} • ${record.deviceModel}", textLeft + 110f, curY, normalPaint)
        curY += 13f

        canvas.drawText("Resolusi Foto:", textLeft, curY, boldPaint)
        canvas.drawText(record.resolution, textLeft + 110f, curY, normalPaint)
        curY += 13f

        if (!record.barcodeValue.isNullOrBlank()) {
            canvas.drawText("Barcode Terdeteksi:", textLeft, curY, boldPaint)
            canvas.drawText("${record.barcodeValue} (${record.barcodeFormat ?: "Code"})", textLeft + 110f, curY, boldPaint)
            curY += 13f
        }

        canvas.drawText("Integritas SHA-256:", textLeft, curY, boldPaint)
        curY += 12f
        canvas.drawText(record.sha256Hash, textLeft + 10f, curY, dimPaint)

        // Footer Note
        val footerY = pageHeight - margin + 8f
        canvas.drawText("Dokumen ini diterbitkan secara otomatis oleh FL PROOF Enterprise System • First Logistics", margin, footerY, dimPaint)
        canvas.drawText("Dicetak: ${DateFormatter.formatFullDateTime()}", pageWidth - margin - 150f, footerY, dimPaint)

        pdfDocument.finishPage(page)
    }

    private fun qrRightCalc(left: Float, size: Float) = left + size

    private fun generateBatchEvidenceReport(
        pdfDocument: PdfDocument,
        records: List<EvidenceEntity>,
        title: String,
        companyName: String,
        officerName: String,
        pageWidth: Int,
        pageHeight: Int,
        margin: Float,
        logoBmp: Bitmap?,
        itemsPerPage: Int,
        totalPages: Int
    ) {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#070D1B")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10B981")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 10f
        }

        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 9f
        }

        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val accentBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E676")
            style = Paint.Style.FILL
        }

        var itemIndex = 0

        for (pageNumber in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var yPos = margin + 15f

            if (logoBmp != null) {
                canvas.drawBitmap(logoBmp, null, RectF(margin, margin, margin + 34f, margin + 34f), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            } else {
                val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF6600"); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(margin, margin, margin + 34f, margin + 34f), 6f, 6f, logoPaint)
            }

            canvas.drawText(companyName, margin + 44f, margin + 18f, titlePaint)
            canvas.drawText("LAPORAN BUKTI DOKUMENTASI RESMI • $title", margin + 44f, margin + 32f, subtitlePaint)

            yPos = margin + 46f
            canvas.drawLine(margin, yPos, pageWidth - margin, yPos, borderPaint)

            if (pageNumber == 1) {
                yPos += 14f
                canvas.drawText("Tanggal Cetak: ${DateFormatter.formatFullDateTime()}", margin, yPos, headerTextPaint)
                canvas.drawText("Petugas: $officerName", margin + 260f, yPos, headerTextPaint)
                yPos += 12f
                canvas.drawText("Total Bukti: ${records.size} Dokumen", margin, yPos, headerTextPaint)
                canvas.drawText("Status: Arsip Resmi First Logistics", margin + 260f, yPos, headerTextPaint)
                yPos += 16f
                canvas.drawLine(margin, yPos, pageWidth - margin, yPos, borderPaint)
                yPos += 14f
            } else {
                yPos += 18f
            }

            var renderedInThisPage = 0
            val cardHeight = 180f

            while (itemIndex < records.size && renderedInThisPage < itemsPerPage) {
                val record = records[itemIndex]
                val cardRect = RectF(margin, yPos, pageWidth - margin, yPos + cardHeight)

                canvas.drawRoundRect(cardRect, 8f, 8f, cardBgPaint)
                canvas.drawRoundRect(cardRect, 8f, 8f, borderPaint)
                canvas.drawRoundRect(RectF(margin, yPos, margin + 4f, yPos + cardHeight), 8f, 8f, accentBarPaint)

                val photoPath = if (File(record.watermarkedPhotoPath).exists()) {
                    record.watermarkedPhotoPath
                } else record.originalPhotoPath

                val thumbWidth = 130f
                val thumbHeight = 160f
                val thumbLeft = margin + 12f
                val thumbTop = yPos + 10f

                val bitmap = loadResizedBitmap(photoPath, thumbWidth.toInt(), thumbHeight.toInt())
                if (bitmap != null) {
                    canvas.drawBitmap(bitmap, thumbLeft, thumbTop, null)
                    bitmap.recycle()
                } else {
                    val placeholderPaint = Paint().apply { color = Color.LTGRAY; style = Paint.Style.FILL }
                    canvas.drawRect(RectF(thumbLeft, thumbTop, thumbLeft + thumbWidth, thumbTop + thumbHeight), placeholderPaint)
                }

                val textX = thumbLeft + thumbWidth + 14f
                var textY = yPos + 22f

                canvas.drawText("ID: ${record.evidenceId}", textX, textY, boldPaint)
                textY += 15f
                canvas.drawText("Kategori: ${record.category}", textX, textY, boldPaint)
                textY += 15f
                canvas.drawText("Waktu: ${record.dateFormatted} • ${record.timeFormatted}", textX, textY, normalPaint)
                textY += 15f

                val shortAddr = if (record.address.length > 50) record.address.take(47) + "..." else record.address
                canvas.drawText("Lokasi: $shortAddr", textX, textY, normalPaint)
                textY += 15f

                val coordText = if (record.latitude != 0.0) {
                    String.format(Locale.US, "GPS: %.6f, %.6f (±%dm)", record.latitude, record.longitude, record.gpsAccuracy.toInt())
                } else "GPS: Tidak tersedia"
                canvas.drawText(coordText, textX, textY, normalPaint)
                textY += 15f

                if (record.compassDirection.isNotBlank()) {
                    canvas.drawText("Arah Kompas: ${record.compassDirection}", textX, textY, normalPaint)
                    textY += 15f
                }

                if (!record.barcodeValue.isNullOrBlank()) {
                    canvas.drawText("Barcode: ${record.barcodeValue} (${record.barcodeFormat ?: "Code"})", textX, textY, boldPaint)
                    textY += 15f
                }

                val shortHash = if (record.sha256Hash.length > 24) record.sha256Hash.take(24) + "..." else record.sha256Hash
                canvas.drawText("SHA-256: $shortHash", textX, textY, normalPaint)

                yPos += cardHeight + 14f
                itemIndex++
                renderedInThisPage++
            }

            val footerY = pageHeight - margin + 10f
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#94A3B8")
                textSize = 8f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("Halaman $pageNumber dari $totalPages • FL PROOF Enterprise Documentation System", pageWidth - margin, footerY, footerPaint)

            pdfDocument.finishPage(page)
        }
    }

    private fun wrapReportText(text: String, maxWidth: Float, paint: Paint): List<String> {
        if (text.isBlank()) return listOf("Alamat tidak terdeteksi")
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""
        for (w in words) {
            val test = if (current.isEmpty()) w else "$current $w"
            if (paint.measureText(test) <= maxWidth) {
                current = test
            } else {
                if (current.isNotEmpty()) lines.add(current)
                current = w
            }
        }
        if (current.isNotEmpty()) lines.add(current)
        return lines.take(4)
    }

    private fun loadResizedBitmap(path: String, targetW: Int, targetH: Int): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)

        var sampleSize = 1
        while (options.outWidth / (sampleSize * 2) >= targetW &&
            options.outHeight / (sampleSize * 2) >= targetH
        ) {
            sampleSize *= 2
        }

        options.inJustDecodeBounds = false
        options.inSampleSize = sampleSize

        val decoded = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
        return Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
    }
}

