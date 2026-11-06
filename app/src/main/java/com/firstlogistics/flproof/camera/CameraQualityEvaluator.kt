package com.firstlogistics.flproof.camera

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.sqrt

data class QualityCheckResult(
    val averageLuminance: Float,
    val sharpnessScore: Float,
    val lightingStatus: String,
    val sharpnessStatus: String,
    val isGoodQuality: Boolean,
    val recommendation: String
)

object CameraQualityEvaluator {

    fun evaluate(bitmap: Bitmap): QualityCheckResult {
        // Downsample bitmap for fast responsive analysis without heavy memory overhead
        val sampleWidth = 120
        val sampleHeight = (sampleWidth * (bitmap.height.toFloat() / bitmap.width.toFloat())).toInt().coerceAtLeast(60)
        val scaled = Bitmap.createScaledBitmap(bitmap, sampleWidth, sampleHeight, false)

        val pixels = IntArray(sampleWidth * sampleHeight)
        scaled.getPixels(pixels, 0, sampleWidth, 0, 0, sampleWidth, sampleHeight)

        var totalLuminance = 0.0
        val lumArray = FloatArray(pixels.size)

        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val lum = 0.299f * r + 0.587f * g + 0.114f * b
            lumArray[i] = lum
            totalLuminance += lum
        }

        val avgLuminance = (totalLuminance / pixels.size).toFloat()

        // Calculate discrete Laplacian gradient variance for sharpness estimation
        var laplacianSum = 0.0
        var count = 0
        for (y in 1 until sampleHeight - 1) {
            for (x in 1 until sampleWidth - 1) {
                val idx = y * sampleWidth + x
                val center = lumArray[idx]
                val laplacian = abs(
                    lumArray[idx - 1] + lumArray[idx + 1] +
                            lumArray[idx - sampleWidth] + lumArray[idx + sampleWidth] -
                            4 * center
                )
                laplacianSum += laplacian
                count++
            }
        }

        val sharpnessScore = if (count > 0) (laplacianSum / count).toFloat() else 0f

        val lightingStatus = when {
            avgLuminance < 45f -> "Terlalu Gelap (Under-exposed)"
            avgLuminance > 215f -> "Terlalu Terang (Over-exposed)"
            else -> "Pencahayaan Baik"
        }

        val sharpnessStatus = when {
            sharpnessScore < 8.0f -> "Kurang Tajam / Berpotensi Buram"
            sharpnessScore < 15.0f -> "Ketajaman Cukup"
            else -> "Sangat Tajam & Jelas"
        }

        val isGoodQuality = avgLuminance in 45f..215f && sharpnessScore >= 8.0f

        val recommendation = when {
            avgLuminance < 45f -> "Gunakan lampu flash atau arahkan ke sumber cahaya."
            avgLuminance > 215f -> "Hindari pantulan cahaya langsung (backlight)."
            sharpnessScore < 8.0f -> "Pegang perangkat lebih stabil sebelum menekan tombol foto."
            else -> "Kualitas foto optimal untuk bukti dokumentasi."
        }

        if (scaled != bitmap) {
            scaled.recycle()
        }

        return QualityCheckResult(
            averageLuminance = avgLuminance,
            sharpnessScore = sharpnessScore,
            lightingStatus = lightingStatus,
            sharpnessStatus = sharpnessStatus,
            isGoodQuality = isGoodQuality,
            recommendation = recommendation
        )
    }
}

