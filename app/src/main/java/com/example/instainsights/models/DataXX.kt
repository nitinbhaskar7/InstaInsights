package com.example.instainsights.models
import java.io.Serializable

data class DataXX(
    val caption: String,
    val comments_count: Int,
    val id: String,
    val like_count: Int,
    val media_type: String,
    val media_url: String,
    val permalink: String,
    val timestamp: String
): Serializable