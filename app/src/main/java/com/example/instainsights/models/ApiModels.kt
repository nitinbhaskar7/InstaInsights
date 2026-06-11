package com.example.instainsights.models

data class TimeSeriesResponse(
    val metric     : String,
    val series     : List<SeriesPoint>,
    val stats      : SeriesStats,
    val ai_overview: AiOverview
)

data class SeriesPoint(
    val date : String,   // "2025-04-28"
    val value: Int
)

data class SeriesStats(
    val avg     : Double,
    val max     : Int,
    val min     : Int,
    val trend   : String,   // "up" | "down" | "flat"
    val peakDate: String
)

data class AiOverview(
    val summary       : String?,
    val recommendation: String?
)