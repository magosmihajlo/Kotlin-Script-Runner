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

class KotlinCompileExecutor(
    private val fileManager: ScriptFileManager
) : ScriptExecutor {

    private var currentProcess: Process? = null
    private var executionJob: Job? = null

    override suspend fun execute(
        scriptContent: String,
        onOutputLine: suspend (OutputLine) -> Unit
    ): ExecutionResult = withContext(Dispatchers.IO) {

        val wrappedScript = if (!scriptContent.contains("fun main")) {
            """
            fun main() {
                $scriptContent
            }
            """.trimIndent()
        } else {
            scriptContent
        }

        val tempDir = File.createTempFile("kotlin_script", "").apply { delete(); mkdirs() }
        val sourceFile = File(tempDir, "Script.kt")
        sourceFile.writeText(wrappedScript)

        val jarFile = File(tempDir, "script.jar")

        try {
            val osName = System.getProperty("os.name").lowercase()
            val isWindows = osName.contains("win")
            val kotlinc = if (isWindows) "kotlinc.bat" else "kotlinc"

            val compileProcess = ProcessBuilder(
                kotlinc,
                sourceFile.absolutePath,
                "-include-runtime",
                "-d", jarFile.absolutePath
            ).start()

            val compileExitCode = compileProcess.waitFor()

            if (compileExitCode != 0) {
                val errorOutput = compileProcess.errorStream.bufferedReader().readText()
                tempDir.deleteRecursively()
                return@withContext ExecutionResult.Failure(
                    ScriptError("Compilation failed: $errorOutput"),
                    listOf(OutputLine(errorOutput, OutputType.STDERR))
                )
            }

            val runProcess = ProcessBuilder(
                "java", "-jar", jarFile.absolutePath
            ).start()

            currentProcess = runProcess
            val outputLines = mutableListOf<OutputLine>()

            executionJob = launch {
                launch {
                    runProcess.inputStream.bufferedReader().use { reader ->
                        readStream(reader, OutputType.STDOUT, onOutputLine, outputLines)
                    }
                }

                launch {
                    runProcess.errorStream.bufferedReader().use { reader ->
                        readStream(reader, OutputType.STDERR, onOutputLine, outputLines)
                    }
                }
            }

            executionJob?.join()
            val exitCode = runProcess.waitFor()

            tempDir.deleteRecursively()

            if (exitCode == 0) {
                ExecutionResult.Success(exitCode, outputLines)
            } else {
                ExecutionResult.Failure(
                    ScriptError("Execution failed with exit code $exitCode"),
                    outputLines
                )
            }
        } catch (e: CancellationException) {
            currentProcess?.destroyForcibly()
            tempDir.deleteRecursively()
            ExecutionResult.Cancelled
        } catch (e: Exception) {
            tempDir.deleteRecursively()
            ExecutionResult.Failure(
                ScriptError("Error: ${e.message}"),
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

    override fun cancel() {
        executionJob?.cancel()
        currentProcess?.destroyForcibly()
    }

    override fun isRunning(): Boolean {
        return currentProcess?.isAlive == true
    }
}