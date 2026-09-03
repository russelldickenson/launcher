package org.fossify.home.extensions

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Process
import android.provider.Settings
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.MenuCompat
import androidx.core.view.forEach
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.isDynamicTheme
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.models.RadioItem
import org.fossify.commons.helpers.isSPlus
import org.fossify.home.R
import org.fossify.home.helpers.ITEM_TYPE_FOLDER
import org.fossify.home.helpers.ITEM_TYPE_ICON
import org.fossify.home.helpers.ITEM_TYPE_WIDGET
import org.fossify.home.helpers.IconCache
import org.fossify.home.helpers.PillPopupMenu
import org.fossify.home.helpers.UNINSTALL_APP_REQUEST_CODE
import org.fossify.home.interfaces.ItemMenuListener
import org.fossify.home.models.HomeScreenGridItem

// a fixed palette instead of a full hue/saturation picker - keeps the picker guaranteed
// Material (MaterialAlertDialogBuilder) on every supported OS version, unlike the commons
// library's ColorPickerDialog, which only gets Material dialog chrome on Android 12+
private val COLOR_PICKER_PALETTE = intArrayOf(
    Color.RED,
    Color.parseColor("#000080"), // navy blue
    Color.YELLOW,
    Color.GREEN,
    Color.CYAN,
)

fun Activity.showColorPickerDialog(currentColor: Int, onColorSelected: (Int) -> Unit) {
    val swatchSize = resources.getDimensionPixelSize(R.dimen.color_picker_swatch_size)
    val spacing = resources.getDimensionPixelSize(R.dimen.color_picker_swatch_spacing)
    val checkSize = (swatchSize * 0.5f).toInt()

    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(spacing, spacing, spacing, spacing)
    }

    lateinit var dialog: androidx.appcompat.app.AlertDialog

    COLOR_PICKER_PALETTE.forEach { color ->
        val swatch = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(swatchSize, swatchSize).apply {
                marginStart = spacing
                marginEnd = spacing
            }

            addView(View(context).apply {
                layoutParams = FrameLayout.LayoutParams(swatchSize, swatchSize)
                background = ContextCompat.getDrawable(context, R.drawable.notification_badge_dot)
                    ?.mutate()
                    ?.apply { setTint(color) }
            })

            if (color == currentColor) {
                addView(ImageView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(checkSize, checkSize, Gravity.CENTER)
                    setImageResource(org.fossify.commons.R.drawable.ic_check_vector)
                    setColorFilter(color.getContrastColor())
                })
            }

            setOnClickListener {
                dialog.dismiss()
                onColorSelected(color)
            }
        }

        row.addView(swatch)
    }

    dialog = MaterialAlertDialogBuilder(this)
        .setTitle(R.string.select_color)
        .setView(row)
        .setNegativeButton(org.fossify.commons.R.string.cancel, null)
        .create()
    dialog.show()
}

fun Activity.showRadioGroupDialog(
    @StringRes titleId: Int? = null,
    items: ArrayList<RadioItem>,
    checkedItemId: Any?,
    onItemSelected: (selectedId: Any) -> Unit,
) {
    val titles = items.map { it.title }.toTypedArray()
    val checkedIndex = items.indexOfFirst { it.id == checkedItemId }
    MaterialAlertDialogBuilder(this)
        .apply {
            if (titleId != null) setTitle(titleId)
        }
        .setSingleChoiceItems(titles, checkedIndex) { dialog, which ->
            dialog.dismiss()
            onItemSelected(items[which].id)
        }
        .setNegativeButton(org.fossify.commons.R.string.cancel, null)
        .show()
}

fun Activity.launchApp(packageName: String, activityName: String) {
    try {
        Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            `package` = packageName
            component = ComponentName.unflattenFromString("$packageName/$activityName")
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            startActivity(this)
        }
    } catch (e: Exception) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            startActivity(launchIntent)
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }
}

fun Activity.launchAppInfo(packageName: String) {
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        startActivity(this)
    }
}

fun Activity.canAppBeUninstalled(packageName: String): Boolean {
    return try {
        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
    } catch (ignored: Exception) {
        false
    }
}

fun Activity.uninstallApp(packageName: String) {
    Intent(Intent.ACTION_DELETE).apply {
        data = Uri.fromParts("package", packageName, null)
        startActivityForResult(this, UNINSTALL_APP_REQUEST_CODE)
    }
}

