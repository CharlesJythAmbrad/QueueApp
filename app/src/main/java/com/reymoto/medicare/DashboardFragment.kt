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

class DashboardFragment : Fragment(), NotificationManager.NotificationListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var notificationBadge: TextView
    
    // Notification tracking
    private var lastNotifiedPosition = -1
    private var hasNotifiedNearServing = false
    private var hasNotifiedYourTurn = false
    private var currentUserQueueNumber: String? = null
    private var currentUserDepartment: String? = null

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
        notificationBadge = view.findViewById<TextView>(R.id.tvNotificationBadge)
        
        notificationIcon.setOnClickListener {
            showStoredNotifications()
        }
        
        // Register for notification updates
        NotificationManager.addListener(this)
        updateNotificationBadge()

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

        // Load current queue numbers with notification monitoring
        loadFinanceQueue(financeQueueText)
        loadRegistrarQueue(registrarQueueText)
        loadMyQueue(myQueueText, myQueueStatusText)
        
        // Start monitoring for notifications
        startQueueNotificationMonitoring()

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
                    // Find most recent pending or serving appointment from today only
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    
                    val activeDoc = snapshot.documents
                        .filter {
                            val status = it.getString("status")
                            val queueNumber = it.getString("queueNumber") ?: ""
                            
                            // Check if queue is from today and is active
                            val isFromToday = queueNumber.startsWith(today)
                            val isActive = status == "Pending" || status == "Serving"
                            
                            isFromToday && isActive
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
                        
                        // Update current user queue info for notifications
                        currentUserQueueNumber = queueNumber
                        currentUserDepartment = department
                        
                        // Reset notification flags when queue status changes
                        if (status == "Serving") {
                            hasNotifiedNearServing = false
                            hasNotifiedYourTurn = false
                        }
                    } else {
                        // No active queue from today
                        queueTextView.text = "No active queue"
                        statusTextView.text = "Request a queue to get started"
                        
                        // Clear user queue info
                        currentUserQueueNumber = null
                        currentUserDepartment = null
                        resetNotificationFlags()
                    }
                } else {
                    queueTextView.text = "No active queue"
                    statusTextView.text = "Request a queue to get started"
                    
                    // Clear user queue info
                    currentUserQueueNumber = null
                    currentUserDepartment = null
                    resetNotificationFlags()
                }
            }
    }

    private fun startQueueNotificationMonitoring() {
        // Monitor serving counters for notification triggers
        db.collection("system").document("servingCounters")
            .addSnapshotListener { document, error ->
                if (error != null || document == null || !document.exists()) {
                    return@addSnapshotListener
                }

                val financeCounter = document.getLong("financeCounter")?.toInt() ?: 0
                val registrarCounter = document.getLong("registrarCounter")?.toInt() ?: 0
                
                // Check if user has active queue and send notifications
                checkAndSendNotifications(financeCounter, registrarCounter)
            }
    }

    private fun checkAndSendNotifications(financeServing: Int, registrarServing: Int) {
        if (currentUserQueueNumber == null || currentUserDepartment == null) {
            return
        }

        // Extract queue number from format like "2026-03-14-FIN-001"
        val queueParts = currentUserQueueNumber!!.split("-")
        if (queueParts.size < 5) return
        
        val userQueueNum = queueParts[4].toIntOrNull() ?: return
        val servingNumber = when (currentUserDepartment) {
            "Finance" -> financeServing
            "Registrar" -> registrarServing
            else -> return
        }
        
        val departmentName = when (currentUserDepartment) {
            "Finance" -> "Finance"
            "Registrar" -> "Registrar"
            else -> currentUserDepartment!!
        }

        // Calculate position difference
        val positionDifference = userQueueNum - servingNumber

        when {
            // User's turn - serving number matches user's queue number
            positionDifference == 0 && !hasNotifiedYourTurn -> {
                showYourTurnNotification(departmentName)
                hasNotifiedYourTurn = true
                hasNotifiedNearServing = false // Reset near serving flag
            }
            // Near serving - within 3 positions
            positionDifference in 1..3 && !hasNotifiedNearServing && !hasNotifiedYourTurn -> {
                showNearServingNotification(departmentName, positionDifference)
                hasNotifiedNearServing = true
            }
            // Reset flags if user moves further away (shouldn't happen normally)
            positionDifference > 3 -> {
                resetNotificationFlags()
            }
        }
    }

    private fun showNearServingNotification(department: String, positionsAway: Int) {
        val title = "Queue Alert"
        val message = when (positionsAway) {
            1 -> "You're next! Get ready to go to $department department."
            2 -> "You're 2nd in line! Prepare to go to $department department."
            3 -> "You're 3rd in line! Get ready to go to $department department."
            else -> "You're near serving! Go to $department department."
        }

        // Store notification
        NotificationManager.addNotification(title, message, NotificationType.NEAR_SERVING)
    }

    private fun showYourTurnNotification(department: String) {
        val title = "Your Turn!"
        val message = "It's your turn! Please proceed to $department department immediately."
        
        // Store notification
        NotificationManager.addNotification(title, message, NotificationType.YOUR_TURN)
    }

    private fun resetNotificationFlags() {
        hasNotifiedNearServing = false
        hasNotifiedYourTurn = false
        lastNotifiedPosition = -1
    }

    private fun showStoredNotifications() {
        val notifications = NotificationManager.getNotifications()
        
        if (notifications.isEmpty()) {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("🔔 Notifications")
                .setMessage("No notifications yet.\n\nYou'll receive notifications when:\n• You're near serving (within 3 positions)\n• It's your turn to be served\n• Important queue updates")
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
                // Show detailed notification
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

    // NotificationManager.NotificationListener implementation
    override fun onNotificationAdded(notification: NotificationItem) {
        // Show immediate popup for important notifications
        when (notification.type) {
            NotificationType.NEAR_SERVING, NotificationType.YOUR_TURN -> {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("🔔 ${notification.title}")
                    .setMessage(notification.message)
                    .setPositiveButton("OK", null)
                    .setCancelable(false)
                    .show()
            }
            else -> {
                // Just update badge for other notifications
            }
        }
    }

    override fun onNotificationCountChanged(count: Int) {
        updateNotificationBadge()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        NotificationManager.removeListener(this)
    }
}