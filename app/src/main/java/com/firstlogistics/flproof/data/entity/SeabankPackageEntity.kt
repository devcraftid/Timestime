package com.firstlogistics.flproof.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "seabank_packages")
data class SeabankPackageEntity(
    @PrimaryKey
    val id: String,
    val awb: String,
    val recipient: String,
    val address: String,
    val photoPath: String? = null,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val createdDateFormatted: String = ""
)

