package com.example

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.model.DeviceRole
import com.example.ui.KidLockViewModel
import com.example.ui.child.ChildLockScreen
import com.example.ui.child.ChildUnlockedScreen
import com.example.ui.navigation.NavRoutes
import com.example.ui.onboarding.ChildSetupScreen
import com.example.ui.onboarding.PinSetupScreen
import com.example.ui.onboarding.RoleSelectionScreen
import com.example.ui.pairing.ChildPairingScreen
import com.example.ui.pairing.ParentPairingScreen
import com.example.ui.parent.GenerateTempCodeDialog
import com.example.ui.parent.ParentDashboardScreen
import com.example.ui.parent.ParentDeviceSettingsScreen
import com.example.ui.parent.ParentPinDialog
import com.example.ui.debug.DebugScreen
import com.example.kiosk.KioskManager
import com.example.ui.theme.KidLockTheme
import com.example.ui.theme.gallery.ThemeGalleryScreen
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: KidLockViewModel by viewModels()
    private lateinit var kioskManager: KioskManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        kioskManager = KioskManager(this)

        setContent {
            val currentLanguage by viewModel.currentLanguage.collectAsState()
            val deviceRole by viewModel.deviceRole.collectAsState()
            val activeTheme by viewModel.activeTheme.collectAsState()
            val isChildLocked by viewModel.isChildLocked.collectAsState()

            // Automatically apply Kiosk / LockTask when in Child role and locked
            androidx.compose.runtime.LaunchedEffect(deviceRole, isChildLocked) {
                if (deviceRole == DeviceRole.CHILD) {
                    if (isChildLocked) {
                        kioskManager.startKioskMode(this@MainActivity)
                    } else {
                        kioskManager.stopKioskMode(this@MainActivity)
                    }
                }
            }

            // Update Configuration Locale dynamically
            val context = LocalContext.current
            val localizedContext = remember(currentLanguage) {
                updateLocale(context, currentLanguage)
            }

            CompositionLocalProvider(
                LocalConfiguration provides localizedContext.resources.configuration,
                LocalContext provides localizedContext
            ) {
                KidLockTheme(
                    kidTheme = activeTheme,
                    isChildMode = deviceRole == DeviceRole.CHILD
                ) {
                    KidLockNavApp(
                        viewModel = viewModel,
                        initialRole = deviceRole,
                        isChildLocked = isChildLocked
                    )
                }
            }
        }
    }

    private fun updateLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}

