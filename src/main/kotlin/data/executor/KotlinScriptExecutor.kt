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

        val properKotlinCode = buildProperKotlinFile(scriptContent)

        val tempDir = createTempDir("kotlin_exec_", "")
        val sourceFile = File(tempDir, "Main.kt")
        sourceFile.writeText(properKotlinCode)

        val jarFile = File(tempDir, "output.jar")

        try {
            val osName = System.getProperty("os.name").lowercase()
            val isWindows = osName.contains("win")
            val kotlinc = if (isWindows) "kotlinc.bat" else "kotlinc"

            onOutputLine(OutputLine("Compiling...", OutputType.SYSTEM))

            val stdlibPath = "C:\\Program Files\\Kotlin\\kotlinc\\lib\\kotlin-stdlib.jar"

            val compileBuilder = ProcessBuilder(
                kotlinc,
                sourceFile.absolutePath,
                "-d",
                jarFile.absolutePath,
                "-classpath",
                stdlibPath,
                "-include-runtime"
            )
            compileBuilder.redirectErrorStream(false)

            val compileProcess = compileBuilder.start()

            val compileErrors = mutableListOf<String>()
            compileProcess.errorStream.bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    compileErrors.add(line)
                }
            }

            val compileExitCode = compileProcess.waitFor()

            if (compileExitCode != 0) {
                tempDir.deleteRecursively()
                onOutputLine(OutputLine("Compilation failed", OutputType.SYSTEM))
                compileErrors.forEach {
                    onOutputLine(OutputLine(it, OutputType.STDERR))
                }
                return@withContext ExecutionResult.Failure(
                    ScriptError("Compilation failed"),
                    compileErrors.map { OutputLine(it, OutputType.STDERR) }
                )
            }

            onOutputLine(OutputLine("Running...", OutputType.SYSTEM))

            val runBuilder = ProcessBuilder("java", "-jar", jarFile.absolutePath)
            runBuilder.redirectErrorStream(false)

            currentProcess = runBuilder.start()
            val process = currentProcess ?: throw IllegalStateException("Process failed to start")

            val outputLines = mutableListOf<OutputLine>()

            executionJob = launch {
                launch {
                    process.inputStream.bufferedReader().use { reader ->
                        readStream(reader, OutputType.STDOUT, onOutputLine, outputLines)
                    }
                }

                launch {
                    process.errorStream.bufferedReader().use { reader ->
                        readStream(reader, OutputType.STDERR, onOutputLine, outputLines)
                    }
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

    private fun buildProperKotlinFile(userCode: String): String {
        val trimmed = userCode.trim()

        return if (trimmed.contains("fun main")) {
            trimmed
        } else {
            """
            fun main(args: Array<String>) {
                $trimmed
            }
            """.trimIndent()
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