package com.example.kiosk

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import com.example.service.KidLockAdminReceiver

class KioskManager(private val context: Context) {

    companion object {
        private const val TAG = "KidLock_Kiosk"
    }

    private val devicePolicyManager: DevicePolicyManager? =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager

    private val adminComponent = ComponentName(context, KidLockAdminReceiver::class.java)

    fun isDeviceOwner(): Boolean {
        return try {
            devicePolicyManager?.isDeviceOwnerApp(context.packageName) == true
        } catch (e: Exception) {
            Log.w(TAG, "Error checking device owner: ${e.message}")
            false
        }
    }

    fun isProfileOwner(): Boolean {
        return try {
            devicePolicyManager?.isProfileOwnerApp(context.packageName) == true
        } catch (e: Exception) {
            false
        }
    }

    fun isLockTaskPermitted(): Boolean {
        return try {
            devicePolicyManager?.isLockTaskPermitted(context.packageName) == true
        } catch (e: Exception) {
            false
        }
    }

    fun configureDeviceOwnerKiosk(): Boolean {
        if (!isDeviceOwner() || devicePolicyManager == null) return false
        return try {
            devicePolicyManager.setLockTaskPackages(adminComponent, arrayOf(context.packageName))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                devicePolicyManager.setLockTaskFeatures(
                    adminComponent,
                    DevicePolicyManager.LOCK_TASK_FEATURE_NONE
                )
            }
            Log.d(TAG, "Device Owner Kiosk configured successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure Device Owner Kiosk", e)
            false
        }
    }

    fun startKioskMode(activity: Activity) {
        try {
            configureDeviceOwnerKiosk()

            val activityManager = activity.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val lockTaskState = activityManager?.lockTaskModeState ?: ActivityManager.LOCK_TASK_MODE_NONE

            if (lockTaskState == ActivityManager.LOCK_TASK_MODE_NONE) {
                activity.startLockTask()
                Log.d(TAG, "startLockTask() called")
            }

            applyImmersiveMode(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Error entering lock task mode: ${e.message}")
        }
    }

    fun stopKioskMode(activity: Activity) {
        try {
            val activityManager = activity.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val lockTaskState = activityManager?.lockTaskModeState ?: ActivityManager.LOCK_TASK_MODE_NONE

            if (lockTaskState != ActivityManager.LOCK_TASK_MODE_NONE) {
                activity.stopLockTask()
                Log.d(TAG, "stopLockTask() called")
            }

            restoreSystemUI(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping lock task mode: ${e.message}")
        }
    }

    fun applyImmersiveMode(activity: Activity) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.setDecorFitsSystemWindows(false)
            val controller = activity.window.insetsController
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }

    fun restoreSystemUI(activity: Activity) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.setDecorFitsSystemWindows(true)
            val controller = activity.window.insetsController
            controller?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    fun getKioskStatusDescription(): String {
        val isOwner = isDeviceOwner()
        val isPermitted = isLockTaskPermitted()
        return when {
            isOwner -> "Device Owner Mode (Exit-Proof Kiosk Active)"
            isPermitted -> "Lock Task Whitelisted"
            else -> "Standard App Pinning Mode (User Confirmation Required)"
        }
    }
}








































