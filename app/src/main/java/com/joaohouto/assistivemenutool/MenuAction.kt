package com.joaohouto.assistivemenutool

enum class MenuAction {
    HOME, BACK, RECENTS, LOCK, VOLUME_UP, VOLUME_DOWN;

    fun iconRes(): Int = when (this) {
        HOME       -> R.drawable.ic_home
        BACK       -> R.drawable.ic_back
        RECENTS    -> R.drawable.ic_recents
        LOCK       -> R.drawable.ic_lock
        VOLUME_UP  -> R.drawable.ic_volume_up
        VOLUME_DOWN-> R.drawable.ic_volume_down
    }

    fun labelRes(): Int = when (this) {
        HOME       -> R.string.action_home
        BACK       -> R.string.action_back
        RECENTS    -> R.string.action_recents
        LOCK       -> R.string.action_lock
        VOLUME_UP  -> R.string.action_volume_up
        VOLUME_DOWN-> R.string.action_volume_down
    }
}
