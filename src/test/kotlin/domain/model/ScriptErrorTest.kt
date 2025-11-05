package domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScriptErrorTest {

    @Test
    fun `ScriptError with line and column should have location`() {
        val error = ScriptError(
            message = "Unresolved reference: foo",
            line = 5,
            column = 10
        )

        assertTrue(error.hasLocation)
        assertEquals(5, error.line)
        assertEquals(10, error.column)
    }

    @Test
    fun `ScriptError without line should not have location`() {
        val error = ScriptError(
            message = "Compilation failed",
            line = null,
            column = 10
        )

        assertFalse(error.hasLocation)
    }

    @Test
    fun `ScriptError without column should not have location`() {
        val error = ScriptError(
            message = "Compilation failed",
            line = 5,
            column = null
        )

        assertFalse(error.hasLocation)
    }

    @Test
    fun `ScriptError with neither line nor column should not have location`() {
        val error = ScriptError(
            message = "General error",
            line = null,
            column = null
        )

        assertFalse(error.hasLocation)
    }
}