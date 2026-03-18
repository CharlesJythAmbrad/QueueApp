package com.reymoto.medicare

import java.util.Date
import java.util.UUID

object NotificationManager {
    private val notifications = mutableListOf<NotificationItem>()
    private val listeners = mutableListOf<NotificationListener>()

    interface NotificationListener {
        fun onNotificationAdded(notification: NotificationItem)
        fun onNotificationCountChanged(count: Int)
    }

    fun addNotification(title: String, message: String, type: NotificationType) {
        val notification = NotificationItem(
            id = UUID.randomUUID().toString(),
            title = title,
            message = message,
            timestamp = Date(),
            type = type
        )
        
        notifications.add(0, notification) // Add to beginning for newest first
        
        // Keep only last 50 notifications
        if (notifications.size > 50) {
            notifications.removeAt(notifications.size - 1)
        }
        
        notifyListeners(notification)
    }

    fun getNotifications(): List<NotificationItem> {
        return notifications.toList()
    }

    fun getUnreadCount(): Int {
        return notifications.count { !it.isRead }
    }

    fun markAllAsRead() {
        for (i in notifications.indices) {
            notifications[i] = notifications[i].copy(isRead = true)
        }
        notifyCountChanged()
    }

    fun markAsRead(notificationId: String) {
        val index = notifications.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            notifications[index] = notifications[index].copy(isRead = true)
            notifyCountChanged()
        }
    }

    fun clearAll() {
        notifications.clear()
        notifyCountChanged()
    }

    fun addListener(listener: NotificationListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: NotificationListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners(notification: NotificationItem) {
        listeners.forEach { it.onNotificationAdded(notification) }
        notifyCountChanged()
    }

    private fun notifyCountChanged() {
        val count = getUnreadCount()
        listeners.forEach { it.onNotificationCountChanged(count) }
    }
}