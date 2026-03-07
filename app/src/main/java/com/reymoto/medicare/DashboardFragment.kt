package com.reymoto.medicare

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class DashboardFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val financeQueueText = view.findViewById<TextView>(R.id.tvFinanceQueue)
        val registrarQueueText = view.findViewById<TextView>(R.id.tvRegistrarQueue)
        val financeDateText = view.findViewById<TextView>(R.id.tvFinanceDate)
        val registrarDateText = view.findViewById<TextView>(R.id.tvRegistrarDate)
        val myQueueText = view.findViewById<TextView>(R.id.tvMyQueue)
        val myQueueStatusText = view.findViewById<TextView>(R.id.tvMyQueueStatus)

        // Set current date
        val dateFormat = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
        val currentDate = dateFormat.format(java.util.Date())
        financeDateText.text = currentDate
        registrarDateText.text = currentDate

        // Load current queue numbers
        loadFinanceQueue(financeQueueText)
        loadRegistrarQueue(registrarQueueText)
        loadMyQueue(myQueueText, myQueueStatusText)

        // Button actions
        val requestQueueButton = view.findViewById<Button>(R.id.btnRequestQueue)
        val queueHistoryButton = view.findViewById<Button>(R.id.btnQueueHistory)
        
        requestQueueButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, BookAppointmentFragment())
                .addToBackStack(null)
                .commit()
        }
        
        queueHistoryButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, QueueFragment())
                .addToBackStack(null)
                .commit()
        }
        
        return view
    }

    private fun loadFinanceQueue(textView: TextView) {
        // Get all Finance appointments and filter in code to avoid index requirement
        db.collection("appointments")
            .whereEqualTo("department", "Finance")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    textView.text = "---"
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    // Find serving appointment first
                    val servingDoc = snapshot.documents.firstOrNull { 
                        it.getString("status") == "Serving" 
                    }
                    
                    if (servingDoc != null) {
                        textView.text = servingDoc.getString("queueNumber") ?: "---"
                    } else {
                        // Find first pending appointment
                        val pendingDoc = snapshot.documents.firstOrNull { 
                            it.getString("status") == "Pending" 
                        }
                        textView.text = pendingDoc?.getString("queueNumber") ?: "---"
                    }
                } else {
                    textView.text = "---"
                }
            }
    }

    private fun loadRegistrarQueue(textView: TextView) {
        // Get all Registrar appointments and filter in code to avoid index requirement
        db.collection("appointments")
            .whereEqualTo("department", "Registrar")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    textView.text = "---"
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    // Find serving appointment first
                    val servingDoc = snapshot.documents.firstOrNull { 
                        it.getString("status") == "Serving" 
                    }
                    
                    if (servingDoc != null) {
                        textView.text = servingDoc.getString("queueNumber") ?: "---"
                    } else {
                        // Find first pending appointment
                        val pendingDoc = snapshot.documents.firstOrNull { 
                            it.getString("status") == "Pending" 
                        }
                        textView.text = pendingDoc?.getString("queueNumber") ?: "---"
                    }
                } else {
                    textView.text = "---"
                }
            }
    }

    private fun loadMyQueue(queueTextView: TextView, statusTextView: TextView) {
        val currentUser = auth.currentUser
        
        if (currentUser == null) {
            queueTextView.text = "Not logged in"
            statusTextView.text = ""
            return
        }

        // Get user's appointments and filter in code to avoid index requirement
        db.collection("appointments")
            .whereEqualTo("studentUID", currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    queueTextView.text = "Error loading"
                    statusTextView.text = ""
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    // Find most recent pending or serving appointment
                    val activeDoc = snapshot.documents
                        .filter { 
                            val status = it.getString("status")
                            status == "Pending" || status == "Serving"
                        }
                        .maxByOrNull { it.getTimestamp("timestamp")?.toDate()?.time ?: 0 }
                    
                    if (activeDoc != null) {
                        val queueNumber = activeDoc.getString("queueNumber")
                        val status = activeDoc.getString("status")
                        val department = activeDoc.getString("department")
                        
                        queueTextView.text = queueNumber ?: "---"
                        
                        val statusText = when (status) {
                            "Serving" -> "🟢 Now being served at $department"
                            "Pending" -> "⏳ Waiting in $department queue"
                            else -> ""
                        }
                        statusTextView.text = statusText
                    } else {
                        queueTextView.text = "No active queue"
                        statusTextView.text = "Request a queue to get started"
                    }
                } else {
                    queueTextView.text = "No active queue"
                    statusTextView.text = "Request a queue to get started"
                }
            }
    }
}