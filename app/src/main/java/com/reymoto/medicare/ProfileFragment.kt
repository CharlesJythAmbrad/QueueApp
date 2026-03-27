package com.reymoto.medicare

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment(), NotificationManager.NotificationListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var notificationBadge: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser

        // Notification icon click
        val notificationIcon = view.findViewById<android.widget.ImageView>(R.id.ivNotification)
        notificationBadge = view.findViewById<TextView>(R.id.tvNotificationBadge)
        
        notificationIcon.setOnClickListener {
            showStoredNotifications()
        }
        
        // Register for notification updates
        NotificationManager.addListener(this)
        updateNotificationBadge()

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
        // Create custom dialog layout
        val dialogLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0) // Remove padding, will add to sections
        }

        // Add header with title and close button
        val headerLayout = android.widget.RelativeLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(50, 50, 50, 30)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#FFFFFF"))
            }
        }

        val titleText = TextView(requireContext()).apply {
            text = "Change Password"
            textSize = 18f
            setTextColor(android.graphics.Color.BLACK)
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT,
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(android.widget.RelativeLayout.CENTER_IN_PARENT)

            }
        }



        headerLayout.addView(titleText)


        // Content layout with padding
        val contentLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 20)
        }

        // Current Password Section
        val currentPasswordLabel = TextView(requireContext()).apply {
            text = "Current Password"
            textSize = 14f
            setTextColor(android.graphics.Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 8)
            }
        }
        
        val currentPasswordEdit = EditText(requireContext()).apply {
            hint = "Enter current password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(20, 0, 70, 0) // Extra right padding for bigger eye icon
            textSize = 16f
            height = (50 * resources.displayMetrics.density).toInt() // Exactly 50dp
            background = android.graphics.drawable.GradientDrawable().apply {
                setStroke(2, android.graphics.Color.GRAY)
                cornerRadius = 8f
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 20)
            }
        }
        
        // Create relative layout to position eye icon inside the EditText
        val currentPasswordContainer = android.widget.RelativeLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 20)
            }
        }
        
        val currentPasswordToggle = ImageView(requireContext()).apply {
            setImageResource(R.drawable.ic_eye_off)
            setPadding(8, 8, 8, 8)
            layoutParams = android.widget.RelativeLayout.LayoutParams(60, 60).apply { // Bigger icon
                addRule(android.widget.RelativeLayout.ALIGN_PARENT_END)
                addRule(android.widget.RelativeLayout.CENTER_VERTICAL)
                setMargins(0, 0, 10, 0)
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            background = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            setOnClickListener {
                togglePasswordVisibility(currentPasswordEdit, this)
            }
        }
        
        currentPasswordEdit.layoutParams = android.widget.RelativeLayout.LayoutParams(
            android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
            (50 * resources.displayMetrics.density).toInt()
        )
        
        currentPasswordContainer.addView(currentPasswordEdit)
        currentPasswordContainer.addView(currentPasswordToggle)

        // New Password Section
        val newPasswordLabel = TextView(requireContext()).apply {
            text = "New Password"
            textSize = 14f
            setTextColor(android.graphics.Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 8)
            }
        }
        
        val newPasswordContainer = android.widget.RelativeLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 20)
            }
        }
        
        val newPasswordEdit = EditText(requireContext()).apply {
            hint = "Enter new password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(20, 0, 70, 0) // Extra right padding for bigger eye icon
            textSize = 16f
            background = android.graphics.drawable.GradientDrawable().apply {
                setStroke(2, android.graphics.Color.GRAY)
                cornerRadius = 8f
            }
            layoutParams = android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                (50 * resources.displayMetrics.density).toInt()
            )
        }
        
        val newPasswordToggle = ImageView(requireContext()).apply {
            setImageResource(R.drawable.ic_eye_off)
            setPadding(8, 8, 8, 8)
            layoutParams = android.widget.RelativeLayout.LayoutParams(60, 60).apply { // Bigger icon
                addRule(android.widget.RelativeLayout.ALIGN_PARENT_END)
                addRule(android.widget.RelativeLayout.CENTER_VERTICAL)
                setMargins(0, 0, 10, 0)
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            background = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            setOnClickListener {
                togglePasswordVisibility(newPasswordEdit, this)
            }
        }
        
        newPasswordContainer.addView(newPasswordEdit)
        newPasswordContainer.addView(newPasswordToggle)

        // Confirm Password Section
        val confirmPasswordLabel = TextView(requireContext()).apply {
            text = "Confirm New Password"
            textSize = 14f
            setTextColor(android.graphics.Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 8)
            }
        }
        
        val confirmPasswordContainer = android.widget.RelativeLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        val confirmPasswordEdit = EditText(requireContext()).apply {
            hint = "Confirm new password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(20, 0, 70, 0) // Extra right padding for bigger eye icon
            textSize = 16f
            background = android.graphics.drawable.GradientDrawable().apply {
                setStroke(2, android.graphics.Color.GRAY)
                cornerRadius = 8f
            }
            layoutParams = android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                (50 * resources.displayMetrics.density).toInt()
            )
        }
        
        val confirmPasswordToggle = ImageView(requireContext()).apply {
            setImageResource(R.drawable.ic_eye_off)
            setPadding(8, 8, 8, 8)
            layoutParams = android.widget.RelativeLayout.LayoutParams(60, 60).apply { // Bigger icon
                addRule(android.widget.RelativeLayout.ALIGN_PARENT_END)
                addRule(android.widget.RelativeLayout.CENTER_VERTICAL)
                setMargins(0, 0, 10, 0)
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            background = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            setOnClickListener {
                togglePasswordVisibility(confirmPasswordEdit, this)
            }
        }
        
        confirmPasswordContainer.addView(confirmPasswordEdit)
        confirmPasswordContainer.addView(confirmPasswordToggle)

        // Add all content components to content layout
        contentLayout.addView(currentPasswordLabel)
        contentLayout.addView(currentPasswordContainer)
        contentLayout.addView(newPasswordLabel)
        contentLayout.addView(newPasswordContainer)
        contentLayout.addView(confirmPasswordLabel)
        contentLayout.addView(confirmPasswordContainer)

        // Add header and content to main dialog layout
        dialogLayout.addView(headerLayout)
        dialogLayout.addView(contentLayout)

        val dialog = AlertDialog.Builder(requireContext())
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
            .create()



        dialog.show()
    }
    
    private fun togglePasswordVisibility(editText: EditText, imageView: ImageView) {
        if (editText.inputType == (android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            // Show password
            editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            imageView.setImageResource(R.drawable.ic_eye)
        } else {
            // Hide password
            editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            imageView.setImageResource(R.drawable.ic_eye_off)
        }
        // Move cursor to end
        editText.setSelection(editText.text.length)
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

    private fun showStoredNotifications() {
        val notifications = NotificationManager.getNotifications()
        
        if (notifications.isEmpty()) {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("🔔 Notifications")
                .setMessage("No notifications yet.\n\nYou'll receive notifications about:\n• Account security alerts\n• Password changes\n• System updates")
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
        // Profile fragment doesn't show immediate popups
    }

    override fun onNotificationCountChanged(count: Int) {
        updateNotificationBadge()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        NotificationManager.removeListener(this)
    }
}