package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.RequestStatus
import com.example.model.UnlockRequest
import kotlinx.coroutines.flow.Flow

@Dao
interface UnlockRequestDao {
    @Query("SELECT * FROM unlock_requests ORDER BY timestamp DESC")
    fun getAllRequests(): Flow<List<UnlockRequest>>

    @Query("SELECT * FROM unlock_requests WHERE status = 'PENDING' ORDER BY timestamp DESC")
    fun getPendingRequests(): Flow<List<UnlockRequest>>

    @Query("SELECT * FROM unlock_requests WHERE requestId = :requestId LIMIT 1")
    suspend fun getRequestById(requestId: String): UnlockRequest?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: UnlockRequest)

    @Query("UPDATE unlock_requests SET status = :status, grantedMinutes = :grantedMinutes WHERE requestId = :requestId")
    suspend fun updateRequestStatus(requestId: String, status: RequestStatus, grantedMinutes: Int)

    @Query("DELETE FROM unlock_requests WHERE requestId = :requestId")
    suspend fun deleteRequest(requestId: String)

    @Query("DELETE FROM unlock_requests")
    suspend fun clearAll()
}
