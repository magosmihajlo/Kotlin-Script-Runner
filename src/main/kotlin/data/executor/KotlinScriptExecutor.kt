package data.executor

import domain.model.ExecutionResult
import domain.model.OutputLine
import domain.model.OutputType
import domain.model.ScriptError
import domain.repository.ScriptExecutor
import domain.repository.ScriptFileManager
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes

class KotlinScriptExecutor(
    private val fileManager: ScriptFileManager
) : ScriptExecutor {
    @Volatile
    private var currentProcess: Process? = null
    @Volatile
    private var executionJob: Job? = null
    private var currentTempDir: File? = null

    override suspend fun execute(
        scriptContent: String,
        onOutputLine: suspend (OutputLine) -> Unit
    ): ExecutionResult = withContext(Dispatchers.IO) {

        val scriptFile = fileManager.createTempScriptFile(scriptContent)
        currentTempDir = scriptFile.parentFile

        try {
            val osName = System.getProperty("os.name").lowercase()
            val isWindows = osName.contains("win")
            val kotlinc = if (isWindows) "kotlinc.bat" else "kotlinc"

            if (!isKotlincAvailable(kotlinc)) {
                return@withContext ExecutionResult.Failure(
                    ScriptError("kotlinc not found in PATH. Please install Kotlin compiler."),
                    emptyList()
                )
            }

            onOutputLine(OutputLine("Running script...", OutputType.SYSTEM))

            val runBuilder = ProcessBuilder(kotlinc, "-script", scriptFile.absolutePath)
            runBuilder.redirectErrorStream(false)

            currentProcess = runBuilder.start()
            val process = currentProcess ?: throw IllegalStateException("Process failed to start")

            val outputLines = mutableListOf<OutputLine>()

            executionJob = launch {
                launch { readStream(process.inputStream.bufferedReader(), OutputType.STDOUT, onOutputLine, outputLines) }
                launch { readStream(process.errorStream.bufferedReader(), OutputType.STDERR, onOutputLine, outputLines) }
            }

            executionJob?.join()
            val exitCode = withTimeout(5.minutes.inWholeMilliseconds) {
                process.waitFor()
            }

            fileManager.cleanup(scriptFile)
            scriptFile.parentFile?.deleteRecursively()

            if (exitCode == 0) {
                ExecutionResult.Success(exitCode, outputLines)
            } else {
                val error = parseError(outputLines)
                ExecutionResult.Failure(error, outputLines)
            }

        } catch (e: IOException) {
            fileManager.cleanup(scriptFile)
            scriptFile.parentFile.deleteRecursively()
            return@withContext ExecutionResult.Failure(
                ScriptError("Failed to start kotlinc: ${e.message}. Ensure kotlinc is installed and in PATH."),
                emptyList()
            )

        } catch (e: CancellationException) {
            try {
                currentProcess?.destroyForcibly()
                currentProcess?.waitFor(1, TimeUnit.SECONDS)
                fileManager.cleanup(scriptFile)
                scriptFile.parentFile?.deleteRecursively()
            } catch (cleanupError: Exception) {
                // TODO: Log cleanup failure
            }
            ExecutionResult.Cancelled
        } catch (e: Exception) {
            scriptFile.parentFile.deleteRecursively()
            ExecutionResult.Failure(
                ScriptError("Execution error: ${e.message}"),
                emptyList()
            )
        } finally {
            currentProcess = null
            executionJob = null
            currentTempDir = null
        }
    }

    private fun isKotlincAvailable(kotlinc: String): Boolean {
        return try {
            val process = ProcessBuilder(kotlinc, "-version").start()
            process.waitFor(5, TimeUnit.SECONDS)
            process.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun readStream(
        reader: BufferedReader,
        outputType: OutputType,
        onOutputLine: suspend (OutputLine) -> Unit,
        outputLines: MutableList<OutputLine>
    ) {
        reader.lineSequence().forEach { line ->
            val outputLine = OutputLine(line, outputType)
            outputLines.add(outputLine)
            onOutputLine(outputLine)
        }
    }


    private fun parseError(outputLines: List<OutputLine>): ScriptError {
        val errorLine = outputLines.firstOrNull { it.type == OutputType.STDERR }

        if (errorLine != null) {
            val regex = """.*:(\d+):(\d+):\s*error:\s*(.+)""".toRegex()
            val match = regex.find(errorLine.text)

            if (match != null) {
                val (line, column, message) = match.destructured
                return ScriptError(
                    message = message,
                    line = line.toIntOrNull(),
                    column = column.toIntOrNull(),
                    source = errorLine.text
                )
            }

            return ScriptError(errorLine.text)
        }

        return ScriptError("Script execution failed with non-zero exit code")
    }

    override fun cancel() {
        executionJob?.cancel()
        currentProcess?.destroyForcibly()
    }

    override fun isRunning(): Boolean {
        return currentProcess?.isAlive == true
    }
}