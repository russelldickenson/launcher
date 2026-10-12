package com.russelldickenson.lelauncher.extensions

import android.app.role.RoleManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.util.Size
import android.util.TypedValue
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import com.russelldickenson.lelauncher.R
import com.russelldickenson.lelauncher.databases.AppsDatabase
import com.russelldickenson.lelauncher.helpers.Config
import com.russelldickenson.lelauncher.helpers.IconContrastHelper
import com.russelldickenson.lelauncher.helpers.IconPackHelper
import com.russelldickenson.lelauncher.helpers.isOnMainThread
import com.russelldickenson.lelauncher.helpers.isQPlus
import com.russelldickenson.lelauncher.helpers.isSPlus
import com.russelldickenson.lelauncher.interfaces.AppLaunchersDao
import com.russelldickenson.lelauncher.interfaces.DrawerFoldersDao
import com.russelldickenson.lelauncher.interfaces.HiddenIconsDao
import com.russelldickenson.lelauncher.interfaces.HomeScreenGridItemsDao
import com.russelldickenson.lelauncher.services.NotificationBadgeListenerService
import kotlin.math.ceil
import kotlin.math.max

val Context.config: Config get() = Config.newInstance(applicationContext)

val Context.launchersDB: AppLaunchersDao
    get() = AppsDatabase.getInstance(applicationContext).AppLaunchersDao()

val Context.homeScreenGridItemsDB: HomeScreenGridItemsDao
    get() = AppsDatabase.getInstance(
        applicationContext
    ).HomeScreenGridItemsDao()

val Context.hiddenIconsDB: HiddenIconsDao
    get() = AppsDatabase.getInstance(applicationContext).HiddenIconsDao()

val Context.drawerFoldersDB: DrawerFoldersDao
    get() = AppsDatabase.getInstance(applicationContext).DrawerFoldersDao()

@get:RequiresApi(Build.VERSION_CODES.Q)
val Context.roleManager: RoleManager
    get() = getSystemService(RoleManager::class.java)

fun Context.getDrawableForPackageName(packageName: String): Drawable? {
    var drawable: Drawable? = null
    try {
        // try getting the properly colored launcher icons
        val launcher = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val activityList = launcher.getActivityList(packageName, Process.myUserHandle())[0]
        drawable = activityList.getBadgedIcon(0)
    } catch (e: Exception) {
    } catch (e: Error) {
    }

    if (drawable == null) {
        drawable = try {
            packageManager.getApplicationIcon(packageName)
        } catch (ignored: Exception) {
            null
        }
    }

    return drawable
}

fun Context.getAppIcon(packageName: String, activityName: String, systemDrawable: () -> Drawable? = { null }): Drawable? {
    val iconPack = config.iconPack
    val icon = if (iconPack.isNotEmpty()) {
        IconPackHelper.getIcon(this, iconPack, packageName, activityName)
    } else {
        null
    } ?: systemDrawable() ?: getDrawableForPackageName(packageName)

    return if (config.reshapeAllIcons && icon != null) {
        IconPackHelper.getShapedIcon(this, "$packageName/$activityName", icon, config.iconShape)
    } else {
        icon
    }
}

// getAppIcon() plus the same low-contrast backdrop treatment the drawer's own app scan applies -
// shared so any other caller that independently fetches an icon (rather than reusing the
// drawer's already-processed IconCache.launchers entry) still gets a consistent result instead of
// a version that's readable in the drawer but not wherever this was called from
fun Context.getAppIconBitmapWithContrastBackdrop(
    packageName: String,
    activityName: String,
    systemDrawable: () -> Drawable? = { null }
): Bitmap? {
    val drawable = getAppIcon(packageName, activityName, systemDrawable) ?: return null
    val bitmap = drawable.toBitmap(
        width = max(drawable.intrinsicWidth, 1),
        height = max(drawable.intrinsicHeight, 1),
        config = Bitmap.Config.ARGB_8888
    )
    return IconContrastHelper.drawContrastBackdropIfNeeded(bitmap, getAppDrawerBackgroundColor())
}

fun Context.getInitialCellSize(
    info: AppWidgetProviderInfo,
    fallbackWidth: Int,
    fallbackHeight: Int
): Size {
    return if (isSPlus() && info.targetCellWidth != 0 && info.targetCellHeight != 0) {
        Size(info.targetCellWidth, info.targetCellHeight)
    } else {
        val widthCells = getCellCount(fallbackWidth)
        val heightCells = getCellCount(fallbackHeight)
        Size(widthCells, heightCells)
    }
}

fun Context.getCellCount(size: Int): Int {
    val tiles = ceil(((size / resources.displayMetrics.density) - 30) / 70.0).toInt()
    return max(tiles, 1)
}

fun Context.isNotificationListenerEnabled(): Boolean {
    val componentName = ComponentName(this, NotificationBadgeListenerService::class.java)
    val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
    return enabledListeners?.contains(componentName.flattenToString()) == true
}

fun Context.openNotificationListenerSettings() {
    try {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    } catch (ignored: Exception) {
    }
}

fun Context.isSystemInLightMode(): Boolean {
    val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return nightMode == Configuration.UI_MODE_NIGHT_NO
}

// darkens text to ~90% black when the system is in light mode, leaving dark mode untouched
fun Context.getThemeAwareTextColor(originalColor: Int): Int {
    return if (isSystemInLightMode()) Color.argb(230, 0, 0, 0) else originalColor
}

