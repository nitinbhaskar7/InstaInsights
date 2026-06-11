package com.example.instainsights.network

import com.example.instainsights.models.AutoDmRequest
import com.example.instainsights.models.InsightsLikes
import com.example.instainsights.models.InsightsReach
import com.example.instainsights.models.Me
import com.example.instainsights.models.Posts
import com.example.instainsights.models.SuggestCaption
import com.example.instainsights.models.TimeSeriesResponse
import com.example.instainsights.models.UserSettings
import com.example.instainsights.models.caption.SuggestCaptionRequest
import com.example.instainsights.models.caption.SuggestCaptionResponse
import com.example.instainsights.models.postModels.ContentStats
import com.example.instainsights.models.postModels.PostPerformance
import com.example.instainsights.models.postModels.PostSettings
import com.example.instainsights.models.postModels.PostSettingsRequest
import com.example.instainsights.models.postModels.ReelStats
import com.example.instainsights.models.postModels.SentimentAnalysis
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface InstagramApiService {
    @GET("me")
    suspend fun getMe(@Query("access_token") token: String): Response<Me>

    @GET("insights/reach")
    suspend fun getInsightsReach(
        @Query("access_token") token: String,
        @Query("instagramId") instagramId: String
    ): Response<InsightsReach>

    @GET("insights/likes")
    suspend fun getInsightsLikes(
        @Query("access_token") token: String,
        @Query("instagramId") instagramId: String
    ): Response<InsightsLikes>

    @GET("posts")
    suspend fun getPosts(
        @Query("access_token") token: String,
        @Query("instagramId") instagramId: String
    ): Response<Posts>

    @GET("suggest-caption")
    suspend fun getSuggestCaption(@Query("image_url") imageUrl: String): Response<SuggestCaption>


    @GET("api/users/{userId}")
    suspend fun getUserSettings(
        @Path("userId") userId: String
    ): Response<UserSettings>

    // Save updated Auto DM settings
    @POST("api/users/auto-dm")
    suspend fun updateAutoDm(
        @Body request: AutoDmRequest
    ): Response<UserSettings>

    @GET("insights/time_series/{metric}")
    suspend fun getTimeSeries(
        @Path("metric")          metric     : String,
        @Query("access_token")   token      : String,
        @Query("instagramId")    instagramId: String,
        @Query("days")           days       : Int = 30
    ): Response<TimeSeriesResponse>

//    Post related endpoints

    @GET("content-stats")
    suspend fun getContentStats(
        @Query("mediaId") mediaId: String,
        @Query("access_token") token: String
    ): Response<ContentStats>

    @GET("reel-stats")
    suspend fun getReelStats(
        @Query("mediaId") mediaId: String,
        @Query("access_token") token: String
    ): Response<ReelStats>

    @GET("post-performance")
    suspend fun getPostPerformance(
        @Query("mediaId") mediaId: String,
        @Query("instagramId") instagramId: String,
        @Query("access_token") token: String
    ): Response<PostPerformance>

    @GET("analytics/comment-sentiment")
    suspend fun getCommentSentiment(
        @Query("mediaId") mediaId: String,
        @Query("access_token") token: String
    ): Response<SentimentAnalysis>


    //    Post settings
// Fetch saved settings for a single post
    @GET("api/posts/{postId}")
    suspend fun getPostSettings(
        @Path("postId") postId: String
    ): Response<PostSettings>

    // Save updated post settings
    @POST("api/posts/settings")
    suspend fun savePostSettings(
        @Body request: PostSettingsRequest
    ): Response<PostSettings>

    // Suggest Caption
    @POST("suggest-caption")
    suspend fun suggestCaption(
        @Body body: SuggestCaptionRequest
    ): Response<SuggestCaptionResponse>
}