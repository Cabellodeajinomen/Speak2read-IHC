package com.example.speak2read

import android.content.Context

object Speak2ReadPrefs {
    private const val SETTINGS_PREFS = "s2r_settings"
    private const val SESSION_PREFS = "s2r_session"
    private const val KEY_CONTEXT = "current_context"
    fun setCurrentContext(context: Context, value: String) {
        settings(context)
            .edit()
            .putString(KEY_CONTEXT, value)
            .apply()
    }

    fun getCurrentContext(context: Context): String {
        return settings(context)
            .getString(KEY_CONTEXT, "GENERAL")
            ?: "GENERAL"
    }
    const val KEY_FONT_SIZE_LARGE = "font_size_large"
    const val KEY_ALARM_DETECTION = "alarm_detection"
    const val KEY_DARK_THEME = "dark_theme"
    private const val KEY_LOGGED_IN = "logged_in"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_TYPE = "user_type"

    const val KEY_CUSTOM_1 = "custom_1"
    const val KEY_CUSTOM_2 = "custom_2"
    const val KEY_CUSTOM_3 = "custom_3"
    const val KEY_CUSTOM_4 = "custom_4"

    fun getCustomReplies(context: Context): List<String> {
        val prefs = settings(context)
        return listOf(
            prefs.getString(KEY_CUSTOM_1, "Hola") ?: "Hola",
            prefs.getString(KEY_CUSTOM_2, "Gracias") ?: "Gracias",
            prefs.getString(KEY_CUSTOM_3, "Necesito ayuda") ?: "Necesito ayuda",
            prefs.getString(KEY_CUSTOM_4, "Adiós") ?: "Adiós"
        )
    }

    fun saveCustomReplies(
        context: Context,
        r1: String,
        r2: String,
        r3: String,
        r4: String
    ) {
        settings(context).edit()
            .putString(KEY_CUSTOM_1, r1)
            .putString(KEY_CUSTOM_2, r2)
            .putString(KEY_CUSTOM_3, r3)
            .putString(KEY_CUSTOM_4, r4)
            .apply()
    }

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

