package com.firstlogistics.flproof.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class DocumentationCategory(
    val title: String,
    val isProblematic: Boolean = false,
    val isIncoming: Boolean = false,
    val isOutgoing: Boolean = false
) {
    NONE("TANPA KATEGORI"),
    BARANG_MASUK("BARANG MASUK", isIncoming = true),
    BARANG_KELUAR("BARANG KELUAR", isOutgoing = true),
    PENERIMAAN_PAKET("PENERIMAAN PAKET", isIncoming = true),
    PENGIRIMAN("PENGIRIMAN", isOutgoing = true),
    PEMERIKSAAN_BARANG("PEMERIKSAAN BARANG"),
    BARANG_RUSAK("BARANG RUSAK", isProblematic = true),
    BARANG_HILANG("BARANG HILANG", isProblematic = true),
    GUDANG("GUDANG"),
    KENDARAAN("KENDARAAN"),
    SERAH_TERIMA("SERAH TERIMA"),
    DOKUMENTASI_UMUM("DOKUMENTASI UMUM");

    companion object {
        fun fromTitle(title: String?): DocumentationCategory {
            return entries.find { it.title.equals(title, ignoreCase = true) } ?: NONE
        }
    }
}

