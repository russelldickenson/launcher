package org.fossify.home.helpers

import android.content.Context
import android.graphics.Color
import org.fossify.commons.helpers.BaseConfig
import org.fossify.home.R

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var wasHomeScreenInit: Boolean
        get() = prefs.getBoolean(WAS_HOME_SCREEN_INIT, false)
        set(wasHomeScreenInit) = prefs.edit().putBoolean(WAS_HOME_SCREEN_INIT, wasHomeScreenInit).apply()

    var homeRowCount: Int
        get() = prefs.getInt(HOME_ROW_COUNT, ROW_COUNT)
        set(homeRowCount) = prefs.edit().putInt(HOME_ROW_COUNT, homeRowCount).apply()

    // despite the name, this now drives the column count of both the app drawer and the home screen grid
    var drawerColumnCount: Int
        get() = prefs.getInt(DRAWER_COLUMN_COUNT, context.resources.getInteger(R.integer.portrait_column_count))
            .coerceIn(MIN_DRAWER_COLUMN_COUNT, MAX_DRAWER_COLUMN_COUNT)
        set(drawerColumnCount) = prefs.edit().putInt(DRAWER_COLUMN_COUNT, drawerColumnCount).apply()

    var showSearchBar: Boolean
        get() = prefs.getBoolean(SHOW_SEARCH_BAR, true)
        set(showSearchBar) = prefs.edit().putBoolean(SHOW_SEARCH_BAR, showSearchBar).apply()

    var closeAppDrawer: Boolean
        get() = prefs.getBoolean(CLOSE_APP_DRAWER, false)
        set(closeAppDrawer) = prefs.edit().putBoolean(CLOSE_APP_DRAWER, closeAppDrawer).apply()

    var autoShowKeyboardInAppDrawer: Boolean
        get() = prefs.getBoolean(AUTO_SHOW_KEYBOARD_IN_APP_DRAWER, false)
        set(autoShowKeyboardInAppDrawer) = prefs.edit()
            .putBoolean(AUTO_SHOW_KEYBOARD_IN_APP_DRAWER, autoShowKeyboardInAppDrawer).apply()

    var showDrawerAppLabels: Boolean
        get() = prefs.getBoolean(SHOW_DRAWER_APP_LABELS, true)
        set(showDrawerAppLabels) = prefs.edit().putBoolean(SHOW_DRAWER_APP_LABELS, showDrawerAppLabels).apply()

    var showHomeAppLabels: Boolean
        get() = prefs.getBoolean(SHOW_HOME_APP_LABELS, true)
        set(showHomeAppLabels) = prefs.edit().putBoolean(SHOW_HOME_APP_LABELS, showHomeAppLabels).apply()

    var drawerLabelMaxLines: Int
        get() = prefs.getInt(DRAWER_LABEL_MAX_LINES, DEFAULT_DRAWER_LABEL_MAX_LINES)
            .coerceIn(MIN_DRAWER_LABEL_MAX_LINES, MAX_DRAWER_LABEL_MAX_LINES)
        set(drawerLabelMaxLines) = prefs.edit().putInt(DRAWER_LABEL_MAX_LINES, drawerLabelMaxLines).apply()

    var drawerIconScalePercent: Int
        get() = prefs.getInt(DRAWER_ICON_SCALE_PERCENT, DEFAULT_DRAWER_ICON_SCALE_PERCENT)
            .coerceIn(MIN_DRAWER_ICON_SCALE_PERCENT, MAX_DRAWER_ICON_SCALE_PERCENT)
        set(drawerIconScalePercent) = prefs.edit().putInt(DRAWER_ICON_SCALE_PERCENT, drawerIconScalePercent).apply()

    var homeIconScalePercent: Int
        get() = prefs.getInt(HOME_ICON_SCALE_PERCENT, DEFAULT_HOME_ICON_SCALE_PERCENT)
            .coerceIn(MIN_HOME_ICON_SCALE_PERCENT, MAX_HOME_ICON_SCALE_PERCENT)
        set(homeIconScalePercent) = prefs.edit().putInt(HOME_ICON_SCALE_PERCENT, homeIconScalePercent).apply()

    var drawerLabelFontSize: Int
        get() = prefs.getInt(DRAWER_LABEL_FONT_SIZE, DEFAULT_DRAWER_LABEL_FONT_SIZE)
            .coerceIn(MIN_DRAWER_LABEL_FONT_SIZE, MAX_DRAWER_LABEL_FONT_SIZE)
        set(drawerLabelFontSize) = prefs.edit().putInt(DRAWER_LABEL_FONT_SIZE, drawerLabelFontSize).apply()

    var multilineHomeAppLabels: Boolean
        get() = prefs.getBoolean(MULTILINE_HOME_APP_LABELS, true)
        set(multilineHomeAppLabels) = prefs.edit().putBoolean(MULTILINE_HOME_APP_LABELS, multilineHomeAppLabels).apply()

    // empty string means the device's default icons are used
    var iconPack: String
        get() = prefs.getString(ICON_PACK, "") ?: ""
        set(iconPack) = prefs.edit().putString(ICON_PACK, iconPack).apply()

    var maskUnthemedIcons: Boolean
        get() = prefs.getBoolean(MASK_UNTHEMED_ICONS, true)
        set(maskUnthemedIcons) = prefs.edit().putBoolean(MASK_UNTHEMED_ICONS, maskUnthemedIcons).apply()

    var iconShape: Int
        get() = prefs.getInt(ICON_SHAPE, ICON_SHAPE_CIRCLE)
        set(iconShape) = prefs.edit().putInt(ICON_SHAPE, iconShape).apply()

    var showNotificationBadges: Boolean
        get() = prefs.getBoolean(SHOW_NOTIFICATION_BADGES, true)
        set(showNotificationBadges) = prefs.edit().putBoolean(SHOW_NOTIFICATION_BADGES, showNotificationBadges).apply()

    var notificationBadgeColor: Int
        get() = prefs.getInt(NOTIFICATION_BADGE_COLOR, Color.RED)
        set(notificationBadgeColor) = prefs.edit().putInt(NOTIFICATION_BADGE_COLOR, notificationBadgeColor).apply()

    var notificationBadgeShape: Int
        get() = prefs.getInt(NOTIFICATION_BADGE_SHAPE, NOTIFICATION_BADGE_SHAPE_CIRCLE)
        set(notificationBadgeShape) = prefs.edit().putInt(NOTIFICATION_BADGE_SHAPE, notificationBadgeShape).apply()

    var showNotificationCount: Boolean
        get() = prefs.getBoolean(SHOW_NOTIFICATION_COUNT, false)
        set(showNotificationCount) = prefs.edit().putBoolean(SHOW_NOTIFICATION_COUNT, showNotificationCount).apply()
}