fun Context.getAppDrawerBackgroundColor(): Int {
    if (!config.useDefaultDrawerColors) {
        val customColor = config.drawerBackgroundColor
        if (customColor != 0) {
            return customColor
        }
    }

    return if (isSystemInLightMode()) {
        getColor(com.russelldickenson.lelauncher.R.color.app_drawer_light_mode_background)
    } else {
        getColor(com.russelldickenson.lelauncher.R.color.app_drawer_dark_mode_background)
    }
}

fun Context.getAppDrawerTextColor(): Int {
    if (!config.useDefaultDrawerColors) {
        val customColor = config.drawerTextColor
        if (customColor != 0) {
            return customColor
        }
    }

    return if (isSystemInLightMode()) {
        getColor(com.russelldickenson.lelauncher.R.color.m3_on_surface)
    } else {
        Color.WHITE
    }
}

// a fill color for the app drawer's search field, so the field itself reads as a subtly distinct
// surface rather than blending flat into the drawer background behind it
fun Context.getAppDrawerSearchFillColor(): Int {
    val blendTarget = if (isSystemInLightMode()) Color.BLACK else Color.WHITE
    return ColorUtils.blendARGB(getAppDrawerBackgroundColor(), blendTarget, 0.06f)
}

// a surface meant to sit on top of the drawer - an open folder's background, a popup menu's card,
// etc: a shade darker than the drawer's background in light mode, a shade lighter in dark mode
// (same "blend toward black/white" trick as the search field colors above), so it still reads as
// its own surface instead of blending flush into the drawer behind it
fun Context.getAppDrawerOverlaySurfaceColor(): Int {
    val blendTarget = if (isSystemInLightMode()) Color.BLACK else Color.WHITE
    return ColorUtils.blendARGB(getAppDrawerBackgroundColor(), blendTarget, 0.08f)
}

// a surface meant to sit on top of an overlay surface itself - the folder menu popup shown over
// an open folder's own card, for example. Blends further in the same direction the overlay
// surface already blended toward, so it still reads as a step up rather than matching the
// overlay surface it's rendered on top of
fun Context.getAppDrawerElevatedSurfaceColor(): Int {
    val blendTarget = if (isSystemInLightMode()) Color.BLACK else Color.WHITE
    return ColorUtils.blendARGB(getAppDrawerOverlaySurfaceColor(), blendTarget, 0.08f)
}

// what an unscaled (100%) icon should measure, in px, shared by the app drawer and the home
// screen so both render icons at the same physical size at 100% - based on the screen width at
// the default column count, independent of whatever column/row count is actually in use, so icon
// scaling never drifts with grid density. The x1.2 accounts for a deliberate rescale: what used
// to be labelled "120%" is now the "100%" baseline, per user request
fun Context.getReferenceIconWidth(): Float {
    val referenceColumnCount = resources.getInteger(R.integer.portrait_column_count)
    val referenceCellWidth = realScreenSize.x / referenceColumnCount
    val referenceMargin = resources.getDimension(R.dimen.icon_side_margin)
    return (referenceCellWidth - 2 * referenceMargin) * 1.2f
}

fun Context.isDefaultLauncher(): Boolean {
    return if (isQPlus()) {
        with(roleManager) {
            isRoleAvailable(RoleManager.ROLE_HOME) && isRoleHeld(RoleManager.ROLE_HOME)
        }
    } else {
        val filters = ArrayList<IntentFilter>()
        val activities = ArrayList<ComponentName>()
        @Suppress("DEPRECATION")
        packageManager.getPreferredActivities(filters, activities, null)
        return activities.indices.any { i ->
            activities[i].packageName == packageName &&
                    filters[i].hasAction(Intent.ACTION_MAIN) &&
                    filters[i].hasCategory(Intent.CATEGORY_HOME)
        }
    }
}

val Context.portrait get() = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

val Context.realScreenSize: Point
    get() = Point().also { (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealSize(it) }

fun Context.isPackageInstalled(packageName: String) = try {
    packageManager.getPackageInfo(packageName, 0)
    true
} catch (_: PackageManager.NameNotFoundException) {
    false
}

fun Context.toast(@StringRes id: Int, length: Int = Toast.LENGTH_SHORT) = toast(getString(id), length)

fun Context.toast(message: String, length: Int = Toast.LENGTH_SHORT) {
    if (isOnMainThread()) {
        Toast.makeText(applicationContext, message, length).show()
    } else {
        Handler(Looper.getMainLooper()).post { Toast.makeText(applicationContext, message, length).show() }
    }
}

fun Context.showErrorToast(exception: Exception, length: Int = Toast.LENGTH_LONG) =
    toast(getString(R.string.error, exception.toString()), length)

// resolves a color attribute against the current (Material 3, dynamic-color aware) theme
fun Context.getThemeColor(@AttrRes attr: Int): Int {
    val value = TypedValue()
    theme.resolveAttribute(attr, value, true)
    return if (value.resourceId != 0) getColor(value.resourceId) else value.data
}

fun Context.getProperTextColor() = getThemeColor(com.google.android.material.R.attr.colorOnSurface)

fun Context.getProperBackgroundColor() = getThemeColor(android.R.attr.colorBackground)

fun Context.getProperPrimaryColor() = getThemeColor(androidx.appcompat.R.attr.colorPrimary)

// tint of the top bar once the content below it has been scrolled
fun Context.getScrolledTopBarColor() = getThemeColor(com.google.android.material.R.attr.colorSurfaceContainer)

fun Context.showErrorToast(message: String, length: Int = Toast.LENGTH_LONG) =
    toast(getString(R.string.error, message), length)
