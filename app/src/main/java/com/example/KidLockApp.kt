package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.SecurityPreferences
import com.example.data.repository.KidLockRepository
import com.example.model.DeviceRole
import com.example.network.BackendApiClient
import com.example.network.LocalDiscoveryManager
import com.example.network.LocalP2PCommunication
import com.example.network.NotificationHelper
import com.example.service.KidLockDeviceService

class KidLockApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var securityPrefs: SecurityPreferences
        private set

    lateinit var discoveryManager: LocalDiscoveryManager
        private set

    lateinit var p2pCommunication: LocalP2PCommunication
        private set

    lateinit var backendApiClient: BackendApiClient
        private set

    lateinit var notificationHelper: NotificationHelper
        private set

    lateinit var repository: KidLockRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        securityPrefs = SecurityPreferences(this)
        discoveryManager = LocalDiscoveryManager(this)
        p2pCommunication = LocalP2PCommunication()
        backendApiClient = BackendApiClient()
        notificationHelper = NotificationHelper(this)

        repository = KidLockRepository(
            context = this,
            database = database,
            securityPrefs = securityPrefs,
            discoveryManager = discoveryManager,
            p2pCommunication = p2pCommunication,
            backendApiClient = backendApiClient,
            notificationHelper = notificationHelper
        )

        // Start P2P server socket listener
        p2pCommunication.startServer(LocalP2PCommunication.SERVER_PORT)

        // If Child device role, start NSD advertisement & background service
        if (securityPrefs.getDeviceRole() == DeviceRole.CHILD) {
            discoveryManager.startAdvertising(
                deviceId = securityPrefs.getDeviceId(),
                deviceName = securityPrefs.getDeviceName()
            )
            KidLockDeviceService.startService(this)
        }
    }

    companion object {
        lateinit var instance: KidLockApp
            private set
    }
}
