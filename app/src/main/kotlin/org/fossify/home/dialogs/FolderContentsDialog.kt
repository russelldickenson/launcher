package org.fossify.home.dialogs

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.fossify.commons.views.MyGridLayoutManager
import org.fossify.home.activities.MainActivity
import org.fossify.home.adapters.LaunchersAdapter
import org.fossify.home.databinding.DialogFolderContentsBinding
import org.fossify.home.extensions.config
import org.fossify.home.extensions.handleGridItemPopupMenu
import org.fossify.home.helpers.ITEM_TYPE_ICON
import org.fossify.home.interfaces.AllAppsListener
import org.fossify.home.interfaces.ItemMenuListener
import org.fossify.home.models.AppLauncher
import org.fossify.home.models.DrawerFolder
import org.fossify.home.models.DrawerGridItem
import org.fossify.home.models.HomeScreenGridItem

// the "mini app drawer" shown over the (dimmed, via the dialog's own default window behavior)
// main app drawer when a folder is tapped - a small grid of just that folder's apps. Tapping an
// app launches it and closes both this dialog and the main app drawer; long-pressing one reuses
// the exact same shared item menu as the main drawer (pin/hide/rename/app info/uninstall), plus a
// "Remove from folder" entry that only appears here
class FolderContentsDialog(
    private val activity: MainActivity,
    folder: DrawerFolder,
    members: List<AppLauncher>,
    private val itemClick: (AppLauncher) -> Unit,
    private val menuListener: ItemMenuListener,
) {
    private val binding = DialogFolderContentsBinding.inflate(activity.layoutInflater)
    private lateinit var dialog: androidx.appcompat.app.AlertDialog

    init {
        dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(folder.title)
            .setView(binding.root)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .show()

        val layoutManager = binding.folderContentsGrid.layoutManager as MyGridLayoutManager
        layoutManager.spanCount = activity.config.drawerColumnCount

        val adapter = LaunchersAdapter(
            activity = activity,
            allAppsListener = object : AllAppsListener {
                override fun onAppLauncherLongPressed(x: Float, y: Float, appLauncher: AppLauncher) {
                    showMemberMenu(x, y, appLauncher)
                }

                // this dialog's own grid only ever holds plain apps, never nested folders
                override fun onFolderClicked(folder: DrawerFolder) = Unit
                override fun onFolderLongPressed(x: Float, y: Float, folder: DrawerFolder) = Unit
            },
            itemClick = {
                val launcher = it as AppLauncher
                dialog.dismiss()
                itemClick(launcher)
            }
        )
        binding.folderContentsGrid.adapter = adapter
        adapter.submitList(members.map { DrawerGridItem.App(it) }.toMutableList())
    }

    private fun showMemberMenu(x: Float, y: Float, launcher: AppLauncher) {
        val gridItem = HomeScreenGridItem(
            id = null,
            left = -1,
            top = -1,
            right = -1,
            bottom = -1,
            page = 0,
            packageName = launcher.packageName,
            activityName = launcher.activityName,
            title = launcher.title,
            type = ITEM_TYPE_ICON,
            className = "",
            widgetId = -1,
            shortcutId = "",
            icon = null,
            docked = false,
            parentId = null,
            drawable = launcher.drawable
        )

        binding.folderContentsPopupAnchor.x = x
        binding.folderContentsPopupAnchor.y = y
        activity.handleGridItemPopupMenu(
            anchorView = binding.folderContentsPopupAnchor,
            gridItem = gridItem,
            isOnAllAppsFragment = true,
            listener = FolderMenuListenerDelegate(menuListener, dialog),
            isInFolderOverlay = true,
        )
    }
}

// every real menu action (pin/hide/rename/remove-from-folder/etc.) should close this "mini app
// drawer" first, same as tapping an app to launch it does - onAnyClick() already fires
// unconditionally before any specific action, so dismissing there covers every case in one place
private class FolderMenuListenerDelegate(
    private val realListener: ItemMenuListener,
    private val dialog: androidx.appcompat.app.AlertDialog,
) : ItemMenuListener by realListener {
    override fun onAnyClick() {
        dialog.dismiss()
        realListener.onAnyClick()
    }
}
