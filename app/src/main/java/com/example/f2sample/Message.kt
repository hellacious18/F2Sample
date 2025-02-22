package com.example.f2sample

data class Message(
    val text: String,
    val isUser: Boolean // true = user message, false = AI response
)
