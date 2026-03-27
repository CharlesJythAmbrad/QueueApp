package com.reymoto.medicare

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Check for saved admin session
        checkAdminSession()

        val idNumber = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etIdNumber)
        val password = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPassword)
        val login = findViewById<Button>(R.id.btnLogin)
        val registerText = findViewById<TextView>(R.id.tvRegister)
        val forgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        login.setOnClickListener {
            val idInput = idNumber.text.toString().trim()
            val passwordInput = password.text.toString().trim()

            // Validation
            if (idInput.isEmpty() || passwordInput.isEmpty()) {
                Toast.makeText(this, "Please enter ID number and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check for admin credentials
            if (idInput == "00-0000-000000" && passwordInput == "QueueAdmin") {
                // Save admin session
                saveAdminSession()
                
                // Admin login - go directly to admin dashboard
                Toast.makeText(this, "Admin login successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, AdminDashboardActivity::class.java))
                finish()
                return@setOnClickListener
            }

            // Validate ID format (05-2425-000000)
            if (!isValidIdFormat(idInput)) {
                idNumber.error = "Invalid ID format. Use: 05-2425-000000"
                Toast.makeText(this, "Invalid ID format. Use: 05-2425-000000", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Look up user's Gmail from Firestore using ID number
            db.collection("users")
                .whereEqualTo("idNumber", idInput)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        Toast.makeText(this, "ID number not found. Please register first.", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    val userDoc = documents.documents[0]
                    val userEmail = userDoc.getString("email")
                    
                    if (userEmail == null) {
                        Toast.makeText(this, "User email not found. Please contact support.", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    // Now authenticate with Firebase using the Gmail
                    auth.signInWithEmailAndPassword(userEmail, passwordInput)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val fullName = userDoc.getString("fullName") ?: "User"
                                val course = userDoc.getString("course") ?: ""
                                showLoginSuccessDialog(fullName, idInput, course)
                            } else {
                                Toast.makeText(this, "Invalid password", Toast.LENGTH_LONG).show()
                            }
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error looking up user: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        forgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }

        registerText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun checkAdminSession() {
        val prefs = getSharedPreferences("AdminSession", MODE_PRIVATE)
        val isAdminLoggedIn = prefs.getBoolean("isAdminLoggedIn", false)
        
        if (isAdminLoggedIn) {
            // Admin is already logged in, go directly to admin dashboard
            startActivity(Intent(this, AdminDashboardActivity::class.java))
            finish()
        }
    }

    private fun saveAdminSession() {
        val prefs = getSharedPreferences("AdminSession", MODE_PRIVATE)
        prefs.edit()
            .putBoolean("isAdminLoggedIn", true)
            .apply()
    }

    private fun showLoginSuccessDialog(fullName: String, idNumber: String, course: String) {
        val message = if (course.isNotEmpty()) {
            "Welcome back, $fullName!\n\nID Number: $idNumber\nCourse: $course\n\nYou can now access the Smart Queue System."
        } else {
            "Welcome back, $fullName!\n\nID Number: $idNumber\n\nYou can now access the Smart Queue System."
        }
        
        AlertDialog.Builder(this)
            .setTitle("✓ Login Successful")
            .setMessage(message)
            .setPositiveButton("Continue") { _, _ ->
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showForgotPasswordDialog() {
        val dialogView = layoutInflater.inflate(android.R.layout.simple_list_item_1, null)
        val editText = EditText(this).apply {
            hint = "Enter your ID Number"
            setPadding(50, 40, 50, 40)
        }

        AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("Enter your ID number to receive a password reset email")
            .setView(editText)
            .setPositiveButton("Send Reset Email") { _, _ ->
                val idInput = editText.text.toString().trim()
                
                if (idInput.isEmpty()) {
                    Toast.makeText(this, "Please enter your ID number", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (!isValidIdFormat(idInput)) {
                    Toast.makeText(this, "Invalid ID format. Use: 05-2425-000000", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Convert ID to email format
                sendPasswordResetEmail(idInput)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendPasswordResetEmail(idNumber: String) {
        // Look up user's Gmail from Firestore using ID number
        db.collection("users")
            .whereEqualTo("idNumber", idNumber)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "ID number not found. Please check your ID number.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val userDoc = documents.documents[0]
                val userEmail = userDoc.getString("email")
                
                if (userEmail == null) {
                    Toast.makeText(this, "User email not found. Please contact support.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                // Send password reset email to the user's Gmail
                auth.sendPasswordResetEmail(userEmail)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            AlertDialog.Builder(this)
                                .setTitle("Email Sent")
                                .setMessage("Password reset email has been sent to $userEmail. Please check your email inbox and follow the instructions to reset your password.")
                                .setPositiveButton("OK", null)
                                .show()
                        } else {
                            Toast.makeText(this, "Failed to send reset email. Please try again.", Toast.LENGTH_LONG).show()
                        }
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error looking up user: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun isValidIdFormat(id: String): Boolean {
        // Format: 05-2425-000000 (2 digits - 4 digits - 6 digits)
        val pattern = "^\\d{2}-\\d{4}-\\d{6}$".toRegex()
        return pattern.matches(id)
    }
}
