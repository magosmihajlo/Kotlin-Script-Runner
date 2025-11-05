package domain.usecase

import domain.model.ExecutionResult
import domain.model.OutputLine
import domain.model.OutputType
import domain.model.ScriptError
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExecutionResultTest {

    @Test
    fun `Success result should contain exit code and output`() {
        val output = listOf(
            OutputLine("Hello", OutputType.STDOUT),
            OutputLine("World", OutputType.STDOUT)
        )
        val result = ExecutionResult.Success(exitCode = 0, output = output)

        assertEquals(0, result.exitCode)
        assertEquals(2, result.output.size)
        assertEquals("Hello", result.output[0].text)
    }

    @Test
    fun `Failure result should contain error and output`() {
        val error = ScriptError("Compilation failed", line = 1, column = 5)
        val output = listOf(OutputLine("Error occurred", OutputType.STDERR))
        val result = ExecutionResult.Failure(error = error, output = output)

        assertEquals("Compilation failed", result.error.message)
        assertEquals(1, result.output.size)
    }

    @Test
    fun `Cancelled result should be singleton`() {
        val result1 = ExecutionResult.Cancelled
        val result2 = ExecutionResult.Cancelled

        assertTrue(result1 === result2)
    }
}