package ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import ui.theme.CodeTextColor
import ui.theme.KeywordColor
import ui.theme.StringColor
import ui.theme.CommentColor

object SyntaxHighlighter {

    private val keywords = setOf(
        "package", "import", "class", "interface", "object", "fun", "val", "var",
        "if", "else", "when", "for", "while", "do", "return", "break", "continue",
        "throw", "try", "catch", "finally", "public", "private", "protected", "internal",
        "abstract", "open", "override", "final", "companion", "data", "sealed",
        "suspend", "inline", "infix", "operator", "tailrec", "external", "annotation",
        "enum", "init", "constructor", "this", "super", "null", "true", "false",
        "is", "in", "as", "typeof", "get", "set"
    )

    fun highlight(code: String): AnnotatedString = buildAnnotatedString {
        var i = 0
        while (i < code.length) {
            when {
                code.startsWith("//", i) -> {
                    val end = code.indexOf('\n', i).let { if (it == -1) code.length else it }
                    withStyle(SpanStyle(color = CommentColor)) {
                        append(code.substring(i, end))
                    }
                    i = end
                }

                code.startsWith("/*", i) -> {
                    val end = code.indexOf("*/", i + 2).let { if (it == -1) code.length else it + 2 }
                    withStyle(SpanStyle(color = CommentColor)) {
                        append(code.substring(i, end))
                    }
                    i = end
                }

                code[i] == '"' -> {
                    val end = findStringEnd(code, i + 1, '"')
                    withStyle(SpanStyle(color = StringColor)) {
                        append(code.substring(i, end))
                    }
                    i = end
                }

                code[i].isLetter() || code[i] == '_' -> {
                    val start = i
                    while (i < code.length && (code[i].isLetterOrDigit() || code[i] == '_')) {
                        i++
                    }
                    val word = code.substring(start, i)
                    if (word in keywords) {
                        withStyle(SpanStyle(color = KeywordColor)) {
                            append(word)
                        }
                    } else {
                        withStyle(SpanStyle(color = CodeTextColor)) {
                            append(word)
                        }
                    }
                }

                else -> {
                    withStyle(SpanStyle(color = CodeTextColor)) {
                        append(code[i])
                    }
                    i++
                }
            }
        }
    }

    private fun findStringEnd(code: String, start: Int, quote: Char): Int {
        var i = start
        while (i < code.length) {
            when {
                code[i] == '\\' -> i += 2
                code[i] == quote -> return i + 1
                else -> i++
            }
        }
        return code.length
    }
}