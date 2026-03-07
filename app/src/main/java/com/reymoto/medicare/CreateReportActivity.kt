package com.reymoto.medicare

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CreateReportActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_report)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Check if user is authenticated
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val titleEditText = findViewById<EditText>(R.id.etReportTitle)
        val descriptionEditText = findViewById<EditText>(R.id.etReportDescription)
        val submitButton = findViewById<Button>(R.id.btnSubmitReport)
        val backButton = findViewById<Button>(R.id.btnBack)

        submitButton.setOnClickListener {
            val title = titleEditText.text.toString().trim()
            val description = descriptionEditText.text.toString().trim()

            // Validation
            if (title.isEmpty()) {
                titleEditText.error = "Title is required"
                return@setOnClickListener
            }

            if (description.isEmpty()) {
                descriptionEditText.error = "Description is required"
                return@setOnClickListener
            }

            // Show loading
            submitButton.isEnabled = false
            submitButton.text = "Submitting..."

            // Create report data
            val reportData = hashMapOf(
                "title" to title,
                "description" to description,
                "userUID" to currentUser.uid,
                "userEmail" to currentUser.email,
                "timestamp" to FieldValue.serverTimestamp()
            )

            // Save to Firestore
            db.collection("reports")
                .add(reportData)
                .addOnSuccessListener { documentReference ->
                    Toast.makeText(this, "Report submitted successfully!", Toast.LENGTH_SHORT).show()
                    
                    // Clear form
                    titleEditText.text.clear()
                    descriptionEditText.text.clear()
                    
                    // Go back to dashboard
                    startActivity(Intent(this, `DashboardFragment`::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error submitting report: ${e.message}", Toast.LENGTH_LONG).show()
                    
                    // Reset button
                    submitButton.isEnabled = true
                    submitButton.text = "Submit Report"
                }
        }

        backButton.setOnClickListener {
            startActivity(Intent(this, `DashboardFragment`::class.java))
            finish()
        }
    }
}