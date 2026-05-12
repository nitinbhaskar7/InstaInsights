package com.example.instainsights.models

// models/ApiModels.kt

// Response from GET /api/user/:userId
data class UserSettings(
    val userId: String,
    val enableAutoDM: String,   // "no" | "static" | "ai"
    val message: String?        // static reply text or AI prompt
)

// Request body for POST /api/user/auto-dm
data class AutoDmRequest(

    val userId: String,
    val enableAutoDM: String,   // "no" | "static" | "ai"
    val message: String?
)
