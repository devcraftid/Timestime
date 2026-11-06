package com.firstlogistics.flproof.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class MultiRotationOcrResult(
    val text: String,
    val bestRotation: Int,
    val rotatedBitmap: Bitmap
)

object OfflineTextOcrScanner {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Memindai teks dari bitmap dengan toleransi orientasi otomatis (0°, 90°, 180°, 270°).
     * Jika foto diambil miring / terbalik / vertikal (seperti label resi menyamping),
     * algoritma ini otomatis menemukan sudut baca yang tepat berdasarkan skor kata kunci bahasa Indonesia.
     */
    suspend fun recognizeTextAutoOrient(bitmap: Bitmap): MultiRotationOcrResult {
        // Coba orientasi normal (0°) terlebih dahulu
        val text0 = recognizeSingle(bitmap, 0)
        val score0 = scoreOcrText(text0)

        // Jika orientasi 0° sudah memiliki skor tinggi dan menemukan kata kunci resi, gunakan langsung
        if (score0 >= 9 && (text0.contains("kepada", ignoreCase = true) || text0.contains("alamat", ignoreCase = true))) {
            return MultiRotationOcrResult(text0, 0, bitmap)
        }

        // Coba sudut lain: 90° (kanan), 270° (kiri), 180° (terbalik)
        val candidateRotations = listOf(90, 270, 180)
        var bestText = text0
        var bestRotation = 0
        var bestScore = score0

        for (angle in candidateRotations) {
            val candidateText = recognizeSingle(bitmap, angle)
            val candidateScore = scoreOcrText(candidateText)

            if (candidateScore > bestScore) {
                bestScore = candidateScore
                bestText = candidateText
                bestRotation = angle

                // Jika sudah menemukan kata kunci kuat, tidak perlu lanjut rotasi berikutnya
                if (candidateScore >= 9) break
            }
        }

        val resultBitmap = if (bestRotation != 0) {
            rotateBitmap(bitmap, bestRotation.toFloat())
        } else {
            bitmap
        }

        return MultiRotationOcrResult(bestText, bestRotation, resultBitmap)
    }

    suspend fun recognizeText(bitmap: Bitmap): String {
        return recognizeTextAutoOrient(bitmap).text
    }

    private suspend fun recognizeSingle(bitmap: Bitmap, rotationDegrees: Int): String =
        suspendCancellableCoroutine { continuation ->
            try {
                val image = InputImage.fromBitmap(bitmap, rotationDegrees)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        continuation.resume(visionText.text)
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        continuation.resume("")
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                continuation.resume("")
            }
        }

    private fun scoreOcrText(text: String): Int {
        if (text.isBlank()) return 0
        val lower = text.lowercase()
        var score = 0

        // Kata kunci khas label paket dan resi logistik Indonesia
        val primaryKeywords = listOf(
            "kepada", "alamat", "di alamat", "penerima", "yth", "dusun", "rt", "rw",
            "kabupaten", "kecamatan", "kelurahan", "desa", "jalan", "jl.", "ogan",
            "komering", "ilir", "cengal", "sumatera", "jawa", "confidential", "seabank",
            "shopee", "blok", "perum", "kampung"
        )
        for (kw in primaryKeywords) {
            if (lower.contains(kw)) score += 3
        }

        // Skor tambahan untuk kata-kata alfabetik normal (menolak teks acak/gibberish)
        val words = lower.split("\\s+".toRegex()).filter { it.length >= 3 && it.all { c -> c.isLetter() } }
        score += words.size.coerceAtMost(8)

        return score
    }

    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return source
        val matrix = Matrix().apply { postRotate(degrees) }
        return try {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } catch (e: Exception) {
            e.printStackTrace()
            source
        }
    }
}
