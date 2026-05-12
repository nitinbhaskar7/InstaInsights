package com.example.instainsights.viewmodel
// viewmodel/PostInsightsViewModelFactory.kt

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.instainsights.repository.PostInsightsRepository

class PostInsightsViewModelFactory(
    private val context   : Context,
    private val mediaId   : String,
    private val mediaType : String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = PostInsightsRepository(context.applicationContext)
        @Suppress("UNCHECKED_CAST")
        return PostInsightsViewModel(repo, mediaId, mediaType) as T
    }
}