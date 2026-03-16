package com.reymoto.medicare

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // References to TextInputEditText
        val firstName = findViewById<TextInputEditText>(R.id.etFirstName)
        val lastName = findViewById<TextInputEditText>(R.id.etLastName)
        val idNumber = findViewById<TextInputEditText>(R.id.etIdNumber)
        val courseDropdown = findViewById<AutoCompleteTextView>(R.id.etCourse)
        val yearLevelDropdown = findViewById<AutoCompleteTextView>(R.id.etYearLevel)
        val contact = findViewById<TextInputEditText>(R.id.etContact)
        val gmail = findViewById<TextInputEditText>(R.id.etGmail)
        val password = findViewById<TextInputEditText>(R.id.etPassword)
        val confirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val rulesCheckBox = findViewById<CheckBox>(R.id.cbRules)
        val registerButton = findViewById<Button>(R.id.btnRegister)
        val loginText = findViewById<TextView>(R.id.tvLogin)

        // Setup course dropdown
        val courses = arrayOf(
            "BS Computer Science",
            "BS Information Technology",
            "BS Business Administration",
            "BS Accountancy",
            "BS Civil Engineering",
            "BS Electrical Engineering",
            "BS Mechanical Engineering",
            "BS Architecture",
            "BS Nursing",
            "BS Psychology",
            "BS Education",
            "BS Hospitality Management",
            "BS Tourism Management",
            "BS Marine Engineering",
            "BS Criminology"
        )
        val courseAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, courses)
        courseDropdown.setAdapter(courseAdapter)

        // Setup year level dropdown
        val yearLevels = arrayOf(
            "1st Year",
            "2nd Year",
            "3rd Year",
            "4th Year",
            "5th Year"
        )
        val yearLevelAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, yearLevels)
        yearLevelDropdown.setAdapter(yearLevelAdapter)

        // Show rules dialog when checkbox clicked
        rulesCheckBox.setOnClickListener {
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.rules_and_regulations)
            dialog.setTitle("Rules & Regulations")

            // Close button
            val closeBtn = dialog.findViewById<ImageButton>(R.id.btnCloseDialog)
            closeBtn.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }


        // Register button click
        registerButton.setOnClickListener {
            if (!rulesCheckBox.isChecked) {
                Toast.makeText(this, "You must agree to the Rules and Regulations", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val firstNameText = firstName.text.toString().trim()
            val lastNameText = lastName.text.toString().trim()
            val idText = idNumber.text.toString().trim()
            val courseText = courseDropdown.text.toString().trim()
            val yearLevelText = yearLevelDropdown.text.toString().trim()
            val contactText = contact.text.toString().trim()
            val gmailText = gmail.text.toString().trim()
            val passText = password.text.toString()
            val confirmText = confirmPassword.text.toString()

            // Validation
            if (firstNameText.isEmpty() || lastNameText.isEmpty() || idText.isEmpty() || 
                courseText.isEmpty() || yearLevelText.isEmpty() || contactText.isEmpty() || 
                gmailText.isEmpty() || passText.isEmpty() || confirmText.isEmpty()
            ) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate ID format (05-2425-000000)
            if (!isValidIdFormat(idText)) {
                idNumber.error = "Invalid ID format"
                Toast.makeText(this, "ID must be in format: 05-2425-000000", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (contactText.length < 11) {
                Toast.makeText(this, "Please enter a valid contact number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate Gmail format
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(gmailText).matches()) {
                gmail.error = "Invalid email format"
                Toast.makeText(this, "Please enter a valid Gmail address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate password with regex
            if (!isValidPassword(passText)) {
                password.error = "Invalid password format"
                Toast.makeText(this, "Password must be at least 8 characters with 1 uppercase, 1 lowercase, 1 number, and 1 special character", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (passText != confirmText) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Use Gmail for Firebase Auth instead of generated email
            val fullName = "$firstNameText $lastNameText"

            // Firebase registration using Gmail
            auth.createUserWithEmailAndPassword(gmailText, passText)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        val user = hashMapOf(
                            "firstName" to firstNameText,
                            "lastName" to lastNameText,
                            "fullName" to fullName,
                            "idNumber" to idText,
                            "course" to courseText,
                            "yearLevel" to yearLevelText,
                            "email" to gmailText,
                            "contact" to contactText
                        )
                        userId?.let { 
                            db.collection("users").document(it).set(user)
                                .addOnSuccessListener {
                                    showSuccessDialog(fullName, idText)
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Error saving user data", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Toast.makeText(this, task.exception?.message ?: "Registration failed", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Navigate to login
        loginText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun showSuccessDialog(fullName: String, idNumber: String) {
        AlertDialog.Builder(this)
            .setTitle("✓ Registration Successful")
            .setMessage("Welcome, $fullName!\n\nYour account has been created successfully.\n\nID Number: $idNumber\n\nYou can now login to access the Smart Queue System.")
            .setPositiveButton("Go to Login") { _, _ ->
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun isValidIdFormat(id: String): Boolean {
        // Format: 05-2425-000000 (2 digits - 4 digits - 6 digits)
        val pattern = "^\\d{2}-\\d{4}-\\d{6}$".toRegex()
        return pattern.matches(id)
    }

    private fun isValidPassword(password: String): Boolean {
        // Password must contain:
        // - At least 8 characters
        // - At least 1 uppercase letter
        // - At least 1 lowercase letter
        // - At least 1 digit
        // - At least 1 special character (@$!%*?&#)
        val pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&#])[A-Za-z\\d@\$!%*?&#]{8,}$".toRegex()
        return pattern.matches(password)
    }
}
