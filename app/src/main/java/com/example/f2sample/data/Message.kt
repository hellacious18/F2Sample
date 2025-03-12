package com.example.f2sample.data

data class Message(
    val text: String? = null,
    var isUser: Boolean = false,
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
