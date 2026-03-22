package com.joaohouto.assistivemenutool

enum class MenuAction {
    HOME, BACK, RECENTS, LOCK,
    VOLUME_UP, VOLUME_DOWN,
    SCREENSHOT, NOTIFICATIONS, QUICK_SETTINGS, OPEN_APP;

    fun iconRes(): Int = when (this) {
        HOME          -> R.drawable.ic_home
        BACK          -> R.drawable.ic_back
        RECENTS       -> R.drawable.ic_recents
        LOCK          -> R.drawable.ic_lock
        VOLUME_UP     -> R.drawable.ic_volume_up
        VOLUME_DOWN   -> R.drawable.ic_volume_down
        SCREENSHOT    -> R.drawable.ic_screenshot
        NOTIFICATIONS -> R.drawable.ic_notifications
        QUICK_SETTINGS -> R.drawable.ic_quick_settings
        OPEN_APP      -> R.drawable.ic_open_app
    }

    fun labelRes(): Int = when (this) {
        HOME          -> R.string.action_home
        BACK          -> R.string.action_back
        RECENTS       -> R.string.action_recents
        LOCK          -> R.string.action_lock
        VOLUME_UP     -> R.string.action_volume_up
        VOLUME_DOWN   -> R.string.action_volume_down
        SCREENSHOT    -> R.string.action_screenshot
        NOTIFICATIONS -> R.string.action_notifications
        QUICK_SETTINGS -> R.string.action_quick_settings
        OPEN_APP      -> R.string.action_open_app
    }
}
