package com.example.instainsights.models.postModels

data class SentimentAnalysis(
    val mediaId: String,
    val overall: Overall,
    val totalComments : Int
)