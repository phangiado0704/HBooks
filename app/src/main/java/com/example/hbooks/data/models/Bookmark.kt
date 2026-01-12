package com.example.hbooks.data.models

data class Bookmark(
    val id: String = "",
    val bookId: String = "",
    val positionMs: Long = 0,
    val label: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
