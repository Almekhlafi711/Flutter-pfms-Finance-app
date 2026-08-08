package com.example.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // REAL_ESTATE, VEHICLE, GOLD, STOCKS, CRYPTO, PROJECT, COLLECTIBLE
    val purchaseValue: Double,
    val currentValue: Double,
    val quantity: Double = 1.0,
    val unit: String = "Unit",
    val currency: String = "SAR",
    val notes: String = "",
    val purchaseDate: Long = System.currentTimeMillis(),
    val status: String = "ACTIVE",
    val purchaseAccountId: String = "",
    val purchaseAccountName: String = "",
    val soldPrice: Double? = null,
    val soldDate: Long? = null,
    val soldAccountId: String? = null,
    val soldAccountName: String? = null
)
