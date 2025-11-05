package data.file

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TempScriptFileManagerTest {

    private val fileManager = TempScriptFileManager()
    private val createdFiles = mutableListOf<File>()

    @AfterEach
    fun cleanup() {
        createdFiles.forEach { file ->
            file.parentFile?.deleteRecursively()
        }
        createdFiles.clear()
    }

    @Test
    fun `createTempScriptFile should create file with correct content`() = runTest {
        val content = "println(\"Hello, World!\")"

        val file = fileManager.createTempScriptFile(content)
        createdFiles.add(file)

        assertTrue(file.exists())
        assertEquals("Main.kts", file.name)
        assertEquals(content, file.readText())
    }

    @Test
    fun `createTempScriptFile should create file in temp directory`() = runTest {
        val content = "println(\"test\")"

        val file = fileManager.createTempScriptFile(content)
        createdFiles.add(file)

        assertTrue(file.parentFile.name.startsWith("kotlin_exec_"))
        assertTrue(file.absolutePath.contains(System.getProperty("java.io.tmpdir")))
    }

    @Test
    fun `createTempScriptFile should handle empty content`() = runTest {
        val content = ""

        val file = fileManager.createTempScriptFile(content)
        createdFiles.add(file)

        assertTrue(file.exists())
        assertEquals("", file.readText())
    }

    @Test
    fun `createTempScriptFile should handle multiline content`() = runTest {
        val content = """
            println("Line 1")
            println("Line 2")
            println("Line 3")
        """.trimIndent()

        val file = fileManager.createTempScriptFile(content)
        createdFiles.add(file)

        assertTrue(file.exists())
        assertEquals(content, file.readText())
    }

    @Test
    fun `cleanup should delete existing file`() = runTest {
        val content = "println(\"cleanup test\")"
        val file = fileManager.createTempScriptFile(content)
        assertTrue(file.exists())

        fileManager.cleanup(file)

        assertFalse(file.exists())
    }

    @Test
    fun `cleanup should not throw when file does not exist`() = runTest {
        val nonExistentFile = File("nonexistent_file_12345.kts")

        fileManager.cleanup(nonExistentFile)
    }

    @Test
    fun `createTempScriptFile should handle special characters`() = runTest {
        val content = "println(\"Hello, 世界! 🌍\")"

        val file = fileManager.createTempScriptFile(content)
        createdFiles.add(file)

        assertTrue(file.exists())
        assertEquals(content, file.readText())
    }
}