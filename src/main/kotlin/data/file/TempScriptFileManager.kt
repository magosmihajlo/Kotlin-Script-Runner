package data.file

import domain.repository.ScriptFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.StandardCharsets

class TempScriptFileManager : ScriptFileManager {

    override suspend fun createTempScriptFile(content: String): File = withContext(Dispatchers.IO) {
        val tempDir = createTempDir("kotlin_exec_", "")
        val scriptFile = File(tempDir, "Main.kts")
        scriptFile.writeText(content, StandardCharsets.UTF_8)
        scriptFile
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