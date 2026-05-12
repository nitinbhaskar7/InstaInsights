package com.example.instainsights.viewmodel
// viewmodel/DashboardViewModelFactory.kt

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.instainsights.repository.DashboardRepository

class DashboardViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = DashboardRepository(context.applicationContext)
        @Suppress("UNCHECKED_CAST")
        return DashboardViewModel(repo) as T
    }
}