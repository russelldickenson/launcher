package com.russelldickenson.lelauncher.interfaces

import com.russelldickenson.lelauncher.models.AppLauncher
import com.russelldickenson.lelauncher.models.DrawerFolder

interface AllAppsListener {
    fun onAppLauncherLongPressed(x: Float, y: Float, appLauncher: AppLauncher)
    fun onFolderClicked(folder: DrawerFolder)
    fun onAppSelectionToggled(appLauncher: AppLauncher)
}
