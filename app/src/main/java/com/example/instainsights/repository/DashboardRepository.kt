package com.example.instainsights.repository

import android.content.Context
import com.example.instainsights.TokenManager
import com.example.instainsights.models.AutoDmRequest
import com.example.instainsights.models.TimeSeriesResponse
import com.example.instainsights.models.UserSettings
import com.example.instainsights.models.caption.SuggestCaptionRequest
import com.example.instainsights.network.RetrofitClient
import okhttp3.Response

class DashboardRepository(private val context: Context) {
    private val api = RetrofitClient.apiService
    private fun getToken() = TokenManager.getAccessToken(context)
        ?: throw IllegalStateException("No access token stored")

    // Instagram User ID is returned by /me; cache it after first call
    // Or store it alongside the token during login
    private fun getInstagramId() = TokenManager.getInstagramId(context)
        ?: throw IllegalStateException("No Instagram ID stored")

    suspend fun fetchMe() = api.getMe(getToken())
    suspend fun fetchReach() = api.getInsightsReach(getToken(), getInstagramId())

    suspend fun fetchTimeSeries(days: Int = 30) =
        api.getTimeSeries(
            metric      = "reach",
            token       = getToken(),
            instagramId = getInstagramId(),
            days        = days
        )
    suspend fun fetchLikes() = api.getInsightsLikes(getToken(), getInstagramId())
    suspend fun fetchPosts() = api.getPosts(getToken() , getInstagramId())

    suspend fun fetchUserSettings() = api.getUserSettings(getInstagramId())

    suspend fun saveAutoDmSettings(
        mode: String,       // "no" | "static" | "ai"
        message: String?
    ) = api.updateAutoDm(
            AutoDmRequest(
                userId     = getInstagramId(),
                enableAutoDM = mode,
                message    = message
            )
        )

    suspend fun suggestCaption(base64Image: String) =
        api.suggestCaption(SuggestCaptionRequest(image_base64 = base64Image))

}