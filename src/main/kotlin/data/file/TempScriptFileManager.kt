package data.file

import domain.repository.ScriptFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

class TempScriptFileManager : ScriptFileManager {

    override suspend fun createTempScriptFile(content: String): File = withContext(Dispatchers.IO) {
        try {
            val tempDir = createTempDir("kotlin_exec_", "")
            val scriptFile = File(tempDir, "Main.kts")

            if (!scriptFile.createNewFile()) {
                throw IOException("Failed to create script file")
            }

            scriptFile.writeText(content, StandardCharsets.UTF_8)
            scriptFile
        } catch (e: Exception) {
            throw IOException("Failed to create temp script file: ${e.message}", e)
        }
    }

    override suspend fun cleanup(file: File): Unit = withContext(Dispatchers.IO) {
        try {
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            System.err.println("Failed to cleanup file: ${e.message}")
        }
    }
}