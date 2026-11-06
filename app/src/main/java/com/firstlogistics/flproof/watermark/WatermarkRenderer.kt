package com.firstlogistics.flproof.watermark

import android.graphics.*
import com.firstlogistics.flproof.data.model.WatermarkConfig
import com.firstlogistics.flproof.data.model.WatermarkPosition
import com.firstlogistics.flproof.data.model.WatermarkSize
import com.firstlogistics.flproof.utils.QrCodeGenerator
import java.util.Locale

object WatermarkRenderer {

    data class WatermarkData(
        val evidenceId: String,
        val category: String,
        val dateString: String,
        val timeString: String,
        val address: String,
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val compassDirection: String,
        val resolution: String,
        val officerName: String? = null,
        val cameraLens: String = "Kamera Belakang",
        val deviceModel: String = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
        val logoBitmap: Bitmap? = null,
        val qrBitmap: Bitmap? = null,
        val qrUrl: String? = null
    )

    fun renderWatermark(
        sourceBitmap: Bitmap,
        data: WatermarkData,
        config: WatermarkConfig = WatermarkConfig()
    ): Bitmap {
        val result = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val bmpWidth = result.width.toFloat()
        val bmpHeight = result.height.toFloat()

        // Base scale proportional to image dimensions and watermark size preference
        val referenceDimension = bmpWidth.coerceAtMost(bmpHeight)
        val baseScale = (referenceDimension / 1000f) * config.size.scale

        val padding = 16f * baseScale
        val panelMargin = 20f * baseScale
        val accentLineWidth = 4f * baseScale

        // Drop shadow for text readability without bulky background block
        val shadowRadius = 5f * baseScale
        val shadowDx = 2f * baseScale
        val shadowDy = 2f * baseScale
        val shadowColor = Color.parseColor("#E6000000") // 90% black shadow

        val headerCompanyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f * baseScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
        }

