package com.willdeep.android.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTextTest {
    @Test
    fun parseMarkdownBlocksSupportsCodeImagesAndSimpleTables() {
        val blocks = parseMarkdownBlocks(
            """
            ```kotlin
            val answer = 42
            ```

            ![Chart](https://example.com/chart.png)

            | Name | Value |
            | --- | ---: |
            | Foo | 42 |
            """.trimIndent(),
        )

        assertTrue(blocks[0] is MdBlock.CodeBlock)
        assertEquals("kotlin", (blocks[0] as MdBlock.CodeBlock).lang)
        assertTrue(blocks[1] is MdBlock.Image)
        assertEquals("Chart", (blocks[1] as MdBlock.Image).alt)
        assertTrue(blocks[2] is MdBlock.Table)
        val table = blocks[2] as MdBlock.Table
        assertEquals(listOf("Name", "Value"), table.headers)
        assertEquals(listOf(listOf("Foo", "42")), table.rows)
    }

    @Test
    fun parseMarkdownBlocksPadsShortTableRows() {
        val blocks = parseMarkdownBlocks(
            """
            | A | B | C |
            | --- | --- | --- |
            | 1 | 2 |
            """.trimIndent(),
        )

        val table = blocks.single() as MdBlock.Table
        assertEquals(listOf("1", "2", ""), table.rows.single())
    }
}
