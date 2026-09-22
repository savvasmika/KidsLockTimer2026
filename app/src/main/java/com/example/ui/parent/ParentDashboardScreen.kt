package com.example.ui.parent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ConnectionStatus
import com.example.model.PairedChildDevice
import com.example.model.UnlockRequest
import com.example.ui.components.ConnectionStatusBadge
import com.example.ui.components.KioskGuideDialog
import com.example.ui.components.LockStatusBadge
import com.example.ui.onboarding.LanguageSelectionDialog
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseDanger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    pairedDevices: List<PairedChildDevice>,
    pendingRequests: List<UnlockRequest>,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    onPairNewDevice: () -> Unit,
    onUnlockDevice: (PairedChildDevice, Int) -> Unit,
    onLockDevice: (PairedChildDevice) -> Unit,
    onGenerateTempCode: (PairedChildDevice, Int) -> Unit,
    onChangeThemeClick: (PairedChildDevice) -> Unit,
    onDeviceSettingsClick: (PairedChildDevice) -> Unit,
    onApproveRequest: (UnlockRequest, Int) -> Unit,
    onDenyRequest: (UnlockRequest) -> Unit,
    onSwitchRoleRequest: () -> Unit,
    onOpenDebugScreen: () -> Unit = {}
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showKioskGuide by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.parent_dashboard_title),
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenDebugScreen,
                        modifier = Modifier.testTag("btn_parent_debug")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Diagnostics",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showKioskGuide = true },
                        modifier = Modifier.testTag("btn_kiosk_guide")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Kiosk Info",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(
                        onClick = { showLanguageDialog = true },
                        modifier = Modifier.testTag("btn_parent_lang")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language"
                        )
                    }
                    IconButton(
                        onClick = onSwitchRoleRequest,
                        modifier = Modifier.testTag("btn_switch_role")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Switch Role"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onPairNewDevice,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.btn_pair_new)) },
                containerColor = IndigoPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_pair_device")
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 760.dp)
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Pending Requests Banner
                    if (pendingRequests.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.section_unlock_requests),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        items(pendingRequests) { req ->
                            PendingRequestCard(
                                request = req,
                                onApprove = { onApproveRequest(req, 30) },
                                onDeny = { onDenyRequest(req) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Paired Devices Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.section_paired_devices),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${pairedDevices.size} active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (pairedDevices.isEmpty()) {
                        item {
                            EmptyDevicesCard(onPairClick = onPairNewDevice)
                        }
                    } else {
                        items(pairedDevices) { device ->
                            PairedDeviceDashboardCard(
                                device = device,
                                onUnlock = { mins -> onUnlockDevice(device, mins) },
                                onLock = { onLockDevice(device) },
                                onGenerateCode = { mins -> onGenerateTempCode(device, mins) },
                                onChangeTheme = { onChangeThemeClick(device) },
                                onSettings = { onDeviceSettingsClick(device) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onSelectLanguage = { lang ->
                onLanguageChange(lang)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showKioskGuide) {
        KioskGuideDialog(onDismiss = { showKioskGuide = false })
    }
}

@Composable
private fun PendingRequestCard(
    request: UnlockRequest,
    onApprove: () -> Unit,
    onDeny: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_request_${request.requestId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(IndigoPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = IndigoPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${request.childName} requested time",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "+${request.requestedMinutes} minutes requested",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onDeny,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(RoseDanger.copy(alpha = 0.15f))
                        .testTag("btn_deny_${request.requestId}")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Deny", tint = RoseDanger)
                }

                IconButton(
                    onClick = onApprove,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(EmeraldSuccess.copy(alpha = 0.15f))
                        .testTag("btn_approve_${request.requestId}")
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Approve", tint = EmeraldSuccess)
                }
            }
        }
    }
}

@Composable
private fun PairedDeviceDashboardCard(
    device: PairedChildDevice,
    onUnlock: (minutes: Int) -> Unit,
    onLock: () -> Unit,
    onGenerateCode: (minutes: Int) -> Unit,
    onChangeTheme: () -> Unit,
    onSettings: () -> Unit
) {
    var selectedUnlockDuration by remember { mutableStateOf(30) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_paired_device_${device.deviceId}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Top Row: Avatar/Device icon, Name, Status, Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(IndigoPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TabletAndroid,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ConnectionStatusBadge(status = device.connectionType)
                        LockStatusBadge(isLocked = device.isLocked)
                    }
                }

                IconButton(
                    onClick = onSettings,
                    modifier = Modifier.testTag("btn_device_settings_${device.deviceId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Duration Chips for Unlock
            Text(
                text = "Unlock Duration:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(15, 30, 60, 120).forEach { mins ->
                    val isSelected = selectedUnlockDuration == mins
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selectedUnlockDuration = mins }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (mins >= 60) "${mins / 60}h" else "${mins}m",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (device.isLocked) {
                    Button(
                        onClick = { onUnlock(selectedUnlockDuration) },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("btn_remote_unlock_${device.deviceId}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_unlock_now), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    Button(
                        onClick = onLock,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("btn_remote_lock_${device.deviceId}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoseDanger)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_lock_now), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                FilledTonalButton(
                    onClick = { onGenerateCode(selectedUnlockDuration) },
                    modifier = Modifier
                        .height(46.dp)
                        .testTag("btn_gen_code_${device.deviceId}"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_temp_code), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                IconButton(
                    onClick = onChangeTheme,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("btn_change_theme_${device.deviceId}")
                ) {
                    Icon(Icons.Default.ColorLens, contentDescription = "Themes", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun EmptyDevicesCard(
    onPairClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(IndigoPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TabletAndroid,
                    contentDescription = null,
                    tint = IndigoPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.no_paired_devices),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.no_paired_devices_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onPairClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                modifier = Modifier.testTag("btn_empty_pair_cta")
            ) {
                Text(stringResource(R.string.btn_pair_new), fontWeight = FontWeight.Bold)
            }
        }
    }
}
