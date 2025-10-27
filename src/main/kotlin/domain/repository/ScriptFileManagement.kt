package domain.repository

import java.io.File

interface ScriptFileManager {
    suspend fun createTempScriptFile(content: String): File
    suspend fun cleanup(file: File)
}