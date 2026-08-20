package org.fossify.home.helpers

object NotificationCache {
    @Volatile
    private var cachedPackagesWithNotifications = emptySet<String>()

    var packagesWithNotifications: Set<String>
        get() = cachedPackagesWithNotifications
        set(value) {
            synchronized(this) {
                cachedPackagesWithNotifications = value
            }
        }

    @Volatile
    var onChanged: (() -> Unit)? = null

    fun clear() {
        packagesWithNotifications = emptySet()
    }
}
