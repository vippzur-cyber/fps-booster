package com.asteroid.fpsbooster

import android.animation.ValueAnimator
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat

class FloatingBoosterService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var panelView: View? = null

    private var bubbleParams: WindowManager.LayoutParams? = null
    private var panelExpanded = false

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceCompat()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        showBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundServiceCompat() {
        val channelId = "fps_booster_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "FPS Booster", NotificationManager.IMPORTANCE_MIN
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("FPS Booster aktif")
            .setContentText("Tap bubble buat boost")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun windowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE
    }

    // ---------- BUBBLE ----------
    private fun showBubble() {
        bubbleView = LayoutInflater.from(this).inflate(R.layout.overlay_bubble, null)

        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 300
        }

        windowManager.addView(bubbleView, bubbleParams)

        val icon = bubbleView!!.findViewById<ImageView>(R.id.bubbleIcon)
        icon.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bubbleParams!!.x
                    initialY = bubbleParams!!.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) isDragging = true
                    bubbleParams!!.x = initialX + dx
                    bubbleParams!!.y = initialY + dy
                    windowManager.updateViewLayout(bubbleView, bubbleParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) togglePanel()
                    true
                }
                else -> false
            }
        }
    }

    // ---------- PANEL ----------
    private fun togglePanel() {
        if (panelExpanded) closePanel() else openPanel()
    }

    private fun openPanel() {
        if (panelView != null) return
        panelExpanded = true
        bubbleView?.visibility = View.GONE

        panelView = LayoutInflater.from(this).inflate(R.layout.overlay_panel, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        windowManager.addView(panelView, params)

        val btnBoost = panelView!!.findViewById<View>(R.id.btnBoost)
        val btnClose = panelView!!.findViewById<View>(R.id.btnClosePanel)
        val statusText = panelView!!.findViewById<TextView>(R.id.statusText)

        btnClose.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) closePanel()
            true
        }

        btnBoost.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                statusText.text = "Boosting..."
                v.animate().rotationBy(360f).setDuration(600).start()
                v.postDelayed({
                    val result = BoosterEngine.runFullBoost(this)
                    val sb = StringBuilder()
                    sb.append("✅ ${result.appsKilled} app dimatiin\n")
                    sb.append("✅ RAM freed: ${result.ramFreedMb}MB\n")
                    sb.append(if (result.dndOn) "✅ DND aktif\n" else "⚠️ DND: perlu izin (buka Settings)\n")
                    sb.append(if (result.animSped) "✅ Animasi dipercepat" else "⚠️ Animasi: perlu WRITE_SECURE_SETTINGS via ADB")
                    statusText.text = sb.toString()
                }, 600)
            }
            true
        }

        val restoreBtn = panelView!!.findViewById<View>(R.id.btnRestore)
        restoreBtn.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                BoosterEngine.disableDnd(this)
                BoosterEngine.resetAnimations(this)
                statusText.text = "Direstore ke normal."
            }
            true
        }
    }

    private fun closePanel() {
        panelView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { }
        }
        panelView = null
        panelExpanded = false
        bubbleView?.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        bubbleView?.let { try { windowManager.removeView(it) } catch (e: Exception) {} }
        panelView?.let { try { windowManager.removeView(it) } catch (e: Exception) {} }
    }
}
