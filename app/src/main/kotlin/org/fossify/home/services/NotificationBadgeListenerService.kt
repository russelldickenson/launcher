package org.fossify.home.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.fossify.home.helpers.NotificationCache

class NotificationBadgeListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        updateCache()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        updateCache()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        updateCache()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        NotificationCache.clear()
        NotificationCache.onChanged?.invoke()
    }

    private fun updateCache() {
        val counts = try {
            activeNotifications
                .filter { it.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0 }
                .groupingBy { it.packageName }
                .eachCount()
        } catch (e: Exception) {
            emptyMap()
        }

        NotificationCache.notificationCounts = counts
        NotificationCache.onChanged?.invoke()
    }
}
