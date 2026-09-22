package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.TempUnlockCode
import kotlinx.coroutines.flow.Flow

@Dao
interface TempUnlockCodeDao {
    @Query("SELECT * FROM temp_unlock_codes ORDER BY createdAt DESC")
    fun getAllCodes(): Flow<List<TempUnlockCode>>

    @Query("SELECT * FROM temp_unlock_codes WHERE code = :code LIMIT 1")
    suspend fun getCode(code: String): TempUnlockCode?

    @Query("SELECT * FROM temp_unlock_codes WHERE childDeviceId = :deviceId AND isUsed = 0 AND expiresAt > :currentTime ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveCodeForDevice(deviceId: String, currentTime: Long = System.currentTimeMillis()): TempUnlockCode?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCode(code: TempUnlockCode)

    @Query("UPDATE temp_unlock_codes SET isUsed = 1 WHERE code = :code")
    suspend fun markCodeUsed(code: String)

    @Query("DELETE FROM temp_unlock_codes WHERE expiresAt < :currentTime OR isUsed = 1")
    suspend fun purgeExpired(currentTime: Long = System.currentTimeMillis())
}
