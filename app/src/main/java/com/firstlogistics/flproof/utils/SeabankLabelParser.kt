package com.firstlogistics.flproof.utils

data class ParsedLabelResult(
    val recipient: String,
    val address: String,
    val cleanText: String,
    val rawText: String
)

object SeabankLabelParser {

    /**
     * Memfilter hasil OCR label alamat SeaBank secara cerdas dan toleran terhadap berbagai variasi format:
     * - Mendeteksi nama penerima dari label "Kepada", "Penerima", "Yth", "Kepada Yth.", dsb.
     * - Mendeteksi alamat dari label "Alamat", "Alamat Rumah", "Di alamat :", "Rumah", "Jl/Jalan", "Dusun", "RT/RW", dsb.
     * - Membersihkan header resi, barcode, disclaimer confidential, telp, website.
     * - Memperbaiki kesalahan umum OCR ("DUSUN |" -> "DUSUN I", "RT 0|" -> "RT 01").
     * - Menghasilkan format konsisten "Penerima :" dan "Alamat :".
     */
    fun extractFields(raw: String): ParsedLabelResult {
        if (raw.isBlank()) {
            return ParsedLabelResult("", "", "", raw)
        }

        val lines = raw.replace("\r", "\n")
            .split("\n")
            .map { it.replace("“", "\"").replace("”", "\"").replace("\\s+".toRegex(), " ").trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) {
            return ParsedLabelResult("", "", "", raw)
        }

        // Cari baris penerima (Kepada / Penerima / Yth / Nama)
        val recipientIndex = lines.indexOfFirst {
            "\\b(?:kepada|penerima|yth|nama\\s*penerima)\\b".toRegex(RegexOption.IGNORE_CASE).containsMatchIn(it)
        }

        var recipient = ""
        val remainingLines = mutableListOf<String>()

        if (recipientIndex >= 0) {
            var recLine = lines[recipientIndex]
            // Bersihkan tag [CONFIDENTIAL] jika menempel di baris penerima
            recLine = recLine.replace("^\\[CONFIDENTIAL\\]\\s*".toRegex(RegexOption.IGNORE_CASE), "").trim()

            // Bersihkan prefix secara berulang (misal "Kepada Yth." -> hapus "Kepada", lalu hapus "Yth.")
            val prefixRegex = "^(?:kepada|penerima|yth\\.?|yang\\s+terhormat|nama\\s*penerima|sdr(?:/i)?\\.?)\\s*[:\\-]?\\s*".toRegex(RegexOption.IGNORE_CASE)
            var cleanRec = recLine
            while (prefixRegex.containsMatchIn(cleanRec)) {
                cleanRec = cleanRec.replace(prefixRegex, "").trim()
            }
            recipient = cleanRec

            // Jika baris "Kepada:" tidak ada nama setelah titik dua, nama mungkin di baris berikutnya
            if (recipient.isBlank() && recipientIndex + 1 < lines.size) {
                var nextLine = lines[recipientIndex + 1].trim()
                while (prefixRegex.containsMatchIn(nextLine)) {
                    nextLine = nextLine.replace(prefixRegex, "").trim()
                }
                recipient = nextLine
                for (i in (recipientIndex + 2) until lines.size) {
                    remainingLines.add(lines[i])
                }
            } else {
                for (i in (recipientIndex + 1) until lines.size) {
                    remainingLines.add(lines[i])
                }
            }
        } else {
            // Jika tidak ada kata "Kepada", baris pertama (yang bukan CONFIDENTIAL) diasumsikan nama penerima
            val nonConfidential = lines.filter { !it.contains("CONFIDENTIAL", ignoreCase = true) }
            recipient = nonConfidential.firstOrNull() ?: lines.first()
            val startIdx = lines.indexOf(recipient)
            if (startIdx >= 0) {
                for (i in (startIdx + 1) until lines.size) {
                    remainingLines.add(lines[i])
                }
            } else {
                remainingLines.addAll(lines.drop(1))
            }
        }

        // Bersihkan nama penerima dari tanda baca berlebih
        recipient = recipient.trim(' ', ':', '-', ',', '.', '"', '\'', '[', ']').replace("\\s{2,}".toRegex(), " ")

        // Cari baris awal alamat
        // Cocokkan: "Di alamat :", "Alamat:", "Alamat Rumah:", "Alamat", "Rumah:", "Jl.", "Dusun", "RT", dsb.
        val addressKeywordIndex = remainingLines.indexOfFirst {
            "^(?:di\\s*)?alamat\\b|\\balamat\\s*(?:rumah|penerima|tujuan|pengiriman)?\\b|\\brumah\\s*:".toRegex(RegexOption.IGNORE_CASE).containsMatchIn(it)
        }

        val addressLines = mutableListOf<String>()

        if (addressKeywordIndex >= 0) {
            val firstLine = remainingLines[addressKeywordIndex].replace(
                "^.*?(?:di\\s*)?alamat\\s*(?:rumah|penerima|tujuan|pengiriman)?\\s*[:\\-]?\\s*|^.*?\\brumah\\s*[:\\-]?\\s*".toRegex(RegexOption.IGNORE_CASE),
                ""
            ).trim()

            if (firstLine.isNotBlank() && !isIgnoredLine(firstLine)) {
                addressLines.add(firstLine)
            }

            for (i in (addressKeywordIndex + 1) until remainingLines.size) {
                val line = remainingLines[i].trim()
                if (isIgnoredLine(line)) continue
                addressLines.add(cleanAddressLine(line))
            }
        } else {
            // Jika tidak ada kata "Alamat", kumpulkan semua baris sisa yang bukan header/noise
            for (line in remainingLines) {
                val trimmed = line.trim()
                if (isIgnoredLine(trimmed)) continue
                addressLines.add(cleanAddressLine(trimmed))
            }
        }

        // Jika address masih kosong tapi ada baris tersisa, ambil baris tersisa
        if (addressLines.isEmpty() && remainingLines.isNotEmpty()) {
            for (line in remainingLines) {
                val trimmed = line.trim()
                if (!isStrictNoiseLine(trimmed)) {
                    addressLines.add(cleanAddressLine(trimmed))
                }
            }
        }

        var address = addressLines.joinToString("\n").trim()

        // Perbaiki OCR confusion yang umum pada alamat resi:
        // "DUSUN |" => "DUSUN I", "DUSUN ||" => "DUSUN II"
        address = address.replace("\\bDUSUN\\s*[|!l1]\\b".toRegex(RegexOption.IGNORE_CASE), "DUSUN I")
            .replace("\\bDUSUN\\s*[|!l1]\\s*$".toRegex(RegexOption.IGNORE_CASE), "DUSUN I")
            .replace("\\bDUSUN\\s*\\|\\|\\s*$".toRegex(RegexOption.IGNORE_CASE), "DUSUN II")
            // Perbaiki pipe pada RT/RW: "RT 0| / RW 0|" => "RT 01 / RW 01"
            .replace("([Rr][Tt]\\s*[0-9]*)\\|([0-9]*)".toRegex(), "$11$2")
            .replace("([Rr][Ww]\\s*[0-9]*)\\|([0-9]*)".toRegex(), "$11$2")
            .replace("\\s{2,}".toRegex(), " ")

        val cleanText = buildString {
            if (recipient.isNotBlank()) append("Penerima : $recipient\n\n")
            if (address.isNotBlank()) append("Alamat :\n$address")
        }.trim()

        return ParsedLabelResult(
            recipient = recipient,
            address = address,
            cleanText = cleanText.ifBlank { raw },
            rawText = raw
        )
    }

