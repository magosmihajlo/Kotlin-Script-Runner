package domain.usecase

import domain.model.ExecutionResult
import domain.model.OutputLine
import domain.repository.ScriptExecutor
import domain.repository.ScriptFileManager

class ExecuteScriptUseCase(
    private val scriptExecutor: ScriptExecutor,
    private val fileManager: ScriptFileManager
) {
    suspend fun execute(
        scriptContent: String,
        onOutputLine: suspend (OutputLine) -> Unit
    ): ExecutionResult {
        return try {
            scriptExecutor.execute(scriptContent, onOutputLine)
        } catch (e: Exception) {
            ExecutionResult.Failure(
                error = domain.model.ScriptError(
                    message = "Execution failed: ${e.message}",
                    line = null,
                    column = null
                ),
                output = emptyList()
            )
        }
    }

    fun cancelExecution() {
        scriptExecutor.cancel()
    }

    fun isExecuting(): Boolean {
        return scriptExecutor.isRunning()
    }
}