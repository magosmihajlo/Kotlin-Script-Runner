package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import ui.theme.CodeTextColor
import ui.theme.CodeTextStyle
import ui.theme.EditorBackground

@Composable
fun CodeEditor(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(content)) }

    LaunchedEffect(content) {
        if (content != textFieldValue.text) {
            textFieldValue = TextFieldValue(content)
        }
    }

    Box(
        modifier = modifier
            .background(EditorBackground)
            .padding(12.dp)
    ) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                onContentChange(newValue.text)
            },
            modifier = Modifier.fillMaxSize(),
            textStyle = CodeTextStyle.copy(color = CodeTextColor),
            enabled = enabled,
            cursorBrush = SolidColor(CodeTextColor),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxSize()) {
                    innerTextField()
                }
            },
            visualTransformation = { text ->
                SyntaxHighlightTransformation.filter(text)
            }
        )
    }
}