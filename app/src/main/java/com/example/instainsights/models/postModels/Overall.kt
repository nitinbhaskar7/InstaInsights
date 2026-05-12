package com.example.instainsights.models.postModels

data class Overall(
    val breakdown: Breakdown,
    val score: Double,
    val sentiment: String,
    val summary: String
)