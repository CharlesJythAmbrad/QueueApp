package com.reymoto.medicare

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

data class Appointment(
    val id: String = "",
    val studentEmail: String = "",
    val transactionType: String = "",
    val appointmentDate: String = "",
    val appointmentTime: String = "",
    val queueNumber: String = "",
    val status: String = "",
    val timestamp: Timestamp? = null
) {
    fun getFormattedDateTime(): String {
        return "$appointmentDate at $appointmentTime"
    }
    
    fun getStatusColor(): Int {
        return when (status) {
            "Pending" -> android.R.color.holo_orange_dark
            "Completed" -> android.R.color.holo_green_dark
            "Cancelled" -> android.R.color.holo_red_dark
            else -> android.R.color.darker_gray
        }
    }
}