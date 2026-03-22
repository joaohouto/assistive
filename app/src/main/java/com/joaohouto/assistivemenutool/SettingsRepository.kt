package com.joaohouto.assistivemenutool

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {

    val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var opacity: Float
        get() = prefs.getFloat(KEY_OPACITY, DEFAULT_OPACITY)
        set(value) = prefs.edit().putFloat(KEY_OPACITY, value).apply()

    var buttonSizeDp: Int
        get() = prefs.getInt(KEY_BUTTON_SIZE, DEFAULT_SIZE_DP)
        set(value) = prefs.edit().putInt(KEY_BUTTON_SIZE, value).apply()

    var menuActions: List<MenuAction>
        get() {
            val stored = prefs.getString(KEY_MENU_ACTIONS, null) ?: return DEFAULT_ACTIONS
            return stored.split(",")
                .mapNotNull { name -> MenuAction.entries.find { it.name == name } }
                .ifEmpty { DEFAULT_ACTIONS }
        }
        set(value) = prefs.edit()
            .putString(KEY_MENU_ACTIONS, value.joinToString(",") { it.name })
            .apply()

    companion object {
        private const val PREFS_NAME = "at_settings"
        const val KEY_OPACITY   = "opacity"
        const val KEY_BUTTON_SIZE = "button_size"
        const val KEY_MENU_ACTIONS = "menu_actions"

        const val DEFAULT_OPACITY  = 0.85f
        const val DEFAULT_SIZE_DP  = 56
        val SIZE_OPTIONS = listOf(48, 56, 68)               // dp values
        val SIZE_LABELS  = listOf("Pequeno", "Médio", "Grande")
        val DEFAULT_ACTIONS = listOf(
            MenuAction.HOME, MenuAction.BACK,
            MenuAction.RECENTS, MenuAction.LOCK
        )
    }
}
