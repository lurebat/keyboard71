package com.lurebat.keyboard71

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RopeTest {

    // ── insert ────────────────────────────────────────────────────────────────

    @Test
    fun `insert into empty rope`() {
        val rope = Rope()
        rope.insert(0, "hello")
        assertEquals(5, rope.length())
        assertEquals("hello", rope.get(0, 5).toString())
    }

    @Test
    fun `insert at end appends`() {
        val rope = Rope()
        rope.insert(0, "hello")
        rope.insert(5, " world")
        assertEquals(11, rope.length())
        assertEquals("hello world", rope.get(0, 11).toString())
    }

    @Test
    fun `insert in middle`() {
        val rope = Rope()
        rope.insert(0, "helo")
        rope.insert(3, "l")
        assertEquals(5, rope.length())
        assertEquals("hello", rope.get(0, 5).toString())
    }

    @Test
    fun `insert at beginning`() {
        val rope = Rope()
        rope.insert(0, "world")
        rope.insert(0, "hello ")
        assertEquals(11, rope.length())
        assertEquals("hello world", rope.get(0, 11).toString())
    }

    @Test
    fun `multiple inserts build correct string`() {
        val rope = Rope()
        rope.insert(0, "ac")
        rope.insert(1, "b")   // "abc"
        rope.insert(3, "de")  // "abcde"
        assertEquals(5, rope.length())
        assertEquals("abcde", rope.get(0, 5).toString())
    }

    // ── get ───────────────────────────────────────────────────────────────────

    @Test
    fun `get full range single leaf`() {
        val rope = Rope()
        rope.insert(0, "hello world")
        assertEquals("hello world", rope.get(0, 11).toString())
    }

    @Test
    fun `get partial range from single leaf`() {
        val rope = Rope()
        rope.insert(0, "hello world")
        assertEquals("hello", rope.get(0, 5).toString())
        assertEquals("world", rope.get(6, 11).toString())
        assertEquals("lo wo", rope.get(3, 8).toString())
    }

    @Test
    fun `get spanning multiple nodes`() {
        val rope = Rope()
        rope.insert(0, "hello")
        rope.insert(5, " ")
        rope.insert(6, "world")
        assertEquals("hello world", rope.get(0, 11).toString())
        assertEquals("lo wo", rope.get(3, 8).toString())
        assertEquals("ello ", rope.get(1, 6).toString())
    }

    @Test
    fun `get is non-destructive - same result twice`() {
        val rope = Rope()
        rope.insert(0, "hello world")
        val first = rope.get(0, 11).toString()
        val second = rope.get(0, 11).toString()
        assertEquals(first, second)
        assertEquals("hello world", first)
    }

    @Test
    fun `get is non-destructive - length unchanged after get`() {
        val rope = Rope()
        rope.insert(0, "hello world")
        rope.get(0, 11)
        rope.get(3, 8)
        assertEquals(11, rope.length())
    }

    @Test
    fun `get after multiple inserts is repeatable`() {
        val rope = Rope()
        rope.insert(0, "abcde")
        rope.insert(5, "fghij")
        val r1 = rope.get(2, 8).toString()
        val r2 = rope.get(2, 8).toString()
        assertEquals(r1, r2)
        assertEquals("cdefgh", r1)
    }

    @Test
    fun `get single character`() {
        val rope = Rope()
        rope.insert(0, "hello")
        assertEquals("e", rope.get(1, 2).toString())
        assertEquals("o", rope.get(4, 5).toString())
    }

    // ── length and weight ─────────────────────────────────────────────────────

    @Test
    fun `length after sequential inserts`() {
        val rope = Rope()
        assertEquals(0, rope.length())
        rope.insert(0, "abc")
        assertEquals(3, rope.length())
        rope.insert(3, "def")
        assertEquals(6, rope.length())
        rope.insert(1, "X")
        assertEquals(7, rope.length())
    }

    @Test
    fun `weight maintained after mid-string inserts`() {
        val rope = Rope()
        rope.insert(0, "hello")
        rope.insert(5, " world")
        rope.insert(5, ",")
        assertEquals(12, rope.length())
        assertEquals("hello, world", rope.get(0, 12).toString())
    }

    @Test
    fun `length equals get range`() {
        val rope = Rope()
        rope.insert(0, "abcdef")
        rope.insert(3, "XYZ")
        val len = rope.length()
        val content = rope.get(0, len).toString()
        assertEquals(len, content.length)
        assertEquals("abcXYZdef", content)
    }

    // ── edge / boundary ───────────────────────────────────────────────────────

    @Test
    fun `get on empty rope returns null`() {
        val rope = Rope()
        assertNull(rope.get(0, 1))
    }

    @Test
    fun `get with start equals end returns null`() {
        val rope = Rope()
        rope.insert(0, "hello")
        assertNull(rope.get(2, 2))
    }

    @Test
    fun `get clamped beyond end returns suffix`() {
        val rope = Rope()
        rope.insert(0, "hello")
        val result = rope.get(3, 100)
        assertNotNull(result)
        assertEquals("lo", result.toString())
    }

    @Test
    fun `get clamped before start returns null`() {
        val rope = Rope()
        rope.insert(0, "hello")
        assertNull(rope.get(-5, 0))
    }

    @Test
    fun `insert empty string is no-op`() {
        val rope = Rope()
        rope.insert(0, "hello")
        rope.insert(2, "")
        assertEquals(5, rope.length())
        assertEquals("hello", rope.get(0, 5).toString())
    }

    @Test
    fun `insert with negative index clamps to 0`() {
        val rope = Rope()
        rope.insert(-10, "hello")
        assertEquals(5, rope.length())
        assertEquals("hello", rope.get(0, 5).toString())
    }

    // ── unicode ───────────────────────────────────────────────────────────────

    @Test
    fun `insert and get with multi-byte unicode chars`() {
        val rope = Rope()
        rope.insert(0, "héllo")
        assertEquals(5, rope.length())
        assertEquals("héllo", rope.get(0, 5).toString())
        assertEquals("éll", rope.get(1, 4).toString())
    }

    @Test
    fun `insert emoji preserved across nodes`() {
        val rope = Rope()
        rope.insert(0, "hi")
        rope.insert(2, "\uD83D\uDE00") // U+1F600, 2 UTF-16 code units
        rope.insert(4, "!")
        assertEquals(5, rope.length())
        assertEquals("hi\uD83D\uDE00!", rope.get(0, 5).toString())
    }
}
