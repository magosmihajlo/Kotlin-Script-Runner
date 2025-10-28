package presentation

import domain.model.ExecutionStatus
import domain.model.ExecutionResult
import domain.usecase.ExecuteScriptUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

class ScriptExecutionViewModel(
    private val executeScriptUseCase: ExecuteScriptUseCase,
    private val coroutineScope: CoroutineScope
) {

    private val _state = MutableStateFlow(ScriptExecutionState())
    val state: StateFlow<ScriptExecutionState> = _state.asStateFlow()

    private val maxOutputLines = 1000

    fun navigateToLine(line: Int, column: Int) {
        println("navigateToLine called: line=$line, column=$column")
        val lines = _state.value.scriptContent.lines()
        if (line > 0 && line <= lines.size) {
            val targetLineIndex = line - 1
            val offset = lines.take(targetLineIndex).sumOf { it.length + 1 } + (column - 1).coerceAtLeast(0)

            println("Calculated offset: $offset")
            println("Total text length: ${_state.value.scriptContent.length}")

            _state.update { currentState ->
                currentState.copy(
                    textFieldValue = currentState.textFieldValue.copy(
                        selection = TextRange(offset)
                    )
                )
            }

            println("State updated! Current textFieldValue selection: ${_state.value.textFieldValue.selection}")
        } else {
            println("Line out of bounds: $line (total lines: ${lines.size})")
        }
    }

    fun executeScript() {
        if (_state.value.isRunning) return

        val content = _state.value.scriptContent.trim()
        if (content.isEmpty()) {
            _state.update {
                it.copy(
                    error = "Cannot execute empty script",
                    status = ExecutionStatus.FAILED,
                    exitCode = -1
                )
            }
            return
        }

        _state.update {
            it.copy(
                status = ExecutionStatus.RUNNING,
                output = emptyList(),
                exitCode = null,
                error = null,
                isRunning = true
            )
        }

        coroutineScope.launch {
            val result = executeScriptUseCase.execute(
                scriptContent = _state.value.scriptContent,
                onOutputLine = { outputLine ->
                    _state.update { currentState ->
                        val newOutput = (currentState.output + outputLine).takeLast(maxOutputLines)
                        currentState.copy(output = newOutput)
                    }
                }
            )

            handleExecutionResult(result)
        }
    }

    private fun handleExecutionResult(result: ExecutionResult) {
        when (result) {
            is ExecutionResult.Success -> {
                _state.update {
                    it.copy(
                        status = ExecutionStatus.COMPLETED,
                        exitCode = result.exitCode,
                        isRunning = false
                    )
                }
            }
            is ExecutionResult.Failure -> {
                _state.update {
                    it.copy(
                        status = ExecutionStatus.FAILED,
                        exitCode = -1,
                        error = result.error.message,
                        isRunning = false
                    )
                }
            }
            is ExecutionResult.Cancelled -> {
                _state.update {
                    it.copy(
                        status = ExecutionStatus.CANCELLED,
                        isRunning = false
                    )
                }
            }
        }
    }

    fun stopExecution() {
        executeScriptUseCase.cancelExecution()
        _state.update {
            it.copy(
                status = ExecutionStatus.CANCELLED,
                isRunning = false
            )
        }
    }

    fun clearOutput() {
        _state.update {
            it.copy(
                output = emptyList(),
                exitCode = null,
                error = null,
                status = ExecutionStatus.IDLE,
            )
        }
    }

    fun updateTextFieldValue(value: TextFieldValue) {
        _state.update {
            it.copy(textFieldValue = value)
        }
    }
}