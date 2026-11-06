package com.firstlogistics.flproof.export

import android.content.Context
import com.firstlogistics.flproof.utils.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupRestoreManager(
    private val context: Context,
    private val storageManager: StorageManager
) {

    suspend fun createBackup(): File? = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupFile = File(storageManager.exportsDir, "FL_PROOF_BACKUP_$timestamp.zip")

            ZipOutputStream(BufferedOutputStream(FileOutputStream(backupFile))).use { zos ->
                // Backup database files
                val dbDir = context.getDatabasePath("fl_proof_database").parentFile
                if (dbDir != null && dbDir.exists()) {
                    val dbFiles = dbDir.listFiles { _, name -> name.startsWith("fl_proof_database") }
                    dbFiles?.forEach { dbFile ->
                        zipFile(dbFile, "database/${dbFile.name}", zos)
                    }
                }

                // Backup Original and Evidence images
                zipDirectory(storageManager.originalDir, "Original", zos)
                zipDirectory(storageManager.evidenceDir, "Evidence", zos)
            }
            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun restoreBackup(zipFile: File): Boolean = withContext(Dispatchers.IO) {
        if (!zipFile.exists()) return@withContext false
        try {
            val dbDir = context.getDatabasePath("fl_proof_database").parentFile
            if (dbDir != null && !dbDir.exists()) dbDir.mkdirs()

            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                val buffer = ByteArray(8192)

                while (entry != null) {
                    val entryName = entry.name
                    if (!entry.isDirectory) {
                        val targetFile: File? = when {
                            entryName.startsWith("database/") -> {
                                val fileName = entryName.removePrefix("database/")
                                File(dbDir, fileName)
                            }
                            entryName.startsWith("Original/") -> {
                                val fileName = entryName.removePrefix("Original/")
                                File(storageManager.originalDir, fileName)
                            }
                            entryName.startsWith("Evidence/") -> {
                                val fileName = entryName.removePrefix("Evidence/")
                                File(storageManager.evidenceDir, fileName)
                            }
                            else -> null
                        }

                        if (targetFile != null) {
                            targetFile.parentFile?.mkdirs()
                            FileOutputStream(targetFile).use { fos ->
                                var len: Int
                                while (zis.read(buffer).also { len = it } > 0) {
                                    fos.write(buffer, 0, len)
                                }
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun zipFile(file: File, entryPath: String, zos: ZipOutputStream) {
        if (!file.exists()) return
        val buffer = ByteArray(8192)
        FileInputStream(file).use { fis ->
            val entry = ZipEntry(entryPath)
            zos.putNextEntry(entry)
            var count: Int
            while (fis.read(buffer).also { count = it } > 0) {
                zos.write(buffer, 0, count)
            }
            zos.closeEntry()
        }
    }

    private fun zipDirectory(dir: File, basePath: String, zos: ZipOutputStream) {
        if (!dir.exists()) return
        val files = dir.listFiles() ?: return
        for (f in files) {
            if (f.isFile) {
                zipFile(f, "$basePath/${f.name}", zos)
            }
        }
    }
}

