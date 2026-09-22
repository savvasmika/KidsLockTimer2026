package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.SecurityPreferences
import com.example.model.ConnectionStatus
import com.example.model.DeviceLockStatus
import com.example.model.DeviceRole
import com.example.model.KidTheme
import com.example.model.PairedChildDevice
import com.example.model.RequestStatus
import com.example.model.TempUnlockCode
import com.example.model.ThemeRegistry
import com.example.model.UnlockRequest
import com.example.network.BackendApiClient
import com.example.network.LocalDiscoveryManager
import com.example.network.LocalP2PCommunication
import com.example.network.NotificationHelper
import com.example.network.P2PMessage
import com.example.security.BruteForceProtector
import com.example.security.CryptoManager
import com.example.security.NonceReplayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class KidLockRepository(
    private val context: Context,
    private val database: AppDatabase,
    val securityPrefs: SecurityPreferences,
    val discoveryManager: LocalDiscoveryManager,
    val p2pCommunication: LocalP2PCommunication,
    val backendApiClient: BackendApiClient,
    val notificationHelper: NotificationHelper,
    val cryptoManager: CryptoManager = CryptoManager(),
    val nonceReplayManager: NonceReplayManager = NonceReplayManager(),
    val bruteForceProtector: BruteForceProtector = BruteForceProtector()
) {
    companion object {
        private const val TAG = "KidLockRepo"
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    val pairedDevices: Flow<List<PairedChildDevice>> = database.pairedDeviceDao().getAllDevices()
    val pendingRequests: Flow<List<UnlockRequest>> = database.unlockRequestDao().getPendingRequests()
    val allRequests: Flow<List<UnlockRequest>> = database.unlockRequestDao().getAllRequests()

    private val _isChildLocked = MutableStateFlow(securityPrefs.isChildLocked())
    val isChildLocked: StateFlow<Boolean> = _isChildLocked.asStateFlow()

    private val _activeTheme = MutableStateFlow(ThemeRegistry.getThemeById(securityPrefs.getActiveThemeId()))
    val activeTheme: StateFlow<KidTheme> = _activeTheme.asStateFlow()

    private val _incomingPairRequest = MutableStateFlow<P2PMessage.PairRequest?>(null)
    val incomingPairRequest: StateFlow<P2PMessage.PairRequest?> = _incomingPairRequest.asStateFlow()

    private val _latestUnlockStatusMessage = MutableStateFlow<String?>(null)
    val latestUnlockStatusMessage: StateFlow<String?> = _latestUnlockStatusMessage.asStateFlow()

    init {
        // Start listening to P2P incoming messages
        scope.launch {
            p2pCommunication.incomingMessages.collect { msg ->
                handleIncomingP2PMessage(msg)
            }
        }

        // Start listening to Cloud remote commands
        scope.launch {
            backendApiClient.cloudUnlockCommands.collect { (deviceId, isLocked) ->
                if (deviceId == securityPrefs.getDeviceId()) {
                    setChildLockState(isLocked)
                    _latestUnlockStatusMessage.value = if (!isLocked) "Parent unlocked via Cloud!" else "Locked by parent"
                }
            }
        }
    }

    private suspend fun handleIncomingP2PMessage(msg: P2PMessage) {
        when (msg) {
            is P2PMessage.PairRequest -> {
                Log.d(TAG, "Pair request received from ${msg.parentName} with code ${msg.confirmationCode}")
                _incomingPairRequest.value = msg
                notificationHelper.showPairingRequestNotification(msg.parentName)
            }

            is P2PMessage.PairResponse -> {
                Log.d(TAG, "Pair response: accepted=${msg.accepted} from ${msg.childName}")
                if (msg.accepted) {
                    val newDevice = PairedChildDevice(
                        deviceId = msg.childDeviceId,
                        name = msg.childName,
                        authToken = msg.authToken,
                        publicKey = msg.childPublicKey,
                        isConnected = true,
                        connectionType = ConnectionStatus.SAME_WIFI
                    )
                    database.pairedDeviceDao().insertDevice(newDevice)
                }
            }

            is P2PMessage.UnlockRequestMsg -> {
                if (msg.nonce.isNotEmpty() && !nonceReplayManager.validateAndRecordNonce(msg.nonce, msg.timestamp)) {
                    Log.w(TAG, "Rejected replay or expired unlock request msg")
                    return
                }
                Log.d(TAG, "Unlock request received for ${msg.childName}")
                val req = UnlockRequest(
                    requestId = msg.requestId,
                    childDeviceId = msg.childDeviceId,
                    childName = msg.childName,
                    requestedMinutes = msg.requestedMinutes,
                    status = RequestStatus.PENDING
                )
                database.unlockRequestDao().insertRequest(req)
                notificationHelper.showUnlockRequestNotification(msg.childName, msg.requestId)
            }

            is P2PMessage.UnlockCommandMsg -> {
                if (msg.nonce.isNotEmpty() && !nonceReplayManager.validateAndRecordNonce(msg.nonce, msg.timestamp)) {
                    Log.w(TAG, "Rejected replay or expired unlock command msg")
                    return
                }
                if (msg.targetDeviceId == securityPrefs.getDeviceId()) {
                    setChildLockState(msg.isLocked)
                    _latestUnlockStatusMessage.value = if (!msg.isLocked) "Tablet Unlocked by Parent!" else "Tablet Locked"
                }
            }

            is P2PMessage.ThemeChangeMsg -> {
                if (msg.targetDeviceId == securityPrefs.getDeviceId()) {
                    setActiveTheme(msg.themeId)
                }
            }

            is P2PMessage.HeartbeatMsg -> {
                database.pairedDeviceDao().updateLockStatus(msg.childDeviceId, msg.isLocked)
            }
        }
    }

    fun setChildLockState(locked: Boolean) {
        securityPrefs.setChildLocked(locked)
        _isChildLocked.value = locked
    }

    fun setActiveTheme(themeId: String) {
        securityPrefs.setActiveThemeId(themeId)
        _activeTheme.value = ThemeRegistry.getThemeById(themeId)
    }

    fun clearPairRequest() {
        _incomingPairRequest.value = null
    }

    // --- PARENT ACTIONS ---

    suspend fun initiatePairing(targetDevice: PairedChildDevice, code: String): Boolean {
        val nonce = cryptoManager.generateSecureNonce()
        val timestamp = System.currentTimeMillis()
        val myPubKey = cryptoManager.getPublicKeyBase64()
        val signature = cryptoManager.signData("${securityPrefs.getDeviceId()}:$code:$timestamp:$nonce")

        val payload = JSONObject().apply {
            put("type", "PAIR_REQUEST")
            put("parentDeviceId", securityPrefs.getDeviceId())
            put("parentName", securityPrefs.getDeviceName())
            put("code", code)
            put("publicKey", myPubKey)
            put("nonce", nonce)
            put("timestamp", timestamp)
            put("signature", signature)
            put("returnPort", LocalP2PCommunication.SERVER_PORT)
        }

        // Try local Wi-Fi first
        val sent = p2pCommunication.sendMessage(targetDevice.ipAddress, targetDevice.port, payload)
        if (sent) {
            database.pairedDeviceDao().insertDevice(targetDevice)
        }
        return sent
    }

    suspend fun acceptChildPairing(request: P2PMessage.PairRequest) {
        val token = securityPrefs.getAuthToken()
        val nonce = cryptoManager.generateSecureNonce()
        val timestamp = System.currentTimeMillis()
        val myPubKey = cryptoManager.getPublicKeyBase64()
        val signature = cryptoManager.signData("${securityPrefs.getDeviceId()}:${request.confirmationCode}:$timestamp:$nonce")

        val payload = JSONObject().apply {
            put("type", "PAIR_RESPONSE")
            put("childDeviceId", securityPrefs.getDeviceId())
            put("childName", securityPrefs.getDeviceName())
            put("code", request.confirmationCode)
            put("publicKey", myPubKey)
            put("authToken", token)
            put("nonce", nonce)
            put("timestamp", timestamp)
            put("signature", signature)
            put("accepted", true)
        }

        p2pCommunication.sendMessage(request.returnIp, request.returnPort, payload)
        securityPrefs.setPairedParentDeviceId(request.parentDeviceId)
        _incomingPairRequest.value = null
    }

    suspend fun rejectChildPairing(request: P2PMessage.PairRequest) {
        val payload = JSONObject().apply {
            put("type", "PAIR_RESPONSE")
            put("childDeviceId", securityPrefs.getDeviceId())
            put("childName", securityPrefs.getDeviceName())
            put("code", request.confirmationCode)
            put("authToken", "")
            put("accepted", false)
        }

        p2pCommunication.sendMessage(request.returnIp, request.returnPort, payload)
        _incomingPairRequest.value = null
    }

    suspend fun unlockChildDeviceRemote(childDevice: PairedChildDevice, grantMinutes: Int = 30) {
        database.pairedDeviceDao().updateLockStatus(childDevice.deviceId, false)

        val nonce = cryptoManager.generateSecureNonce()
        val timestamp = System.currentTimeMillis()
        val signature = cryptoManager.signData("${childDevice.deviceId}:UNLOCK:$grantMinutes:$timestamp:$nonce")

        val payload = JSONObject().apply {
            put("type", "UNLOCK_COMMAND")
            put("targetDeviceId", childDevice.deviceId)
            put("authToken", childDevice.authToken)
            put("isLocked", false)
            put("grantedMinutes", grantMinutes)
            put("nonce", nonce)
            put("timestamp", timestamp)
            put("signature", signature)
        }

        // 1. Try local socket
        val localSuccess = p2pCommunication.sendMessage(childDevice.ipAddress, childDevice.port, payload)
        if (!localSuccess) {
            // 2. Fallback to Cloud relay
            backendApiClient.sendRemoteUnlockCommand(childDevice.deviceId, childDevice.authToken, false, grantMinutes)
        }
    }

    suspend fun lockChildDeviceRemote(childDevice: PairedChildDevice) {
        database.pairedDeviceDao().updateLockStatus(childDevice.deviceId, true)

        val nonce = cryptoManager.generateSecureNonce()
        val timestamp = System.currentTimeMillis()
        val signature = cryptoManager.signData("${childDevice.deviceId}:LOCK:$timestamp:$nonce")

        val payload = JSONObject().apply {
            put("type", "UNLOCK_COMMAND")
            put("targetDeviceId", childDevice.deviceId)
            put("authToken", childDevice.authToken)
            put("isLocked", true)
            put("grantedMinutes", 0)
            put("nonce", nonce)
            put("timestamp", timestamp)
            put("signature", signature)
        }

        val localSuccess = p2pCommunication.sendMessage(childDevice.ipAddress, childDevice.port, payload)
        if (!localSuccess) {
            backendApiClient.sendRemoteUnlockCommand(childDevice.deviceId, childDevice.authToken, true, 0)
        }
    }

    suspend fun generateTemporaryUnlockCode(childDeviceId: String, grantMinutes: Int = 30): TempUnlockCode {
        val codeStr = securityPrefs.generateSecure6DigitCode()
        val tempCode = TempUnlockCode(
            code = codeStr,
            childDeviceId = childDeviceId,
            grantedMinutes = grantMinutes,
            expiresAt = System.currentTimeMillis() + 120_000, // 2 mins
            isUsed = false
        )
        database.tempUnlockCodeDao().insertCode(tempCode)
        return tempCode
    }

    suspend fun verifyAndApplyUnlockCode(enteredCode: String): Boolean {
        if (!bruteForceProtector.canAttempt()) {
            return false
        }

        val codeObj = database.tempUnlockCodeDao().getCode(enteredCode)
        val now = System.currentTimeMillis()

        if (codeObj == null || codeObj.isUsed || codeObj.expiresAt < now) {
            bruteForceProtector.recordFailedAttempt()
            return false
        }

        bruteForceProtector.recordSuccess()
        database.tempUnlockCodeDao().markCodeUsed(enteredCode)
        setChildLockState(false)
        _latestUnlockStatusMessage.value = "Code accepted! Tablet unlocked for ${codeObj.grantedMinutes} min."
        return true
    }

    suspend fun approveUnlockRequest(request: UnlockRequest, grantMinutes: Int = 30) {
        database.unlockRequestDao().updateRequestStatus(request.requestId, RequestStatus.APPROVED, grantMinutes)
        val device = database.pairedDeviceDao().getDeviceByIdDirect(request.childDeviceId)
        if (device != null) {
            unlockChildDeviceRemote(device, grantMinutes)
        } else {
            // Direct unlock broadcast
            val dummyDevice = PairedChildDevice(
                deviceId = request.childDeviceId,
                name = request.childName,
                ipAddress = "127.0.0.1",
                port = LocalP2PCommunication.SERVER_PORT
            )
            unlockChildDeviceRemote(dummyDevice, grantMinutes)
        }
    }

    suspend fun denyUnlockRequest(request: UnlockRequest) {
        database.unlockRequestDao().updateRequestStatus(request.requestId, RequestStatus.DENIED, 0)
    }

    suspend fun sendUnlockRequestFromChild(minutes: Int = 30): Boolean {
        val parentId = securityPrefs.getPairedParentDeviceId()
        val nonce = cryptoManager.generateSecureNonce()
        val timestamp = System.currentTimeMillis()
        val reqId = "REQ-" + System.currentTimeMillis().toString().takeLast(6)
        val signature = cryptoManager.signData("$reqId:${securityPrefs.getDeviceId()}:$minutes:$timestamp:$nonce")

        val payload = JSONObject().apply {
            put("type", "UNLOCK_REQUEST")
            put("requestId", reqId)
            put("childDeviceId", securityPrefs.getDeviceId())
            put("childName", securityPrefs.getDeviceName())
            put("minutes", minutes)
            put("nonce", nonce)
            put("timestamp", timestamp)
            put("signature", signature)
        }

        // Also submit to cloud backend
        backendApiClient.submitRemoteUnlockRequest(securityPrefs.getDeviceId(), securityPrefs.getDeviceName(), minutes)

        // Also post local request in case this is a test or same device
        val localReq = UnlockRequest(
            requestId = reqId,
            childDeviceId = securityPrefs.getDeviceId(),
            childName = securityPrefs.getDeviceName(),
            requestedMinutes = minutes,
            status = RequestStatus.PENDING
        )
        database.unlockRequestDao().insertRequest(localReq)
        notificationHelper.showUnlockRequestNotification(securityPrefs.getDeviceName(), localReq.requestId)
        return true
    }

    suspend fun changeRemoteChildTheme(childDevice: PairedChildDevice, themeId: String) {
        database.pairedDeviceDao().updateTheme(childDevice.deviceId, themeId)
        val payload = JSONObject().apply {
            put("type", "THEME_CHANGE")
            put("targetDeviceId", childDevice.deviceId)
            put("authToken", childDevice.authToken)
            put("themeId", themeId)
        }
        p2pCommunication.sendMessage(childDevice.ipAddress, childDevice.port, payload)
    }

    suspend fun unpairDevice(deviceId: String) {
        database.pairedDeviceDao().deleteDeviceById(deviceId)
    }

    suspend fun renameDevice(deviceId: String, newName: String) {
        database.pairedDeviceDao().renameDevice(deviceId, newName)
    }

    suspend fun updateInactivityTimeout(deviceId: String, timeoutMinutes: Int) {
        database.pairedDeviceDao().updateInactivityTimeout(deviceId, timeoutMinutes)
    }
}
