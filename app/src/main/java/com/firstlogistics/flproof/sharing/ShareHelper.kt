package com.firstlogistics.flproof.sharing

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.firstlogistics.flproof.data.entity.EvidenceEntity
import java.io.File
import java.util.ArrayList

object ShareHelper {

    fun formatShareText(evidence: EvidenceEntity): String {
        val coordText = if (evidence.latitude != 0.0) {
            "${evidence.latitude}, ${evidence.longitude}"
        } else {
            "Tidak tersedia"
        }

        return """
            FIRST LOGISTICS
            BUKTI DOKUMENTASI

            ID: ${evidence.evidenceId}
            Kategori: ${evidence.category}
            Tanggal: ${evidence.dateFormatted}
            Jam: ${evidence.timeFormatted}
            Lokasi: ${evidence.address}
            Koordinat: $coordText

            Dokumentasi dibuat menggunakan FL PROOF.
        """.trimIndent()
    }

    fun shareEvidence(
        context: Context,
        evidence: EvidenceEntity,
        useWatermarked: Boolean = true
    ) {
        val photoPath = if (useWatermarked) evidence.watermarkedPhotoPath else evidence.originalPhotoPath
        val file = File(photoPath)

        if (!file.exists()) {
            Toast.makeText(context, "File foto tidak ditemukan: $photoPath", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val text = formatShareText(evidence)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_SUBJECT, "Bukti Dokumentasi FL PROOF: ${evidence.evidenceId}")
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(context.contentResolver, "Evidence Photo", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Bagikan Bukti Dokumentasi (${evidence.evidenceId})").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Grant read URI permission to all possible receiving apps (e.g. WhatsApp)
            val resInfoList = context.packageManager.queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membagikan ke WhatsApp/Aplikasi lain: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun shareMultiple(
        context: Context,
        evidences: List<EvidenceEntity>,
        useWatermarked: Boolean = true
    ) {
        if (evidences.isEmpty()) return

        try {
            val imageUris = ArrayList<Uri>()
            val sb = StringBuilder()
            sb.append("FIRST LOGISTICS\nBUKTI DOKUMENTASI KOLEKTIF\n\n")

            for (e in evidences) {
                val path = if (useWatermarked) e.watermarkedPhotoPath else e.originalPhotoPath
                val file = File(path)
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    imageUris.add(uri)
                }
                sb.append("• ${e.evidenceId} [${e.category}] - ${e.dateFormatted} ${e.timeFormatted}\n")
            }
            sb.append("\nDokumentasi dibuat menggunakan FL PROOF.")

            if (imageUris.isEmpty()) {
                Toast.makeText(context, "Tidak ada file foto valid untuk dibagikan", Toast.LENGTH_SHORT).show()
                return
            }

            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/jpeg"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)
                putExtra(Intent.EXTRA_TEXT, sb.toString())

                val clipData = ClipData.newUri(context.contentResolver, "Evidence 1", imageUris.first())
                for (i in 1 until imageUris.size) {
                    clipData.addItem(ClipData.Item(imageUris[i]))
                }
                this.clipData = clipData
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Bagikan ${evidences.size} Bukti Dokumentasi").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val resInfoList = context.packageManager.queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                for (uri in imageUris) {
                    context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membagikan bukti kolektif: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        if (!file.exists()) {
            Toast.makeText(context, "File tidak ditemukan: ${file.name}", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                clipData = ClipData.newUri(context.contentResolver, file.name, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val resInfoList = context.packageManager.queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membagikan file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
