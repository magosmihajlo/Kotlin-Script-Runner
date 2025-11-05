package presentation

import app.cash.turbine.test
import domain.model.*
import domain.usecase.ExecuteScriptUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ScriptExecutionViewModelTest {

    private lateinit var mockUseCase: ExecuteScriptUseCase
    private lateinit var viewModel: ScriptExecutionViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockUseCase = mockk()
        viewModel = ScriptExecutionViewModel(mockUseCase, testScope)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `initial state should be idle with default script`() = testScope.runTest {
        val state = viewModel.state.value

        assertEquals(ExecutionStatus.IDLE, state.status)
        assertFalse(state.isRunning)
        assertTrue(state.scriptContent.isNotEmpty())
        assertTrue(state.output.isEmpty())
    }

    @Test
    fun `executeScript should update state to running`() = testScope.runTest {
        coEvery {
            mockUseCase.execute(any(), any())
        } coAnswers {
            val callback = secondArg<suspend (OutputLine) -> Unit>()
            callback(OutputLine("Running...", OutputType.SYSTEM))
            ExecutionResult.Success(0, emptyList())
        }

        viewModel.executeScript()
        advanceUntilIdle()

        val finalState = viewModel.state.value
        assertEquals(ExecutionStatus.COMPLETED, finalState.status)
        assertEquals(0, finalState.exitCode)
        assertFalse(finalState.isRunning)
    }

    @Test
    fun `executeScript should not run if already running`() = testScope.runTest {
        coEvery {
            mockUseCase.execute(any(), any())
        } returns ExecutionResult.Success(0, emptyList())

        viewModel.executeScript()
        viewModel.executeScript()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockUseCase.execute(any(), any()) }
    }

    @Test
    fun `executeScript should handle success result`() = testScope.runTest {
        val output = listOf(OutputLine("Success", OutputType.STDOUT))
        coEvery {
            mockUseCase.execute(any(), any())
        } coAnswers {
            val callback = secondArg<suspend (OutputLine) -> Unit>()
            output.forEach { callback(it) }
            ExecutionResult.Success(0, output)
        }

        viewModel.executeScript()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ExecutionStatus.COMPLETED, state.status)
        assertEquals(0, state.exitCode)
        assertFalse(state.isRunning)
        assertTrue(state.output.isNotEmpty())
    }

    @Test
    fun `executeScript should handle failure result`() = testScope.runTest {
        val error = ScriptError("Compilation failed", line = 1, column = 5)
        coEvery {
            mockUseCase.execute(any(), any())
        } returns ExecutionResult.Failure(error, emptyList())

        viewModel.executeScript()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ExecutionStatus.FAILED, state.status)
        assertEquals(-1, state.exitCode)
        assertEquals("Compilation failed", state.error)
        assertFalse(state.isRunning)
    }

    @Test
    fun `executeScript should handle cancelled result`() = testScope.runTest {
        coEvery {
            mockUseCase.execute(any(), any())
        } returns ExecutionResult.Cancelled

        viewModel.executeScript()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ExecutionStatus.CANCELLED, state.status)
        assertFalse(state.isRunning)
    }

    @Test
    fun `executeScript should reject empty script`() = testScope.runTest {
        viewModel.updateTextFieldValue(
            androidx.compose.ui.text.input.TextFieldValue("")
        )

        viewModel.executeScript()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ExecutionStatus.FAILED, state.status)
        assertEquals("Cannot execute empty script", state.error)
        assertEquals(-1, state.exitCode)
    }

    @Test
    fun `stopExecution should call cancelExecution`() = testScope.runTest {
        every { mockUseCase.cancelExecution() } just Runs

        viewModel.stopExecution()
        advanceUntilIdle()

        verify(exactly = 1) { mockUseCase.cancelExecution() }
        val state = viewModel.state.value
        assertEquals(ExecutionStatus.CANCELLED, state.status)
        assertFalse(state.isRunning)
    }

    @Test
    fun `clearOutput should reset output and status`() = testScope.runTest {
        coEvery {
            mockUseCase.execute(any(), any())
        } returns ExecutionResult.Success(0, listOf(OutputLine("test", OutputType.STDOUT)))

        viewModel.executeScript()
        advanceUntilIdle()

        viewModel.clearOutput()

        val state = viewModel.state.value
        assertTrue(state.output.isEmpty())
        assertEquals(ExecutionStatus.IDLE, state.status)
        assertEquals(null, state.exitCode)
        assertEquals(null, state.error)
    }

    @Test
    fun `output should be limited to maxOutputLines`() = testScope.runTest {
        val manyLines = (1..1500).map {
            OutputLine("Line $it", OutputType.STDOUT)
        }

        coEvery {
            mockUseCase.execute(any(), any())
        } coAnswers {
            val callback = secondArg<suspend (OutputLine) -> Unit>()
            manyLines.forEach { callback(it) }
            ExecutionResult.Success(0, manyLines)
        }

        viewModel.executeScript()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1000, state.output.size)
        assertEquals("Line 501", state.output.first().text)
        assertEquals("Line 1500", state.output.last().text)
    }

    @Test
    fun `navigateToLine should update textFieldValue selection`() = testScope.runTest {
        val scriptContent = "line1\nline2\nline3\n"
        viewModel.updateTextFieldValue(
            androidx.compose.ui.text.input.TextFieldValue(scriptContent)
        )

        viewModel.navigateToLine(line = 2, column = 3)
        advanceUntilIdle()

        val state = viewModel.state.value
        val expectedOffset = 6 + 2
        assertEquals(expectedOffset, state.textFieldValue.selection.start)
    }

    @Test
    fun `navigateToLine should handle out of bounds line`() = testScope.runTest {
        val scriptContent = "line1\nline2\n"
        viewModel.updateTextFieldValue(
            androidx.compose.ui.text.input.TextFieldValue(scriptContent)
        )

        val initialSelection = viewModel.state.value.textFieldValue.selection

        viewModel.navigateToLine(line = 100, column = 1)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(initialSelection, state.textFieldValue.selection)
    }
}