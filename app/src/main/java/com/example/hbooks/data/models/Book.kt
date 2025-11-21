package com.example.hbooks.data.models
data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val coverImageUrl: String = "",
    val audioUrl: String = "",
    val categories: List<String> = emptyList()
)
