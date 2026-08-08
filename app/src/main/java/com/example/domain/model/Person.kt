package com.example.domain.model

data class Person(
    val id: String,
    val name: String,
    val phone: String? = null,
    val category: String = "Personal", // Personal, Institutional
    val currency: String = "SAR",
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
