package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class DeviceRole {
    UNSET,
    PARENT,
    CHILD
}

enum class DeviceLockStatus {
    LOCKED,
    UNLOCKED
}

enum class ConnectionStatus {
    SAME_WIFI,
    REMOTE_CLOUD,
    CONNECTING,
    OFFLINE
}

enum class RequestStatus {
    PENDING,
    APPROVED,
    DENIED,
    EXPIRED
}

@Entity(tableName = "paired_devices")
data class PairedChildDevice(
    @PrimaryKey val deviceId: String = UUID.randomUUID().toString(),
    val name: String,
    val deviceType: String = "Android Tablet",
    val ipAddress: String = "",
    val port: Int = 8899,
    val isConnected: Boolean = true,
    val connectionType: ConnectionStatus = ConnectionStatus.SAME_WIFI,
    val batteryPercent: Int = 85,
    val isLocked: Boolean = true,
    val lastActivityTimestamp: Long = System.currentTimeMillis(),
    val activeThemeId: String = "space",
    val languageCode: String = "en",
    val authToken: String = UUID.randomUUID().toString(),
    val publicKey: String = "",
    val inactivityTimeoutMinutes: Int = 15,
    val dailyLimitMinutes: Int = 120,
    val usedMinutesToday: Int = 45,
    val isKioskActive: Boolean = false
)

@Entity(tableName = "unlock_requests")
data class UnlockRequest(
    @PrimaryKey val requestId: String = UUID.randomUUID().toString(),
    val childDeviceId: String,
    val childName: String,
    val requestedMinutes: Int = 30,
    val timestamp: Long = System.currentTimeMillis(),
    val status: RequestStatus = RequestStatus.PENDING,
    val grantedMinutes: Int = 0
)

@Entity(tableName = "temp_unlock_codes")
data class TempUnlockCode(
    @PrimaryKey val code: String,
    val childDeviceId: String,
    val grantedMinutes: Int = 30,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 120_000, // 2 minutes standard
    val isUsed: Boolean = false
)
