package domain.model

data class ScriptError(
    val message: String,
    val line: Int? = null,
    val column: Int? = null,
    val source: String? = null
) {
    val hasLocation: Boolean
        get() = line != null && column != null
}

sealed class ExecutionResult {
    data class Success(val exitCode: Int, val output: List<OutputLine>) : ExecutionResult()
    data class Failure(val error: ScriptError, val output: List<OutputLine>) : ExecutionResult()
    data object Cancelled : ExecutionResult()
}