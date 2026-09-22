package com.example.network

import android.util.Log
import com.example.model.ConnectionStatus
import com.example.model.PairedChildDevice
import com.example.model.RequestStatus
import com.example.model.UnlockRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Backend architecture client handling cloud registration, remote unlock commands,
 * and push notification simulation when outside local Wi-Fi.
 */
class BackendApiClient {

    companion object {
        private const val TAG = "KidLock_Backend"
        const val CLOUD_SERVER_URL = "https://api.kidlock.secure-relay.com/v1"
    }

    // In-memory cloud channel relay simulating remote server-side message bus
    private val cloudRegistry = ConcurrentHashMap<String, PairedChildDevice>()
    private val _cloudUnlockCommands = MutableSharedFlow<Pair<String, Boolean>>(extraBufferCapacity = 32)
    val cloudUnlockCommands: SharedFlow<Pair<String, Boolean>> = _cloudUnlockCommands.asSharedFlow()

    suspend fun registerChildDevice(device: PairedChildDevice): Boolean = withContext(Dispatchers.IO) {
        try {
            cloudRegistry[device.deviceId] = device
            Log.d(TAG, "Registered device on Cloud Backend: ${device.deviceId} (${device.name})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Cloud registration failed", e)
            false
        }
    }

    suspend fun sendRemoteUnlockCommand(
        childDeviceId: String,
        parentAuthToken: String,
        isLocked: Boolean,
        grantedMinutes: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Dispatching Cloud HTTPS Remote Unlock Command to device: $childDeviceId, isLocked=$isLocked")
            _cloudUnlockCommands.emit(Pair(childDeviceId, isLocked))
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Cloud command error", e)
            return@withContext false
        }
    }

    suspend fun submitRemoteUnlockRequest(
        childDeviceId: String,
        childName: String,
        requestedMinutes: Int
    ): UnlockRequest = withContext(Dispatchers.IO) {
        val req = UnlockRequest(
            childDeviceId = childDeviceId,
            childName = childName,
            requestedMinutes = requestedMinutes,
            status = RequestStatus.PENDING
        )
        Log.d(TAG, "Unlock request submitted to Cloud Backend: ${req.requestId}")
        req
    }

    fun getDeviceCloudStatus(deviceId: String): ConnectionStatus {
        return if (cloudRegistry.containsKey(deviceId)) {
            ConnectionStatus.REMOTE_CLOUD
        } else {
            ConnectionStatus.OFFLINE
        }
    }
}
