package com.zico.dynamicisland

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnOverlayPermission).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "قبلا فعال شده", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnNotificationPermission).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.btnPhonePermission).setOnClickListener {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.READ_PHONE_STATE,
                    android.Manifest.permission.READ_CALL_LOG
                ),
                101
            )
        }

        findViewById<Button>(R.id.btnPostNotifPermission).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    102
                )
            } else {
                Toast.makeText(this, "نیازی نیست", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnBattery).setOnClickListener {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "قبلا فعال شده", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "اول اجازه نمایش روی برنامه‌ها رو بده", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val intent = Intent(this, IslandOverlayService::class.java)
            intent.action = IslandOverlayService.ACTION_START
            ContextCompat.startForegroundService(this, intent)
            Toast.makeText(this, "روشن شد", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            val intent = Intent(this, IslandOverlayService::class.java)
            intent.action = IslandOverlayService.ACTION_STOP
            startService(intent)
        }

        findViewById<Button>(R.id.btnStartTimer).setOnClickListener {
            val minutesText = findViewById<EditText>(R.id.edtTimerMinutes).text.toString()
            val minutes = minutesText.toIntOrNull()
            if (minutes == null || minutes <= 0) {
                Toast.makeText(this, "یه عدد درست وارد کن", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, IslandOverlayService::class.java)
            intent.action = IslandOverlayService.ACTION_START_TIMER
            intent.putExtra(IslandOverlayService.EXTRA_MINUTES, minutes)
            ContextCompat.startForegroundService(this, intent)
        }
    }
}
