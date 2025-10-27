package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import domain.model.OutputLine
import domain.model.OutputType
import ui.theme.CodeTextStyle
import ui.theme.OutputBackground

@Composable
fun OutputPanel(
    output: List<OutputLine>,
    onErrorLineClick: (line: Int, column: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(output.size) {
        if (output.isNotEmpty()) {
            listState.animateScrollToItem(output.size - 1)
        }
    }

    Box(
        modifier = modifier
            .background(OutputBackground)
            .padding(12.dp)
    ) {
        if (output.isEmpty()) {
            Text(
                text = "Output will appear here...",
                style = CodeTextStyle,
                color = Color.Gray,
                modifier = Modifier.padding(4.dp)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(output) { line ->
                    OutputLineItem(
                        line = line,
                        onErrorLineClick = onErrorLineClick
                    )
                }
            }
        }
    }
}

@Composable
private fun OutputLineItem(
    line: OutputLine,
    onErrorLineClick: (line: Int, column: Int) -> Unit
) {
    val errorRegex = """.*:(\d+):(\d+):\s*error:.*""".toRegex()
    val match = errorRegex.matchEntire(line.text)

    val textColor = when (line.type) {
        OutputType.STDOUT -> Color(0xFFA9B7C6)
        OutputType.STDERR -> Color(0xFFFF6B6B)
        OutputType.SYSTEM -> Color(0xFF64B5F6)
    }

    if (match != null && line.type == OutputType.STDERR) {
        val (lineNum, colNum) = match.destructured
        Text(
            text = line.text,
            style = CodeTextStyle.copy(textDecoration = TextDecoration.Underline),
            color = textColor,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onErrorLineClick(lineNum.toInt(), colNum.toInt())
                }
                .padding(vertical = 2.dp)
        )
    } else {
        Text(
            text = line.text,
            style = CodeTextStyle,
            color = textColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
        )
    }
}