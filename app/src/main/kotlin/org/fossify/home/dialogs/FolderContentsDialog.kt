package org.fossify.home.dialogs

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.ViewTreeObserver
import android.view.Window
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import org.fossify.commons.views.MyGridLayoutManager
import org.fossify.home.activities.MainActivity
import org.fossify.home.adapters.LaunchersAdapter
import org.fossify.home.databinding.DialogFolderContentsBinding
import org.fossify.home.extensions.config
import org.fossify.home.extensions.getAppDrawerOverlaySurfaceColor
import org.fossify.home.extensions.getAppDrawerTextColor
import org.fossify.home.extensions.handleGridItemPopupMenu
import org.fossify.home.extensions.moveToScreenPosition
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
// drawer-like surface. Rounded corners (folder_contents_card_corner_radius, smaller than the
// standard material_dialog_corner_radius other dialogs use, since this card fills most of the
// screen width and a full dialog radius reads as too aggressive at that size) are applied to the
// inner card view instead, inset from the dialog window's own edges by activity_margin so the
// rounding has room to actually read as rounded rather than clipping into the screen edge.
// Dismissing is tap-outside only, no separate cancel button needed
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
        val backgroundColor = activity.getAppDrawerOverlaySurfaceColor()
        val cornerRadius = activity.resources.getDimension(org.fossify.home.R.dimen.folder_contents_card_corner_radius)
        binding.folderContentsCard.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            this.cornerRadius = cornerRadius
            setColor(backgroundColor)
        }
        binding.folderContentsTitle.text = folder.title
        binding.folderContentsTitle.setTextColor(activity.getAppDrawerTextColor())

        // the folder's own menu (Add/Rename/Delete) only lives here, on the open folder - the
        // closed drawer tile is tap-to-open only, so this is the sole place to reach it. Opening
        // the menu doesn't close this dialog - it's anchored to folderContentsPopupAnchor (a view
        // inside this dialog's own window, same trick showMemberMenu below uses) so it renders as
        // part of this dialog rather than the activity window behind it, and the dialog only
        // actually closes for the two actions that need to leave the open-folder view: Add
        // (which hands off to the main drawer's selection mode) and Delete (the folder is gone)
        binding.folderContentsKebab.imageTintList = ColorStateList.valueOf(activity.getAppDrawerTextColor())
        binding.folderContentsKebab.setOnClickListener {
            val location = IntArray(2)
            binding.folderContentsKebab.getLocationOnScreen(location)
            val x = (location[0] + binding.folderContentsKebab.width / 2).toFloat()
            val y = location[1].toFloat()
            activity.showFolderMenu(
                x, y, folder,
                anchorView = binding.folderContentsPopupAnchor,
                onFolderRenamed = { newTitle -> binding.folderContentsTitle.text = newTitle },
                onFolderDeleted = { dialog.dismiss() },
                onAddSelected = { dialog.dismiss() },
            )
        }

        dialog.show()

        val layoutManager = binding.folderContentsGrid.layoutManager as MyGridLayoutManager
        layoutManager.spanCount = activity.config.drawerColumnCount

        val adapter = LaunchersAdapter(
            activity = activity,
            allAppsListener = object : AllAppsListener {
                override fun onAppLauncherLongPressed(x: Float, y: Float, appLauncher: AppLauncher) {
                    showMemberMenu(x, y, appLauncher)
                }

                // this dialog's own grid only ever holds plain apps, never nested folders, and
                // never enters the folder "Add" selection mode (that's driven by the main drawer)
                override fun onFolderClicked(folder: DrawerFolder) = Unit
                override fun onAppSelectionToggled(appLauncher: AppLauncher) = Unit
            },
            itemClick = {
                val launcher = it as AppLauncher
                dialog.dismiss()
                itemClick(launcher)
            }
        )
        binding.folderContentsGrid.adapter = adapter
        adapter.submitList(members.map { DrawerGridItem.App(it) }.toMutableList())

        capHeightIfNeeded(expectedItemCount = members.size)
    }

    // the grid has no height cap of its own (wrap_content, in a WRAP_CONTENT dialog window), so a
    // folder with enough apps would otherwise grow the dialog past the screen edge with no way to
    // scroll to the rest. Waits for the adapter's item count to actually reach the full member
    // list before measuring - ListAdapter.submitList() diffs asynchronously, so the very first
    // layout pass right after it would still show an empty (or stale) grid, not the real height
    private fun capHeightIfNeeded(expectedItemCount: Int) {
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if ((binding.folderContentsGrid.adapter?.itemCount ?: 0) < expectedItemCount) {
                    return
                }
                binding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val maxDialogHeight = (activity.resources.displayMetrics.heightPixels * MAX_DIALOG_HEIGHT_FRACTION).toInt()
                if (binding.root.height > maxDialogHeight) {
                    val chromeHeight = binding.root.height - binding.folderContentsGrid.height
                    binding.folderContentsGrid.layoutParams = binding.folderContentsGrid.layoutParams.apply {
                        height = (maxDialogHeight - chromeHeight).coerceAtLeast(0)
                    }
                }
            }
        })
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

        binding.folderContentsPopupAnchor.moveToScreenPosition(x, y)
        activity.handleGridItemPopupMenu(
            anchorView = binding.folderContentsPopupAnchor,
            gridItem = gridItem,
            isOnAllAppsFragment = true,
            listener = FolderMenuListenerDelegate(menuListener, dialog),
            isInFolderOverlay = true,
        )
    }

    companion object {
        private const val MAX_DIALOG_HEIGHT_FRACTION = 0.75f
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
