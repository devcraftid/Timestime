package com.firstlogistics.flproof.data.model

enum class EvidenceStatus(val label: String) {
    VERIFIED("Terverifikasi"),
    PENDING("Menunggu Review"),
    FLAGGED("Bermasalah"),
    ARCHIVED("Diarsipkan"),
    TRASHED("Di Sampah");

    companion object {
        fun fromLabel(label: String?): EvidenceStatus {
            return entries.find { it.name.equals(label, ignoreCase = true) || it.label.equals(label, ignoreCase = true) } ?: VERIFIED
        }
    }
}

