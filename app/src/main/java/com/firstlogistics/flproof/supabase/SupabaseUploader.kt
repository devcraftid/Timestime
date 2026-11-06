package com.firstlogistics.flproof.supabase

import android.content.Context
import com.firstlogistics.flproof.data.entity.EvidenceEntity
import com.firstlogistics.flproof.data.repository.EvidenceRepository
import com.firstlogistics.flproof.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class SupabaseUploader(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val evidenceRepository: EvidenceRepository
) {

    /**
     * Mengunggah satu file fisik (Foto JPEG atau Video MP4) ke Supabase Storage Bucket 'flproof-media'.
     * Mengembalikan URL publik file yang berhasil diunggah.
     */
    suspend fun uploadFileToStorage(
        file: File,
        remoteFileName: String,
        mimeType: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val baseUrl = settingsRepository.supabaseUrl.trim().trimEnd('/')
        val anonKey = settingsRepository.supabaseAnonKey.trim()

        if (baseUrl.isBlank() || anonKey.isBlank()) {
            return@withContext Result.failure(Exception("Supabase URL atau Anon Key belum dikonfigurasi di Pengaturan."))
        }

        try {
            val endpoint = "$baseUrl/storage/v1/object/flproof-media/$remoteFileName"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $anonKey")
                setRequestProperty("Content-Type", mimeType)
                setRequestProperty("x-upsert", "true")
                connectTimeout = 30000
                readTimeout = 45000
            }

            FileInputStream(file).use { input ->
                conn.outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val publicUrl = "$baseUrl/storage/v1/object/public/flproof-media/$remoteFileName"
                Result.success(publicUrl)
            } else {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                Result.failure(Exception("Upload gagal ($code): $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mendaftarkan atau memperbarui Sesi Bukti pada tabel 'evidence_sessions'.
     */
    suspend fun upsertSession(evidence: EvidenceEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = settingsRepository.supabaseUrl.trim().trimEnd('/')
        val anonKey = settingsRepository.supabaseAnonKey.trim()

        if (baseUrl.isBlank() || anonKey.isBlank()) return@withContext Result.failure(Exception("Supabase belum dikonfigurasi."))

        try {
            val endpoint = "$baseUrl/rest/v1/evidence_sessions"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $anonKey")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "resolution=merge-duplicates")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val json = JSONObject().apply {
                put("session_id", evidence.sessionId ?: evidence.evidenceId)
                put("officer_name", evidence.officerName)
                put("category", evidence.category)
                put("address", evidence.address)
                put("latitude", evidence.latitude)
                put("longitude", evidence.longitude)
                put("gps_accuracy", evidence.gpsAccuracy.toDouble())
                put("status", evidence.status)
                put("barcode_value", evidence.barcodeValue ?: evidence.sessionId ?: evidence.evidenceId)
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use {
                it.write(json.toString())
            }

            val code = conn.responseCode
            if (code in 200..299) {
                Result.success(Unit)
            } else {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                Result.failure(Exception("Upsert session gagal ($code): $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Menyisipkan item foto atau video ke tabel 'evidence_media'.
     */
    suspend fun insertMediaRecord(
        sessionId: String,
        mediaType: String,
        fileUrl: String,
        sessionIndex: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = settingsRepository.supabaseUrl.trim().trimEnd('/')
        val anonKey = settingsRepository.supabaseAnonKey.trim()

        if (baseUrl.isBlank() || anonKey.isBlank()) return@withContext Result.failure(Exception("Supabase belum dikonfigurasi."))

        try {
            val endpoint = "$baseUrl/rest/v1/evidence_media"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $anonKey")
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val json = JSONObject().apply {
                put("session_id", sessionId)
                put("media_type", mediaType)
                put("file_url", fileUrl)
                put("session_index", sessionIndex)
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use {
                it.write(json.toString())
            }

            val code = conn.responseCode
            if (code in 200..299) {
                Result.success(Unit)
            } else {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                Result.failure(Exception("Insert media gagal ($code): $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sinkronisasi seluruh media (foto dan video) dalam satu sesi ke Supabase.
     */
    suspend fun syncSessionToCloud(sessionId: String): Result<Int> = withContext(Dispatchers.IO) {
        val items = evidenceRepository.getSessionPhotosList(sessionId)
        if (items.isEmpty()) {
            return@withContext Result.failure(Exception("Tidak ada media untuk ID $sessionId"))
        }

        val primaryEvidence = items.first()
        val sessionResult = upsertSession(primaryEvidence)
        if (sessionResult.isFailure) {
            return@withContext Result.failure(sessionResult.exceptionOrNull()!!)
        }

        var uploadedCount = 0
        for ((idx, item) in items.withIndex()) {
            val isVideo = item.mediaType == "video"
            val fileToUpload = if (isVideo) {
                item.videoPath?.let { File(it) }
            } else {
                File(item.watermarkedPhotoPath).let { if (it.exists()) it else File(item.originalPhotoPath) }
            }

            if (fileToUpload != null && fileToUpload.exists() && item.remoteUrl.isNullOrBlank()) {
                val ext = if (isVideo) "mp4" else "jpg"
                val mime = if (isVideo) "video/mp4" else "image/jpeg"
                val remoteName = "${sessionId}_${item.sessionIndex}_${System.currentTimeMillis()}.$ext"

                val uploadRes = uploadFileToStorage(fileToUpload, remoteName, mime)
                if (uploadRes.isSuccess) {
                    val publicUrl = uploadRes.getOrNull()!!
                    insertMediaRecord(sessionId, item.mediaType, publicUrl, item.sessionIndex)
                    evidenceRepository.updateEvidence(item.copy(remoteUrl = publicUrl, isSynced = true))
                    uploadedCount++
                }
            } else if (!item.remoteUrl.isNullOrBlank()) {
                uploadedCount++
            }
        }

        Result.success(uploadedCount)
    }
}
