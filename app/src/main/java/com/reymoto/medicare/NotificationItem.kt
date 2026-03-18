package com.reymoto.medicare

import java.util.Date

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Date,
    val type: NotificationType,
    val isRead: Boolean = false
)

enum class NotificationType {
    NEAR_SERVING,
    YOUR_TURN,
    QUEUE_UPDATE,
    SYSTEM
}