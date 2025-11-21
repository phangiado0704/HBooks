package com.example.hbooks.data.models

data class Playlist(
    val id: String = "",
    val name: String = "",
    val bookIds: List<String> = emptyList()
)
