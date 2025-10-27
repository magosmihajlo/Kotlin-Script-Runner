package ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import presentation.StatusColor
import ui.theme.ErrorColor
import ui.theme.RunningColor
import ui.theme.SuccessColor

@Composable
fun StatusIndicator(
    statusColor: StatusColor,
    exitCode: Int?,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusDot(statusColor, isRunning)
        StatusText(statusColor, exitCode, isRunning)
    }
}

@Composable
private fun StatusDot(
    statusColor: StatusColor,
    isRunning: Boolean
) {
    val color = when (statusColor) {
        StatusColor.IDLE -> Color.Gray
        StatusColor.RUNNING -> RunningColor
        StatusColor.SUCCESS -> SuccessColor
        StatusColor.ERROR -> ErrorColor
    }

    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(300)
    )

    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(12.dp)
            .scale(if (isRunning) scale else 1f)
            .background(animatedColor, CircleShape)
    )
}

@Composable
private fun StatusText(
    statusColor: StatusColor,
    exitCode: Int?,
    isRunning: Boolean
) {
    val text = when {
        isRunning -> "Running..."
        statusColor == StatusColor.SUCCESS -> "Success (Exit: $exitCode)"
        statusColor == StatusColor.ERROR -> "Failed (Exit: $exitCode)"
        statusColor == StatusColor.IDLE -> "Ready"
        else -> "Idle"
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium
    )
}