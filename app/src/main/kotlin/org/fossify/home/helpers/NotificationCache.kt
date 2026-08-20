package org.fossify.home.helpers

object NotificationCache {
    @Volatile
    private var cachedNotificationCounts = emptyMap<String, Int>()

    // packageName -> number of active notifications for that package
    var notificationCounts: Map<String, Int>
        get() = cachedNotificationCounts
        set(value) {
            synchronized(this) {
                cachedNotificationCounts = value
            }
        }

    @Volatile
    var onChanged: (() -> Unit)? = null

    fun clear() {
        notificationCounts = emptyMap()
    }
}
