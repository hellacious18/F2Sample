package com.example.f2sample.data

data class Post(
    val imageUrl: String = "",
    val userProfileImageUrl: String = "",
    val userName: String = "",
    val likes: Int = 0,
    val comments: List<String> = emptyList(),
    val rating: Float = 0f
)
