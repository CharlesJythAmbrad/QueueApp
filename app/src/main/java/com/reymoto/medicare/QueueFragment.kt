package com.reymoto.medicare

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class QueueFragment : Fragment(), NotificationManager.NotificationListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var queueAdapter: QueueAdapter
    private lateinit var notificationBadge: TextView
    private lateinit var spinnerDepartment: Spinner
    private val queueList = mutableListOf<Appointment>()
    private val allQueueList = mutableListOf<Appointment>() // Store all queues for filtering
    private var selectedDepartment = "Finance" // Default to Finance

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.history_queue, container, false)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Notification icon click
        val notificationIcon = view.findViewById<android.widget.ImageView>(R.id.ivNotification)
        notificationBadge = view.findViewById<TextView>(R.id.tvNotificationBadge)
        
        notificationIcon.setOnClickListener {
            showStoredNotifications()
        }
        
        // Register for notification updates
        NotificationManager.addListener(this)
        updateNotificationBadge()

        initViews(view)
        setupDepartmentFilter()
        initializeAdapter()
        setupRecyclerView()
        loadMyQueueHistory()


        
        return view
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewQueue)
        progressBar = view.findViewById(R.id.progressBar)
        emptyView = view.findViewById(R.id.tvEmptyView)
        spinnerDepartment = view.findViewById(R.id.spinnerDepartment)
    }
    
    private fun initializeAdapter() {
        queueAdapter = QueueAdapter(queueList) { appointment ->
            // Handle queue item click if needed
            showQueueDetails(appointment)
        }
    }
    
    private fun setupDepartmentFilter() {
        val departments = arrayOf("💰 Finance Department", "📄 Registrar Department")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, departments)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDepartment.adapter = adapter
        
        // Set Finance as default (index 0)
        spinnerDepartment.setSelection(0)
        
        spinnerDepartment.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedDepartment = when (position) {
                    0 -> "Finance"
                    1 -> "Registrar"
                    else -> "Finance"
                }
                filterQueuesByDepartment()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRecyclerView() {

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = queueAdapter
    }

    private fun loadMyQueueHistory() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        db.collection("appointments")
            .whereEqualTo("studentUID", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                showLoading(false)
                allQueueList.clear()

                for (document in documents) {
                    val status = document.getString("status") ?: "Pending"
                    
                    // Only include non-pending queues in history
                    if (status != "Pending") {
                        val appointment = Appointment(
                            id = document.id,
                            studentEmail = document.getString("studentEmail") ?: "",
                            transactionType = document.getString("transactionType") ?: "",
                            appointmentDate = document.getString("appointmentDate") ?: "",
                            appointmentTime = document.getString("appointmentTime") ?: "",
                            queueNumber = document.getString("queueNumber") ?: "",
                            status = if (status == "Serving") "Served" else status, // Change "Serving" to "Served"
                            timestamp = document.getTimestamp("timestamp")
                        )
                        allQueueList.add(appointment)
                    }
                }

                // Sort by timestamp in descending order (newest first)
                allQueueList.sortByDescending { it.timestamp?.toDate() }

                // Apply department filter
                filterQueuesByDepartment()
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                showEmptyView(true)
                // Don't show technical error messages, just show user-friendly message
                Toast.makeText(requireContext(), "Unable to load queue history. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun filterQueuesByDepartment() {
        queueList.clear()
        
        for (appointment in allQueueList) {
            // Extract department from queue number (format: YYYY-MM-DD-DEPT-XXX)
            val department = if (appointment.queueNumber.isNotEmpty()) {
                val parts = appointment.queueNumber.split("-")
                if (parts.size >= 4) {
                    when (parts[3]) {
                        "FIN" -> "Finance"
                        "REG" -> "Registrar"
                        else -> parts[3]
                    }
                } else {
                    "Unknown"
                }
            } else {
                "Unknown"
            }
            
            // Add to filtered list if department matches
            if (department == selectedDepartment) {
                queueList.add(appointment)
            }
        }
        
        queueAdapter.notifyDataSetChanged()
        
        if (queueList.isEmpty()) {
            showEmptyView(true)
        } else {
            showEmptyView(false)
        }
    }





    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEmptyView(show: Boolean) {
        emptyView.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showStoredNotifications() {
        val notifications = NotificationManager.getNotifications()
        
        if (notifications.isEmpty()) {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("🔔 Notifications")
                .setMessage("No notifications yet.\n\nYou'll receive notifications about:\n• Queue status changes\n• Completion updates\n• Service reminders")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // Mark all as read when viewing
        NotificationManager.markAllAsRead()

        // Create notification list dialog
        val notificationTexts = notifications.map { notification ->
            val timeFormat = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
            val timeStr = timeFormat.format(notification.timestamp)
            val icon = when (notification.type) {
                NotificationType.NEAR_SERVING -> "⏰"
                NotificationType.YOUR_TURN -> "🎯"
                NotificationType.QUEUE_UPDATE -> "📋"
                NotificationType.SYSTEM -> "🔔"
            }
            "$icon ${notification.title}\n${notification.message}\n$timeStr"
        }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("🔔 Notifications (${notifications.size})")
            .setItems(notificationTexts) { _, which ->
                val notification = notifications[which]
                showNotificationDetail(notification)
            }
            .setNegativeButton("Clear All") { _, _ ->
                NotificationManager.clearAll()
            }
            .setNeutralButton("Close", null)
            .show()
    }

    private fun showNotificationDetail(notification: NotificationItem) {
        val timeFormat = java.text.SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault())
        val timeStr = timeFormat.format(notification.timestamp)
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(notification.title)
            .setMessage("${notification.message}\n\nReceived: $timeStr")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun updateNotificationBadge() {
        val count = NotificationManager.getUnreadCount()
        if (count > 0) {
            notificationBadge.text = if (count > 99) "99+" else count.toString()
            notificationBadge.visibility = android.view.View.VISIBLE
        } else {
            notificationBadge.visibility = android.view.View.GONE
        }
    }
    
    private fun showQueueDetails(appointment: Appointment) {
        val status = when (appointment.status) {
            "Pending" -> "⏳ Waiting in queue"
            "Serving" -> "🟢 Currently being served"
            "Served" -> "🟢 Served"
            "Completed" -> "✅ Completed"
            "Cancelled" -> "❌ Cancelled"
            else -> appointment.status ?: "Unknown"
        }
        
        // Extract department from queue number (format: YYYY-MM-DD-DEPT-XXX)
        val department = if (appointment.queueNumber.isNotEmpty()) {
            val parts = appointment.queueNumber.split("-")
            if (parts.size >= 4) {
                when (parts[3]) {
                    "FIN" -> "Finance"
                    "REG" -> "Registrar"
                    else -> parts[3]
                }
            } else {
                "Unknown"
            }
        } else {
            "Unknown"
        }
        
        val message = buildString {
            append("Queue Number: ${appointment.queueNumber}\n")
            append("Department: $department\n")
            append("Transaction: ${appointment.transactionType}\n")
            append("Status: $status\n")
            append("Date: ${appointment.appointmentDate}\n")
            if (!appointment.appointmentTime.isNullOrEmpty()) {
                append("Time: ${appointment.appointmentTime}\n")
            }
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Queue Details")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    // NotificationManager.NotificationListener implementation
    override fun onNotificationAdded(notification: NotificationItem) {
        // History fragment doesn't show immediate popups
    }

    override fun onNotificationCountChanged(count: Int) {
        updateNotificationBadge()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        NotificationManager.removeListener(this)
    }
}