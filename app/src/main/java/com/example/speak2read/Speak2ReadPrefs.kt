package com.example.speak2read

import android.content.Context

object Speak2ReadPrefs {
    private const val SETTINGS_PREFS = "s2r_settings"
    private const val SESSION_PREFS = "s2r_session"

    const val KEY_FONT_SIZE_LARGE = "font_size_large"
    const val KEY_ALARM_DETECTION = "alarm_detection"
    const val KEY_DARK_THEME = "dark_theme"
    private const val KEY_LOGGED_IN = "logged_in"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_TYPE = "user_type"

    fun settings(context: Context) = context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
    fun session(context: Context) = context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)

    fun isLargeFont(context: Context): Boolean = settings(context).getBoolean(KEY_FONT_SIZE_LARGE, true)
    fun isAlarmDetectionEnabled(context: Context): Boolean = settings(context).getBoolean(KEY_ALARM_DETECTION, true)
    fun isDarkTheme(context: Context): Boolean = settings(context).getBoolean(KEY_DARK_THEME, true)

    fun fontScale(context: Context): Float = if (isLargeFont(context)) 1.18f else 1.0f

    fun setLoggedUser(context: Context, name: String, type: String) {
        session(context).edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_TYPE, type)
            .apply()
    }

    fun isLoggedIn(context: Context): Boolean = session(context).getBoolean(KEY_LOGGED_IN, false)
    fun currentUserName(context: Context): String = session(context).getString(KEY_USER_NAME, "Invitado") ?: "Invitado"
    fun currentUserType(context: Context): String = session(context).getString(KEY_USER_TYPE, "Invitado") ?: "Invitado"

    fun clearSession(context: Context) {
        session(context).edit().clear().apply()
    }
}

