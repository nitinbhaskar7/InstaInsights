package com.example.instainsights.models.postModels

import java.io.Serializable


data class Post(
    val id: String,
    val caption: String?,
    val media_type: String,
    val media_url: String?,
    val permalink: String,
    val timestamp: String,
    val like_count: Int,
    val comments_count: Int
) : Serializable