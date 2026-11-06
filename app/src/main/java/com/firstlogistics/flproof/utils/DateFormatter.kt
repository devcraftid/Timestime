package com.firstlogistics.flproof.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateFormatter {

    private val indonesianLocale = Locale("id", "ID")

    fun getCurrentDateFormatted(timestamp: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", indonesianLocale)
        return sdf.format(Date(timestamp))
    }

    fun getCurrentShortDateFormatted(timestamp: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", indonesianLocale)
        return sdf.format(Date(timestamp))
    }

    fun getCurrentTimeFormatted(timestamp: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
        val tzSuffix = getTimeZoneAbbreviation()
        return "${sdf.format(Date(timestamp))} $tzSuffix"
    }

    fun getTimeZoneAbbreviation(): String {
        val tz = TimeZone.getDefault()
        val rawOffsetHours = tz.rawOffset / (1000 * 60 * 60)
        return when (rawOffsetHours) {
            7 -> "WIB"
            8 -> "WITA"
            9 -> "WIT"
            else -> {
                val displayName = tz.getDisplayName(false, TimeZone.SHORT)
                if (displayName.contains("GMT") || displayName.contains("UTC")) displayName else "GMT+$rawOffsetHours"
            }
        }
    }

    fun formatFullDateTime(timestamp: Long = System.currentTimeMillis()): String {
        return "${getCurrentDateFormatted(timestamp)}, ${getCurrentTimeFormatted(timestamp)}"
    }
}

