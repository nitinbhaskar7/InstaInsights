package com.example.instainsights.repository
import android.content.Context
import com.example.instainsights.TokenManager
import com.example.instainsights.models.postModels.PostSettings
import com.example.instainsights.models.postModels.PostSettingsRequest
import com.example.instainsights.network.RetrofitClient
import retrofit2.Response

class PostInsightsRepository(private val context: Context) {
    private val api   = RetrofitClient.apiService
    private fun getToken() = TokenManager.getAccessToken(context)
        ?: throw IllegalStateException("No access token stored")

    // Instagram User ID is returned by /me; cache it after first call
    // Or store it alongside the token during login
    private fun getInstagramId() = TokenManager.getInstagramId(context)
        ?: throw IllegalStateException("No Instagram ID stored")

    suspend fun fetchContentStats(mediaId: String) = api.getContentStats(mediaId , getToken())
    suspend fun fetchReelStats(mediaId: String) = api.getReelStats(mediaId , getToken())
    suspend fun fetchPostPerformance(mediaId: String) =  api.getPostPerformance(mediaId, getInstagramId() , getToken())
    suspend fun fetchCommentSentiment(mediaId: String) =  api.getCommentSentiment(mediaId, getToken())

    suspend fun fetchPostSettings(postId: String) = api.getPostSettings(postId)

    suspend fun savePostSettings(
        postId         : String,
        enableAutoHide : Boolean,
        enableAutoreply: String,
        message        : String?
    ): Response<PostSettings> {
        return api.savePostSettings(
            PostSettingsRequest(
                userId          = getInstagramId(),
                postId          = postId,
                enableAutoHide  = enableAutoHide,
                enableAutoreply = enableAutoreply,
                message         = message
            )
        )
    }
}