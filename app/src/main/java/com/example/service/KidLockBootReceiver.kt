package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.MainActivity
import com.example.data.local.SecurityPreferences
import com.example.model.DeviceRole

/**
 * Handles device boot completion. If the device is configured as a CHILD role
 * and was locked prior to reboot, it automatically restarts the Lock Screen
 * and background service to prevent bypass via rebooting.
 */
class KidLockBootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "KidLock_Boot"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "Device boot completed detected")
            val securityPrefs = SecurityPreferences(context)

            if (securityPrefs.getDeviceRole() == DeviceRole.CHILD) {
                // Ensure service is running
                KidLockDeviceService.startService(context)

                // If locked, launch MainActivity to present lock screen immediately
                if (securityPrefs.isChildLocked()) {
                    Log.d(TAG, "Child device is LOCKED. Launching KidLock screen.")
                    val lockIntent = Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        putExtra("EXTRA_BOOT_LOCKED", true)
                    }
                    context.startActivity(lockIntent)
                }
            }
        }
    }
}
