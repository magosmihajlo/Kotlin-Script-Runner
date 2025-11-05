package presentation

import androidx.compose.ui.text.input.TextFieldValue
import domain.model.ExecutionStatus
import domain.model.OutputLine
import domain.model.OutputType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScriptExecutionStateTest {

    @Test
    fun `default state should be idle`() {
        val state = ScriptExecutionState()

        assertEquals(ExecutionStatus.IDLE, state.status)
        assertFalse(state.isRunning)
        assertFalse(state.hasError)
        assertEquals(StatusColor.IDLE, state.statusColor)
    }

    @Test
    fun `scriptContent should return text from textFieldValue`() {
        val text = "println(\"test\")"
        val state = ScriptExecutionState(
            textFieldValue = TextFieldValue(text)
        )

        assertEquals(text, state.scriptContent)
    }

    @Test
    fun `hasError should be true when exitCode is non-zero`() {
        val state = ScriptExecutionState(exitCode = 1)

        assertTrue(state.hasError)
    }

    @Test
    fun `hasError should be false when exitCode is zero`() {
        val state = ScriptExecutionState(exitCode = 0)

        assertFalse(state.hasError)
    }

    @Test
    fun `hasError should be false when exitCode is null`() {
        val state = ScriptExecutionState(exitCode = null)

        assertFalse(state.hasError)
    }

    @Test
    fun `statusColor should be RUNNING when isRunning is true`() {
        val state = ScriptExecutionState(
            isRunning = true,
            status = ExecutionStatus.RUNNING
        )

        assertEquals(StatusColor.RUNNING, state.statusColor)
    }

    @Test
    fun `statusColor should be ERROR when exitCode is non-zero`() {
        val state = ScriptExecutionState(
            exitCode = 1,
            isRunning = false
        )

        assertEquals(StatusColor.ERROR, state.statusColor)
    }

    @Test
    fun `statusColor should be SUCCESS when exitCode is zero`() {
        val state = ScriptExecutionState(
            exitCode = 0,
            isRunning = false
        )

        assertEquals(StatusColor.SUCCESS, state.statusColor)
    }

    @Test
    fun `statusColor should be IDLE by default`() {
        val state = ScriptExecutionState()

        assertEquals(StatusColor.IDLE, state.statusColor)
    }

    @Test
    fun `state should handle output list`() {
        val output = listOf(
            OutputLine("Line 1", OutputType.STDOUT),
            OutputLine("Line 2", OutputType.STDERR)
        )
        val state = ScriptExecutionState(output = output)

        assertEquals(2, state.output.size)
        assertEquals("Line 1", state.output[0].text)
        assertEquals(OutputType.STDERR, state.output[1].type)
    }

    @Test
    fun `state should handle error message`() {
        val errorMessage = "Compilation failed"
        val state = ScriptExecutionState(
            error = errorMessage,
            exitCode = 1
        )

        assertEquals(errorMessage, state.error)
        assertTrue(state.hasError)
    }
}