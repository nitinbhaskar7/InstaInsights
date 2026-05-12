package com.example.instainsights

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object TokenManager {

    private const val FILE_NAME = "secure_prefs"
    private const val KEY_TOKEN = "access_token"
    private const val KEY_INSTAGRAM_ID = "instagram_id"


    private fun getPrefs(context: Context) = EncryptedSharedPreferences.create(
        FILE_NAME,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveSession(context: Context, token: String, instagramId: String) {
        val prefs = getPrefs(context)
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_INSTAGRAM_ID, instagramId)
            .apply()
    }

    fun getAccessToken(context: Context): String? {
        return getPrefs(context).getString(KEY_TOKEN, null)
    }

    fun getInstagramId(context: Context): String? {
        return getPrefs(context).getString(KEY_INSTAGRAM_ID, null)
    }

    fun clear(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}