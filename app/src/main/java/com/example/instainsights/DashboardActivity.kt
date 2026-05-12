package com.example.instainsights

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class DashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
//
//        val token = TokenManager.getAccessToken(this)
//        val instagramId = TokenManager.getInstagramId(this)
//
//        if (token == null || instagramId == null) {
//            navigateToLogin()
//            return
//        }

        setContentView(R.layout.activity_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

//        val tokenTextView = findViewById<TextView>(R.id.tvToken)
//        val idTextView = findViewById<TextView>(R.id.tvInstaId)
//        val logoutButton = findViewById<Button>(R.id.button2)
//
//        tokenTextView.text = "Access Token:\n$token"
//        idTextView.text = "Instagram ID:\n$instagramId"
//
//        logoutButton.setOnClickListener {
//            logout()
//        }
    }
    private fun logout() {
        TokenManager.clear(this)

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}