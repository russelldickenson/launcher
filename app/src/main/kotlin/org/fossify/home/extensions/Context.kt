package org.fossify.home.extensions

import android.app.role.RoleManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Size
import androidx.annotation.RequiresApi
import org.fossify.commons.helpers.isQPlus
import org.fossify.commons.helpers.isSPlus
import org.fossify.home.databases.AppsDatabase
import org.fossify.home.helpers.Config
import org.fossify.home.helpers.IconPackHelper
import org.fossify.home.interfaces.AppLaunchersDao
import org.fossify.home.interfaces.HiddenIconsDao
import org.fossify.home.interfaces.HomeScreenGridItemsDao
import org.fossify.home.services.NotificationBadgeListenerService
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
    if (iconPack.isNotEmpty()) {
        val packIcon = IconPackHelper.getIcon(this, iconPack, packageName, activityName)
        if (packIcon != null) {
            return packIcon
        }

        if (config.maskUnthemedIcons) {
            val originalIcon = systemDrawable() ?: getDrawableForPackageName(packageName)
            return if (originalIcon != null) {
                IconPackHelper.getMaskedIcon(this, iconPack, originalIcon) ?: originalIcon
            } else {
                null
            }
        }
    }

    return systemDrawable() ?: getDrawableForPackageName(packageName)
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
