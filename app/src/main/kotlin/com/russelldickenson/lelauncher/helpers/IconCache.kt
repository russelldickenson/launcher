package com.russelldickenson.lelauncher.helpers

import com.russelldickenson.lelauncher.models.AppLauncher
import com.russelldickenson.lelauncher.models.DrawerFolder
import com.russelldickenson.lelauncher.models.DrawerTab

object IconCache {
    @Volatile
    private var cachedLaunchers = emptyList<AppLauncher>()

    @Volatile
    private var cachedFolders = emptyList<DrawerFolder>()

    @Volatile
    private var cachedTabs = emptyList<DrawerTab>()

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

    var tabs: List<DrawerTab>
        get() = cachedTabs
        set(value) {
            synchronized(this) {
                cachedTabs = value
            }
        }

    fun clear() {
        launchers = emptyList()
        folders = emptyList()
        tabs = emptyList()
    }
}