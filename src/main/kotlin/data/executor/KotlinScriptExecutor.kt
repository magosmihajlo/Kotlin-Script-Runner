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

class KotlinScriptExecutor(
    private val fileManager: ScriptFileManager
) : ScriptExecutor {

    private var currentProcess: Process? = null
    private var executionJob: Job? = null

    override suspend fun execute(
        scriptContent: String,
        onOutputLine: suspend (OutputLine) -> Unit
    ): ExecutionResult = withContext(Dispatchers.IO) {

        val tempDir = createTempDir("kotlin_exec_", "")
        val scriptFile = File(tempDir, "Main.kts")
        scriptFile.writeText(scriptContent)

        try {
            val osName = System.getProperty("os.name").lowercase()
            val isWindows = osName.contains("win")
            val kotlinc = if (isWindows) "kotlinc.bat" else "kotlinc"

            onOutputLine(OutputLine("Running script...", OutputType.SYSTEM))

            val runBuilder = ProcessBuilder(
                kotlinc,
                "-script",
                scriptFile.absolutePath
            )
            runBuilder.redirectErrorStream(true)

            currentProcess = runBuilder.start()
            val process = currentProcess ?: throw IllegalStateException("Process failed to start")

            val outputLines = mutableListOf<OutputLine>()

            executionJob = launch {
                process.inputStream.bufferedReader().use { reader ->
                    readStream(reader, OutputType.STDOUT, onOutputLine, outputLines)
                }
            }

            executionJob?.join()

            val exitCode = process.waitFor()
            tempDir.deleteRecursively()

            if (exitCode == 0) {
                ExecutionResult.Success(exitCode, outputLines)
            } else {
                val error = parseError(outputLines)
                ExecutionResult.Failure(error, outputLines)
            }


        } catch (e: CancellationException) {
            currentProcess?.destroyForcibly()
            tempDir.deleteRecursively()
            ExecutionResult.Cancelled
        } catch (e: Exception) {
            tempDir.deleteRecursively()
            ExecutionResult.Failure(
                ScriptError("Execution error: ${e.message}"),
                emptyList()
            )
        } finally {
            currentProcess = null
            executionJob = null
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
