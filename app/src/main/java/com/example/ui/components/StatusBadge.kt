package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ConnectionStatus
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseDanger

@Composable
fun ConnectionStatusBadge(
    status: ConnectionStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, dotColor, textRes) = when (status) {
        ConnectionStatus.SAME_WIFI -> Triple(
            EmeraldSuccess.copy(alpha = 0.15f),
            EmeraldSuccess,
            R.string.conn_wifi
        )
        ConnectionStatus.REMOTE_CLOUD -> Triple(
            CyanAccent.copy(alpha = 0.15f),
            CyanAccent,
            R.string.conn_remote
        )
        ConnectionStatus.CONNECTING -> Triple(
            AmberWarning.copy(alpha = 0.15f),
            AmberWarning,
            R.string.conn_connecting
        )
        ConnectionStatus.OFFLINE -> Triple(
            RoseDanger.copy(alpha = 0.15f),
            RoseDanger,
            R.string.conn_offline
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(textRes),
            color = dotColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun LockStatusBadge(
    isLocked: Boolean,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, textRes) = if (isLocked) {
        Triple(RoseDanger.copy(alpha = 0.15f), RoseDanger, R.string.status_locked)
    } else {
        Triple(EmeraldSuccess.copy(alpha = 0.15f), EmeraldSuccess, R.string.status_unlocked)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isLocked) "🔒 " else "🔓 ",
            fontSize = 12.sp
        )
        Text(
            text = stringResource(textRes),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
