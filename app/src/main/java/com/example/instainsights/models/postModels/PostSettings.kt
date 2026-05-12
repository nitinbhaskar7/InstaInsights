package com.example.instainsights.models.postModels
// Response from GET /api/posts/:postId
data class PostSettings(
    val postId         : String,
    val userId         : String,
    val enableAutoHide : Boolean,
    val enableAutoreply: String,   // "no" | "static" | "ai"
    val message        : String?
)

// Request body for POST /api/posts/settings
data class PostSettingsRequest(
    val userId         : String,
    val postId         : String,
    val enableAutoHide : Boolean,
    val enableAutoreply: String,
    val message        : String?
)