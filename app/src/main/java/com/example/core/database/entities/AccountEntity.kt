package com.example.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // CASH, BANK, WALLET, SAVINGS, CREDIT_CARD, INVESTMENT, CRYPTO
    val balance: Double,
    val currency: String = "SAR",
    val accountNumber: String = "",
    val colorHex: String = "#0EA5E9",
    val iconName: String = "bank",
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
