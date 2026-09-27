# Dynamic Island Android App — Full Source

## INSTRUCTIONS FOR THE AI AGENT (read this first)

You are working inside a GitHub repository. Below is the complete source code for an Android app ("Dynamic Island"). Each file is marked with a line like:

`### FILE: path/to/file.ext`

followed by a fenced code block containing that file's exact content.

Do the following, in order:
1. For every `### FILE:` entry below, create a file at that exact path (relative to the repo root), with the exact content inside the code block that follows it. Do not modify, reformat, "improve", or add comments to the code — copy it exactly as given.
2. Do not skip any file, including the small XML icon files and the GitHub Actions workflow file.
3. Do not create any extra files, boilerplate, or README beyond what is listed here.
4. After all files are created, commit them to a new branch and open a pull request.
5. Verify that `.github/workflows/build.yml` exists at that exact path (not `.github/workflow/` or any variant) since GitHub only runs workflows from `.github/workflows/`.
6. Once the PR is merged (or the files are pushed to `main`), the GitHub Actions workflow should run automatically and produce a downloadable APK as a workflow artifact named `dynamic-island-apk`.
7. If the build fails, read the Actions log, fix only what is necessary to make the Gradle build succeed, and do not change the app's logic or UI.

Do not ask me clarifying questions about design or features — everything needed is fully specified below.

---

## PROJECT FILE TREE

```
DynamicIsland/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/zico/dynamicisland/
│       │   ├── MainActivity.kt
│       │   ├── IslandOverlayService.kt
│       │   └── IslandNotificationListenerService.kt
│       └── res/
│           ├── values/themes.xml
│           ├── layout/activity_main.xml
│           ├── layout/overlay_island.xml
│           └── drawable/ (icon files)
└── .github/workflows/build.yml
```

---

