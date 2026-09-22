package com.example.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.example.model.ConnectionStatus
import com.example.model.PairedChildDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.InetAddress

class LocalDiscoveryManager(private val context: Context) {

    companion object {
        private const val TAG = "KidLock_NSD"
        const val SERVICE_TYPE = "_kidlock._tcp."
        const val DEFAULT_PORT = 8899
    }

    private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager: WifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null

    private val _discoveredDevices = MutableStateFlow<List<PairedChildDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<PairedChildDevice>> = _discoveredDevices.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private val scope = CoroutineScope(Dispatchers.IO)

    fun startAdvertising(deviceId: String, deviceName: String, port: Int = DEFAULT_PORT) {
        if (_isAdvertising.value) return

        try {
            acquireMulticastLock()

            val serviceInfo = NsdServiceInfo().apply {
                // Name format: KidLock-DeviceName-DeviceId
                serviceName = "KidLock-${deviceName.replace(" ", "_")}-$deviceId"
                serviceType = SERVICE_TYPE
                setPort(port)
            }

            registrationListener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(service: NsdServiceInfo) {
                    Log.d(TAG, "NSD Service registered: ${service.serviceName}")
                    _isAdvertising.value = true
                }

                override fun onRegistrationFailed(service: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "NSD Registration failed: $errorCode")
                    _isAdvertising.value = false
                }

                override fun onServiceUnregistered(service: NsdServiceInfo) {
                    Log.d(TAG, "NSD Service unregistered: ${service.serviceName}")
                    _isAdvertising.value = false
                }

                override fun onUnregistrationFailed(service: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "NSD Unregistration failed: $errorCode")
                }
            }

            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting advertising", e)
        }
    }

    fun stopAdvertising() {
        try {
            registrationListener?.let {
                nsdManager.unregisterService(it)
                registrationListener = null
            }
            _isAdvertising.value = false
            releaseMulticastLock()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping advertising", e)
        }
    }

    fun startDiscovery() {
        if (_isSearching.value) return

        try {
            acquireMulticastLock()
            _discoveredDevices.value = emptyList()
            _isSearching.value = true

            discoveryListener = object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    Log.e(TAG, "NSD Discovery start failed: $errorCode")
                    _isSearching.value = false
                }

                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    Log.e(TAG, "NSD Discovery stop failed: $errorCode")
                }

                override fun onDiscoveryStarted(serviceType: String) {
                    Log.d(TAG, "NSD Discovery started for $serviceType")
                    _isSearching.value = true
                }

                override fun onDiscoveryStopped(serviceType: String) {
                    Log.d(TAG, "NSD Discovery stopped")
                    _isSearching.value = false
                }

                override fun onServiceFound(service: NsdServiceInfo) {
                    Log.d(TAG, "NSD Service found: ${service.serviceName}")
                    if (service.serviceType.contains("kidlock", ignoreCase = true) ||
                        service.serviceName.startsWith("KidLock-")
                    ) {
                        resolveService(service)
                    }
                }

                override fun onServiceLost(service: NsdServiceInfo) {
                    Log.d(TAG, "NSD Service lost: ${service.serviceName}")
                    scope.launch {
                        _discoveredDevices.value = _discoveredDevices.value.filterNot {
                            service.serviceName.contains(it.deviceId)
                        }
                    }
                }
            }

            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting discovery", e)
            _isSearching.value = false
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(service: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolve failed for ${service.serviceName}: $errorCode")
            }

            override fun onServiceResolved(service: NsdServiceInfo) {
                val host: InetAddress? = service.host
                val port: Int = service.port
                val ip = host?.hostAddress ?: "127.0.0.1"
                val rawName = service.serviceName

                Log.d(TAG, "Resolved service: $rawName at $ip:$port")

                // Parse KidLock-Name-Id
                val parts = rawName.split("-")
                val cleanName = if (parts.size >= 2) parts[1].replace("_", " ") else rawName
                val parsedDeviceId = if (parts.size >= 3) parts[2] else rawName

                val device = PairedChildDevice(
                    deviceId = parsedDeviceId,
                    name = cleanName,
                    deviceType = "Android Tablet",
                    ipAddress = ip,
                    port = port,
                    isConnected = true,
                    connectionType = ConnectionStatus.SAME_WIFI,
                    isLocked = true
                )

                scope.launch {
                    val current = _discoveredDevices.value.toMutableList()
                    val index = current.indexOfFirst { it.deviceId == device.deviceId }
                    if (index >= 0) {
                        current[index] = device
                    } else {
                        current.add(device)
                    }
                    _discoveredDevices.value = current
                }
            }
        }

        try {
            nsdManager.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling resolveService", e)
        }
    }

    fun stopDiscovery() {
        try {
            discoveryListener?.let {
                nsdManager.stopServiceDiscovery(it)
                discoveryListener = null
            }
            _isSearching.value = false
            releaseMulticastLock()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping discovery", e)
        }
    }

    private fun acquireMulticastLock() {
        try {
            if (multicastLock == null) {
                multicastLock = wifiManager.createMulticastLock("kidlock_multicast_lock").apply {
                    setReferenceCounted(true)
                    acquire()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire multicast lock: ${e.message}")
        }
    }

    private fun releaseMulticastLock() {
        try {
            multicastLock?.let {
                if (it.isHeld) it.release()
            }
            multicastLock = null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release multicast lock: ${e.message}")
        }
    }
}
