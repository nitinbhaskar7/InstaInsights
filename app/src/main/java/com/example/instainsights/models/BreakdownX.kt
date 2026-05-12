package com.example.instainsights.models

data class BreakdownX(
    val dimension_keys: List<String>,
    val results: List<Result>
)