package com.reymoto.medicare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        auth = FirebaseAuth.getInstance()

        val ivAdminMenu = findViewById<ImageView>(R.id.ivAdminMenu)
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

        // Admin menu icon click
        ivAdminMenu.setOnClickListener { showAdminMenu(ivAdminMenu) }
    }

    private fun showAdminMenu(anchor: View) {
        val popupMenu = PopupMenu(this, anchor)
        
        // Add menu items


        popupMenu.menu.add(0, 4, 0, "⚙️ Admin Settings")
        popupMenu.menu.add(0, 5, 0, "📱 System Info")
        popupMenu.menu.add(0, 7, 0, "📊 Limit Queue")
        popupMenu.menu.add(0, 6, 0, "🚪 Logout")
        
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {


                4 -> {
                    showAdminSettings()
                    true
                }
                5 -> {
                    showSystemInfo()
                    true
                }
                7 -> {
                    showLimitQueueDialog()
                    true
                }
                6 -> {
                    showLogoutConfirmation()
                    true
                }
                else -> false
            }
        }
        
        popupMenu.show()
    }







    private fun showAdminSettings() {
        AlertDialog.Builder(this)
            .setTitle("⚙️ Admin Settings")
            .setMessage("Current Configuration:\n\n• Auto-reset: Daily at midnight\n• Location radius: within campus\n• Max daily queues: 1500\n\nContact system administrator for changes.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSystemInfo() {
        AlertDialog.Builder(this)
            .setTitle("📱 System Information")
            .setMessage("MediCare Queue Management System\n\nVersion: 1.0.0\nBuild: 2024.03.13\nDatabase: Firebase Firestore\nAuthentication: Firebase Auth\n\nDeveloped for:\nSouthwestern University PHINMA\nUrgello, Cebu City\n\nAdmin Panel Features:\n• Real-time queue management\n• Analytics & reporting\n• Multi-department support\n• Location-based validation")
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showLimitQueueDialog() {
        AlertDialog.Builder(this)
            .setTitle("📊 Queue Limit Settings")
            .setMessage("Daily Queue Limit Configuration:\n\n• Maximum queues per day: 1500\n• Limit helps manage server load\n• Resets automatically at midnight\n\nNote: This is a system configuration setting.")
            .setPositiveButton("OK", null)
            .show()
    }


    private fun showLogoutConfirmation() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("🚪 Logout")
            .setMessage("Are you sure you want to logout from the admin panel?")
            .setPositiveButton("Logout", null)
            .setNegativeButton("Cancel", null)
            .create()
            
        dialog.setOnShowListener {
            // Make the logout button text red
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(android.graphics.Color.RED)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                performLogout()
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }

    private fun performLogout() {
        auth.signOut()
        
        // Clear admin session
        val adminSessionPrefs = getSharedPreferences("AdminSession", MODE_PRIVATE)
        adminSessionPrefs.edit().clear().apply()
        
        // Clear admin preferences
        val prefs = getSharedPreferences("AdminQueuePrefs", MODE_PRIVATE)
        prefs.edit().clear().apply()
        
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        
        // Navigate back to login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.adminFragmentContainer, fragment)
            .commit()
    }
}