    private fun isIgnoredLine(line: String): Boolean {
        if (line.length < 3) return true
        val lower = line.lowercase()

        // Abaikan disclaimer, barcode, web, kontak, dan logo perusahaan
        val noisePatterns = listOf(
            "confidential", "dokumen rahasia", "master barcode", "head office",
            "www.", "http", ".com", ".id", "website", "telepon", "telp", "fax",
            "customer service", "cs:", "call center", "seabank", "first logistics",
            "shopee xpress", "spx id", "j&t express", "sicepat", "pos indonesia"
        )
        if (noisePatterns.any { lower.contains(it) }) return true

        // Abaikan deretan barcode murni / angka acak panjang tanpa konteks alamat
        if ("^[0-9\\s|/.-]{8,}$".toRegex().matches(line) && !lower.contains("rt") && !lower.contains("rw")) {
            return true
        }

        // Abaikan baris simbol murni
        if ("^[\\W_]+$".toRegex().matches(line)) return true

        return false
    }

    private fun isStrictNoiseLine(line: String): Boolean {
        if (line.length < 3) return true
        val lower = line.lowercase()
        return lower.contains("confidential") || lower.contains("website") || lower.contains("barcode")
    }

    private fun cleanAddressLine(line: String): String {
        return line.replace("“", "\"").replace("”", "\"")
            .replace("\\s{2,}".toRegex(), " ")
            .trim()
    }
}
