package com.example.f2sample.data

data class Message(
    val text: String,
    val isUser: Boolean, // true = user message, false = AI response
    val imageUrl: String? = null // Optional image URL
)