fun Activity.handleGridItemPopupMenu(
    anchorView: View,
    gridItem: HomeScreenGridItem,
    isOnAllAppsFragment: Boolean,
    listener: ItemMenuListener,
    isInFolderOverlay: Boolean = false,
): PillPopupMenu {
    return PillPopupMenu(this, anchorView, Gravity.TOP or Gravity.END).apply {
        inflate(R.menu.menu_app_icon)

        val isPinned = IconCache.launchers.any {
            it.packageName == gridItem.packageName && it.activityName == gridItem.activityName && it.pinned
        }
        menu.findItem(R.id.pin_icon).apply {
            title = getString(if (isPinned) R.string.unpin_app else R.string.pin_app)
            setIcon(if (isPinned) R.drawable.ic_heart_filled_vector else R.drawable.ic_heart_vector)
        }

        val iconTint = ColorStateList.valueOf(
            MaterialColors.getColor(
                this@handleGridItemPopupMenu,
                com.google.android.material.R.attr.colorOnSurface,
                getProperTextColor()
            )
        )
        menu.forEach {
            if (it.groupId == R.id.group_main) {
                it.iconTintList = iconTint
            }
        }
        menu.findItem(R.id.rename).isVisible =
            gridItem.type == ITEM_TYPE_ICON || (gridItem.type == ITEM_TYPE_FOLDER && !isOnAllAppsFragment)
        menu.findItem(R.id.pin_icon).isVisible =
            gridItem.type == ITEM_TYPE_ICON && isOnAllAppsFragment
        menu.findItem(R.id.hide_icon).isVisible =
            gridItem.type == ITEM_TYPE_ICON && isOnAllAppsFragment
        menu.findItem(R.id.add_to_folder).isVisible =
            gridItem.type == ITEM_TYPE_ICON && isOnAllAppsFragment && !isInFolderOverlay
        menu.findItem(R.id.remove_from_folder).isVisible =
            gridItem.type == ITEM_TYPE_ICON && isInFolderOverlay
        menu.findItem(R.id.resize).isVisible = gridItem.type == ITEM_TYPE_WIDGET
        menu.findItem(R.id.app_info).isVisible = gridItem.type == ITEM_TYPE_ICON
        menu.findItem(R.id.uninstall).isVisible = gridItem.type == ITEM_TYPE_ICON
                && canAppBeUninstalled(gridItem.packageName)
                && gridItem.packageName != packageName
        menu.findItem(R.id.remove).isVisible = !isOnAllAppsFragment

        val launcherApps =
            applicationContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val shortcuts = if (launcherApps.hasShortcutHostPermission()) {
            try {
                val query = LauncherApps.ShortcutQuery().setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                ).setPackage(gridItem.packageName)
                launcherApps.getShortcuts(query, Process.myUserHandle())
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }

        val hasShortcuts = !shortcuts.isNullOrEmpty()
        MenuCompat.setGroupDividerEnabled(menu, hasShortcuts)
        menu.setGroupVisible(R.id.group_shortcuts, hasShortcuts)
        val shortcutIds = HashMap<Int, String>()
        if (hasShortcuts) {
            val iconSize = resources.getDimensionPixelSize(R.dimen.menu_icon_size)
            shortcuts?.forEach { shortcutInfo ->
                val iconDrawable = launcherApps.getShortcutIconDrawable(
                    shortcutInfo, resources.displayMetrics.densityDpi
                )
                val shortcutItemId = View.generateViewId()
                shortcutIds[shortcutItemId] = shortcutInfo.id
                menu.add(R.id.group_shortcuts, shortcutItemId, Menu.NONE, shortcutInfo.getLabel())
                    .setIcon(
                        (iconDrawable ?: Color.TRANSPARENT.toDrawable())
                            .toBitmap(width = iconSize, height = iconSize)
                            .toDrawable(resources)
                    )
            }
        }

        setOnMenuItemClickListener { item ->
            listener.onAnyClick()
            val shortcutId = shortcutIds[item.itemId]
            if (shortcutId != null) {
                launcherApps.startShortcut(
                    gridItem.packageName,
                    shortcutId,
                    Rect(),
                    null,
                    Process.myUserHandle()
                )
                return@setOnMenuItemClickListener true
            }
            when (item.itemId) {
                R.id.pin_icon -> listener.pinToggled(gridItem)
                R.id.hide_icon -> listener.hide(gridItem)
                R.id.rename -> listener.rename(gridItem)
                R.id.resize -> listener.resize(gridItem)
                R.id.app_info -> listener.appInfo(gridItem)
                R.id.remove -> listener.remove(gridItem)
                R.id.uninstall -> listener.uninstall(gridItem)
                R.id.add_to_folder -> listener.addToFolder(gridItem)
                R.id.remove_from_folder -> listener.removeFromFolder(gridItem)
            }
            true
        }

        setOnDismissListener {
            listener.onDismiss()
        }

        listener.beforeShow(menu)

        show()
    }
}
