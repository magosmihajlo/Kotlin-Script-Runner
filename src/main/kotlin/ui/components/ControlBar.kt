package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import presentation.StatusColor

@Composable
fun ControlBar(
    onRunClick: () -> Unit,
    onStopClick: () -> Unit,
    onClearClick: () -> Unit,
    isRunning: Boolean,
    statusColor: StatusColor,
    exitCode: Int?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onRunClick,
                enabled = !isRunning
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Run Script"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Run")
            }

            Button(
                onClick = onStopClick,
                enabled = isRunning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop Script"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Stop")
            }

            OutlinedButton(
                onClick = onClearClick,
                enabled = !isRunning
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear Output"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear")
            }
        }

        StatusIndicator(
            statusColor = statusColor,
            exitCode = exitCode,
            isRunning = isRunning
        )
    }
}