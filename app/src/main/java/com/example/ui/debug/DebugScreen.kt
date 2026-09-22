package com.example.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kiosk.KioskManager
import com.example.network.LocalP2PCommunication
import com.example.ui.KidLockViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    viewModel: KidLockViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val kioskManager = remember { KioskManager(context) }

    val role by viewModel.deviceRole.collectAsState()
    val isLocked by viewModel.isChildLocked.collectAsState()
    val isSearching by viewModel.isSearchingDevices.collectAsState()
    val isAdvertising by viewModel.isAdvertising.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val latestStatus by viewModel.latestUnlockStatusMessage.collectAsState()

    val cryptoManager = viewModel.repository.cryptoManager
    val nonceManager = viewModel.repository.nonceReplayManager
    val bruteForce = viewModel.repository.bruteForceProtector
    val isLockedOut by bruteForce.isLockedOut.collectAsState()
    val remainingLockout by bruteForce.remainingLockoutSeconds.collectAsState()

    var testLogMessage by remember { mutableStateOf<String?>("Ready. Secure logs active (no secrets logged).") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("System Diagnostics & Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("debug_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Device Identity & Cryptography
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Device Identity & Keys", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    DebugInfoRow("Device ID", viewModel.securityPrefs.getDeviceId())
                    DebugInfoRow("Device Name", viewModel.securityPrefs.getDeviceName())
                    DebugInfoRow("Role", role.name)
                    DebugInfoRow("Public Key Fingerprint", cryptoManager.getPublicKeyFingerprint())
                    DebugInfoRow("Replay Nonces Tracked", "${nonceManager.getTrackedCount()} active")
                    DebugInfoRow("Brute-force Throttling", if (isLockedOut) "LOCKED OUT (${remainingLockout}s)" else "Normal (0 errors)")
                }
            }

            // Section 2: Local Wi-Fi & NSD
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Local Network & NSD (mDNS)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    DebugInfoRow("NSD Service Type", "_kidlock._tcp.")
                    DebugInfoRow("Server Port", "${LocalP2PCommunication.SERVER_PORT}")
                    DebugInfoRow("NSD Advertising", if (isAdvertising) "ACTIVE 🟢" else "INACTIVE ⚪")
                    DebugInfoRow("NSD Discovery", if (isSearching) "SCANNING 🔵" else "IDLE ⚪")
                    DebugInfoRow("Discovered in LAN", "${discoveredDevices.size} devices")
                    DebugInfoRow("Paired Devices in DB", "${pairedDevices.size} devices")
                }
            }

            // Section 3: Kiosk Mode & Device Policy
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kiosk & Device Admin State", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    DebugInfoRow("Device Lock State", if (isLocked) "LOCKED 🔒" else "UNLOCKED 🔓")
                    DebugInfoRow("Device Owner", if (kioskManager.isDeviceOwner()) "YES (Exit-Proof Kiosk)" else "NO (Standard Pinning)")
                    DebugInfoRow("Profile Owner", if (kioskManager.isProfileOwner()) "YES" else "NO")
                    DebugInfoRow("Lock Task Whitelisted", if (kioskManager.isLockTaskPermitted()) "YES" else "NO")
                    DebugInfoRow("Kiosk Mode Policy", kioskManager.getKioskStatusDescription())
                }
            }

            // Section 4: Live Event Log
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Security Event Monitor", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = testLogMessage ?: "No recent events",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (latestStatus != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Latest Status: $latestStatus",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Section 5: Diagnostic Actions
            Text("Diagnostic Controls", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.toggleChildLock()
                        testLogMessage = "Toggled lock state. New state: ${if (!isLocked) "LOCKED" else "UNLOCKED"}"
                    },
                    modifier = Modifier.weight(1f).testTag("debug_toggle_lock"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isLocked) "Unlock" else "Lock")
                }

                OutlinedButton(
                    onClick = {
                        nonceManager.clearAll()
                        bruteForce.reset()
                        testLogMessage = "Cleared replay nonce cache and reset brute-force throttle."
                    },
                    modifier = Modifier.weight(1f).testTag("debug_reset_security")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Nonce/Throttle")
                }
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        viewModel.sendUnlockRequest()
                        testLogMessage = "Dispatched cryptographically signed Unlock Request to paired Parent."
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("debug_test_request")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simulate Signed Child Unlock Request")
            }
        }
    }
}

@Composable
private fun DebugInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
