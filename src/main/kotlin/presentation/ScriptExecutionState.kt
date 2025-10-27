package presentation

import domain.model.OutputLine
import domain.model.ExecutionStatus
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

data class ScriptExecutionState(
    val scriptContent: String = DEFAULT_SCRIPT,
    val textFieldValue: TextFieldValue = TextFieldValue(DEFAULT_SCRIPT),
    val output: List<OutputLine> = emptyList(),
    val status: ExecutionStatus = ExecutionStatus.IDLE,
    val exitCode: Int? = null,
    val error: String? = null,
    val isRunning: Boolean = false,
    val cursorPosition: Int? = null
) {
    val hasError: Boolean
        get() = exitCode != null && exitCode != 0

    val statusColor: StatusColor
        get() = when {
            isRunning -> StatusColor.RUNNING
            hasError -> StatusColor.ERROR
            exitCode == 0 -> StatusColor.SUCCESS
            else -> StatusColor.IDLE
        }

    companion object {
        private const val DEFAULT_SCRIPT = """println("Hello, Kotlin!")

            for (i in 1..5) {
                println("Count: ${'$'}i")
                Thread.sleep(500)
            }
            
            println("Done!")"""
        }
    }

enum class StatusColor {
    IDLE,
    RUNNING,
    SUCCESS,
    ERROR
}