package com.example.instainsights

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent?.data

        val token = uri?.getQueryParameter("token")
        val instagramId = uri?.getQueryParameter("instagram_id")

        if (token != null && instagramId != null) {

            TokenManager.saveSession(this, token, instagramId)

            startActivity(Intent(this, DashboardActivity::class.java))
            finish()

        } else {
            finish()
        }
    }
}