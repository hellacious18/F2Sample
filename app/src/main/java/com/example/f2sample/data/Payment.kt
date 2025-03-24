package com.example.f2sample.data

data class Payment(
    val paymentId: String = "",
    val amount: Double = 0.0,
    val currency: String = "",
    val timestamp: com.google.firebase.Timestamp? = null
)
