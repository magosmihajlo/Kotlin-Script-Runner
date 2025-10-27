import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import di.AppModule
import ui.screens.MainScreen
import ui.theme.AppTypography
import ui.theme.DarkColorScheme

fun main() = application {
    val windowState = rememberWindowState(
        width = 1400.dp,
        height = 900.dp
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "Kotlin Script Runner",
        state = windowState
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = AppTypography
        ) {
            MainScreen(
                viewModel = AppModule.viewModel
            )
        }
    }
}