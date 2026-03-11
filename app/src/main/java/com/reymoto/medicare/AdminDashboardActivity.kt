package com.reymoto.medicare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class AdminDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val bottomNav = findViewById<BottomNavigationView>(R.id.adminBottomNavigation)

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(AdminDashboardFragment())
        }

        // Bottom navigation listener
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_admin_dashboard -> {
                    loadFragment(AdminDashboardFragment())
                    true
                }
                R.id.nav_admin_analytics -> {
                    loadFragment(AdminAnalyticsFragment())
                    true
                }
                else -> false
            }
        }

        // Logout button
        btnLogout.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.adminFragmentContainer, fragment)
            .commit()
    }
}
