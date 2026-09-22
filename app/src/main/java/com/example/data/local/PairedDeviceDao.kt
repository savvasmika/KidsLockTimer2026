package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.PairedChildDevice
import kotlinx.coroutines.flow.Flow

@Dao
interface PairedDeviceDao {
    @Query("SELECT * FROM paired_devices ORDER BY lastActivityTimestamp DESC")
    fun getAllDevices(): Flow<List<PairedChildDevice>>

    @Query("SELECT * FROM paired_devices WHERE deviceId = :deviceId LIMIT 1")
    fun getDeviceById(deviceId: String): Flow<PairedChildDevice?>

    @Query("SELECT * FROM paired_devices WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getDeviceByIdDirect(deviceId: String): PairedChildDevice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: PairedChildDevice)

    @Update
    suspend fun updateDevice(device: PairedChildDevice)

    @Query("UPDATE paired_devices SET isLocked = :isLocked, lastActivityTimestamp = :timestamp WHERE deviceId = :deviceId")
    suspend fun updateLockStatus(deviceId: String, isLocked: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE paired_devices SET isConnected = :connected, connectionType = :connectionType WHERE deviceId = :deviceId")
    suspend fun updateConnectionStatus(deviceId: String, connected: Boolean, connectionType: com.example.model.ConnectionStatus)

    @Query("UPDATE paired_devices SET activeThemeId = :themeId WHERE deviceId = :deviceId")
    suspend fun updateTheme(deviceId: String, themeId: String)

    @Query("UPDATE paired_devices SET inactivityTimeoutMinutes = :timeout WHERE deviceId = :deviceId")
    suspend fun updateInactivityTimeout(deviceId: String, timeout: Int)

    @Query("UPDATE paired_devices SET name = :name WHERE deviceId = :deviceId")
    suspend fun renameDevice(deviceId: String, name: String)

    @Delete
    suspend fun deleteDevice(device: PairedChildDevice)

    @Query("DELETE FROM paired_devices WHERE deviceId = :deviceId")
    suspend fun deleteDeviceById(deviceId: String)
}
