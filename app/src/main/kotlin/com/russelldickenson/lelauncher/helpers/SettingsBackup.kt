package com.russelldickenson.lelauncher.helpers

import org.json.JSONException
import org.json.JSONObject

object SettingsBackup {
    private const val FORMAT_KEY = "format"
    private const val FORMAT_VALUE = "le-launcher-settings"
    private const val VALUES_KEY = "values"

    private enum class Type { BOOLEAN, INT, STRING }

    // only these keys travel in a backup; anything else in the file is ignored, and a value of
    // the wrong type is skipped, so a hand-edited file can't crash Config's typed getters
    private val keys = mapOf(
        HOME_ROW_COUNT to Type.INT,
        HOME_COLUMN_COUNT to Type.INT,
        DOCK_COLUMN_COUNT to Type.INT,
        DRAWER_COLUMN_COUNT to Type.INT,
        SHOW_SEARCH_BAR to Type.BOOLEAN,
        CLOSE_APP_DRAWER to Type.BOOLEAN,
        AUTO_SHOW_KEYBOARD_IN_APP_DRAWER to Type.BOOLEAN,
        SHOW_DRAWER_APP_LABELS to Type.BOOLEAN,
        DRAWER_LABEL_MAX_LINES to Type.INT,
        DRAWER_ICON_SCALE_PERCENT to Type.INT,
        HOME_ICON_SCALE_PERCENT to Type.INT,
        DRAWER_LABEL_FONT_SIZE to Type.INT,
        DRAWER_BACKGROUND_COLOR to Type.INT,
        DRAWER_TEXT_COLOR to Type.INT,
        USE_DEFAULT_DRAWER_COLORS to Type.BOOLEAN,
        ICON_PACK to Type.STRING,
        RESHAPE_ALL_ICONS to Type.BOOLEAN,
        ICON_SHAPE to Type.INT,
        SHOW_NOTIFICATION_BADGES to Type.BOOLEAN,
        NOTIFICATION_BADGE_COLOR to Type.INT,
        NOTIFICATION_BADGE_SHAPE to Type.INT,
        SHOW_NOTIFICATION_COUNT to Type.BOOLEAN,
        FAVOURITES_PLACEMENT to Type.INT,
        INCLUDE_TAB_APPS_IN_ALL to Type.BOOLEAN,
        SHOW_DRAWER_ICON_DROP_SHADOW to Type.BOOLEAN,
    )

    fun export(config: Config): String {
        val values = JSONObject()
        val all = config.prefs.all
        keys.keys.forEach { key ->
            all[key]?.let { values.put(key, it) }
        }
        return JSONObject()
            .put(FORMAT_KEY, FORMAT_VALUE)
            .put(VALUES_KEY, values)
            .toString(2)
    }

    // returns false without touching any setting if the text isn't a settings backup
    fun import(config: Config, json: String): Boolean {
        val values = try {
            val root = JSONObject(json)
            if (root.optString(FORMAT_KEY) != FORMAT_VALUE) return false
            root.getJSONObject(VALUES_KEY)
        } catch (_: JSONException) {
            return false
        }

        val editor = config.prefs.edit()
        keys.forEach { (key, type) ->
            // a key missing from the backup means "default", so clear whatever is set now
            editor.remove(key)
            val value = values.opt(key)
            when {
                type == Type.BOOLEAN && value is Boolean -> editor.putBoolean(key, value)
                type == Type.INT && value is Int -> editor.putInt(key, value)
                type == Type.STRING && value is String -> editor.putString(key, value)
            }
        }
        editor.apply()
        return true
    }
}