        val headerDocPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 12f * baseScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
            setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
        }

        val titleBuktiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E676") // Green Accent
            textSize = 16f * baseScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
        }

        val sectionHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF6600") // Orange accent for section heads
            textSize = 12f * baseScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.06f
            setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
        }

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 12.5f * baseScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
        }

        val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 13f * baseScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
        }

        val dimTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 11f * baseScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
        }

        // Texts setup
        val officerName = data.officerName?.ifBlank { "FADEL RIPAI" } ?: "FADEL RIPAI"
        val categoryName = if (data.category.isNotBlank() && data.category != "TANPA KATEGORI") data.category.uppercase() else "BARANG MASUK"
        val compassStr = if (data.compassDirection.isNotBlank()) data.compassDirection else "Utara (0°)"
        val coordStr = if (data.latitude != 0.0 || data.longitude != 0.0) {
            String.format(Locale.US, "GPS: %.6f, %.6f", data.latitude, data.longitude)
        } else "GPS: -6.272052, 106.744109"
        val accStr = if (data.accuracy > 0f) "Akurasi: ±${data.accuracy.toInt()} meter" else "Akurasi: ±15 meter"

        val panelWidth = (bmpWidth * 0.88f * config.size.scale)
            .coerceIn(400f * baseScale, bmpWidth - (panelMargin * 2))
        val textMaxContentWidth = panelWidth - (padding * 2) - accentLineWidth
        val addressLines = wrapText(data.address, textMaxContentWidth, bodyPaint)

        val lineHeight = 16.5f * baseScale
        val sectionSpacing = 10f * baseScale

        // Calculate height
        var totalHeight = padding * 2
        totalHeight += 38f * baseScale // Header + Subtitle
        totalHeight += 20f * baseScale // BUKTI DOKUMENTASI
        totalHeight += 4f * baseScale  // Divider line

        // IDENTITAS
        totalHeight += sectionSpacing + lineHeight * 3 // Section title + 3 lines

        // WAKTU
        totalHeight += sectionSpacing + lineHeight * 2 // Section title + 2 lines

        // LOKASI
        totalHeight += sectionSpacing + (addressLines.size * lineHeight) + (lineHeight * 3) // title + address + gps + acc + compass

        // INFORMASI FOTO
        totalHeight += sectionSpacing + lineHeight * 4 // title + res + cam + device + evidenceId

        // QR SECTION (Kiri Bawah Panel Watermark - QR Biasa Saja)
        val qrBoxSize = 90f * baseScale
        val qrSectionHeight = qrBoxSize + 8f * baseScale
        totalHeight += sectionSpacing + qrSectionHeight

        totalHeight += 12f * baseScale // bottom padding margin

        // Position coordinates
        val (left, top) = when (config.position) {
            WatermarkPosition.BOTTOM_LEFT -> Pair(panelMargin, bmpHeight - totalHeight - panelMargin)
            WatermarkPosition.BOTTOM_RIGHT -> Pair(bmpWidth - panelWidth - panelMargin, bmpHeight - totalHeight - panelMargin)
            WatermarkPosition.TOP_LEFT -> Pair(panelMargin, panelMargin + (160f * baseScale))
            WatermarkPosition.TOP_RIGHT -> Pair(bmpWidth - panelWidth - panelMargin, panelMargin + (160f * baseScale))
        }

        val right = left + panelWidth
        val bottom = top + totalHeight

        // Subtle gradient feather at the text area so text is 100% readable without any bulky solid background box
        val featherPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, top - 20f * baseScale,
                0f, bottom + 10f * baseScale,
                intArrayOf(Color.TRANSPARENT, Color.parseColor("#44000000"), Color.parseColor("#88000000")),
                floatArrayOf(0f, 0.35f, 1f),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawRect(RectF(0f, top - 20f * baseScale, bmpWidth, bmpHeight), featherPaint)

        // Green accent vertical line indicator on the left
        val accentBarRect = RectF(left, top, left + accentLineWidth, bottom - 10f * baseScale)
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E676")
            style = Paint.Style.FILL
            setShadowLayer(shadowRadius, 0f, 0f, shadowColor)
        }
        canvas.drawRoundRect(accentBarRect, 2f * baseScale, 2f * baseScale, accentPaint)

        val contentX = left + padding + accentLineWidth
        var currentY = top + padding + 16f * baseScale

        // 1. Header with Brand
        val logoBoxSize = 34f * baseScale
        val logoRect = RectF(contentX, top + padding, contentX + logoBoxSize, top + padding + logoBoxSize)
        if (data.logoBitmap != null) {
            val clipPath = Path().apply {
                addRoundRect(logoRect, 4f * baseScale, 4f * baseScale, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(clipPath)
            canvas.drawBitmap(data.logoBitmap, null, logoRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            canvas.restore()
        } else {
            val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF6600"); style = Paint.Style.FILL }
            canvas.drawRoundRect(logoRect, 4f * baseScale, 4f * baseScale, fallbackPaint)
            val fbText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 16f * baseScale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("FL", logoRect.centerX(), logoRect.centerY() + 6f * baseScale, fbText)
        }

        val headerTextX = contentX + logoBoxSize + 10f * baseScale
        canvas.drawText("FIRST LOGISTICS", headerTextX, currentY, headerCompanyPaint)
        currentY += 14f * baseScale
        canvas.drawText("DOCUMENTATION EVIDENCE SYSTEM", headerTextX, currentY, headerDocPaint)

        currentY += 22f * baseScale
        canvas.drawText("BUKTI DOKUMENTASI", contentX, currentY, titleBuktiPaint)

        // Divider
        currentY += 8f * baseScale
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#80FFFFFF")
            strokeWidth = 1.2f * baseScale
            setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
        }
        canvas.drawLine(contentX, currentY, right - padding, currentY, divPaint)

        // Section: IDENTITAS
        currentY += sectionSpacing + 4f * baseScale
        canvas.drawText("IDENTITAS", contentX, currentY, sectionHeaderPaint)
        currentY += lineHeight
        canvas.drawText("- ID: ${data.evidenceId}", contentX, currentY, bodyBoldPaint)
        currentY += lineHeight
        canvas.drawText("- Petugas: $officerName", contentX, currentY, bodyPaint)
        currentY += lineHeight
        canvas.drawText("- Kategori: $categoryName", contentX, currentY, bodyPaint)

        // Section: WAKTU
        currentY += sectionSpacing
        canvas.drawText("WAKTU", contentX, currentY, sectionHeaderPaint)
        currentY += lineHeight
        canvas.drawText("- ${data.dateString}", contentX, currentY, bodyPaint)
        currentY += lineHeight
        canvas.drawText("- ${data.timeString}", contentX, currentY, bodyBoldPaint)

        // Section: LOKASI
        currentY += sectionSpacing
        canvas.drawText("LOKASI", contentX, currentY, sectionHeaderPaint)
        for (line in addressLines) {
            currentY += lineHeight
            canvas.drawText("- $line", contentX, currentY, bodyPaint)
        }
        currentY += lineHeight
        canvas.drawText("- $coordStr", contentX, currentY, bodyPaint)
        currentY += lineHeight
        canvas.drawText("- $accStr", contentX, currentY, bodyPaint)
        currentY += lineHeight
        canvas.drawText("- Arah: $compassStr", contentX, currentY, bodyPaint)

        // Section: INFORMASI FOTO
        currentY += sectionSpacing
        canvas.drawText("INFORMASI FOTO", contentX, currentY, sectionHeaderPaint)
        currentY += lineHeight
        canvas.drawText("- Resolusi: ${data.resolution}", contentX, currentY, dimTextPaint)
        currentY += lineHeight
        canvas.drawText("- Kamera: ${data.cameraLens}", contentX, currentY, dimTextPaint)
        currentY += lineHeight
        canvas.drawText("- Device: ${data.deviceModel}", contentX, currentY, dimTextPaint)
        currentY += lineHeight
        canvas.drawText("- Evidence ID: ${data.evidenceId}", contentX, currentY, dimTextPaint)

        // ----------------------------------------------------
        // BOTTOM-LEFT PANEL: QR BARCODE & WEB TRACKING PELANGGAN
        // ----------------------------------------------------
        currentY += sectionSpacing + 4f * baseScale

        // Divider line
        val qrDivPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#40FFFFFF")
            strokeWidth = 1f * baseScale
            setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
        }
        canvas.drawLine(contentX, currentY, right - padding, currentY, qrDivPaint)
        currentY += 8f * baseScale

        val qrRect = RectF(contentX, currentY, contentX + qrBoxSize, currentY + qrBoxSize)

        // White background container for QR code (high contrast, easy scanning)
        val qrBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
        }
        canvas.drawRoundRect(qrRect, 6f * baseScale, 6f * baseScale, qrBgPaint)

        // Generate and draw QR Code
        val qrPayload = data.qrUrl?.ifBlank { null } ?: data.evidenceId
        val qrBitmap = data.qrBitmap ?: QrCodeGenerator.generateQrBitmap(qrPayload, 300)
        if (qrBitmap != null) {
            val qrMargin = 4f * baseScale
            val qrInnerRect = RectF(
                qrRect.left + qrMargin,
                qrRect.top + qrMargin,
                qrRect.right - qrMargin,
                qrRect.bottom - qrMargin
            )
            canvas.drawBitmap(qrBitmap, null, qrInnerRect, Paint(Paint.ANTI_ALIAS_FLAG))
        }

        // To the right of QR Code: Clean Barcode 1D of Evidence ID (Tanpa tulisan teks ekstra)
        val qrInfoX = contentX + qrBoxSize + 14f * baseScale
        val qrInfoMaxW = right - padding - qrInfoX

        if (qrInfoMaxW > 100f * baseScale) {
            val barcodeBmp = QrCodeGenerator.generateBarcodeBitmap(data.evidenceId, 280, 50)
            if (barcodeBmp != null) {
                val barcodeW = (180f * baseScale).coerceAtMost(qrInfoMaxW)
                val barcodeH = 34f * baseScale
                val barcodeRect = RectF(qrInfoX, currentY + 12f * baseScale, qrInfoX + barcodeW, currentY + 12f * baseScale + barcodeH)

                val bcBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
                canvas.drawRoundRect(barcodeRect, 4f * baseScale, 4f * baseScale, bcBgPaint)
                canvas.drawBitmap(barcodeBmp, null, barcodeRect, Paint(Paint.ANTI_ALIAS_FLAG))
            }
        }

        // ----------------------------------------------------
        // TOP-LEFT: GOOGLE MAPS GPS MINIMAP (FITUR GAMBAR PETA DI KIRI ATAS)
        // ----------------------------------------------------
        drawGoogleMapsMinimap(
            canvas = canvas,
            x = panelMargin,
            y = panelMargin,
            width = 175f * baseScale,
            height = 145f * baseScale,
            baseScale = baseScale,
            latitude = data.latitude,
            longitude = data.longitude,
            accuracy = data.accuracy,
            compassDirection = compassStr
        )

        // ----------------------------------------------------
        // TOP-RIGHT: BRAND WATERMARK LOGO FL DI BAGIAN KANAN ATAS
        // ----------------------------------------------------
        drawTopRightLogoBadge(
            canvas = canvas,
            bmpWidth = bmpWidth,
            baseScale = baseScale,
            logoBitmap = data.logoBitmap
        )

        return result
    }

    /**
     * Menggambar miniatur peta GPS bergaya Google Maps otentik di sudut kiri atas foto.
     */
    private fun drawGoogleMapsMinimap(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        baseScale: Float,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        compassDirection: String
    ) {
        val cornerRadius = 12f * baseScale
        val mapRect = RectF(x, y, x + width, y + height)

        // 1. Shadow background container
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#66000000")
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(6f * baseScale, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(RectF(x + 2f, y + 2f, x + width + 4f, y + height + 4f), cornerRadius, cornerRadius, shadowPaint)

        // Save canvas layer for clipping to rounded map card
        canvas.save()
        val clipPath = Path().apply {
            addRoundRect(mapRect, cornerRadius, cornerRadius, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)

        // 2. Google Maps terrain / base land color (#ECEEE9)
        val landPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F4F3F0")
            style = Paint.Style.FILL
        }
        canvas.drawRect(mapRect, landPaint)

        // 3. Green park / organic vegetation polygon (#C8E6C9)
        val parkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4EDDA")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(x + width * 0.2f, y + height * 0.25f, 32f * baseScale, parkPaint)
        canvas.drawRoundRect(
            RectF(x + width * 0.65f, y + 6f * baseScale, x + width + 10f, y + height * 0.45f),
            10f * baseScale, 10f * baseScale, parkPaint
        )

        // 4. Water / River strip (#CFE8FC)
        val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CCE5FF")
            style = Paint.Style.STROKE
            strokeWidth = 14f * baseScale
            strokeCap = Paint.Cap.ROUND
        }
        val riverPath = Path().apply {
            moveTo(x - 10f, y + height * 0.75f)
            quadTo(x + width * 0.5f, y + height * 0.65f, x + width + 10f, y + height * 0.85f)
        }
        canvas.drawPath(riverPath, waterPaint)

        // 5. Road grid (Secondary roads in white)
        val secondaryRoadOutline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E5E7EB")
            style = Paint.Style.STROKE
            strokeWidth = 5.5f * baseScale
        }
        val secondaryRoadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 4f * baseScale
        }

        // Horizontal street
        val hStreetY = y + height * 0.42f
        canvas.drawLine(x, hStreetY, x + width, hStreetY, secondaryRoadOutline)
        canvas.drawLine(x, hStreetY, x + width, hStreetY, secondaryRoadPaint)

        // Vertical street
        val vStreetX = x + width * 0.38f
        canvas.drawLine(vStreetX, y, vStreetX, y + height, secondaryRoadOutline)
        canvas.drawLine(vStreetX, y, vStreetX, y + height, secondaryRoadPaint)

        // Another vertical street
        val vStreetX2 = x + width * 0.72f
        canvas.drawLine(vStreetX2, y, vStreetX2, y + height, secondaryRoadOutline)
        canvas.drawLine(vStreetX2, y, vStreetX2, y + height, secondaryRoadPaint)

        // 6. Arterial Highway in Google Maps orange-yellow
        val highwayOutline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F59E0B")
            style = Paint.Style.STROKE
            strokeWidth = 8f * baseScale
            strokeCap = Paint.Cap.ROUND
        }
        val highwayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FDE68A")
            style = Paint.Style.STROKE
            strokeWidth = 6f * baseScale
            strokeCap = Paint.Cap.ROUND
        }
        val highwayPath = Path().apply {
            moveTo(x, y + height * 0.15f)
            lineTo(x + width * 0.5f, y + height * 0.42f)
            lineTo(x + width, y + height * 0.60f)
        }
        canvas.drawPath(highwayPath, highwayOutline)
        canvas.drawPath(highwayPath, highwayPaint)

        // 7. Center GPS location radar waves
        val pinX = x + width * 0.5f
        val pinY = y + height * 0.45f

        val radarWave1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334285F4") // Google Blue translucent
            style = Paint.Style.FILL
        }
        canvas.drawCircle(pinX, pinY, 26f * baseScale, radarWave1)

        val radarWave2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4D4285F4")
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * baseScale
        }
        canvas.drawCircle(pinX, pinY, 18f * baseScale, radarWave2)

        // 8. Google Maps Red Marker Pin (📍)
        val pinShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#40000000")
            style = Paint.Style.FILL
        }
        canvas.drawOval(RectF(pinX - 5f * baseScale, pinY - 1f, pinX + 5f * baseScale, pinY + 3f * baseScale), pinShadow)

        // Red Pin Body
        val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EA4335") // Google Red
            style = Paint.Style.FILL
        }
        val pinHeadY = pinY - 10f * baseScale
        val pinRadius = 7f * baseScale
        canvas.drawCircle(pinX, pinHeadY, pinRadius, pinPaint)

        val pinTeardrop = Path().apply {
            moveTo(pinX - pinRadius * 0.85f, pinHeadY + 2f * baseScale)
            lineTo(pinX, pinY)
            lineTo(pinX + pinRadius * 0.85f, pinHeadY + 2f * baseScale)
            close()
        }
        canvas.drawPath(pinTeardrop, pinPaint)

        // White Center Dot
        val pinDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(pinX, pinHeadY, 2.5f * baseScale, pinDotPaint)

        // 9. Top-Left Floating Badge: "GOOGLE MAPS GPS"
        val topBadgeRect = RectF(x + 6f * baseScale, y + 6f * baseScale, x + 90f * baseScale, y + 20f * baseScale)
        val topBadgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D90F172A")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(topBadgeRect, 4f * baseScale, 4f * baseScale, topBadgeBg)

        val dotGreen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E676")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(x + 12f * baseScale, y + 13f * baseScale, 2.5f * baseScale, dotGreen)

        val mapLabelText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 7.5f * baseScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("GOOGLE MAPS", x + 18f * baseScale, y + 16f * baseScale, mapLabelText)

        // 10. Compass Indicator in Top-Right
        val compassPill = RectF(x + width - 24f * baseScale, y + 6f * baseScale, x + width - 6f * baseScale, y + 20f * baseScale)
        canvas.drawRoundRect(compassPill, 4f * baseScale, 4f * baseScale, topBadgeBg)
        val compText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF6600")
            textSize = 8f * baseScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("N ↑", compassPill.centerX(), compassPill.centerY() + 3f * baseScale, compText)

        // 11. Bottom GPS Bar with Coordinates & Accuracy
        val botBarHeight = 34f * baseScale
        val botBarRect = RectF(x, y + height - botBarHeight, x + width, y + height)
        val botBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E60F172A")
            style = Paint.Style.FILL
        }
        canvas.drawRect(botBarRect, botBarPaint)

        val coordTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8.5f * baseScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val accTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E676")
            textSize = 7.5f * baseScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val coordDisplay = if (latitude != 0.0 || longitude != 0.0) {
            String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
        } else "-6.27205, 106.74411"

        canvas.drawText(coordDisplay, x + 8f * baseScale, y + height - 19f * baseScale, coordTextPaint)
        canvas.drawText("Akurasi: ±${accuracy.toInt().coerceAtLeast(1)}m • $compassDirection", x + 8f * baseScale, y + height - 8f * baseScale, accTextPaint)

        canvas.restore()

        // 12. Outer Border of Map Card
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * baseScale
        }
        canvas.drawRoundRect(mapRect, cornerRadius, cornerRadius, borderPaint)
    }

    /**
     * Menggambar badge logo First Logistics di sudut kanan atas foto.
     */
    private fun drawTopRightLogoBadge(
        canvas: Canvas,
        bmpWidth: Float,
        baseScale: Float,
        logoBitmap: Bitmap?
    ) {
        val trMargin = 20f * baseScale
        val trLogoSize = 40f * baseScale
        val trPadding = 10f * baseScale
        val trCornerRadius = 10f * baseScale

        val trTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 13.5f * baseScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val trSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E676") // Green Accent
            textSize = 9.5f * baseScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val brandTitle = "FIRST LOGISTICS"
        val brandTagline = "EVIDENCE PROOF"
        val brandTextWidth = trTitlePaint.measureText(brandTitle).coerceAtLeast(trSubPaint.measureText(brandTagline))

        val badgeWidth = trPadding + trLogoSize + (10f * baseScale) + brandTextWidth + trPadding
        val badgeHeight = trLogoSize + (trPadding * 2)

        val badgeLeft = bmpWidth - badgeWidth - trMargin
        val badgeTop = trMargin
        val badgeRight = badgeLeft + badgeWidth
        val badgeBottom = badgeTop + badgeHeight

        val badgeRect = RectF(badgeLeft, badgeTop, badgeRight, badgeBottom)

        // Translucent dark background
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D90F172A")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(badgeRect, trCornerRadius, trCornerRadius, badgeBgPaint)

        // Green accent border
        val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6600E676")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * baseScale
        }
        canvas.drawRoundRect(badgeRect, trCornerRadius, trCornerRadius, badgeBorderPaint)

        // Green accent vertical indicator bar on left edge of badge
        val badgeAccentBar = RectF(badgeLeft, badgeTop, badgeLeft + (4f * baseScale), badgeBottom)
        val badgeAccentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E676")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(badgeAccentBar, trCornerRadius, trCornerRadius, badgeAccentPaint)

        // Draw Logo Bitmap in top-right badge
        val trLogoRect = RectF(
            badgeLeft + trPadding + (3f * baseScale),
            badgeTop + trPadding,
            badgeLeft + trPadding + (3f * baseScale) + trLogoSize,
            badgeTop + trPadding + trLogoSize
        )

        if (logoBitmap != null) {
            val clipPath = Path().apply {
                addRoundRect(trLogoRect, 6f * baseScale, 6f * baseScale, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(clipPath)
            canvas.drawBitmap(logoBitmap, null, trLogoRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            canvas.restore()
        } else {
            val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FF6600")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(trLogoRect, 6f * baseScale, 6f * baseScale, fallbackPaint)
            val fbText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 18f * baseScale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("FL", trLogoRect.centerX(), trLogoRect.centerY() + 7f * baseScale, fbText)
        }

        val textStartX = trLogoRect.right + (8f * baseScale)
        canvas.drawText(brandTitle, textStartX, badgeTop + trPadding + (16f * baseScale), trTitlePaint)
        canvas.drawText(brandTagline, textStartX, badgeTop + trPadding + (30f * baseScale), trSubPaint)
    }

    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        if (text.isBlank()) return listOf("Alamat: -")
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine.append(if (currentLine.isEmpty()) word else " $word")
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines.take(3)
    }
}
