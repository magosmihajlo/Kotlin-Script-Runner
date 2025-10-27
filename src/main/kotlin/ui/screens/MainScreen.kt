package ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import presentation.ScriptExecutionViewModel
import ui.components.CodeEditor
import ui.components.ControlBar
import ui.components.OutputPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ScriptExecutionViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Kotlin Script Runner") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        ControlBar(
            onRunClick = { viewModel.executeScript() },
            onStopClick = { viewModel.stopExecution() },
            onClearClick = { viewModel.clearOutput() },
            isRunning = state.isRunning,
            statusColor = state.statusColor,
            exitCode = state.exitCode
        )

        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            CodeEditor(
                content = state.scriptContent,
                onContentChange = { viewModel.updateScriptContent(it) },
                enabled = !state.isRunning,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            VerticalDivider()

            OutputPanel(
                output = state.output,
                onErrorLineClick = { line, column ->
                    viewModel.navigateToLine(line, column)
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        if (state.error != null) {
            HorizontalDivider()
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Error: ${state.error}",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}