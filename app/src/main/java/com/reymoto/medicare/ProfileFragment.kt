package com.reymoto.medicare

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
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
        val uidText = view.findViewById<TextView>(R.id.tvUID)
        val logoutButton = view.findViewById<Button>(R.id.btnLogout)

        // Display UID immediately
        uidText.text = currentUser?.uid ?: "N/A"

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

        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        
        return view
    }
}