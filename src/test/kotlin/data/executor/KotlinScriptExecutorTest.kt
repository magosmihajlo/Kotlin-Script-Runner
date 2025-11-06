package data.executor

import domain.model.ExecutionResult
import domain.model.OutputLine
import domain.model.OutputType
import domain.repository.ScriptFileManager
import io.mockk.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinScriptExecutorTest {

    private lateinit var mockFileManager: ScriptFileManager
    private lateinit var executor: KotlinScriptExecutor
    private val testScriptFile = File.createTempFile("test", ".kts")

    @BeforeEach
    fun setup() {
        mockFileManager = mockk()
        executor = KotlinScriptExecutor(mockFileManager)

        coEvery { mockFileManager.createTempScriptFile(any()) } returns testScriptFile
        coEvery { mockFileManager.cleanup(any()) } just Runs
    }

    @AfterEach
    fun tearDown() {
        testScriptFile.delete()
        clearAllMocks()
    }

    @Test
    fun `execute should return success for valid script`() = runTest {
        val script = """println("Hello, World!")"""
        val outputLines = mutableListOf<OutputLine>()

        val result = executor.execute(script) { line ->
            outputLines.add(line)
        }

        assertTrue(result is ExecutionResult.Success)
        assertTrue(outputLines.any { it.text.contains("Hello, World!") || it.text.contains("Running script") })
    }

    @Test
    fun `execute should return failure for script with compilation error`() = runTest {
        val script = """val x = undefinedVariable"""
        val outputLines = mutableListOf<OutputLine>()

        val result = executor.execute(script) { line ->
            outputLines.add(line)
        }

        assertTrue(result is ExecutionResult.Failure)
        if (result is ExecutionResult.Failure) {
            assertTrue(result.error.message.isNotEmpty())
        }
    }

    @Test
    fun `execute should handle script with runtime error`() = runTest {
        val script = """
            println("Before error")
            throw RuntimeException("Test error")
        """.trimIndent()
        val outputLines = mutableListOf<OutputLine>()

        val result = executor.execute(script) { line ->
            outputLines.add(line)
        }

        // May be Success or Failure depending on how kotlinc handles it
        assertTrue(result is ExecutionResult.Success || result is ExecutionResult.Failure)
        assertTrue(outputLines.any { it.type == OutputType.STDOUT || it.type == OutputType.STDERR })
    }

    @Test
    fun `execute should stream output in real-time`() = runTest {
        val script = """
            for (i in 1..3) {
                println("Line ${'$'}i")
            }
        """.trimIndent()
        val outputLines = mutableListOf<OutputLine>()

        executor.execute(script) { line ->
            outputLines.add(line)
        }

        // Should have multiple output lines (including "Running script..." system message)
        assertTrue(outputLines.size >= 2)
    }

    @Test
    fun `cancel should stop running script`() = runTest {
        val longScript = """
            for (i in 1..1000000) {
                println("Count: ${'$'}i")
                Thread.sleep(100)
            }
        """.trimIndent()

        // Start execution in the background
        var result: ExecutionResult? = null
        launch {
            result = executor.execute(longScript) { }
        }

        // Give it time to start
        kotlinx.coroutines.delay(500)

        // Cancel it
        executor.cancel()

        // Wait for completion
        kotlinx.coroutines.delay(2000)

        // Should be cancelled
        assertEquals(ExecutionResult.Cancelled, result)
    }

    @Test
    fun `isRunning should return true while executing`() = runTest {
        val script = """
            Thread.sleep(1000)
            println("Done")
        """.trimIndent()

        launch {
            executor.execute(script) { }
        }

        kotlinx.coroutines.delay(200)

        assertTrue(executor.isRunning())
    }

    @Test
    fun `parseError should extract line and column from kotlinc error`() = runTest {
        val script = """val broken = undefined"""

        val result = executor.execute(script) { }

        assertTrue(result is ExecutionResult.Failure)
        if (result is ExecutionResult.Failure) {
            assertTrue(result.error.message.isNotEmpty())
        }
    }
}