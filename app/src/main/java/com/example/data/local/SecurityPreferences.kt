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
        private const val KEY_CHILD_AGE_RANGE = "key_child_age_range"
        private const val KEY_CHILD_AVATAR = "key_child_avatar"
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
    }

    init {
        if (getDeviceId().isEmpty()) {
            val generatedId = "KID-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
            prefs.edit().putString(KEY_DEVICE_ID, generatedId).apply()
        }
        if (getAuthToken().isEmpty()) {
            val token = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
        }
    }

    fun getDeviceId(): String {
        return prefs.getString(KEY_DEVICE_ID, "") ?: ""
    }

    fun getAuthToken(): String {
        return prefs.getString(KEY_AUTH_TOKEN, "") ?: ""
    }

    fun getDeviceRole(): DeviceRole {
        val roleStr = prefs.getString(KEY_DEVICE_ROLE, DeviceRole.UNSET.name) ?: DeviceRole.UNSET.name
        return try {
            DeviceRole.valueOf(roleStr)
        } catch (e: Exception) {
            DeviceRole.UNSET
        }
    }

    fun setDeviceRole(role: DeviceRole) {
        prefs.edit().putString(KEY_DEVICE_ROLE, role.name).apply()
    }

    fun getDeviceName(): String {
        val defaultName = if (getDeviceRole() == DeviceRole.PARENT) "Parent Phone" else "Kid's Tablet"
        return prefs.getString(KEY_DEVICE_NAME, defaultName) ?: defaultName
    }

    fun setDeviceName(name: String) {
        prefs.edit().putString(KEY_DEVICE_NAME, name).apply()
    }

    fun getChildAgeRange(): String {
        return prefs.getString(KEY_CHILD_AGE_RANGE, "6-10") ?: "6-10"
    }

    fun setChildAgeRange(age: String) {
        prefs.edit().putString(KEY_CHILD_AGE_RANGE, age).apply()
    }

    fun getChildAvatar(): String {
        return prefs.getString(KEY_CHILD_AVATAR, "mascot_astronaut") ?: "mascot_astronaut"
    }

    fun setChildAvatar(avatar: String) {
        prefs.edit().putString(KEY_CHILD_AVATAR, avatar).apply()
    }

    fun hasParentPin(): Boolean {
        return prefs.getString(KEY_PARENT_PIN_HASH, null) != null
    }

    fun setParentPin(pin: String) {
        val salt = generateRandomSalt()
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString(KEY_PARENT_PIN_SALT, salt)
            .putString(KEY_PARENT_PIN_HASH, hash)
            .apply()
    }

    fun verifyParentPin(enteredPin: String): Boolean {
        val salt = prefs.getString(KEY_PARENT_PIN_SALT, null) ?: return false
        val savedHash = prefs.getString(KEY_PARENT_PIN_HASH, null) ?: return false
        val inputHash = hashPin(enteredPin, salt)
        return savedHash == inputHash
    }

    private fun generateRandomSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest((pin + salt).toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun getActiveThemeId(): String {
        return prefs.getString(KEY_ACTIVE_THEME, "space") ?: "space"
    }

    fun setActiveThemeId(themeId: String) {
        prefs.edit().putString(KEY_ACTIVE_THEME, themeId).apply()
    }

    fun getLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, "en") ?: "en"
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
    }

    fun isAnimationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_ANIMATIONS_ENABLED, true)
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ANIMATIONS_ENABLED, enabled).apply()
    }

    fun isSoundEnabled(): Boolean {
        return prefs.getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun isChildLocked(): Boolean {
        return prefs.getBoolean(KEY_IS_CHILD_LOCKED, true)
    }

    fun setChildLocked(locked: Boolean) {
        prefs.edit().putBoolean(KEY_IS_CHILD_LOCKED, locked).apply()
    }

    fun getInactivityTimeout(): Int {
        return prefs.getInt(KEY_INACTIVITY_TIMEOUT, 15)
    }

    fun setInactivityTimeout(minutes: Int) {
        prefs.edit().putInt(KEY_INACTIVITY_TIMEOUT, minutes).apply()
    }

    fun isKioskModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_KIOSK_MODE_ENABLED, false)
    }

    fun setKioskModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KIOSK_MODE_ENABLED, enabled).apply()
    }

    fun getPairedParentDeviceId(): String? {
        return prefs.getString(KEY_PAIRED_PARENT_DEVICE_ID, null)
    }

    fun setPairedParentDeviceId(id: String?) {
        prefs.edit().putString(KEY_PAIRED_PARENT_DEVICE_ID, id).apply()
    }

    fun resetApp() {
        prefs.edit().clear().apply()
    }

    /**
     * Generates a non-guessable, cryptographically secure 6-digit numeric code.
     */
    fun generateSecure6DigitCode(): String {
        val random = SecureRandom()
        val number = 100000 + random.nextInt(900000)
        return number.toString()
    }
}
