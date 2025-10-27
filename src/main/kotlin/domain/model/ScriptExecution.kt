package domain.model

data class ScriptExecution(
    val id: String,
    val content: String,
    val status: ExecutionStatus,
    val output: List<OutputLine>,
    val exitCode: Int? = null,
    val startTime: Long? = null,
    val endTime: Long? = null
)

enum class ExecutionStatus {
    IDLE,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class OutputLine(
    val text: String,
    val type: OutputType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class OutputType {
    STDOUT,
    STDERR,
    SYSTEM
}