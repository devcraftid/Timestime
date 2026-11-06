package com.firstlogistics.flproof

import com.firstlogistics.flproof.data.entity.EvidenceEntity
import com.firstlogistics.flproof.data.model.DocumentationCategory
import com.firstlogistics.flproof.sharing.ShareHelper
import com.firstlogistics.flproof.utils.DateFormatter
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class FLProofUnitTest {

    @Test
    fun testDocumentationCategoryFlags() {
        val barangRusak = DocumentationCategory.BARANG_RUSAK
        assertTrue(barangRusak.isProblematic)

        val barangHilang = DocumentationCategory.BARANG_HILANG
        assertTrue(barangHilang.isProblematic)

        val barangMasuk = DocumentationCategory.BARANG_MASUK
        assertTrue(barangMasuk.isIncoming)
        assertFalse(barangMasuk.isProblematic)

        val barangKeluar = DocumentationCategory.BARANG_KELUAR
        assertTrue(barangKeluar.isOutgoing)
        assertFalse(barangKeluar.isProblematic)

        val parseCategory = DocumentationCategory.fromTitle("PENERIMAAN PAKET")
        assertEquals(DocumentationCategory.PENERIMAAN_PAKET, parseCategory)
    }

    @Test
    fun testDateFormatting() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 5, 10, 30, 25)
        }
        val dateString = DateFormatter.getCurrentDateFormatted(cal.timeInMillis)
        assertTrue(dateString.contains("November 2026") || dateString.contains("2026"))

        val timeString = DateFormatter.getCurrentTimeFormatted(cal.timeInMillis)
        assertTrue(timeString.contains("10:30:25"))
    }

    @Test
    fun testShareMessageTemplate() {
        val entity = EvidenceEntity(
            id = 1,
            evidenceId = "FL-2026-000001",
            fileName = "EVD_FL-2026-000001.jpg",
            dateFormatted = "Kamis, 05 November 2026",
            timeFormatted = "10:30:25 WIB",
            timestamp = 1793863825000L,
            latitude = -6.2088,
            longitude = 106.8456,
            address = "Gudang Utama First Logistics, Jakarta Pusat",
            gpsAccuracy = 4.2f,
            compassDegrees = 315f,
            compassDirection = "BL 315°",
            originalPhotoPath = "/data/user/0/com.firstlogistics.flproof/files/FL_PROOF/Original/test.jpg",
            watermarkedPhotoPath = "/data/user/0/com.firstlogistics.flproof/files/FL_PROOF/Evidence/test.jpg",
            sha256Hash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            status = "VERIFIED",
            category = "BARANG MASUK",
            deviceModel = "Google Pixel 8 Pro",
            resolution = "4032x3024",
            fileSizeBytes = 3456789L
        )

        val formattedText = ShareHelper.formatShareText(entity)

        assertTrue(formattedText.contains("FIRST LOGISTICS"))
        assertTrue(formattedText.contains("BUKTI DOKUMENTASI"))
        assertTrue(formattedText.contains("FL-2026-000001"))
        assertTrue(formattedText.contains("BARANG MASUK"))
        assertTrue(formattedText.contains("Kamis, 05 November 2026"))
        assertTrue(formattedText.contains("10:30:25 WIB"))
        assertTrue(formattedText.contains("-6.2088, 106.8456"))
        assertTrue(formattedText.contains("FL PROOF"))
    }
}

