package domain.repository

import domain.model.ExecutionResult
import domain.model.OutputLine

interface ScriptExecutor {
    suspend fun execute(
        scriptContent: String,
        onOutputLine: suspend (OutputLine) -> Unit
    ): ExecutionResult

    fun cancel()

    fun isRunning(): Boolean
}