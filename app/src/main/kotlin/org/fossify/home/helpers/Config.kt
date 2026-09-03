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

    var homeColumnCount: Int
        get() = prefs.getInt(HOME_COLUMN_COUNT, context.resources.getInteger(R.integer.portrait_column_count))
            .coerceIn(MIN_DRAWER_COLUMN_COUNT, MAX_DRAWER_COLUMN_COUNT)
        set(homeColumnCount) = prefs.edit().putInt(HOME_COLUMN_COUNT, homeColumnCount).apply()

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

    // home screen labels share the app drawer's own show/hide setting - both surfaces read and
    // write showDrawerAppLabels directly, so there's exactly one stored value, not two kept in sync
    var showHomeAppLabels: Boolean
        get() = showDrawerAppLabels
        set(value) {
            showDrawerAppLabels = value
        }

    var drawerLabelMaxLines: Int
        get() = prefs.getInt(DRAWER_LABEL_MAX_LINES, DEFAULT_DRAWER_LABEL_MAX_LINES)
            .coerceIn(MIN_DRAWER_LABEL_MAX_LINES, MAX_DRAWER_LABEL_MAX_LINES)
        set(drawerLabelMaxLines) = prefs.edit().putInt(DRAWER_LABEL_MAX_LINES, drawerLabelMaxLines).apply()

    // home screen shares the app drawer's own label max-lines setting - see showHomeAppLabels
    var homeLabelMaxLines: Int
        get() = drawerLabelMaxLines
        set(value) {
            drawerLabelMaxLines = value
        }

    var drawerIconScalePercent: Int
        get() = prefs.getInt(DRAWER_ICON_SCALE_PERCENT, DEFAULT_DRAWER_ICON_SCALE_PERCENT)
            .coerceIn(MIN_DRAWER_ICON_SCALE_PERCENT, MAX_DRAWER_ICON_SCALE_PERCENT)
        set(drawerIconScalePercent) = prefs.edit().putInt(DRAWER_ICON_SCALE_PERCENT, drawerIconScalePercent).apply()

    // home screen shares the app drawer's own icon scale setting - see showHomeAppLabels
    var homeIconScalePercent: Int
        get() = drawerIconScalePercent
        set(value) {
            drawerIconScalePercent = value
        }

    var drawerLabelFontSize: Int
        get() = prefs.getInt(DRAWER_LABEL_FONT_SIZE, DEFAULT_DRAWER_LABEL_FONT_SIZE)
            .coerceIn(MIN_DRAWER_LABEL_FONT_SIZE, MAX_DRAWER_LABEL_FONT_SIZE)
        set(drawerLabelFontSize) = prefs.edit().putInt(DRAWER_LABEL_FONT_SIZE, drawerLabelFontSize).apply()

    // 0 (fully transparent) means "no custom colour set - use the automatic theme/light-mode-based
    // colour computed by getAppDrawerBackgroundColor()/getAppDrawerTextColor()", since it isn't a
    // colour anyone would deliberately pick for a drawer background or label colour
    var drawerBackgroundColor: Int
        get() = prefs.getInt(DRAWER_BACKGROUND_COLOR, 0)
        set(drawerBackgroundColor) = prefs.edit().putInt(DRAWER_BACKGROUND_COLOR, drawerBackgroundColor).apply()

    var drawerTextColor: Int
        get() = prefs.getInt(DRAWER_TEXT_COLOR, 0)
        set(drawerTextColor) = prefs.edit().putInt(DRAWER_TEXT_COLOR, drawerTextColor).apply()

    var useDefaultDrawerColors: Boolean
        get() = prefs.getBoolean(USE_DEFAULT_DRAWER_COLORS, true)
        set(useDefaultDrawerColors) = prefs.edit().putBoolean(USE_DEFAULT_DRAWER_COLORS, useDefaultDrawerColors).apply()

    // empty string means the device's default icons are used
    var iconPack: String
        get() = prefs.getString(ICON_PACK, "") ?: ""
        set(iconPack) = prefs.edit().putString(ICON_PACK, iconPack).apply()

    var reshapeAllIcons: Boolean
        get() = prefs.getBoolean(RESHAPE_ALL_ICONS, true)
        set(reshapeAllIcons) = prefs.edit().putBoolean(RESHAPE_ALL_ICONS, reshapeAllIcons).apply()

    var iconShape: Int
        get() = prefs.getInt(ICON_SHAPE, ICON_SHAPE_CIRCLE)
        set(iconShape) = prefs.edit().putInt(ICON_SHAPE, iconShape).apply()

    var showIconShadow: Boolean
        get() = prefs.getBoolean(SHOW_ICON_SHADOW, false)
        set(showIconShadow) = prefs.edit().putBoolean(SHOW_ICON_SHADOW, showIconShadow).apply()

    var showNotificationBadges: Boolean
        get() = prefs.getBoolean(SHOW_NOTIFICATION_BADGES, true)
        set(showNotificationBadges) = prefs.edit().putBoolean(SHOW_NOTIFICATION_BADGES, showNotificationBadges).apply()

    var showFavouritesDivider: Boolean
        get() = prefs.getBoolean(SHOW_FAVOURITES_DIVIDER, true)
        set(showFavouritesDivider) = prefs.edit().putBoolean(SHOW_FAVOURITES_DIVIDER, showFavouritesDivider).apply()

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
