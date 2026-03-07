package com.reymoto.medicare

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

data class Report(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val userEmail: String = "",
    val timestamp: Timestamp? = null
) {
    fun getFormattedDate(): String {
        return if (timestamp != null) {
            val sdf = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
            sdf.format(timestamp.toDate())
        } else {
            "Unknown date"
        }
    }
}