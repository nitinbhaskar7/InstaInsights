package com.example.instainsights.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instainsights.models.AiOverview
import com.example.instainsights.models.DataXX
import com.example.instainsights.models.InsightsLikes
import com.example.instainsights.models.InsightsReach
import com.example.instainsights.models.Me
import com.example.instainsights.models.SeriesStats
import com.example.instainsights.models.TimeSeriesResponse
import com.example.instainsights.models.UserSettings
import com.example.instainsights.repository.DashboardRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
sealed class SaveState {
    object Idle    : SaveState()
    object Saving  : SaveState()
    object Saved   : SaveState()
    data class Error(val message: String) : SaveState()
}

sealed class CaptionState {
    object Idle                          : CaptionState()
    object Loading                       : CaptionState()
    data class Success(val caption: String) : CaptionState()
    data class Error(val message: String)   : CaptionState()
}
data class DashboardData(
    val profile: Me,
    val insightsreach: InsightsReach,
    val insightslikes: InsightsLikes,
    val allPosts: List<DataXX>,
    val userSettings   : UserSettings,
    val timeSeries    : TimeSeriesResponse
)
class DashboardViewModel(private val repo: DashboardRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<DashboardData>>(UiState.Loading)
    val uiState: StateFlow<UiState<DashboardData>> = _uiState


    private val _captionState = MutableStateFlow<CaptionState>(CaptionState.Idle)
    val captionState: StateFlow<CaptionState> = _captionState
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // Fire all 6 network calls in parallel using async
                val meDeferred = async { repo.fetchMe() }
                val reachDeferred = async { repo.fetchReach() }
                val likesDeferred = async { repo.fetchLikes() }
                val postsDeferred = async { repo.fetchPosts() }
                val settingsDeferred = async { repo.fetchUserSettings() }
                val timeSeriesDeferred   = async { repo.fetchTimeSeries() }
                // Await all — if any throws, the catch block handles it
                val me = meDeferred.await()
                val reach = reachDeferred.await()
                val likes = likesDeferred.await()
                val posts = postsDeferred.await()
                val settings = settingsDeferred.await()
                val timeSeries   = timeSeriesDeferred.await()
                // Check HTTP codes before accessing bodies
                if (!me.isSuccessful || !posts.isSuccessful) {
                    _uiState.value = UiState.Error("Failed to load profile or posts")
                    return@launch
                }

                Log.i("LOL2" , timeSeries.body().toString())

                val postsResponse = posts.body()

                val postList: List<DataXX> = postsResponse?.data
                    ?: emptyList()

                // Sum up daily values from insight responses
//                val reachSum   = reach.body()?.data?.firstOrNull()
//                    ?.data?.sumOf { it.value } ?: 0
//                val likesSum   = likes.body()?.data?.firstOrNull()
//                    ?.data?.sumOf { it.value } ?: 0


                _uiState.value = UiState.Success(
                    DashboardData(
                        profile = me.body()!!,
                        insightslikes = likes.body()!!,
                        insightsreach = reach.body()!!,
                        allPosts = postList,
                        userSettings = settings.body()
                        // Fallback to "no" if user record doesn't exist yet
                            ?: UserSettings(
                                userId = "",
                                enableAutoDM = "no",
                                message = null
                            ),
                        timeSeries     = timeSeries.body()
                            ?: TimeSeriesResponse(      // fallback if endpoint fails
                                metric      = "reach",
                                series      = emptyList(),
                                stats       = SeriesStats(0.0, 0, 0, "flat", ""),
                                ai_overview = AiOverview(null, null)
                            )
                    )
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun saveAutoDmSettings(mode: String, message: String?) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            try {
                val response = repo.saveAutoDmSettings(mode, message)
                _saveState.value = if (response.isSuccessful) SaveState.Saved
                else SaveState.Error("Save failed")
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "Unknown error")
            } finally {
                // Reset to Idle after 2 s so "Saved" badge disappears
                kotlinx.coroutines.delay(2000)
                _saveState.value = SaveState.Idle
            }
        }
    }

    fun suggestCaption(base64Image: String) {
        viewModelScope.launch {
            _captionState.value = CaptionState.Loading
            try {
                val response = repo.suggestCaption(base64Image)
                if (response.isSuccessful && response.body() != null) {
                    _captionState.value = CaptionState.Success(response.body()!!.caption)
                } else {
                    _captionState.value = CaptionState.Error("Failed to generate caption")
                }
            } catch (e: Exception) {
                _captionState.value = CaptionState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // Call this when the user dismisses the result so state resets cleanly
    fun resetCaptionState() {
        _captionState.value = CaptionState.Idle
    }
}
