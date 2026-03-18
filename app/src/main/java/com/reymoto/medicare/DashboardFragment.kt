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

        // Notification icon click
        val notificationIcon = view.findViewById<android.widget.ImageView>(R.id.ivNotification)
        notificationIcon.setOnClickListener {
            showNotificationsDialog()
        }

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
        // Listen to the serving counters document in Firestore for real-time sync
        db.collection("system").document("servingCounters")
            .addSnapshotListener { document, error ->
                if (error != null) {
                    // Fallback to admin prefs if Firestore fails
                    loadFromAdminPrefs(textView, "Finance")
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    val financeCounter = document.getLong("financeCounter")?.toInt() ?: 0
                    textView.text = financeCounter.toString()
                } else {
                    // Document doesn't exist yet, fallback to admin prefs
                    loadFromAdminPrefs(textView, "Finance")
                }
            }
    }

    private fun loadRegistrarQueue(textView: TextView) {
        // Listen to the serving counters document in Firestore for real-time sync
        db.collection("system").document("servingCounters")
            .addSnapshotListener { document, error ->
                if (error != null) {
                    // Fallback to admin prefs if Firestore fails
                    loadFromAdminPrefs(textView, "Registrar")
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    val registrarCounter = document.getLong("registrarCounter")?.toInt() ?: 0
                    textView.text = registrarCounter.toString()
                } else {
                    // Document doesn't exist yet, fallback to admin prefs
                    loadFromAdminPrefs(textView, "Registrar")
                }
            }
    }

    private fun loadFromAdminPrefs(textView: TextView, department: String) {
        val prefs = requireContext().getSharedPreferences("AdminQueuePrefs", android.content.Context.MODE_PRIVATE)
        val counter = when (department) {
            "Finance" -> prefs.getInt("financeCounter", 0)
            "Registrar" -> prefs.getInt("registrarCounter", 0)
            else -> 0
        }
        textView.text = counter.toString()
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

                        // Extract only the department prefix and number from full queue number
                        // Format: "2026-03-14-FIN-001" -> "FIN-001"
                        val displayQueueNumber = if (queueNumber != null) {
                            val parts = queueNumber.split("-")
                            if (parts.size >= 5) {
                                "${parts[3]}-${parts[4]}" // FIN-001 or REG-001
                            } else {
                                queueNumber // fallback to full number if format is unexpected
                            }
                        } else {
                            "---"
                        }

                        queueTextView.text = displayQueueNumber

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

    private fun showNotificationsDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("🔔 Notifications")
            .setMessage("Queue Status Updates:\n\n• Your queue requests will appear here\n• Real-time serving number updates\n• Important announcements\n\nStay tuned for updates!")
            .setPositiveButton("OK", null)
            .show()
    }
}