package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.SecurityPreferences
import com.example.data.repository.KidLockRepository
import com.example.model.DeviceRole
import com.example.model.PairedChildDevice
import com.example.model.TempUnlockCode
import com.example.model.ThemeRegistry
import com.example.network.BackendApiClient
import com.example.network.LocalDiscoveryManager
import com.example.network.LocalP2PCommunication
import com.example.network.NotificationHelper
import com.example.security.BruteForceProtector
import com.example.security.CryptoManager
import com.example.security.NonceReplayManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var securityPrefs: SecurityPreferences
    private lateinit var database: AppDatabase
    private lateinit var cryptoManager: CryptoManager
    private lateinit var nonceReplayManager: NonceReplayManager
    private lateinit var bruteForceProtector: BruteForceProtector
    private lateinit var repository: KidLockRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        securityPrefs = SecurityPreferences(context)
        database = AppDatabase.getDatabase(context)
        cryptoManager = CryptoManager()
        nonceReplayManager = NonceReplayManager()
        bruteForceProtector = BruteForceProtector()

        repository = KidLockRepository(
            context = context,
            database = database,
            securityPrefs = securityPrefs,
            discoveryManager = LocalDiscoveryManager(context),
            p2pCommunication = LocalP2PCommunication(),
            backendApiClient = BackendApiClient(),
            notificationHelper = NotificationHelper(context),
            cryptoManager = cryptoManager,
            nonceReplayManager = nonceReplayManager,
            bruteForceProtector = bruteForceProtector
        )
    }

    @Test
    fun testAppNameResource() {
        val appName = context.getString(R.string.app_name)
        assertEquals("KidLock", appName)
    }

    @Test
    fun testParentPinHashingAndVerification() {
        val pin = "1234"
        securityPrefs.setParentPin(pin)

        assertTrue(securityPrefs.hasParentPin())
        assertTrue(securityPrefs.verifyParentPin("1234"))
        assertFalse(securityPrefs.verifyParentPin("0000"))
        assertFalse(securityPrefs.verifyParentPin("1235"))
    }

    @Test
    fun testSecure6DigitCodeGeneration() {
        val code = securityPrefs.generateSecure6DigitCode()
        assertEquals(6, code.length)
        assertTrue(code.all { it.isDigit() })
    }

    @Test
    fun testThemeRegistryCompleteness() {
        assertEquals(12, ThemeRegistry.themes.size)
        val spaceTheme = ThemeRegistry.getThemeById("space")
        assertNotNull(spaceTheme)
        assertEquals("space", spaceTheme.id)
    }

    @Test
    fun testRoomDatabaseDeviceInsertion() = runBlocking {
        val testDevice = PairedChildDevice(
            deviceId = "TEST-DEV-123",
            name = "Alex Tablet"
        )
        database.pairedDeviceDao().insertDevice(testDevice)

        val retrieved = database.pairedDeviceDao().getDeviceByIdDirect("TEST-DEV-123")
        assertNotNull(retrieved)
        assertEquals("Alex Tablet", retrieved?.name)
    }

    @Test
    fun testTemporaryUnlockCodeDao() = runBlocking {
        val code = TempUnlockCode(
            code = "998877",
            childDeviceId = "TEST-DEV-123",
            expiresAt = System.currentTimeMillis() + 60_000
        )
        database.tempUnlockCodeDao().insertCode(code)

        val retrieved = database.tempUnlockCodeDao().getCode("998877")
        assertNotNull(retrieved)
        assertFalse(retrieved!!.isUsed)

        database.tempUnlockCodeDao().markCodeUsed("998877")
        val updated = database.tempUnlockCodeDao().getCode("998877")
        assertTrue(updated!!.isUsed)
    }

    @Test
    fun testCryptoManagerSignatureAndVerification() {
        val payload = "TEST_DEVICE_123:UNLOCK:30:1700000000"
        val signature = cryptoManager.signData(payload)
        assertNotNull(signature)
        assertTrue(signature.isNotEmpty())

        val pubKey = cryptoManager.getPublicKeyBase64()
        assertTrue(pubKey.isNotEmpty())

        val verified = cryptoManager.verifyData(payload, signature, pubKey)
        assertTrue(verified)
    }

    @Test
    fun testNonceReplayProtection() {
        val nonce = "NONCE_TEST_" + System.currentTimeMillis()
        val now = System.currentTimeMillis()

        // 1. Fresh nonce should succeed
        val firstAttempt = nonceReplayManager.validateAndRecordNonce(nonce, now)
        assertTrue(firstAttempt)

        // 2. Replay of same nonce should be rejected
        val replayAttempt = nonceReplayManager.validateAndRecordNonce(nonce, now)
        assertFalse(replayAttempt)

        // 3. Stale timestamp should be rejected
        val staleTimestamp = now - 600_000 // 10 minutes old (window is 5 mins)
        val staleAttempt = nonceReplayManager.validateAndRecordNonce("NONCE_STALE_123", staleTimestamp)
        assertFalse(staleAttempt)
    }

    @Test
    fun testBruteForceProtectorThrottling() {
        val protector = BruteForceProtector(maxAttempts = 5, lockoutDurationSeconds = 30)
        assertTrue(protector.canAttempt())

        repeat(4) {
            protector.recordFailedAttempt()
            assertTrue(protector.canAttempt())
        }

        // 5th failed attempt triggers lockout
        protector.recordFailedAttempt()
        assertFalse(protector.canAttempt())
        assertTrue(protector.isLockedOut.value)

        // Reset clears lockout
        protector.reset()
        assertTrue(protector.canAttempt())
        assertFalse(protector.isLockedOut.value)
    }

    @Test
    fun testTemporaryUnlockCodeLifecycleInRepository() = runBlocking {
        // 1. Generate 6-digit code
        val tempCode = repository.generateTemporaryUnlockCode("CHILD_DEV_001", 45)
        assertEquals(6, tempCode.code.length)
        assertFalse(tempCode.isUsed)

        // 2. Verify invalid code fails
        val invalidResult = repository.verifyAndApplyUnlockCode("000000")
        assertFalse(invalidResult)

        // 3. Verify valid code unlocks tablet and is marked used
        val validResult = repository.verifyAndApplyUnlockCode(tempCode.code)
        assertTrue(validResult)
        assertFalse(repository.isChildLocked.value)

        // 4. Using same code a second time fails (single-use enforcement)
        val reuseResult = repository.verifyAndApplyUnlockCode(tempCode.code)
        assertFalse(reuseResult)
    }
}
