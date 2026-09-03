package org.fossify.home.interfaces

import org.fossify.home.models.AppLauncher
import org.fossify.home.models.DrawerFolder

interface AllAppsListener {
    fun onAppLauncherLongPressed(x: Float, y: Float, appLauncher: AppLauncher)
    fun onFolderClicked(folder: DrawerFolder)
    fun onFolderLongPressed(x: Float, y: Float, folder: DrawerFolder)
}
