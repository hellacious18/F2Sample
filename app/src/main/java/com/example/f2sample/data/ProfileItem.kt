package com.example.f2sample.data

import androidx.annotation.DrawableRes

data class ProfileItem(
    @DrawableRes val icon: Int?,
    val title: String,
    val value: String? = null,
    val iconTint: Int? = null
)