package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.model.DeviceRole
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

class SecurityPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("kidlock_secure_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DEVICE_ROLE = "key_device_role"
        private const val KEY_DEVICE_ID = "key_device_id"
        private const val KEY_DEVICE_NAME = "key_device_name"
        private const val KEY_PARENT_PIN_HASH = "key_parent_pin_hash"
        private const val KEY_PARENT_PIN_SALT = "key_parent_pin_salt"
        private const val KEY_ACTIVE_THEME = "key_active_theme"
        private const val KEY_LANGUAGE = "key_language"
        private const val KEY_ANIMATIONS_ENABLED = "key_animations_enabled"
        private const val KEY_SOUND_ENABLED = "key_sound_enabled"
        private const val KEY_IS_CHILD_LOCKED = "key_is_child_locked"
        private const val KEY_INACTIVITY_TIMEOUT = "key_inactivity_timeout"
        private const val KEY_KIOSK_MODE_ENABLED = "key_kiosk_mode_enabled"
        private const val KEY_PAIRED_PARENT_DEVICE_ID = "key_paired_parent_device_id"
        private const val KEY_AUTH_TOKEN = "key_auth_token"
        private const val KEY_SESSION_END_MILLIS = "key_session_end_millis"
        private const val KEY_SESSION_ACTIVE = "key_session_active"
    }

    init {
        if (getDeviceId().isEmpty()) prefs.edit().putString(KEY_DEVICE_ID, "KID-${UUID.randomUUID().toString().take(8).uppercase()}").apply()
        if (getAuthToken().isEmpty()) prefs.edit().putString(KEY_AUTH_TOKEN, UUID.randomUUID().toString()).apply()
    }

    fun getDeviceId() = prefs.getString(KEY_DEVICE_ID, "") ?: ""
    fun getAuthToken() = prefs.getString(KEY_AUTH_TOKEN, "") ?: ""
    fun getDeviceRole(): DeviceRole = runCatching { DeviceRole.valueOf(prefs.getString(KEY_DEVICE_ROLE, DeviceRole.UNSET.name)!!) }.getOrDefault(DeviceRole.UNSET)
    fun setDeviceRole(role: DeviceRole) { prefs.edit().putString(KEY_DEVICE_ROLE, role.name).apply() }
    fun getDeviceName(): String = prefs.getString(KEY_DEVICE_NAME, if (getDeviceRole() == DeviceRole.PARENT) "Parent Phone" else "Kid's Tablet") ?: "Kid's Tablet"
    fun setDeviceName(name: String) { prefs.edit().putString(KEY_DEVICE_NAME, name).apply() }
    fun getChildAgeRange() = "6-10"
    fun setChildAgeRange(@Suppress("UNUSED_PARAMETER") age: String) {}
    fun getChildAvatar() = "mascot_astronaut"
    fun setChildAvatar(@Suppress("UNUSED_PARAMETER") avatar: String) {}

    fun hasParentPin() = prefs.getString(KEY_PARENT_PIN_HASH, null) != null
    fun setParentPin(pin: String) { val salt = salt(); prefs.edit().putString(KEY_PARENT_PIN_SALT, salt).putString(KEY_PARENT_PIN_HASH, hash(pin, salt)).apply() }
    fun verifyParentPin(pin: String): Boolean { val salt = prefs.getString(KEY_PARENT_PIN_SALT, null) ?: return false; return prefs.getString(KEY_PARENT_PIN_HASH, null) == hash(pin, salt) }
    private fun salt(): String = ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
    private fun hash(value: String, salt: String): String = MessageDigest.getInstance("SHA-256").digest((value + salt).toByteArray()).joinToString("") { "%02x".format(it) }

    fun getActiveThemeId() = prefs.getString(KEY_ACTIVE_THEME, "space") ?: "space"
    fun setActiveThemeId(id: String) { prefs.edit().putString(KEY_ACTIVE_THEME, id).apply() }
    fun getLanguage() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
    fun setLanguage(value: String) { prefs.edit().putString(KEY_LANGUAGE, value).apply() }
    fun isAnimationsEnabled() = prefs.getBoolean(KEY_ANIMATIONS_ENABLED, true)
    fun setAnimationsEnabled(value: Boolean) { prefs.edit().putBoolean(KEY_ANIMATIONS_ENABLED, value).apply() }
    fun isSoundEnabled() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
    fun setSoundEnabled(value: Boolean) { prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply() }
    fun isChildLocked() = prefs.getBoolean(KEY_IS_CHILD_LOCKED, true)
    fun setChildLocked(value: Boolean) { prefs.edit().putBoolean(KEY_IS_CHILD_LOCKED, value).apply() }
    fun getInactivityTimeout() = prefs.getInt(KEY_INACTIVITY_TIMEOUT, 15)
    fun setInactivityTimeout(value: Int) { prefs.edit().putInt(KEY_INACTIVITY_TIMEOUT, value).apply() }
    fun isKioskModeEnabled() = prefs.getBoolean(KEY_KIOSK_MODE_ENABLED, false)
    fun setKioskModeEnabled(value: Boolean) { prefs.edit().putBoolean(KEY_KIOSK_MODE_ENABLED, value).apply() }
    fun getPairedParentDeviceId() = prefs.getString(KEY_PAIRED_PARENT_DEVICE_ID, null)
    fun setPairedParentDeviceId(id: String?) { prefs.edit().putString(KEY_PAIRED_PARENT_DEVICE_ID, id).apply() }

    fun getSessionEndMillis() = prefs.getLong(KEY_SESSION_END_MILLIS, 0L)
    fun setSessionEndMillis(value: Long) { prefs.edit().putLong(KEY_SESSION_END_MILLIS, value).apply() }
    fun isSessionActive() = prefs.getBoolean(KEY_SESSION_ACTIVE, false)
    fun setSessionActive(value: Boolean) { prefs.edit().putBoolean(KEY_SESSION_ACTIVE, value).apply() }
    fun clearSession() { prefs.edit().remove(KEY_SESSION_END_MILLIS).putBoolean(KEY_SESSION_ACTIVE, false).putBoolean(KEY_IS_CHILD_LOCKED, true).apply() }
    fun resetApp() { prefs.edit().clear().apply() }
    fun generateSecure6DigitCode() = (100000 + SecureRandom().nextInt(900000)).toString()
}
