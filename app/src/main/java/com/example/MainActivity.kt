package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.SecurityPreferences
import com.example.network.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class KidLockDeviceService : Service() {

    private lateinit var prefs: SecurityPreferences
    private var monitorJob: Job? = null

    companion object {
        private const val TAG = "KidLockService"
        private const val NOTIF_SERVICE_ID = 9901

        fun startService(context: Context) {
            val intent = Intent(context, KidLockDeviceService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, KidLockDeviceService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = SecurityPreferences(this)
        Log.d(TAG, "KidLock background service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createServiceNotification()
        startForeground(NOTIF_SERVICE_ID, notification)

        monitorJob?.cancel()
        monitorJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                delay(1000L)
                if (prefs.isSessionActive() && prefs.getSessionEndMillis() > 0L) {
                    if (System.currentTimeMillis() >= prefs.getSessionEndMillis()) {
                        forceLockScreen()
                        return@launch
                    }
                }
            }
        }

        return START_STICKY
    }

    private fun forceLockScreen() {
        prefs.setSessionActive(false)
        prefs.setChildLocked(true)

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("EXTRA_FORCE_LOCK", true)
        }
        startActivity(intent)
        Log.d(TAG, "Timer expired: forcing KidLock screen to foreground")
    }

    private fun createServiceNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_CHILD_STATUS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.break_time_title))
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        monitorJob?.cancel()
        Log.d(TAG, "KidLock background service stopped")
    }
}
































































































