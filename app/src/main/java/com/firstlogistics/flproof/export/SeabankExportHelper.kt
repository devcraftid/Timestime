package com.firstlogistics.flproof.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.firstlogistics.flproof.data.entity.SeabankPackageEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SeabankExportHelper {

    fun exportToCsv(context: Context, packages: List<SeabankPackageEntity>): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "First_Logistics_SeaBank_${packages.size}_AWB_$timeStamp.csv"
        val exportDir = File(context.filesDir, "FL_PROOF/Exports").apply { if (!exists()) mkdirs() }
        val file = File(exportDir, fileName)

        FileOutputStream(file).use { fos ->
            // Write UTF-8 BOM so Excel opens it with proper encoding
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                writer.append("No,Nomor AWB,Penerima,Alamat,Tanggal Input\n")
                packages.forEachIndexed { index, item ->
                    val cleanRecipient = item.recipient.replace("\"", "\"\"")
                    val cleanAddress = item.address.replace("\n", " ").replace("\"", "\"\"")
                    writer.append("${index + 1},\"${item.awb}\",\"$cleanRecipient\",\"$cleanAddress\",\"${item.createdDateFormatted}\"\n")
                }
            }
        }
        return file
    }

    fun exportToDocument(context: Context, packages: List<SeabankPackageEntity>): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "First_Logistics_SeaBank_${packages.size}_AWB_$timeStamp.doc"
        val exportDir = File(context.filesDir, "FL_PROOF/Exports").apply { if (!exists()) mkdirs() }
        val file = File(exportDir, fileName)

        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                writer.append("====================================================\n")
                writer.append("FIRST LOGISTICS — DATA ALAMAT PENERIMA SEABANK\n")
                writer.append("Total Paket: ${packages.size} AWB\n")
                writer.append("Tanggal Ekspor: ${SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date())}\n")
                writer.append("====================================================\n\n")

                packages.forEachIndexed { index, item ->
                    writer.append("[${index + 1}] NOMOR AWB: ${item.awb}\n")
                    writer.append("Penerima : ${item.recipient}\n")
                    writer.append("Alamat :\n${item.address}\n")
                    writer.append("Waktu Input: ${item.createdDateFormatted}\n")
                    writer.append("----------------------------------------------------\n\n")
                }
            }
        }
        return file
    }

    fun exportToPdf(context: Context, packages: List<SeabankPackageEntity>): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "First_Logistics_SeaBank_${packages.size}_AWB_$timeStamp.pdf"
        val exportDir = File(context.filesDir, "FL_PROOF/Exports").apply { if (!exists()) mkdirs() }
        val file = File(exportDir, fileName)

        val document = PdfDocument()
        val pageWidth = 595 // A4 standard width in points
        val pageHeight = 842 // A4 standard height

        val titlePaint = Paint().apply {
            color = Color.rgb(16, 43, 78) // Navy #102B4E
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subPaint = Paint().apply {
            color = Color.rgb(23, 105, 170) // Blue #1769AA
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyBoldPaint = Paint().apply {
            color = Color.rgb(23, 37, 54) // #172536
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.rgb(97, 114, 134) // #617286
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(220, 229, 238) // #DCE5EE
            strokeWidth = 1f
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        var y = 45f

        fun drawHeader() {
            canvas.drawText("FIRST LOGISTICS — ALOKASI SEABANK", 36f, y, titlePaint)
            y += 16f
            canvas.drawText("DATA ALAMAT PENERIMA PAKET & DOKUMENTASI RESI", 36f, y, subPaint)
            y += 12f
            canvas.drawLine(36f, y, (pageWidth - 36).toFloat(), y, linePaint)
            y += 18f
        }

        drawHeader()

        packages.forEachIndexed { idx, pkg ->
            if (y > pageHeight - 110) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 45f
                drawHeader()
            }

            canvas.drawText("${idx + 1}. AWB: ${pkg.awb}", 36f, y, bodyBoldPaint)
            canvas.drawText(pkg.createdDateFormatted, (pageWidth - 160).toFloat(), y, bodyPaint)
            y += 14f

            canvas.drawText("Penerima : ${pkg.recipient}", 48f, y, bodyBoldPaint)
            y += 13f

            val addressLines = pkg.address.split("\n")
            canvas.drawText("Alamat :", 48f, y, bodyBoldPaint)
            y += 11f
            for (line in addressLines) {
                if (line.isNotBlank()) {
                    canvas.drawText(line, 58f, y, bodyPaint)
                    y += 11f
                }
            }

            y += 4f
            canvas.drawLine(36f, y, (pageWidth - 36).toFloat(), y, linePaint)
            y += 14f
        }

        document.finishPage(page)

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        return file
    }
}

