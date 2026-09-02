package org.fossify.home.helpers

import org.fossify.home.models.AppLauncher
import org.fossify.home.models.DrawerFolder

object IconCache {
    @Volatile
    private var cachedLaunchers = emptyList<AppLauncher>()

    @Volatile
    private var cachedFolders = emptyList<DrawerFolder>()

    var launchers: List<AppLauncher>
        get() = cachedLaunchers
        set(value) {
            synchronized(this) {
                cachedLaunchers = value
            }
        }

    var folders: List<DrawerFolder>
        get() = cachedFolders
        set(value) {
            synchronized(this) {
                cachedFolders = value
            }
        }

    fun clear() {
        launchers = emptyList()
        folders = emptyList()
    }
}