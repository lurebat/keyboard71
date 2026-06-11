package com.lurebat.keyboard71

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LazyStringRopeTest {
    private class DocumentRefresher(private val document: String, var cursor: Int, var selectionEnd: Int = cursor) : Refresher {
        var beforeRequests = 0
        var afterRequests = 0
        var selectionRequests = 0

        override fun beforeCursor(count: Int): CharSequence {
            beforeRequests++
            val start = (cursor - count).coerceAtLeast(0)
            return document.substring(start, cursor)
        }

        override fun afterCursor(count: Int): CharSequence {
            afterRequests++
            val end = (selectionEnd + count).coerceAtMost(document.length)
            return document.substring(selectionEnd, end)
        }

        override fun atCursor(): CharSequence {
            selectionRequests++
            return document.substring(cursor, selectionEnd)
        }
    }

    @Test
    fun `large document initial window stays aligned to absolute cursor`() {
        val document = buildString {
            repeat(2000) { append(('a'.code + (it % 26)).toChar()) }
        }
        val cursor = 1500
        val refresher = DocumentRefresher(document, cursor)
        val lazy = LazyStringRope(
            SimpleCursor(cursor, cursor),
            SimpleCursor(-1, -1),
            document.substring(cursor - 1000, cursor),
            "",
            document.substring(cursor, cursor + 100),
            refresher
        )

        assertEquals(document.substring(cursor - 12, cursor), lazy.getCharsBeforeCursor(12).toString())
        assertEquals(0, refresher.beforeRequests)
        assertEquals(document.substring(cursor, cursor + 12), lazy.getCharsAfterCursor(12).toString())
        assertEquals(0, refresher.afterRequests)
    }

    @Test
    fun `jump outside cached window fetches fresh context instead of reusing clamped stale text`() {
        val document = buildString {
            repeat(2000) { append(('a'.code + (it % 26)).toChar()) }
        }
        val initialCursor = 1500
        val refresher = DocumentRefresher(document, initialCursor)
        val lazy = LazyStringRope(
            SimpleCursor(initialCursor, initialCursor),
            SimpleCursor(-1, -1),
            document.substring(initialCursor - 1000, initialCursor),
            "",
            document.substring(initialCursor, initialCursor + 100),
            refresher
        )

        val jumpedCursor = 100
        refresher.cursor = jumpedCursor
        refresher.selectionEnd = jumpedCursor
        lazy.setSelection(jumpedCursor, jumpedCursor)

        assertEquals(document.substring(jumpedCursor - 8, jumpedCursor), lazy.getCharsBeforeCursor(8).toString())
        assertEquals(1, refresher.beforeRequests)
        assertEquals(document.substring(jumpedCursor, jumpedCursor + 8), lazy.getCharsAfterCursor(8).toString())
        assertEquals(1, refresher.afterRequests)
    }

    @Test
    fun `edits before cached window shift absolute cache start`() {
        val document = "abcdefghijklmnopqrstuvwxyz"
        val cursor = 20
        val refresher = DocumentRefresher(document, cursor)
        val lazy = LazyStringRope(
            SimpleCursor(cursor, cursor),
            SimpleCursor(-1, -1),
            document.substring(10, cursor),
            "",
            document.substring(cursor),
            refresher
        )

        lazy.delete(0, 5)
        lazy.setSelection(cursor - 5, cursor - 5)
        refresher.cursor = cursor - 5
        refresher.selectionEnd = cursor - 5

        assertEquals(document.substring(10, cursor).takeLast(5), lazy.getCharsBeforeCursor(5).toString())
    }
}
