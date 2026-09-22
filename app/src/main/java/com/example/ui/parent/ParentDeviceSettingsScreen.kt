package com.example.ui.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.PairedChildDevice
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseDanger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDeviceSettingsScreen(
    device: PairedChildDevice,
    onSaveName: (String) -> Unit,
    onSaveTimeout: (Int) -> Unit,
    onUnpair: () -> Unit,
    onBack: () -> Unit
) {
    var deviceName by remember { mutableStateOf(device.name) }
    var timeoutMinutes by remember { mutableFloatStateOf(device.inactivityTimeoutMinutes.toFloat()) }
    var showUnpairConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.device_settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_device_settings")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp)
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Device Name Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = stringResource(R.string.child_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = deviceName,
                                onValueChange = { deviceName = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_edit_device_name"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onSaveName(deviceName.trim()) },
                                enabled = deviceName.isNotBlank() && deviceName != device.name,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(stringResource(R.string.save))
                            }
                        }
                    }

                    // Inactivity Timeout Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.inactivity_timeout),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${timeoutMinutes.toInt()} min",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = IndigoPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.inactivity_timeout_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Slider(
                                value = timeoutMinutes,
                                onValueChange = { timeoutMinutes = it },
                                onValueChangeFinished = { onSaveTimeout(timeoutMinutes.toInt()) },
                                valueRange = 5f..60f,
                                steps = 10,
                                modifier = Modifier.testTag("slider_inactivity_timeout")
                            )
                        }
                    }

                    // Danger Zone: Unpair
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Danger Zone",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = RoseDanger
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Unpairing will disconnect this tablet and revoke remote unlock access.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = { showUnpairConfirm = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("btn_unpair_device")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = RoseDanger)
                                Text(" Unpair Device", color = RoseDanger, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUnpairConfirm) {
        AlertDialog(
            onDismissRequest = { showUnpairConfirm = false },
            title = { Text("Unpair ${device.name}?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove this device from your KidLock dashboard?") },
            confirmButton = {
                Button(
                    onClick = {
                        showUnpairConfirm = false
                        onUnpair()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger)
                ) {
                    Text("Unpair")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnpairConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