### FILE: settings.gradle.kts
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "DynamicIsland"
include(":app")
```

### FILE: build.gradle.kts
```kotlin
plugins {
    id("com.android.application") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
```

### FILE: gradle.properties
```
org.gradle.jvmargs=-Xmx2048m
android.useAndroidX=true
kotlin.code.style=official
```

### FILE: app/build.gradle.kts
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.zico.dynamicisland"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.zico.dynamicisland"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.palette:palette-ktx:1.0.0")
}
```

### FILE: app/src/main/AndroidManifest.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    <uses-permission android:name="android.permission.READ_CALL_LOG" />

    <application
        android:allowBackup="true"
        android:icon="@drawable/ic_launcher_island"
        android:label="داینامیک آیلند"
        android:supportsRtl="true"
        android:theme="@style/Theme.DynamicIsland">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".IslandOverlayService"
            android:exported="false"
            android:foregroundServiceType="mediaPlayback" />

        <service
            android:name=".IslandNotificationListenerService"
            android:exported="true"
            android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
            android:label="داینامیک آیلند">
            <intent-filter>
                <action android:name="android.service.notification.NotificationListenerService" />
            </intent-filter>
        </service>

    </application>
</manifest>
```

### FILE: app/src/main/res/values/themes.xml
```xml
<resources>
    <style name="Theme.DynamicIsland" parent="Theme.AppCompat.Light.NoActionBar" />
</resources>
```

### FILE: app/src/main/res/layout/activity_main.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#0E0E10">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="24dp">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="داینامیک آیلند"
            android:textColor="#FFFFFF"
            android:textSize="24sp"
            android:textStyle="bold"
            android:layout_marginBottom="24dp" />

        <Button
            android:id="@+id/btnOverlayPermission"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="۱. اجازه نمایش روی سایر برنامه‌ها" />

        <Button
            android:id="@+id/btnNotificationPermission"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:text="۲. دسترسی نوتیفیکیشن (برای موزیک)" />

        <Button
            android:id="@+id/btnPhonePermission"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:text="۳. دسترسی تماس" />

        <Button
            android:id="@+id/btnPostNotifPermission"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:text="۴. اجازه نمایش نوتیفیکیشن" />

        <Button
            android:id="@+id/btnBattery"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:text="۵. غیرفعال کردن بهینه‌سازی باتری" />

        <View
            android:layout_width="match_parent"
            android:layout_height="1dp"
            android:background="#333333"
            android:layout_marginTop="20dp"
            android:layout_marginBottom="20dp" />

        <Button
            android:id="@+id/btnStart"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="روشن کردن Dynamic Island"
            android:backgroundTint="#4CAF50" />

        <Button
            android:id="@+id/btnStop"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:text="خاموش کردن"
            android:backgroundTint="#F44336" />

        <View
            android:layout_width="match_parent"
            android:layout_height="1dp"
            android:background="#333333"
            android:layout_marginTop="20dp"
            android:layout_marginBottom="20dp" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="تایمر (دقیقه):"
            android:textColor="#FFFFFF"
            android:layout_marginBottom="8dp" />

        <EditText
            android:id="@+id/edtTimerMinutes"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="number"
            android:hint="مثلا 5"
            android:textColor="#FFFFFF"
            android:textColorHint="#888888" />

        <Button
            android:id="@+id/btnStartTimer"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:text="شروع تایمر" />

    </LinearLayout>
</ScrollView>
```

### FILE: app/src/main/res/layout/overlay_island.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/islandRoot"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content">

    <LinearLayout
        android:id="@+id/compactView"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingStart="14dp"
        android:paddingEnd="14dp"
        android:paddingTop="8dp"
        android:paddingBottom="8dp">

        <ImageView
            android:id="@+id/compactIcon"
            android:layout_width="22dp"
            android:layout_height="22dp"
            android:src="@drawable/ic_music_note" />

        <TextView
            android:id="@+id/compactText"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="8dp"
            android:textColor="#FFFFFF"
            android:textSize="13sp"
            android:maxLines="1"
            android:ellipsize="end"
            android:maxWidth="140dp"
            android:visibility="gone" />

    </LinearLayout>

    <LinearLayout
        android:id="@+id/expandedView"
        android:layout_width="260dp"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp"
        android:visibility="gone">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">

            <ImageView
                android:id="@+id/expandedIcon"
                android:layout_width="42dp"
                android:layout_height="42dp"
                android:src="@drawable/ic_music_note" />

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:layout_marginStart="12dp"
                android:orientation="vertical">

                <TextView
                    android:id="@+id/expandedTitle"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:textColor="#FFFFFF"
                    android:textStyle="bold"
                    android:textSize="14sp"
                    android:maxLines="1"
                    android:ellipsize="end" />

                <TextView
                    android:id="@+id/expandedSubtitle"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:textColor="#CCCCCC"
                    android:textSize="12sp"
                    android:maxLines="1"
                    android:ellipsize="end" />
            </LinearLayout>

            <ImageView
                android:id="@+id/btnClose"
                android:layout_width="20dp"
                android:layout_height="20dp"
                android:src="@drawable/ic_close" />

        </LinearLayout>

        <LinearLayout
            android:id="@+id/controlsRow"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center"
            android:layout_marginTop="14dp">

            <ImageView
                android:id="@+id/btnPrev"
                android:layout_width="30dp"
                android:layout_height="30dp"
                android:src="@drawable/ic_prev"
                android:layout_marginEnd="24dp" />

            <ImageView
                android:id="@+id/btnPlayPause"
                android:layout_width="38dp"
                android:layout_height="38dp"
                android:src="@drawable/ic_play" />

            <ImageView
                android:id="@+id/btnNext"
                android:layout_width="30dp"
                android:layout_height="30dp"
                android:src="@drawable/ic_next"
                android:layout_marginStart="24dp" />

        </LinearLayout>

        <LinearLayout
            android:id="@+id/callControlsRow"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center"
            android:layout_marginTop="14dp"
            android:visibility="gone">

            <ImageView
                android:id="@+id/btnCallAccept"
                android:layout_width="42dp"
                android:layout_height="42dp"
                android:src="@drawable/ic_call_accept"
                android:layout_marginEnd="30dp" />

            <ImageView
                android:id="@+id/btnCallEnd"
                android:layout_width="42dp"
                android:layout_height="42dp"
                android:src="@drawable/ic_call_end" />

        </LinearLayout>

    </LinearLayout>

</FrameLayout>
```

### FILE: app/src/main/res/drawable/ic_play.xml
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF" android:pathData="M8,5v14l11,-7z" />
</vector>
```

### FILE: app/src/main/res/drawable/ic_pause.xml
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF" android:pathData="M6,5h4v14h-4z" />
    <path android:fillColor="#FFFFFF" android:pathData="M14,5h4v14h-4z" />
</vector>
```

### FILE: app/src/main/res/drawable/ic_next.xml
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF" android:pathData="M6,6v12l8.5,-6z" />
    <path android:fillColor="#FFFFFF" android:pathData="M16,6h2v12h-2z" />
</vector>
```

### FILE: app/src/main/res/drawable/ic_prev.xml
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF" android:pathData="M18,6v12l-8.5,-6z" />
    <path android:fillColor="#FFFFFF" android:pathData="M6,6h2v12h-2z" />
</vector>
```

### FILE: app/src/main/res/drawable/ic_call_accept.xml
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#4CAF50" android:pathData="M6.62,10.79c1.44,2.83 3.76,5.14 6.59,6.59l2.2,-2.2c0.27,-0.27 0.67,-0.36 1.02,-0.24 1.12,0.37 2.33,0.57 3.57,0.57 0.55,0 1,0.45 1,1v3.5c0,0.55 -0.45,1 -1,1 -9.39,0 -17,-7.61 -17,-17 0,-0.55 0.45,-1 1,-1h3.5c0.55,0 1,0.45 1,1 0,1.24 0.2,2.45 0.57,3.57 0.11,0.35 0.03,0.74 -0.25,1.02z" />
</vector>
```

### FILE: app/src/main/res/drawable/ic_call_end.xml
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#F44336" android:pathData="M12,9c-1.6,0 -3.15,0.25 -4.6,0.72v3.1c0,0.39 -0.23,0.74 -0.56,0.9 -0.98,0.49 -1.87,1.12 -2.66,1.87 -0.19,0.19 -0.45,0.3 -0.72,0.3 -0.28,0 -0.53,-0.11 -0.71,-0.29L0.29,13.14c-0.18,-0.17 -0.29,-0.42 -0.29,-0.7 0,-0.28 0.11,-0.53 0.29,-0.71C3.34,8.78 7.46,7 12,7s8.66,1.78 11.71,4.73c0.18,0.18 0.29,0.43 0.29,0.71 0,0.28 -0.11,0.53 -0.29,0.7l-2.48,2.48c-0.18,0.18 -0.43,0.29 -0.71,0.29 -0.27,0 -0.53,-0.11 -0.72,-0.3 -0.79,-0.75 -1.68,-1.38 -2.66,-1.87 -0.33,-0.16 -0.56,-0.51 -0.56,-0.9v-3.1C15.15,9.25 13.6,9 12,9z" />
</vector>
```

### FILE: app/src/main/res/drawable/ic_battery.xml
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF" android:pathData="M15.67,4H14V2h-4v2H8.33C7.6,4 7,4.6 7,5.33v15.33C7,21.4 7.6,22 8.33,22h7.33C16.4,22 17,21.4 17,20.67V5.33C17,4.6 16.4,4 15.67,4zM13,18l-2,0l0,-3l-1.5,0l3,-6v4.5l1.5,0z" />
</vector>
```

### FILE: app/src/main/res/drawable/ic_timer.xml
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF" android:pathData="M15,1H9v2h6V1zM11,14h2V8h-2v6zM19.03,7.39l1.42,-1.42c-0.43,-0.51 -0.9,-0.98 -1.41,-1.41l-1.42,1.42C16.07,4.74 14.12,4 12,4c-4.97,0 -9,4.03 -9,9s4.02,9 9,9 9,-4.03 9,-9c0,-2.12 -0.74,-4.07 -1.97,-5.61zM12,20c-3.87,0 -7,-3.13 -7,-7s3.13,-7 7,-7 7,3.13 7,7 -3.13,7 -7,7z" />
</vector>
```

### FILE: app/src/main/res/drawable/ic_close.xml
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF" android:pathData="M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z" />
</vector>
```

### FILE: app/src/main/res/drawable/ic_music_note.xml
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF" android:pathData="M12,3v10.55c-0.59,-0.34 -1.27,-0.55 -2,-0.55 -2.21,0 -4,1.79 -4,4s1.79,4 4,4 4,-1.79 4,-4V7h4V3z" />
</vector>
```

### FILE: app/src/main/res/drawable/ic_launcher_island.xml
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#0E0E10" android:pathData="M0,0h108v108h-108z" />
    <path android:fillColor="#FFFFFF" android:pathData="M34,44 h40 a10,10 0 0 1 10,10 v0 a10,10 0 0 1 -10,10 h-40 a10,10 0 0 1 -10,-10 v0 a10,10 0 0 1 10,-10 z" />
</vector>
```

### FILE: app/src/main/java/com/zico/dynamicisland/MainActivity.kt
```kotlin
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
```

### FILE: app/src/main/java/com/zico/dynamicisland/IslandOverlayService.kt
```kotlin
package com.zico.dynamicisland

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.BatteryManager
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.palette.graphics.Palette

class IslandOverlayService : Service() {

    companion object {
        const val ACTION_START = "com.zico.dynamicisland.START"
        const val ACTION_STOP = "com.zico.dynamicisland.STOP"
        const val ACTION_START_TIMER = "com.zico.dynamicisland.START_TIMER"
        const val EXTRA_MINUTES = "extra_minutes"
        const val CHANNEL_ID = "island_channel"
    }

    private lateinit var windowManager: WindowManager
    private var islandView: View? = null
    private lateinit var params: WindowManager.LayoutParams

    private var isExpanded = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var autoCollapseRunnable: Runnable? = null

    private var mediaSessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null
    private var mediaCallback: MediaController.Callback? = null

    private lateinit var telephonyManager: TelephonyManager
    private var phoneStateListener: PhoneStateListener? = null

    private var batteryReceiver: BroadcastReceiver? = null
    private var lastChargePercent = -1

    private var timer: CountDownTimer? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_TIMER -> {
                startForeground(1, buildNotification())
                ensureIslandAdded()
                val minutes = intent.getIntExtra(EXTRA_MINUTES, 5)
                startTimer(minutes)
                return START_STICKY
            }
            else -> {
                startForeground(1, buildNotification())
                ensureIslandAdded()
                setupMediaListener()
                setupCallListener()
                setupBatteryListener()
            }
        }
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("داینامیک آیلند فعاله")
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Dynamic Island", NotificationManager.IMPORTANCE_MIN
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun ensureIslandAdded() {
        if (islandView != null) return

        islandView = LayoutInflater.from(this).inflate(R.layout.overlay_island, null)

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.y = 20

        windowManager.addView(islandView, params)

        setPillColor(Color.parseColor("#1C1C1E"))
        setCompactText(null)

        islandView?.findViewById<LinearLayout>(R.id.compactView)?.setOnClickListener {
            toggleExpand()
        }
        islandView?.findViewById<ImageView>(R.id.btnClose)?.setOnClickListener {
            collapse()
        }
        islandView?.findViewById<ImageView>(R.id.btnPlayPause)?.setOnClickListener {
            activeController?.let {
                val playing = it.playbackState?.state == PlaybackState.STATE_PLAYING
                if (playing) it.transportControls.pause() else it.transportControls.play()
            }
        }
        islandView?.findViewById<ImageView>(R.id.btnNext)?.setOnClickListener {
            activeController?.transportControls?.skipToNext()
        }
        islandView?.findViewById<ImageView>(R.id.btnPrev)?.setOnClickListener {
            activeController?.transportControls?.skipToPrevious()
        }
        islandView?.findViewById<ImageView>(R.id.btnCallEnd)?.setOnClickListener {
            openDialerApp()
            collapse()
        }
        islandView?.findViewById<ImageView>(R.id.btnCallAccept)?.setOnClickListener {
            openDialerApp()
        }

        islandView?.visibility = View.GONE
    }

    private fun openDialerApp() {
        try {
            val tm = getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
            val pkg = tm.defaultDialerPackage ?: "com.android.dialer"
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent != null) startActivity(intent)
        } catch (e: Exception) {
            // اگه دسترسی نبود، کاری نمی‌کنیم
        }
    }

    private fun showIsland() {
        islandView?.visibility = View.VISIBLE
    }

    private fun hideIslandCompletely() {
        islandView?.visibility = View.GONE
        isExpanded = false
    }

    private fun setPillColor(color: Int) {
        val bgCompact = GradientDrawable()
        bgCompact.cornerRadius = 999f
        bgCompact.setColor(color)
        islandView?.findViewById<LinearLayout>(R.id.compactView)?.background = bgCompact

        val bgExpanded = GradientDrawable()
        bgExpanded.cornerRadius = 40f
        bgExpanded.setColor(color)
        islandView?.findViewById<LinearLayout>(R.id.expandedView)?.background = bgExpanded
    }

    private fun setCompactText(text: String?) {
        val tv = islandView?.findViewById<TextView>(R.id.compactText)
        if (text.isNullOrEmpty()) {
            tv?.visibility = View.GONE
        } else {
            tv?.text = text
            tv?.visibility = View.VISIBLE
        }
    }

    private fun setCompactIcon(resId: Int) {
        islandView?.findViewById<ImageView>(R.id.compactIcon)?.setImageResource(resId)
    }

    private fun toggleExpand() {
        isExpanded = !isExpanded
        applyExpandState()
    }

    private fun collapse() {
        isExpanded = false
        applyExpandState()
    }

    private fun applyExpandState() {
        islandView?.findViewById<View>(R.id.compactView)?.visibility =
            if (isExpanded) View.GONE else View.VISIBLE
        islandView?.findViewById<View>(R.id.expandedView)?.visibility =
            if (isExpanded) View.VISIBLE else View.GONE
        try {
            islandView?.let { windowManager.updateViewLayout(it, params) }
        } catch (e: Exception) { }
    }

    private fun scheduleAutoCollapse(seconds: Long) {
        autoCollapseRunnable?.let { mainHandler.removeCallbacks(it) }
        autoCollapseRunnable = Runnable { hideIslandCompletely() }
        mainHandler.postDelayed(autoCollapseRunnable!!, seconds * 1000)
    }

    private fun setupMediaListener() {
        try {
            mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val componentName = ComponentName(this, IslandNotificationListenerService::class.java)

            val listener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
                bindToController(controllers?.firstOrNull())
            }
            mediaSessionManager?.addOnActiveSessionsChangedListener(listener, componentName)

            val current = mediaSessionManager?.getActiveSessions(componentName)
            bindToController(current?.firstOrNull())
        } catch (e: SecurityException) {
            // دسترسی نوتیفیکیشن هنوز فعال نشده
        }
    }

    private fun bindToController(controller: MediaController?) {
        mediaCallback?.let { activeController?.unregisterCallback(it) }

        activeController = controller
        if (controller == null) return

        mediaCallback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
                updateMusicUi(controller, metadata)
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                updatePlayPauseIcon(state)
                if (state?.state == PlaybackState.STATE_PLAYING) {
                    showIsland()
                    autoCollapseRunnable?.let { mainHandler.removeCallbacks(it) }
                } else if (state?.state == PlaybackState.STATE_PAUSED ||
                    state?.state == PlaybackState.STATE_STOPPED
                ) {
                    scheduleAutoCollapse(6)
                }
            }
        }
        controller.registerCallback(mediaCallback!!)
        updateMusicUi(controller, controller.metadata)
        updatePlayPauseIcon(controller.playbackState)
    }

    private fun updateMusicUi(controller: MediaController?, metadata: android.media.MediaMetadata?) {
        if (controller == null || metadata == null) return

        val title = metadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "در حال پخش"
        val artist = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        val art: Bitmap? = metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)

        islandView?.findViewById<TextView>(R.id.expandedTitle)?.text = title
        islandView?.findViewById<TextView>(R.id.expandedSubtitle)?.text = artist
        setCompactText(title)

        var color = Color.parseColor("#1C1C1E")
        if (art != null) {
            islandView?.findViewById<ImageView>(R.id.compactIcon)?.setImageBitmap(art)
            islandView?.findViewById<ImageView>(R.id.expandedIcon)?.setImageBitmap(art)
            try {
                val palette = Palette.from(art).generate()
                color = palette.getDarkVibrantColor(
                    palette.getVibrantColor(Color.parseColor("#1C1C1E"))
                )
            } catch (e: Exception) { }
        } else {
            setCompactIcon(R.drawable.ic_music_note)
            islandView?.findViewById<ImageView>(R.id.expandedIcon)?.setImageResource(R.drawable.ic_music_note)
        }
        setPillColor(color)

        islandView?.findViewById<LinearLayout>(R.id.controlsRow)?.visibility = View.VISIBLE
        islandView?.findViewById<LinearLayout>(R.id.callControlsRow)?.visibility = View.GONE

        showIsland()
    }

    private fun updatePlayPauseIcon(state: PlaybackState?) {
        val playing = state?.state == PlaybackState.STATE_PLAYING
        islandView?.findViewById<ImageView>(R.id.btnPlayPause)?.setImageResource(
            if (playing) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun setupCallListener() {
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        try {
            phoneStateListener = object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    when (state) {
                        TelephonyManager.CALL_STATE_RINGING -> showCallUi(phoneNumber, true)
                        TelephonyManager.CALL_STATE_OFFHOOK -> showCallUi(phoneNumber, false)
                        TelephonyManager.CALL_STATE_IDLE -> scheduleAutoCollapse(2)
                    }
                }
            }
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        } catch (e: SecurityException) {
            // دسترسی READ_PHONE_STATE هنوز داده نشده
        }
    }

    private fun showCallUi(number: String?, ringing: Boolean) {
        setCompactIcon(if (ringing) R.drawable.ic_call_accept else R.drawable.ic_call_end)
        setCompactText(if (ringing) "تماس ورودی" else "در حال مکالمه")
        islandView?.findViewById<TextView>(R.id.expandedTitle)?.text =
            if (ringing) "تماس ورودی" else "در حال مکالمه"
        islandView?.findViewById<TextView>(R.id.expandedSubtitle)?.text = number ?: ""
        islandView?.findViewById<ImageView>(R.id.expandedIcon)?.setImageResource(
            if (ringing) R.drawable.ic_call_accept else R.drawable.ic_call_end
        )
        setPillColor(Color.parseColor(if (ringing) "#F44336" else "#4CAF50"))

        islandView?.findViewById<LinearLayout>(R.id.controlsRow)?.visibility = View.GONE
        islandView?.findViewById<LinearLayout>(R.id.callControlsRow)?.visibility =
            if (ringing) View.VISIBLE else View.GONE

        autoCollapseRunnable?.let { mainHandler.removeCallbacks(it) }
        showIsland()
    }

    private fun setupBatteryListener() {
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else -1

                if (isCharging && percent >= 0 && percent != lastChargePercent) {
                    lastChargePercent = percent
                    showChargingUi(percent)
                }
            }
        }
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun showChargingUi(percent: Int) {
        setCompactIcon(R.drawable.ic_battery)
        setCompactText("$percent% در حال شارژ")
        islandView?.findViewById<TextView>(R.id.expandedTitle)?.text = "در حال شارژ"
        islandView?.findViewById<TextView>(R.id.expandedSubtitle)?.text = "$percent%"
        islandView?.findViewById<ImageView>(R.id.expandedIcon)?.setImageResource(R.drawable.ic_battery)
        setPillColor(Color.parseColor("#4CAF50"))

        islandView?.findViewById<LinearLayout>(R.id.controlsRow)?.visibility = View.GONE
        islandView?.findViewById<LinearLayout>(R.id.callControlsRow)?.visibility = View.GONE

        showIsland()
        scheduleAutoCollapse(4)
    }

    private fun startTimer(minutes: Int) {
        timer?.cancel()
        val totalMillis = minutes * 60_000L

        setCompactIcon(R.drawable.ic_timer)
        islandView?.findViewById<ImageView>(R.id.expandedIcon)?.setImageResource(R.drawable.ic_timer)
        islandView?.findViewById<TextView>(R.id.expandedTitle)?.text = "تایمر"
        setPillColor(Color.parseColor("#FF9800"))
        islandView?.findViewById<LinearLayout>(R.id.controlsRow)?.visibility = View.GONE
        islandView?.findViewById<LinearLayout>(R.id.callControlsRow)?.visibility = View.GONE
        autoCollapseRunnable?.let { mainHandler.removeCallbacks(it) }
        showIsland()

        timer = object : CountDownTimer(totalMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val m = (millisUntilFinished / 1000) / 60
                val s = (millisUntilFinished / 1000) % 60
                val text = String.format("%02d:%02d", m, s)
                setCompactText(text)
                islandView?.findViewById<TextView>(R.id.expandedSubtitle)?.text = text
            }

            override fun onFinish() {
                setCompactText("تمام شد")
                islandView?.findViewById<TextView>(R.id.expandedSubtitle)?.text = "تمام شد"
                scheduleAutoCollapse(6)
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { islandView?.let { windowManager.removeView(it) } } catch (e: Exception) { }
        try { telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE) } catch (e: Exception) { }
        try { batteryReceiver?.let { unregisterReceiver(it) } } catch (e: Exception) { }
        timer?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
```

### FILE: app/src/main/java/com/zico/dynamicisland/IslandNotificationListenerService.kt
```kotlin
package com.zico.dynamicisland

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class IslandNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // فعلا کاری با نوتیفیکیشن‌های عادی نداریم؛
        // وجود همین سرویس فقط برای گرفتن اطلاعات موزیک (MediaSession) لازمه.
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    }
}
```

### FILE: .github/workflows/build.yml
```yaml
name: Build Dynamic Island APK

on:
  push:
    branches: [ main ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Set up Android SDK
        uses: android-actions/setup-android@v3

      - name: Install SDK packages
        run: |
          yes | sdkmanager --licenses || true
          sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: 8.7

      - name: Build debug APK
        run: gradle assembleDebug --stacktrace

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: dynamic-island-apk
          path: app/build/outputs/apk/debug/app-debug.apk
```

--- END OF FILES ---
