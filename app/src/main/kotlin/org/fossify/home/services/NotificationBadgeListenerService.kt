package org.fossify.home.services

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
        val packages = try {
            activeNotifications.map { it.packageName }.toSet()
        } catch (e: Exception) {
            emptySet()
        }

        NotificationCache.packagesWithNotifications = packages
        NotificationCache.onChanged?.invoke()
    }
}
