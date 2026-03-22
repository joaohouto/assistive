package com.joaohouto.assistivetouch

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat

class FloatingButtonService : Service() {

    // ── State ────────────────────────────────────────────────────────────────

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    private var menuOverlayView: View? = null

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        addFloatingButton()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        dismissMenuImmediate()
        if (::floatingView.isInitialized) windowManager.removeView(floatingView)
        super.onDestroy()
    }

    // ── Floating button ──────────────────────────────────────────────────────

    private fun addFloatingButton() {
        val sizePx = dpToPx(BUTTON_SIZE_DP)
        floatingView = buildButtonView(sizePx)

        params = WindowManager.LayoutParams(
            sizePx, sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = dpToPx(200)
        }

        windowManager.addView(floatingView, params)
        floatingView.setOnTouchListener(::onTouch)
    }

    private fun buildButtonView(sizePx: Int): View {
        val outerBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.argb(204, 20, 20, 20))
        }
        val innerBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.WHITE)
        }
        val innerSizePx = (sizePx * 0.52f).toInt()
        val innerView = View(this).apply {
            background = innerBg
            layoutParams = FrameLayout.LayoutParams(innerSizePx, innerSizePx, Gravity.CENTER)
        }
        return FrameLayout(this).apply {
            background = outerBg
            elevation = dpToPx(6).toFloat()
            addView(innerView)
        }
    }

    // ── Touch handling ───────────────────────────────────────────────────────

    private fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                floatingView.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).start()
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - initialTouchX).toInt()
                val dy = (event.rawY - initialTouchY).toInt()
                if (!isDragging && (kotlin.math.abs(dx) > DRAG_THRESHOLD || kotlin.math.abs(dy) > DRAG_THRESHOLD)) {
                    isDragging = true
                }
                if (isDragging) {
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager.updateViewLayout(floatingView, params)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                floatingView.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                if (isDragging) snapToEdge() else onButtonClick()
            }
        }
        return true
    }

    private fun snapToEdge() {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val sizePx = dpToPx(BUTTON_SIZE_DP)
        val targetX = if (params.x + sizePx / 2 < screenWidth / 2) 0 else screenWidth - sizePx
        val targetY = params.y.coerceIn(0, screenHeight - sizePx)
        val startX = params.x
        val startY = params.y

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = SNAP_DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val f = anim.animatedValue as Float
                params.x = (startX + (targetX - startX) * f).toInt()
                params.y = (startY + (targetY - startY) * f).toInt()
                try { windowManager.updateViewLayout(floatingView, params) } catch (_: Exception) {}
            }
            start()
        }
    }

    // ── Menu ─────────────────────────────────────────────────────────────────

    private fun onButtonClick() {
        if (menuOverlayView != null) hideMenu() else showMenu()
    }

    private fun showMenu() {
        val overlay = buildMenuOverlay()
        val overlayParams = WindowManager.LayoutParams(
            MATCH_PARENT, MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(overlay, overlayParams)
        menuOverlayView = overlay

        val frame = overlay as FrameLayout
        val dim = frame.getChildAt(0)
        val panel = frame.getChildAt(1)

        dim.alpha = 0f
        dim.animate().alpha(1f).setDuration(MENU_ANIM_SHOW_MS).start()

        panel.scaleX = 0.75f
        panel.scaleY = 0.75f
        panel.alpha = 0f
        panel.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(MENU_ANIM_SHOW_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun hideMenu() {
        val overlay = menuOverlayView ?: return
        val frame = overlay as FrameLayout
        val dim = frame.getChildAt(0)
        val panel = frame.getChildAt(1)

        panel.animate()
            .scaleX(0.75f).scaleY(0.75f).alpha(0f)
            .setDuration(MENU_ANIM_HIDE_MS)
            .start()
        dim.animate()
            .alpha(0f)
            .setDuration(MENU_ANIM_HIDE_MS)
            .withEndAction {
                try { windowManager.removeView(overlay) } catch (_: Exception) {}
                menuOverlayView = null
            }
            .start()
    }

    private fun dismissMenuImmediate() {
        menuOverlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            menuOverlayView = null
        }
    }

    private fun buildMenuOverlay(): FrameLayout {
        val overlay = FrameLayout(this)

        // Semi-transparent dim layer — tap outside to close
        val dim = View(this).apply {
            setBackgroundColor(Color.argb(110, 0, 0, 0))
            setOnClickListener { hideMenu() }
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }

        // Menu panel — intercepts clicks so they don't reach the dim
        val panel = buildMenuPanel()
        val panelParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER)
        panelParams.setMargins(dpToPx(32), 0, dpToPx(32), 0)
        panel.layoutParams = panelParams
        panel.setOnClickListener { /* consume */ }

        overlay.addView(dim)
        overlay.addView(panel)
        return overlay
    }

    private fun buildMenuPanel(): LinearLayout {
        val items = listOf(
            MenuItem(R.drawable.ic_home,    R.string.action_home,    MenuAction.HOME),
            MenuItem(R.drawable.ic_back,    R.string.action_back,    MenuAction.BACK),
            MenuItem(R.drawable.ic_recents, R.string.action_recents, MenuAction.RECENTS),
            MenuItem(R.drawable.ic_lock,    R.string.action_lock,    MenuAction.LOCK),
        )

        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(20).toFloat()
            setColor(Color.argb(235, 28, 28, 30))
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = bg
            val p = dpToPx(12)
            setPadding(p, p, p, p)

            // 2 items per row
            items.chunked(2).forEach { rowItems ->
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    rowItems.forEach { addView(buildMenuItem(it)) }
                })
            }
        }
    }

    private fun buildMenuItem(item: MenuItem): View {
        val containerSize = dpToPx(MENU_ITEM_SIZE_DP)
        val iconSize = dpToPx(46)
        val iconPad = dpToPx(11)

        val iconBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.argb(75, 255, 255, 255))
        }

        val icon = ImageView(this).apply {
            setImageResource(item.iconRes)
            setColorFilter(Color.WHITE)
            background = iconBg
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(iconPad, iconPad, iconPad, iconPad)
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }

        val label = TextView(this).apply {
            text = getString(item.labelRes)
            textSize = 10f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dpToPx(5)
            }
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(containerSize, containerSize)
            addView(icon)
            addView(label)
            setOnClickListener {
                animate().scaleX(0.88f).scaleY(0.88f).setDuration(70).withEndAction {
                    animate().scaleX(1f).scaleY(1f).setDuration(70).start()
                }.start()
                onMenuAction(item.action)
            }
        }
    }

    private fun onMenuAction(action: MenuAction) {
        hideMenu()
        AssistiveTouchAccessibilityService.instance?.execute(action)
    }

    // ── Data types ───────────────────────────────────────────────────────────

    enum class MenuAction { HOME, BACK, RECENTS, LOCK }

    private data class MenuItem(
        val iconRes: Int,
        val labelRes: Int,
        val action: MenuAction
    )

    // ── Notification ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, FloatingButtonService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_assistivetouch)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.action_stop), stopIntent)
            .setOngoing(true)
            .build()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    companion object {
        var isRunning = false
            private set

        const val ACTION_STOP = "com.joaohouto.assistivetouch.STOP"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "assistive_touch"
        private const val BUTTON_SIZE_DP = 56
        private const val MENU_ITEM_SIZE_DP = 90
        private const val DRAG_THRESHOLD = 8
        private const val SNAP_DURATION_MS = 260L
        private const val MENU_ANIM_SHOW_MS = 200L
        private const val MENU_ANIM_HIDE_MS = 150L
    }
}
