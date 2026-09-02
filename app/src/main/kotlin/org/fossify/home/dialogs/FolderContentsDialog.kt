package org.fossify.home.dialogs

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Window
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import org.fossify.commons.views.MyGridLayoutManager
import org.fossify.home.activities.MainActivity
import org.fossify.home.adapters.LaunchersAdapter
import org.fossify.home.databinding.DialogFolderContentsBinding
import org.fossify.home.extensions.config
import org.fossify.home.extensions.getAppDrawerBackgroundColor
import org.fossify.home.extensions.getAppDrawerTextColor
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
// "Remove from folder" entry that only appears here. A plain Dialog rather than
// MaterialAlertDialogBuilder - the Material dialog's own card chrome (title bar, corner radius,
// content insets) shows through in a different colour than the drawer background we set on our
// own content view, so the whole thing reads as two mismatched panels rather than one seamless
// drawer-like surface. Rounded corners (matching material_dialog_corner_radius, the same radius
// used elsewhere in this app's dialogs) are applied to the inner card view instead, inset from the
// dialog window's own edges by activity_margin so the rounding has room to actually read as
// rounded rather than clipping into the screen edge. Dismissing is tap-outside only, no separate
// cancel button needed
class FolderContentsDialog(
    private val activity: MainActivity,
    folder: DrawerFolder,
    members: List<AppLauncher>,
    private val itemClick: (AppLauncher) -> Unit,
    private val menuListener: ItemMenuListener,
) {
    private val binding = DialogFolderContentsBinding.inflate(activity.layoutInflater)
    private val dialog = Dialog(activity).apply {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        setCanceledOnTouchOutside(true)
        // binding.root is just a transparent inset frame around the real (rounded, coloured)
        // card view - make the window background transparent too, or the theme's default dialog
        // window background would show through around/behind that rounded card
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(MATCH_PARENT, WRAP_CONTENT)
    }

    init {
        val backgroundColor = activity.getAppDrawerBackgroundColor()
        val cornerRadius = activity.resources.getDimension(org.fossify.commons.R.dimen.material_dialog_corner_radius)
        binding.folderContentsCard.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            this.cornerRadius = cornerRadius
            setColor(backgroundColor)
        }
        binding.folderContentsTitle.text = folder.title
        binding.folderContentsTitle.setTextColor(activity.getAppDrawerTextColor())

        dialog.show()

        val layoutManager = binding.folderContentsGrid.layoutManager as MyGridLayoutManager
        layoutManager.spanCount = activity.config.drawerColumnCount

        val adapter = LaunchersAdapter(
            activity = activity,
            allAppsListener = object : AllAppsListener {
                override fun onAppLauncherLongPressed(x: Float, y: Float, appLauncher: AppLauncher) {
                    showMemberMenu(x, y, appLauncher)
                }

                // selection mode (for adding apps to a folder) is never entered from within this
                // dialog - you're already inside a folder's contents
                override fun onAppLauncherSelectionToggled(appLauncher: AppLauncher) = Unit
                override fun onSelectionDragRequested(appLauncher: AppLauncher) = Unit

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
    private val dialog: Dialog,
) : ItemMenuListener by realListener {
    override fun onAnyClick() {
        dialog.dismiss()
        realListener.onAnyClick()
    }
}
