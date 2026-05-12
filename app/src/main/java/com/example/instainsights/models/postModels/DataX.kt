package com.example.instainsights.models.postModels

data class DataX(
    val description: String,
    val id: String,
    val name: String,
    val period: String,
    val title: String,
    val values: List<ValueX>
)