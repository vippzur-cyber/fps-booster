package com.asteroid.fpsbooster

import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.mainStatusText)

        findViewById<Button>(R.id.btnGrantOverlay).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "Overlay udah diizinin ✅", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnGrantDnd).setOnClickListener {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (!nm.isNotificationPolicyAccessGranted) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            } else {
                Toast.makeText(this, "DND access udah diizinin ✅", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnAdbGuide).setOnClickListener {
            statusText.text = "Buat animasi speed-up, sambungin HP ke PC lalu jalanin lewat ADB:\n\n" +
                "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS\n\n" +
                "Sekali aja, abis itu fitur speed-up animasi otomatis jalan."
        }

        findViewById<Button>(R.id.btnStartBubble).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Izinin overlay dulu bro", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startService(Intent(this, FloatingBoosterService::class.java))
            Toast.makeText(this, "Bubble booster aktif, minimize app sekarang", Toast.LENGTH_LONG).show()
        }

        findViewById<Button>(R.id.btnStopBubble).setOnClickListener {
            stopService(Intent(this, FloatingBoosterService::class.java))
            Toast.makeText(this, "Bubble dimatiin", Toast.LENGTH_SHORT).show()
        }
    }
}
