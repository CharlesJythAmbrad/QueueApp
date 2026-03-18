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
    private val queueList = mutableListOf<Appointment>()

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
        setupRecyclerView()
        loadMyQueueHistory()

        view.findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            loadMyQueueHistory()
        }
        
        return view
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewQueue)
        progressBar = view.findViewById(R.id.progressBar)
        emptyView = view.findViewById(R.id.tvEmptyView)
    }

    private fun setupRecyclerView() {
        queueAdapter = QueueAdapter(queueList) { appointment ->
            showCancelConfirmation(appointment)
        }
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
                queueList.clear()

                for (document in documents) {
                    val appointment = Appointment(
                        id = document.id,
                        studentEmail = document.getString("studentEmail") ?: "",
                        transactionType = document.getString("transactionType") ?: "",
                        appointmentDate = document.getString("appointmentDate") ?: "",
                        appointmentTime = document.getString("appointmentTime") ?: "",
                        queueNumber = document.getString("queueNumber") ?: "",
                        status = document.getString("status") ?: "Pending",
                        timestamp = document.getTimestamp("timestamp")
                    )
                    queueList.add(appointment)
                }

                // Sort by timestamp in descending order (newest first)
                queueList.sortByDescending { it.timestamp?.toDate() }

                queueAdapter.notifyDataSetChanged()
                
                if (queueList.isEmpty()) {
                    showEmptyView(true)
                } else {
                    showEmptyView(false)
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                showEmptyView(true)
                // Don't show technical error messages, just show user-friendly message
                Toast.makeText(requireContext(), "Unable to load queue history. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showCancelConfirmation(appointment: Appointment) {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Queue")
            .setMessage("Are you sure you want to cancel queue ${appointment.queueNumber}?")
            .setPositiveButton("Yes") { _, _ ->
                cancelQueue(appointment)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelQueue(appointment: Appointment) {
        db.collection("appointments")
            .document(appointment.id)
            .update("status", "Cancelled")
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Queue cancelled successfully", Toast.LENGTH_SHORT).show()
                loadMyQueueHistory()
            }
            .addOnFailureListener { exception ->
                // Don't show technical error messages
                Toast.makeText(requireContext(), "Unable to cancel queue. Please try again.", Toast.LENGTH_SHORT).show()
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