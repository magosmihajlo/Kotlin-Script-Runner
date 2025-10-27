package di

import data.executor.KotlinScriptExecutor
import data.file.TempScriptFileManager
import domain.repository.ScriptExecutor
import domain.repository.ScriptFileManager
import domain.usecase.ExecuteScriptUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import presentation.ScriptExecutionViewModel

object AppModule {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val fileManager: ScriptFileManager by lazy {
        TempScriptFileManager()
    }

    private val scriptExecutor: ScriptExecutor by lazy {
        KotlinScriptExecutor(fileManager)
    }

    private val executeScriptUseCase: ExecuteScriptUseCase by lazy {
        ExecuteScriptUseCase(scriptExecutor, fileManager)
    }

    val viewModel: ScriptExecutionViewModel by lazy {
        ScriptExecutionViewModel(executeScriptUseCase, applicationScope)
    }
}