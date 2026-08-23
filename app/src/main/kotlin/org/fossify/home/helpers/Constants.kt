package org.fossify.home.helpers

const val WIDGET_LIST_SECTION = 0
const val WIDGET_LIST_ITEMS_HOLDER = 1

const val REPOSITORY_NAME = "Launcher"

// shared prefs
const val WAS_HOME_SCREEN_INIT = "was_home_screen_init"
const val HOME_ROW_COUNT = "home_row_count"
const val HOME_COLUMN_COUNT = "home_column_count"
const val DRAWER_COLUMN_COUNT = "drawer_column_count"
const val SHOW_SEARCH_BAR = "show_search_bar"
const val CLOSE_APP_DRAWER = "close_app_drawer"
const val AUTO_SHOW_KEYBOARD_IN_APP_DRAWER = "auto_show_keyboard_in_app_drawer"
const val SHOW_DRAWER_APP_LABELS = "show_drawer_app_labels"
const val DRAWER_LABEL_MAX_LINES = "drawer_label_max_lines"
const val DRAWER_ICON_SCALE_PERCENT = "drawer_icon_scale_percent"
const val DRAWER_LABEL_FONT_SIZE = "drawer_label_font_size"
const val ICON_PACK = "icon_pack"
const val RESHAPE_ALL_ICONS = "mask_unthemed_icons"
const val SHOW_NOTIFICATION_BADGES = "show_notification_badges"
const val NOTIFICATION_BADGE_COLOR = "notification_badge_color"
const val NOTIFICATION_BADGE_SHAPE = "notification_badge_shape"
const val SHOW_NOTIFICATION_COUNT = "show_notification_count"
const val SHOW_FAVOURITES_DIVIDER = "show_favourites_divider"

const val NOTIFICATION_BADGE_SHAPE_CIRCLE = 0
const val NOTIFICATION_BADGE_SHAPE_ROUNDED_SQUARE = 1
const val NOTIFICATION_BADGE_SHAPE_SHARP_SQUARE = 2

const val ICON_SHAPE = "icon_shape"

const val ICON_SHAPE_CIRCLE = 0
const val ICON_SHAPE_SQUIRCLE = 1
const val ICON_SHAPE_ROUNDED_SQUARE = 2
const val ICON_SHAPE_SQUARE = 3

// default home screen grid size
const val ROW_COUNT = 6
const val MIN_ROW_COUNT = 2
const val MAX_ROW_COUNT = 15

// icon grid column count
const val MIN_DRAWER_COLUMN_COUNT = 3
const val MAX_DRAWER_COLUMN_COUNT = 6

// icon scale, in percent, shared by the app drawer and the home screen - "100%" is what used to
// be labelled "120%" before the rescale
const val MIN_DRAWER_ICON_SCALE_PERCENT = 80
const val MAX_DRAWER_ICON_SCALE_PERCENT = 120
const val DRAWER_ICON_SCALE_PERCENT_STEP = 10
const val DEFAULT_DRAWER_ICON_SCALE_PERCENT = 100

// drawer label font size, in sp
const val MIN_DRAWER_LABEL_FONT_SIZE = 12
const val MAX_DRAWER_LABEL_FONT_SIZE = 20
const val DRAWER_LABEL_FONT_SIZE_STEP = 2
const val DEFAULT_DRAWER_LABEL_FONT_SIZE = 12

// label max lines, shared by the app drawer and the home screen
const val MIN_DRAWER_LABEL_MAX_LINES = 1
const val MAX_DRAWER_LABEL_MAX_LINES = 3
const val DRAWER_LABEL_MAX_LINES_STEP = 1
const val DEFAULT_DRAWER_LABEL_MAX_LINES = 2

const val UNINSTALL_APP_REQUEST_CODE = 50
const val REQUEST_CONFIGURE_WIDGET = 51
const val REQUEST_ALLOW_BINDING_WIDGET = 52
const val REQUEST_CREATE_SHORTCUT = 53
const val REQUEST_SET_DEFAULT = 54

const val ITEM_TYPE_ICON = 0
const val ITEM_TYPE_WIDGET = 1
const val ITEM_TYPE_SHORTCUT = 2
const val ITEM_TYPE_FOLDER = 3

const val WIDGET_HOST_ID = 12345
const val MAX_CLICK_DURATION = 150