@Composable
fun KidLockNavApp(
    viewModel: KidLockViewModel,
    initialRole: DeviceRole,
    isChildLocked: Boolean
) {
    val navController = rememberNavController()

    val startDestination = when (initialRole) {
        DeviceRole.UNSET -> NavRoutes.ROLE_SELECTION
        DeviceRole.PARENT -> NavRoutes.PARENT_DASHBOARD
        DeviceRole.CHILD -> if (isChildLocked) NavRoutes.CHILD_LOCK else NavRoutes.CHILD_UNLOCKED
    }

    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val pendingRequests by viewModel.pendingRequests.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val isSearchingDevices by viewModel.isSearchingDevices.collectAsState()
    val parentPairingCode by viewModel.parentPairingCode.collectAsState()
    val pairingTargetDevice by viewModel.pairingTargetDevice.collectAsState()
    val pairingSuccess by viewModel.pairingSuccess.collectAsState()
    val incomingPairRequest by viewModel.incomingPairRequest.collectAsState()
    val activeTempCode by viewModel.activeTempCode.collectAsState()
    val tempCodeRemainingSeconds by viewModel.tempCodeRemainingSeconds.collectAsState()
    val animationsEnabled by viewModel.animationsEnabled.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val childName by viewModel.childName.collectAsState()
    val activeTheme by viewModel.activeTheme.collectAsState()
    val unlockStatusMessage by viewModel.unlockStatusMessage.collectAsState()

    var showParentSwitchPinDialog by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Role Selection Screen
        composable(NavRoutes.ROLE_SELECTION) {
            RoleSelectionScreen(
                currentLanguage = currentLanguage,
                onLanguageChange = { lang -> viewModel.setLanguage(lang) },
                onRoleSelected = { role ->
                    if (role == DeviceRole.PARENT) {
                        if (!viewModel.hasParentPin()) {
                            navController.navigate(NavRoutes.PIN_SETUP)
                        } else {
                            viewModel.selectDeviceRole(DeviceRole.PARENT)
                            navController.navigate(NavRoutes.PARENT_DASHBOARD) {
                                popUpTo(NavRoutes.ROLE_SELECTION) { inclusive = true }
                            }
                        }
                    } else {
                        navController.navigate(NavRoutes.CHILD_SETUP)
                    }
                }
            )
        }

        // 2. PIN Setup Screen (for Parent)
        composable(NavRoutes.PIN_SETUP) {
            PinSetupScreen(
                onPinCreated = { pin ->
                    viewModel.saveParentPin(pin)
                    viewModel.selectDeviceRole(DeviceRole.PARENT)
                    navController.navigate(NavRoutes.PARENT_DASHBOARD) {
                        popUpTo(NavRoutes.ROLE_SELECTION) { inclusive = true }
                    }
                }
            )
        }

        // 3. Child Setup Screen
        composable(NavRoutes.CHILD_SETUP) {
            ChildSetupScreen(
                initialName = childName,
                onComplete = { name, age, avatar ->
                    viewModel.setChildName(name)
                    viewModel.selectDeviceRole(DeviceRole.CHILD)
                    navController.navigate(NavRoutes.CHILD_PAIRING) {
                        popUpTo(NavRoutes.ROLE_SELECTION) { inclusive = true }
                    }
                }
            )
        }

        // 4. Parent Dashboard
        composable(NavRoutes.PARENT_DASHBOARD) {
            ParentDashboardScreen(
                pairedDevices = pairedDevices,
                pendingRequests = pendingRequests,
                currentLanguage = currentLanguage,
                onLanguageChange = { lang -> viewModel.setLanguage(lang) },
                onPairNewDevice = { navController.navigate(NavRoutes.PARENT_PAIRING) },
                onUnlockDevice = { device, minutes -> viewModel.unlockDevice(device, minutes) },
                onLockDevice = { device -> viewModel.lockDevice(device) },
                onGenerateTempCode = { device, minutes -> viewModel.generateTempCodeForDevice(device, minutes) },
                onChangeThemeClick = { device ->
                    navController.navigate(NavRoutes.THEME_GALLERY)
                },
                onDeviceSettingsClick = { device ->
                    navController.navigate("${NavRoutes.DEVICE_SETTINGS}/${device.deviceId}")
                },
                onApproveRequest = { req, mins -> viewModel.approveRequest(req, mins) },
                onDenyRequest = { req -> viewModel.denyRequest(req) },
                onSwitchRoleRequest = { showParentSwitchPinDialog = true },
                onOpenDebugScreen = { navController.navigate(NavRoutes.DEBUG) }
            )
        }

        // 5. Parent Pairing Screen (NSD Discovery)
        composable(NavRoutes.PARENT_PAIRING) {
            ParentPairingScreen(
                discoveredDevices = discoveredDevices,
                isSearching = isSearchingDevices,
                pairingCode = parentPairingCode,
                pairingTargetDevice = pairingTargetDevice,
                isPairingSuccess = pairingSuccess,
                onStartSearch = { viewModel.startSearchingDevices() },
                onStopSearch = { viewModel.stopSearchingDevices() },
                onSelectDeviceToPair = { device -> viewModel.startPairingWithDevice(device) },
                onCompletePairing = {
                    viewModel.clearPairingSession()
                    navController.navigate(NavRoutes.PARENT_DASHBOARD) {
                        popUpTo(NavRoutes.PARENT_DASHBOARD) { inclusive = true }
                    }
                },
                onCancelPairing = { viewModel.clearPairingSession() },
                onBack = { navController.popBackStack() }
            )
        }

        // 6. Child Pairing Waiting Screen
        composable(NavRoutes.CHILD_PAIRING) {
            ChildPairingScreen(
                childName = childName,
                incomingPairRequest = incomingPairRequest,
                onAcceptPairing = { req ->
                    viewModel.acceptPairingRequest(req)
                    navController.navigate(NavRoutes.CHILD_LOCK) {
                        popUpTo(NavRoutes.CHILD_PAIRING) { inclusive = true }
                    }
                },
                onRejectPairing = { req -> viewModel.rejectPairingRequest(req) },
                onProceedToLockScreen = {
                    navController.navigate(NavRoutes.CHILD_LOCK) {
                        popUpTo(NavRoutes.CHILD_PAIRING) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // 7. Child Lock Screen (Kiosk Mode Lock)
        composable(NavRoutes.CHILD_LOCK) {
            // Prevent back escape
            BackHandler(enabled = true) {
                // Do not allow exiting lock screen
            }

            ChildLockScreen(
                childName = childName,
                theme = activeTheme,
                animationsEnabled = animationsEnabled,
                statusMessage = unlockStatusMessage,
                onRequestUnlock = { mins -> viewModel.requestUnlockFromParent(mins) },
                onSubmitUnlockCode = { code, onResult -> viewModel.submitUnlockCode(code, onResult) },
                onVerifyParentPin = { pin -> viewModel.verifyPin(pin) },
                onParentPinUnlockSuccess = {
                    viewModel.unlockLocallyViaPin()
                    navController.navigate(NavRoutes.CHILD_UNLOCKED) {
                        popUpTo(NavRoutes.CHILD_LOCK) { inclusive = true }
                    }
                },
                onOpenThemeGallery = { navController.navigate(NavRoutes.THEME_GALLERY) },
                onOpenPairing = { navController.navigate(NavRoutes.CHILD_PAIRING) }
            )
        }

        // 8. Child Unlocked Screen
        composable(NavRoutes.CHILD_UNLOCKED) {
            ChildUnlockedScreen(
                childName = childName,
                theme = activeTheme,
                animationsEnabled = animationsEnabled,
                onLockTablet = {
                    viewModel.lockLocally()
                    navController.navigate(NavRoutes.CHILD_LOCK) {
                        popUpTo(NavRoutes.CHILD_UNLOCKED) { inclusive = true }
                    }
                },
                onSelectTheme = { themeId -> viewModel.selectTheme(themeId) },
                onOpenThemeGallery = { navController.navigate(NavRoutes.THEME_GALLERY) },
                onVerifyParentPin = { pin -> viewModel.verifyPin(pin) },
                onOpenParentSettings = {
                    viewModel.resetDeviceRole()
                    navController.navigate(NavRoutes.ROLE_SELECTION) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // 9. Theme Gallery Screen
        composable(NavRoutes.THEME_GALLERY) {
            ThemeGalleryScreen(
                currentThemeId = activeTheme.id,
                animationsEnabled = animationsEnabled,
                onToggleAnimations = { enabled -> viewModel.setAnimationsEnabled(enabled) },
                onSelectTheme = { themeId ->
                    viewModel.selectTheme(themeId)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        // 10. Device Settings Screen
        composable(
            route = "${NavRoutes.DEVICE_SETTINGS}/{deviceId}",
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
            val device = pairedDevices.find { it.deviceId == deviceId }
            if (device != null) {
                ParentDeviceSettingsScreen(
                    device = device,
                    onSaveName = { newName -> viewModel.renameDevice(device.deviceId, newName) },
                    onSaveTimeout = { timeout -> viewModel.setInactivityTimeout(timeout) },
                    onUnpair = {
                        viewModel.unpairDevice(device.deviceId)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // 11. Debug / Diagnostics Screen
        composable(NavRoutes.DEBUG) {
            DebugScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }

    // Temporary Code Generation Popup for Parent
    if (activeTempCode != null) {
        GenerateTempCodeDialog(
            tempCode = activeTempCode,
            remainingSeconds = tempCodeRemainingSeconds,
            onDismiss = { viewModel.dismissTempCode() }
        )
    }

    // PIN dialog for switching device role from parent dashboard
    if (showParentSwitchPinDialog) {
        ParentPinDialog(
            onVerifyPin = { pin -> viewModel.verifyPin(pin) },
            onSuccess = {
                showParentSwitchPinDialog = false
                viewModel.resetDeviceRole()
                navController.navigate(NavRoutes.ROLE_SELECTION) {
                    popUpTo(0) { inclusive = true }
                }
            },
            onDismiss = { showParentSwitchPinDialog = false }
        )
    }
}
