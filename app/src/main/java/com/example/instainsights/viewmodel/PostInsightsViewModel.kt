package com.example.instainsights.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instainsights.models.postModels.ContentStats
import com.example.instainsights.models.postModels.PostPerformance
import com.example.instainsights.models.postModels.PostSettings
import com.example.instainsights.models.postModels.ReelStats
import com.example.instainsights.models.postModels.SentimentAnalysis
import com.example.instainsights.repository.PostInsightsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


data class PostInsightsData(
    val contentStats : ContentStats,
    val performance  : PostPerformance,
    val sentiment    : SentimentAnalysis,
    // Null when the post is not a reel (VIDEO type)
    val reelStats    : ReelStats?,
    val postSettings : PostSettings
)

class PostInsightsViewModel(private val repo    : PostInsightsRepository,
private val mediaId : String,
private val mediaType: String           // "VIDEO" | "IMAGE" | "CAROUSEL_ALBUM"
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<PostInsightsData>>(UiState.Loading)
    val uiState: StateFlow<UiState<PostInsightsData>> = _uiState

    sealed class SaveState {
        object Idle : SaveState()
        object Saving : SaveState()
        object Saved : SaveState()
        data class Error(val message: String) : SaveState()
    }

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // Always fetch these three in parallel
                val statsDeferred = async { repo.fetchContentStats(mediaId) }
                val performanceDeferred = async { repo.fetchPostPerformance(mediaId) }
                val sentimentDeferred = async { repo.fetchCommentSentiment(mediaId) }
                val settingsDeferred = async { repo.fetchPostSettings(mediaId) }

                // Only fetch reel endpoints when it's a VIDEO
                val isReel = mediaType == "VIDEO"
                val reelStatsDeferred = if (isReel) async { repo.fetchReelStats(mediaId) } else null

                val stats = statsDeferred.await()
                val performance = performanceDeferred.await()
                val sentiment = sentimentDeferred.await()
                val reelStats = reelStatsDeferred?.await()
                val settings = settingsDeferred.await()

                if (!stats.isSuccessful || !performance.isSuccessful) {
                    _uiState.value = UiState.Error("Failed to load post insights")
                    return@launch
                }

                _uiState.value = UiState.Success(
                    PostInsightsData(
                        contentStats = stats.body()!!,
                        performance = performance.body()!!,
                        reelStats = reelStats?.body(),
                        sentiment = sentiment.body()!!,
                        postSettings = settings.body()
                        // Fallback defaults if post has no saved settings yet
                            ?: PostSettings(
                                postId = mediaId,
                                userId = "",
                                enableAutoHide = false,
                                enableAutoreply = "no",
                                message = null
                            )
                    )
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun savePostSettings(
        postId: String,
        enableAutoHide: Boolean,
        enableAutoreply: String,
        message: String?
    ) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            try {
                val response = repo.savePostSettings(
                    postId, enableAutoHide, enableAutoreply, message
                )
                _saveState.value = if (response.isSuccessful) SaveState.Saved
                else SaveState.Error("Failed to save settings")
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "Unknown error")
            } finally {
                kotlinx.coroutines.delay(2000)
                _saveState.value = SaveState.Idle
            }
        }


    }
}