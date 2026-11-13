package com.prostuti.student.dataStore

object PrefKeys {
    val JWT_TOKEN = "jwt_token"
    val JWT_REFRESH_TOKEN = "jwt_refresh_token"
    val USER_ID = "user_id"
    val IS_FIRST_TIME = "is_first_time"
    val USER = "user"

    /** True once the local bookmark table matches the server for this user (even when both are empty). */
    val BOOKMARKS_SYNCED = "bookmarks_synced"

    /** The home screen widget's last loaded data (JSON, see WidgetSnapshot). */
    val WIDGET_SNAPSHOT = "widget_snapshot"

    /** App version whose widget-picker preview was published (Android 15+). */
    val WIDGET_PREVIEW_VERSION = "widget_preview_version"

    /** "Add the widget" card on Home: how often it was closed, and when last. */
    val WIDGET_PROMO_DISMISS_COUNT = "widget_promo_dismiss_count"
    val WIDGET_PROMO_DISMISSED_AT = "widget_promo_dismissed_at"
}
