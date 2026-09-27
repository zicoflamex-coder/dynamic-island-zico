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
