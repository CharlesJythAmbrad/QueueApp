package com.reymoto.medicare

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser

        val fullNameText = view.findViewById<TextView>(R.id.tvFullName)
        val courseText = view.findViewById<TextView>(R.id.tvCourse)
        val idNumberText = view.findViewById<TextView>(R.id.tvIdNumber)
        val contactText = view.findViewById<TextView>(R.id.tvContact)
        val emailText = view.findViewById<TextView>(R.id.tvEmail)
        val changePasswordButton = view.findViewById<Button>(R.id.btnChangePassword)
        val logoutButton = view.findViewById<Button>(R.id.btnLogout)

        // Fetch user data from Firestore
        currentUser?.uid?.let { userId ->
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        fullNameText.text = document.getString("fullName") ?: "N/A"
                        courseText.text = document.getString("course") ?: "N/A"
                        idNumberText.text = document.getString("idNumber") ?: "N/A"
                        contactText.text = document.getString("contact") ?: "N/A"
                        emailText.text = document.getString("email") ?: "N/A"
                    } else {
                        fullNameText.text = "No data found"
                        courseText.text = "N/A"
                        idNumberText.text = "N/A"
                        contactText.text = "N/A"
                        emailText.text = currentUser.email ?: "N/A"
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                    fullNameText.text = "Error loading data"
                    courseText.text = "N/A"
                    idNumberText.text = "N/A"
                    contactText.text = "N/A"
                    emailText.text = currentUser.email ?: "N/A"
                }
        }

        changePasswordButton.setOnClickListener {
            showChangePasswordDialog()
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        
        return view
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(android.R.layout.simple_list_item_1, null)
        
        // Create custom dialog layout
        val dialogLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        val currentPasswordEdit = EditText(requireContext()).apply {
            hint = "Current Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(20, 20, 20, 20)
        }

        val newPasswordEdit = EditText(requireContext()).apply {
            hint = "New Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(20, 20, 20, 20)
        }

        val confirmPasswordEdit = EditText(requireContext()).apply {
            hint = "Confirm New Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(20, 20, 20, 20)
        }

        dialogLayout.addView(currentPasswordEdit)
        dialogLayout.addView(newPasswordEdit)
        dialogLayout.addView(confirmPasswordEdit)

        AlertDialog.Builder(requireContext())
            .setTitle("Change Password")
            .setView(dialogLayout)
            .setPositiveButton("Change") { _, _ ->
                val currentPassword = currentPasswordEdit.text.toString()
                val newPassword = newPasswordEdit.text.toString()
                val confirmPassword = confirmPasswordEdit.text.toString()

                if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword != confirmPassword) {
                    Toast.makeText(requireContext(), "New passwords don't match", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword.length < 6) {
                    Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                changePassword(currentPassword, newPassword)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        val currentUser = auth.currentUser ?: return
        val email = currentUser.email ?: return

        // Re-authenticate user with current password
        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        
        currentUser.reauthenticate(credential)
            .addOnSuccessListener {
                // Update password
                currentUser.updatePassword(newPassword)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Password changed successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error changing password: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Current password is incorrect", Toast.LENGTH_SHORT).show()
            }
    }
}