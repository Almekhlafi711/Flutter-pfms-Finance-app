package com.example.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "asset_logs")
data class AssetLogEntity(
    @PrimaryKey val id: String,
    val assetId: String,
    val type: String, // PURCHASE, VALUE_UPDATE, SALE
    val title: String,
    val amount: Double,
    val previousValue: Double = 0.0,
    val newValue: Double = 0.0,
    val accountName: String = "",
    val date: Long = System.currentTimeMillis(),
    val notes: String = ""
)
