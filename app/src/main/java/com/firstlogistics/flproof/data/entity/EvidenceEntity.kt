package com.firstlogistics.flproof.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "evidence_records")
data class EvidenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val evidenceId: String,
    val fileName: String,
    val dateFormatted: String,
    val timeFormatted: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val gpsAccuracy: Float,
    val compassDegrees: Float,
    val compassDirection: String,
    val originalPhotoPath: String,
    val watermarkedPhotoPath: String,
    val sha256Hash: String,
    val status: String,
    val category: String,
    val barcodeValue: String? = null,
    val barcodeFormat: String? = null,
    val deviceModel: String,
    val resolution: String,
    val fileSizeBytes: Long,
    val notes: String? = null,
    val pairEvidenceId: String? = null,
    val isSoftDeleted: Boolean = false,
    val sessionId: String? = null,
    val sessionIndex: Int = 1,
    val officerName: String = "Petugas Lapangan",
    val cameraLens: String = "Kamera Belakang",
    val mediaType: String = "photo", // "photo" atau "video"
    val videoPath: String? = null,
    val remoteUrl: String? = null,
    val isSynced: Boolean = false
)
