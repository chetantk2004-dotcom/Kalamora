package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.literaryFontFor

enum class RichFormatAction {
    BOLD, ITALIC, UNDERLINE, HEADING, PARAGRAPH, BULLET_LIST, NUMBERED_LIST, QUOTE, DIVIDER
}

object RichTextEditorOps {
    fun applyFormat(value: TextFieldValue, action: RichFormatAction): TextFieldValue {
        val text = value.text
        val start = value.selection.min.coerceIn(0, text.length)
        val end = value.selection.max.coerceIn(0, text.length)
        val selected = text.substring(start, end)

        return when (action) {
            RichFormatAction.BOLD -> wrapSelection(text, start, end, selected, "**", "**", "bold text")
            RichFormatAction.ITALIC -> wrapSelection(text, start, end, selected, "*", "*", "italic text")
            RichFormatAction.UNDERLINE -> wrapSelection(text, start, end, selected, "__", "__", "underlined")
            RichFormatAction.HEADING -> prefixLine(text, start, "## ")
            RichFormatAction.PARAGRAPH -> {
                val clean = stripLinePrefix(text, start)
                TextFieldValue(clean.first, TextRange(clean.second))
            }
            RichFormatAction.BULLET_LIST -> prefixLine(text, start, "- ")
            RichFormatAction.NUMBERED_LIST -> prefixLine(text, start, "1. ")
            RichFormatAction.QUOTE -> prefixLine(text, start, "> ")
            RichFormatAction.DIVIDER -> {
                val insertion = if (start > 0 && text[start - 1] != '\n') "\n\n---\n\n" else "\n---\n\n"
                val newText = text.substring(0, start) + insertion + text.substring(end)
                val cursor = start + insertion.length
                TextFieldValue(newText, TextRange(cursor))
            }
        }
    }

    private fun wrapSelection(
        text: String,
        start: Int,
        end: Int,
        selected: String,
        prefix: String,
        suffix: String,
        placeholder: String
    ): TextFieldValue {
        val inner = selected.ifEmpty { placeholder }
        val replacement = "$prefix$inner$suffix"
        val newText = text.substring(0, start) + replacement + text.substring(end)
        val newSelStart = start + prefix.length
        val newSelEnd = newSelStart + inner.length
        return TextFieldValue(newText, TextRange(newSelStart, newSelEnd))
    }

    private fun prefixLine(text: String, cursor: Int, prefix: String): TextFieldValue {
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
        return TextFieldValue(newText, TextRange(cursor + prefix.length))
    }

    private fun stripLinePrefix(text: String, cursor: Int): Pair<String, Int> {
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', lineStart).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)
        val stripped = line.replace(Regex("^(#{1,3}\\s+|>\\s+|-\\s+|\\d+\\.\\s+)"), "")
        val delta = line.length - stripped.length
        val newText = text.substring(0, lineStart) + stripped + text.substring(lineEnd)
        return Pair(newText, (cursor - delta).coerceAtLeast(lineStart))
    }
}

@Composable
fun LiteraryRichTextView(
    content: String,
    languageHint: String = "",
    fontSize: TextUnit = 17.sp,
    textAlign: TextAlign = TextAlign.Start,
    modifier: Modifier = Modifier
) {
    val fontFamily: FontFamily = literaryFontFor(content, languageHint)
    val lines = content.split("\n")

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        lines.forEach { rawLine ->
            val trimmed = rawLine.trim()
            when {
                trimmed == "---" || trimmed == "***" -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f),
                        thickness = 1.dp
                    )
                }
                trimmed.startsWith("# ") -> {
                    Text(
                        text = parseInlineStyles(trimmed.removePrefix("# ")),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = fontFamily,
                            fontSize = (fontSize.value + 7f).sp,
                            lineHeight = (fontSize.value + 15f).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = textAlign,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = parseInlineStyles(trimmed.removePrefix("## ")),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = fontFamily,
                            fontSize = (fontSize.value + 4f).sp,
                            lineHeight = (fontSize.value + 12f).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = textAlign,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
                trimmed.startsWith("> ") -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(24.dp)
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = parseInlineStyles(trimmed.removePrefix("> ")),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = fontFamily,
                                fontStyle = FontStyle.Italic,
                                fontSize = fontSize,
                                lineHeight = (fontSize.value * 1.6f).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                trimmed.startsWith("- ") || trimmed.startsWith("• ") -> {
                    val body = trimmed.removePrefix("- ").removePrefix("• ")
                    Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp)) {
                        Text(
                            text = "•  ",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        )
                        Text(
                            text = parseInlineStyles(body),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = fontFamily,
                                fontSize = fontSize,
                                lineHeight = (fontSize.value * 1.65f).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                trimmed.isEmpty() -> {
                    Spacer(modifier = Modifier.height(6.dp))
                }
                else -> {
                    Text(
                        text = parseInlineStyles(rawLine),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = fontFamily,
                            fontSize = fontSize,
                            lineHeight = (fontSize.value * 1.7f).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = textAlign,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private val inlineTokenRegex = Regex("(\\*\\*[^*]+\\*\\*|__[^_]+__|\\*[^*]+\\*)")

fun parseInlineStyles(line: String): AnnotatedString = buildAnnotatedString {
    var lastIndex = 0
    for (match in inlineTokenRegex.findAll(line)) {
        if (match.range.first > lastIndex) {
            append(line.substring(lastIndex, match.range.first))
        }
        val token = match.value
        when {
            token.startsWith("**") && token.endsWith("**") && token.length > 4 -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(token.substring(2, token.length - 2))
                }
            }
            token.startsWith("__") && token.endsWith("__") && token.length > 4 -> {
                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                    append(token.substring(2, token.length - 2))
                }
            }
            token.startsWith("*") && token.endsWith("*") && token.length > 2 -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(token.substring(1, token.length - 1))
                }
            }
            else -> append(token)
        }
        lastIndex = match.range.last + 1
    }
    if (lastIndex < line.length) {
        append(line.substring(lastIndex))
    }
}
