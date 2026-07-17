package com.willdeep.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

internal sealed interface MdBlock {
    data class Paragraph(val text: String) : MdBlock
    data class Heading(val level: Int, val text: String) : MdBlock
    data class CodeBlock(val lang: String?, val code: String) : MdBlock
    data class BulletList(val items: List<String>) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Image(val alt: String, val url: String) : MdBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MdBlock
}

@Composable
internal fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    val blocks = remember(text) { parseMarkdownBlocks(text) }
    val codeBg = MaterialTheme.colorScheme.surfaceVariant
    val linkColor = MaterialTheme.colorScheme.primary
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.CodeBlock -> CodeBlockView(block.lang, block.code)
                is MdBlock.Image -> MarkdownImage(block.alt, block.url)
                is MdBlock.Table -> MarkdownTable(block, codeBg, linkColor, style, color)
                is MdBlock.Heading -> Text(
                    text = parseInline(block.text, codeBg, linkColor),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    },
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
                is MdBlock.BulletList -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    block.items.forEach { item ->
                        Row {
                            Text("•  ", style = style, color = color)
                            Text(
                                text = parseInline(item, codeBg, linkColor),
                                style = style,
                                color = color,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                is MdBlock.Quote -> Surface(
                    color = codeBg.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = parseInline(block.text, codeBg, linkColor),
                        style = style.copy(fontStyle = FontStyle.Italic),
                        color = color,
                        modifier = Modifier.padding(10.dp),
                    )
                }
                is MdBlock.Paragraph -> Text(
                    text = parseInline(block.text, codeBg, linkColor),
                    style = style,
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun MarkdownImage(alt: String, url: String) {
    AsyncImage(
        model = url,
        contentDescription = alt.ifBlank { null },
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp)
            .clip(MaterialTheme.shapes.small),
    )
}

@Composable
private fun CodeBlockView(lang: String?, code: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (!lang.isNullOrBlank()) {
                Text(
                    text = lang,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Text(
                text = code,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun MarkdownTable(
    table: MdBlock.Table,
    codeBg: Color,
    linkColor: Color,
    style: TextStyle,
    color: Color,
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    Surface(
        color = Color.Transparent,
        shape = MaterialTheme.shapes.extraSmall,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        Column {
            TableRow(
                cells = table.headers,
                codeBg = codeBg,
                linkColor = linkColor,
                style = style,
                color = color,
                fontWeight = FontWeight.SemiBold,
            )
            table.rows.forEach { row ->
                TableRow(
                    cells = row,
                    codeBg = codeBg,
                    linkColor = linkColor,
                    style = style,
                    color = color,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun TableRow(
    cells: List<String>,
    codeBg: Color,
    linkColor: Color,
    style: TextStyle,
    color: Color,
    fontWeight: FontWeight,
) {
    Row {
        cells.forEach { cell ->
            Surface(
                color = Color.Transparent,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(
                    text = parseInline(cell, codeBg, linkColor),
                    style = style.copy(fontWeight = fontWeight),
                    color = color,
                    modifier = Modifier
                        .widthIn(min = 96.dp, max = 220.dp)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
    }
}

internal fun parseMarkdownBlocks(input: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = input.lineSequence().toList()
    var i = 0
    val para = StringBuilder()
    val listItems = mutableListOf<String>()

    fun flushPara() {
        if (para.isNotEmpty()) {
            blocks.add(MdBlock.Paragraph(para.toString().trimEnd()))
            para.clear()
        }
    }
    fun flushList() {
        if (listItems.isNotEmpty()) {
            blocks.add(MdBlock.BulletList(listItems.toList()))
            listItems.clear()
        }
    }

    val headingRegex = Regex("""^#{1,6}\s+""")
    val listRegex = Regex("""^[-*+]\s+""")
    val imageRegex = Regex("""^!\[([^]]*)]\((\S+?)(?:\s+"[^"]*")?\)$""")

    while (i < lines.size) {
        val line = lines[i]
        val trimmedLine = line.trim()
        val imageMatch = imageRegex.matchEntire(trimmedLine)
        when {
            line.trimStart().startsWith("```") -> {
                flushPara(); flushList()
                val fence = line.trimStart()
                val lang = fence.removePrefix("```").trim().ifBlank { null }
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                blocks.add(MdBlock.CodeBlock(lang, codeLines.joinToString("\n")))
                if (i < lines.size) i++
            }
            imageMatch != null -> {
                flushPara(); flushList()
                blocks.add(
                    MdBlock.Image(
                        alt = imageMatch.groupValues[1].trim(),
                        url = imageMatch.groupValues[2].trim(),
                    ),
                )
                i++
            }
            isTableHeaderAt(lines, i) -> {
                flushPara(); flushList()
                val headers = splitTableRow(lines[i])
                val rows = mutableListOf<List<String>>()
                i += 2
                while (i < lines.size && isTableRow(lines[i])) {
                    rows.add(normalizeTableRow(splitTableRow(lines[i]), headers.size))
                    i++
                }
                blocks.add(MdBlock.Table(headers, rows))
            }
            headingRegex.containsMatchIn(line) -> {
                flushPara(); flushList()
                val hashes = line.takeWhile { it == '#' }.length
                blocks.add(MdBlock.Heading(hashes, line.drop(hashes).trim()))
                i++
            }
            listRegex.containsMatchIn(line) -> {
                flushPara()
                listItems.add(line.replaceFirst(listRegex, ""))
                i++
            }
            line.startsWith(">") -> {
                flushPara(); flushList()
                blocks.add(MdBlock.Quote(line.removePrefix(">").trim()))
                i++
            }
            line.isBlank() -> {
                flushPara(); flushList()
                i++
            }
            else -> {
                flushList()
                if (para.isNotEmpty()) para.append("\n")
                para.append(line)
                i++
            }
        }
    }
    flushPara(); flushList()
    return blocks
}

private fun isTableHeaderAt(lines: List<String>, index: Int): Boolean {
    return index + 1 < lines.size &&
        isTableRow(lines[index]) &&
        isTableSeparator(lines[index + 1])
}

private fun isTableRow(line: String): Boolean {
    return splitTableRow(line).size >= 2
}

private fun isTableSeparator(line: String): Boolean {
    val cells = splitTableRow(line)
    return cells.size >= 2 && cells.all { cell ->
        cell.matches(Regex(""":?-{3,}:?"""))
    }
}

private fun splitTableRow(line: String): List<String> {
    val trimmed = line.trim()
    if (!trimmed.contains("|")) return emptyList()
    val body = trimmed
        .removePrefix("|")
        .removeSuffix("|")
    return body.split("|").map { it.trim() }
}

private fun normalizeTableRow(row: List<String>, columnCount: Int): List<String> {
    return when {
        row.size < columnCount -> row + List(columnCount - row.size) { "" }
        row.size > columnCount -> row.take(columnCount)
        else -> row
    }
}

internal fun parseInline(text: String, codeBg: Color, linkColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '*' && i + 1 < text.length && text[i + 1] == '*' -> {
                    val end = text.indexOf("**", i + 2)
                    if (end >= 0) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(text.substring(i + 2, end))
                        pop()
                        i = end + 2
                    } else {
                        append(c); i++
                    }
                }
                (c == '*' || c == '_') &&
                    i + 1 < text.length &&
                    text[i + 1] != c &&
                    !text[i + 1].isWhitespace() -> {
                    val end = text.indexOf(c, i + 1)
                    if (end > i + 1 && !text[end - 1].isWhitespace()) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(text.substring(i + 1, end))
                        pop()
                        i = end + 1
                    } else {
                        append(c); i++
                    }
                }
                c == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i) {
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = codeBg,
                            ),
                        )
                        append(text.substring(i + 1, end))
                        pop()
                        i = end + 1
                    } else {
                        append(c); i++
                    }
                }
                c == '[' -> {
                    val close = text.indexOf(']', i + 1)
                    if (close > 0 && close + 1 < text.length && text[close + 1] == '(') {
                        val urlEnd = text.indexOf(')', close + 2)
                        if (urlEnd > 0) {
                            pushStyle(
                                SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline,
                                ),
                            )
                            append(text.substring(i + 1, close))
                            pop()
                            i = urlEnd + 1
                        } else {
                            append(c); i++
                        }
                    } else {
                        append(c); i++
                    }
                }
                else -> {
                    append(c); i++
                }
            }
        }
    }
}
