package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.KidLockApp
import com.example.model.DeviceRole
import com.example.model.KidTheme
import com.example.model.PairedChildDevice
import com.example.model.RequestStatus
import com.example.model.TempUnlockCode
import com.example.model.ThemeRegistry
import com.example.model.UnlockRequest
import com.example.network.P2PMessage
import com.example.service.KidLockDeviceService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KidLockViewModel(application: Application) : AndroidViewModel(application) {

    val app = application as KidLockApp
    val repository = app.repository
    val securityPrefs = app.securityPrefs
    private val repo = repository
    private val prefs = securityPrefs

    // Navigation and Role state
    private val _deviceRole = MutableStateFlow(prefs.getDeviceRole())
    val deviceRole: StateFlow<DeviceRole> = _deviceRole.asStateFlow()

    val pairedDevices: StateFlow<List<PairedChildDevice>> = repo.pairedDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingRequests: StateFlow<List<UnlockRequest>> = repo.pendingRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val discoveredDevices: StateFlow<List<PairedChildDevice>> = repo.discoveryManager.discoveredDevices
    val isSearchingDevices: StateFlow<Boolean> = repo.discoveryManager.isSearching
    val isAdvertising: StateFlow<Boolean> = repo.discoveryManager.isAdvertising

    val isChildLocked: StateFlow<Boolean> = repo.isChildLocked
    val activeTheme: StateFlow<KidTheme> = repo.activeTheme
    val incomingPairRequest: StateFlow<P2PMessage.PairRequest?> = repo.incomingPairRequest
    val unlockStatusMessage: StateFlow<String?> = repo.latestUnlockStatusMessage
    val latestUnlockStatusMessage: StateFlow<String?> = repo.latestUnlockStatusMessage

    fun toggleChildLock() {
        repo.setChildLockState(!isChildLocked.value)
    }

    fun sendUnlockRequest(minutes: Int = 30) {
        requestUnlockFromParent(minutes)
    }

    // Pairing Session State
    private val _parentPairingCode = MutableStateFlow<String?>(null)
    val parentPairingCode: StateFlow<String?> = _parentPairingCode.asStateFlow()

    private val _pairingTargetDevice = MutableStateFlow<PairedChildDevice?>(null)
    val pairingTargetDevice: StateFlow<PairedChildDevice?> = _pairingTargetDevice.asStateFlow()

    private val _pairingSuccess = MutableStateFlow(false)
    val pairingSuccess: StateFlow<Boolean> = _pairingSuccess.asStateFlow()

    // Temporary Code Generation State
    private val _activeTempCode = MutableStateFlow<TempUnlockCode?>(null)
    val activeTempCode: StateFlow<TempUnlockCode?> = _activeTempCode.asStateFlow()

    private val _tempCodeRemainingSeconds = MutableStateFlow(0)
    val tempCodeRemainingSeconds: StateFlow<Int> = _tempCodeRemainingSeconds.asStateFlow()
    private var codeTimerJob: Job? = null

    // App Preferences UI State
    private val _animationsEnabled = MutableStateFlow(prefs.isAnimationsEnabled())
    val animationsEnabled: StateFlow<Boolean> = _animationsEnabled.asStateFlow()

    private val _currentLanguage = MutableStateFlow(prefs.getLanguage())
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _inactivityTimeout = MutableStateFlow(prefs.getInactivityTimeout())
    val inactivityTimeout: StateFlow<Int> = _inactivityTimeout.asStateFlow()

    private val _childName = MutableStateFlow(prefs.getDeviceName())
    val childName: StateFlow<String> = _childName.asStateFlow()

    // Idle Detection
    private var idleJob: Job? = null
    private var lastUserInteractionTime = System.currentTimeMillis()

    init {
        resetIdleTimer()
    }

    // Role Selection
    fun selectDeviceRole(role: DeviceRole) {
        prefs.setDeviceRole(role)
        _deviceRole.value = role

        if (role == DeviceRole.CHILD) {
            app.discoveryManager.startAdvertising(
                deviceId = prefs.getDeviceId(),
                deviceName = prefs.getDeviceName()
            )
            KidLockDeviceService.startService(app)
        } else {
            app.discoveryManager.stopAdvertising()
        }
    }

    fun resetDeviceRole() {
        prefs.setDeviceRole(DeviceRole.UNSET)
        _deviceRole.value = DeviceRole.UNSET
        app.discoveryManager.stopAdvertising()
        app.discoveryManager.stopDiscovery()
    }

    // Parent PIN Verification
    fun verifyPin(pin: String): Boolean {
        return prefs.verifyParentPin(pin)
    }

    fun saveParentPin(pin: String) {
        prefs.setParentPin(pin)
    }

    fun hasParentPin(): Boolean {
        return prefs.hasParentPin()
    }

    // Wi-Fi Discovery & Pairing
    fun startSearchingDevices() {
        repo.discoveryManager.startDiscovery()
    }

    fun stopSearchingDevices() {
        repo.discoveryManager.stopDiscovery()
    }

    fun startPairingWithDevice(childDevice: PairedChildDevice) {
        viewModelScope.launch {
            val code = prefs.generateSecure6DigitCode()
            _parentPairingCode.value = code
            _pairingTargetDevice.value = childDevice
            val sent = repo.initiatePairing(childDevice, code)
            if (sent) {
                _pairingSuccess.value = true
            }
        }
    }

    fun acceptPairingRequest(request: P2PMessage.PairRequest) {
        viewModelScope.launch {
            repo.acceptChildPairing(request)
        }
    }

    fun rejectPairingRequest(request: P2PMessage.PairRequest) {
        viewModelScope.launch {
            repo.rejectChildPairing(request)
        }
    }

    fun clearPairingSession() {
        _parentPairingCode.value = null
        _pairingTargetDevice.value = null
        _pairingSuccess.value = false
    }

    // Remote Unlock Operations
    fun unlockDevice(device: PairedChildDevice, minutes: Int = 30) {
        viewModelScope.launch {
            repo.unlockChildDeviceRemote(device, minutes)
        }
    }

    fun lockDevice(device: PairedChildDevice) {
        viewModelScope.launch {
            repo.lockChildDeviceRemote(device)
        }
    }

    fun generateTempCodeForDevice(device: PairedChildDevice, minutes: Int = 30) {
        viewModelScope.launch {
            codeTimerJob?.cancel()
            val code = repo.generateTemporaryUnlockCode(device.deviceId, minutes)
            _activeTempCode.value = code
            _tempCodeRemainingSeconds.value = 120

            codeTimerJob = viewModelScope.launch {
                while (_tempCodeRemainingSeconds.value > 0) {
                    delay(1000)
                    _tempCodeRemainingSeconds.value -= 1
                }
                _activeTempCode.value = null
            }
        }
    }

    fun dismissTempCode() {
        codeTimerJob?.cancel()
        _activeTempCode.value = null
        _tempCodeRemainingSeconds.value = 0
    }

    fun approveRequest(request: UnlockRequest, minutes: Int = 30) {
        viewModelScope.launch {
            repo.approveUnlockRequest(request, minutes)
        }
    }

    fun denyRequest(request: UnlockRequest) {
        viewModelScope.launch {
            repo.denyUnlockRequest(request)
        }
    }

    // Child Actions
    fun requestUnlockFromParent(minutes: Int = 30) {
        viewModelScope.launch {
            repo.sendUnlockRequestFromChild(minutes)
        }
    }

    fun submitUnlockCode(code: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repo.verifyAndApplyUnlockCode(code)
            onResult(success)
        }
    }

    fun unlockLocallyViaPin() {
        repo.setChildLockState(false)
    }

    fun lockLocally() {
        repo.setChildLockState(true)
    }

    // Settings & Personalization
    fun setAnimationsEnabled(enabled: Boolean) {
        prefs.setAnimationsEnabled(enabled)
        _animationsEnabled.value = enabled
    }

    fun setLanguage(lang: String) {
        prefs.setLanguage(lang)
        _currentLanguage.value = lang
    }

    fun setInactivityTimeout(minutes: Int) {
        prefs.setInactivityTimeout(minutes)
        _inactivityTimeout.value = minutes
        resetIdleTimer()
    }

    fun setChildName(name: String) {
        prefs.setDeviceName(name)
        _childName.value = name
    }

    fun selectTheme(themeId: String) {
        repo.setActiveTheme(themeId)
    }

    fun changeRemoteTheme(device: PairedChildDevice, themeId: String) {
        viewModelScope.launch {
            repo.changeRemoteChildTheme(device, themeId)
        }
    }

    fun unpairDevice(deviceId: String) {
        viewModelScope.launch {
            repo.unpairDevice(deviceId)
        }
    }

    fun renameDevice(deviceId: String, newName: String) {
        viewModelScope.launch {
            repo.renameDevice(deviceId, newName)
        }
    }

    // Idle Detection
    fun notifyUserInteraction() {
        lastUserInteractionTime = System.currentTimeMillis()
    }

    private fun resetIdleTimer() {
        idleJob?.cancel()
        if (prefs.getDeviceRole() == DeviceRole.CHILD) {
            idleJob = viewModelScope.launch {
                while (true) {
                    delay(30_000)
                    val elapsedMinutes = (System.currentTimeMillis() - lastUserInteractionTime) / 60_000
                    if (elapsedMinutes >= _inactivityTimeout.value && !isChildLocked.value) {
                        repo.setChildLockState(true)
                    }
                }
            }
        }
    }
}
