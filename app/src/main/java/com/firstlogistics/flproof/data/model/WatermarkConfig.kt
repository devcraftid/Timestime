package com.firstlogistics.flproof.data.model

enum class WatermarkPosition(val label: String) {
    BOTTOM_LEFT("Bawah Kiri"),
    BOTTOM_RIGHT("Bawah Kanan"),
    TOP_LEFT("Atas Kiri"),
    TOP_RIGHT("Atas Kanan")
}

enum class WatermarkSize(val label: String, val scale: Float) {
    SMALL("Small", 0.75f),
    MEDIUM("Medium", 1.0f),
    LARGE("Large", 1.25f)
}

data class WatermarkConfig(
    val position: WatermarkPosition = WatermarkPosition.BOTTOM_LEFT,
    val size: WatermarkSize = WatermarkSize.MEDIUM,
    val showLogo: Boolean = true,
    val showCoordinates: Boolean = true,
    val showAddress: Boolean = true,
    val showAccuracy: Boolean = true,
    val showCompass: Boolean = true,
    val showResolution: Boolean = true,
    val companyName: String = "FIRST LOGISTICS",
    val officerName: String = ""
)

