package com.example.f2sample.data

data class Comment(
    val userId: String = "",
    val userName: String = "",
    val userProfileImageUrl: String = "",
    val commentText: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class Post(
    val postId: String = "",
    val imageUrl: String = "",
    val userProfileImageUrl: String = "",
    val userName: String = "",
    val userId: String = "",
    var likes: Int = 0,
    var likedBy: List<String> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val rating: Float = 0f
)
