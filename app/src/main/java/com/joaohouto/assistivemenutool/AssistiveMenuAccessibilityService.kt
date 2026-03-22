package com.joaohouto.assistivemenutool

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent

class AssistiveMenuAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    fun execute(action: MenuAction) {
        when (action) {
            MenuAction.HOME    -> performGlobalAction(GLOBAL_ACTION_HOME)
            MenuAction.BACK    -> performGlobalAction(GLOBAL_ACTION_BACK)
            MenuAction.RECENTS -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            MenuAction.LOCK    -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                }
            }
            else -> Unit  // volume handled by AudioManager in FloatingButtonService
        }
    }

    companion object {
        var instance: AssistiveMenuAccessibilityService? = null
            private set

        val isEnabled get() = instance != null
    }
}
