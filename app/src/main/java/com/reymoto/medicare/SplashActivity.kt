package com.reymoto.medicare

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_splash)
            
            auth = FirebaseAuth.getInstance()

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    // Check if user is already logged in
                    val currentUser = auth.currentUser
                    
                    if (currentUser != null) {
                        // User is logged in, go to dashboard
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // User is not logged in, go to login form
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } catch (e: Exception) {
                    Log.e("SplashActivity", "Error checking authentication", e)
                    // On error, default to login
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }, 3000) // 3 seconds delay
            
        } catch (e: Exception) {
            Log.e("SplashActivity", "Error in onCreate", e)
            // If there's an error, try to go directly to login
            try {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } catch (ex: Exception) {
                Log.e("SplashActivity", "Critical error", ex)
            }
        }
    }
}
