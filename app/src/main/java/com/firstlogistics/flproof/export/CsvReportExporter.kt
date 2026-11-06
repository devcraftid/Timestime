package com.firstlogistics.flproof.export

import com.firstlogistics.flproof.data.entity.EvidenceEntity
import com.firstlogistics.flproof.utils.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvReportExporter(private val storageManager: StorageManager) {

    suspend fun exportToCsv(records: List<EvidenceEntity>): File? = withContext(Dispatchers.IO) {
        try {
            val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val csvFile = File(storageManager.reportsDir, "DATA_FLPROOF_$timestampStr.csv")

            OutputStreamWriter(FileOutputStream(csvFile), Charsets.UTF_8).use { writer ->
                // Write UTF-8 BOM for Excel compatibility
                writer.write("\uFEFF")

                // CSV Header
                writer.write("ID Dokumentasi,Kategori,Tanggal,Jam,Alamat,Latitude,Longitude,Akurasi (m),Arah Kompas,Kode Barcode,Tipe Barcode,SHA-256 Hash,Status,Perangkat,Resolusi,Ukuran File (bytes),Path Asli,Path Watermark\n")

                for (r in records) {
                    val line = listOf(
                        escapeCsv(r.evidenceId),
                        escapeCsv(r.category),
                        escapeCsv(r.dateFormatted),
                        escapeCsv(r.timeFormatted),
                        escapeCsv(r.address),
                        r.latitude.toString(),
                        r.longitude.toString(),
                        r.gpsAccuracy.toString(),
                        escapeCsv(r.compassDirection),
                        escapeCsv(r.barcodeValue ?: ""),
                        escapeCsv(r.barcodeFormat ?: ""),
                        escapeCsv(r.sha256Hash),
                        escapeCsv(r.status),
                        escapeCsv(r.deviceModel),
                        escapeCsv(r.resolution),
                        r.fileSizeBytes.toString(),
                        escapeCsv(r.originalPhotoPath),
                        escapeCsv(r.watermarkedPhotoPath)
                    ).joinToString(",")

                    writer.write(line + "\n")
                }
            }
            csvFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}

