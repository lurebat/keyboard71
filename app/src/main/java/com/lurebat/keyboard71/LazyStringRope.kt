package com.lurebat.keyboard71

import android.view.inputmethod.InputConnection
import java.text.BreakIterator
import kotlin.math.abs

interface Cursor {
    var start: Int
    var end: Int
    var min: Int
    var max: Int
    var rangeInclusive: IntRange
    var rangeExclusive: IntRange
    fun refresh()
    fun set(start: Int, end: Int)
    fun move(deltaStart: Int, deltaEnd: Int)

    fun isEmpty(): Boolean {
        return start == end
    }

    fun isNotEmpty(): Boolean {
        return !isEmpty()
    }

    fun length(): Int {
        return max - min
    }

    fun addLength(index: Int, count: Int) {
        if (index < min) {
            start += count
            end += count
        } else if (index < max) {
            if (end == max) {
                end += count
            } else {
                start += count
            }
        }
        refresh()
    }

    fun deleteLength(index: Int, count: Int) {
        if (index < min) {
            start -= count
            end -= count
        } else if (index < max) {
            if (end == max) {
                end -= count
            } else {
                start -= count
            }
        }
        refresh()
    }

    fun isNegative(): Boolean {
        return start < 0 && end < 0
    }

    fun zeroIfNegative(): Cursor {
        if (start < 0) {
            start = 0
        }
        if (end < 0) {
            end = 0
        }
        refresh()
        return this
    }
}

interface Refresher {
    fun beforeCursor(count: Int): CharSequence?
    fun afterCursor(count: Int): CharSequence?
    fun atCursor(): CharSequence?
}

class InputConnectionRefresher(val inputConnection: () -> InputConnection?) : Refresher {
    override fun beforeCursor(count: Int): CharSequence? {
        return inputConnection()?.getTextBeforeCursor(count, 0)
    }

    override fun afterCursor(count: Int): CharSequence? {
        return inputConnection()?.getTextAfterCursor(count, 0)
    }

    override fun atCursor(): CharSequence? {
        return inputConnection()?.getSelectedText(0)
    }

}

interface LazyString {
    var selection: SimpleCursor
    var candidate: SimpleCursor
    val refresher: Refresher
    fun moveSelection(deltaStart: Int, deltaEnd: Int)
    fun moveSelectionAndCandidate(
        deltaStart: Int,
        deltaEnd: Int,
        deltaCandidateStart: Int,
        deltaCandidateEnd: Int
    )

    fun setSelection(start: Int, end: Int)
    fun setSelectionAndCandidate(start: Int?, end: Int?, candidateStart: Int?, candidateEnd: Int?)
    fun getCharsBeforeCursor(count: Int): CharSequence
    fun getCharsAfterCursor(count: Int): CharSequence
    fun getGraphemesBeforeCursor(count: Int): Int
    fun getStringByBytesBeforeCursor(byteCount: Int): String
    fun addString(index: Int, string: String)
    fun delete(start: Int, end: Int)
    fun getStringByIndex(start: Int, end: Int): String
    fun getCandidate(): CharSequence?
    fun getGraphemesAfterCursor(count: Int): Int
    fun getGraphemesAtIndex(startIndex: Int, isBackwards: Boolean, isWord: Boolean, count: Int): Int
    fun byteOffsetToGraphemeOffset(index: Int, byteCount: Int): Int
    fun selectedText(): CharSequence
    fun getWordsBeforeCursor(count: Int): CharSequence
    fun findCharBeforeCursor(charOptions: CharArray): Int
}

data class SimpleCursor(override var start: Int, override var end: Int = start) : Cursor {
    override var min: Int = -1
    override var max: Int = -1
    override var rangeInclusive = -1..-1
    override var rangeExclusive = -1..-1

    override fun refresh() {
        min = minOf(start, end)
        max = maxOf(start, end)
        rangeInclusive = min..max
        rangeExclusive = min until max
    }

    init {
        refresh()
    }

    override fun set(start: Int, end: Int) {
        this.start = start
        this.end = end
        refresh()
    }

    override fun move(deltaStart: Int, deltaEnd: Int) {
        start += deltaStart
        end += deltaEnd
        refresh()
    }
}


abstract class BaseLazyString(
    override var selection: SimpleCursor,
    override var candidate: SimpleCursor,
    override val refresher: Refresher
): LazyString {
    override fun moveSelection(deltaStart: Int, deltaEnd: Int) {
        selection.move(deltaStart, deltaEnd)
    }

    override fun moveSelectionAndCandidate(
        deltaStart: Int,
        deltaEnd: Int,
        deltaCandidateStart: Int,
        deltaCandidateEnd: Int
    ) {
        selection.move(deltaStart, deltaEnd)
        candidate.move(deltaCandidateStart, deltaCandidateEnd)
    }

    override fun setSelectionAndCandidate(start: Int?, end: Int?, candidateStart: Int?, candidateEnd: Int?) {
        selection.set(start ?: selection.start, end ?: selection.end)
        candidate.set(candidateStart ?: candidate.start, candidateEnd ?: candidate.end)
    }

    override fun setSelection(start: Int, end: Int) {
        selection.set(start, end)
    }

    abstract override fun getCharsBeforeCursor(count: Int): CharSequence
    override fun findCharBeforeCursor(charOptions: CharArray): Int {
        var bufferCount = 100
        while (true) {
            val chars = getCharsBeforeCursor(bufferCount)
            val index = chars.lastIndexOfAny(charOptions)
            if (index != -1) {
                return selection.min - chars.length - index
            }
            bufferCount *= 2
            if (chars.length >= selection.min) {
                return 0
            }
        }
    }

    abstract override fun getCharsAfterCursor(count: Int): CharSequence
    abstract override fun selectedText(): CharSequence
    override fun getStringByBytesBeforeCursor(byteCount: Int): String {
        getCharsBeforeCursor(byteCount * 2).toString().toByteArray().takeLast(byteCount)
            .toByteArray().let {
                return String(it)
            }
    }

    override fun byteOffsetToGraphemeOffset(index: Int, byteCount: Int): Int {
        val newIndex =
            minOf(index + byteCount, index)
        val newStartEnd =
            maxOf(index + byteCount, index)
        val startString = getStringByIndex(newIndex, newStartEnd)
        val startBytes = startString.toByteArray()
        val startChars =
            String(startBytes, 0, minOf(abs(byteCount), startBytes.size), Charsets.UTF_8).length
        return getGraphemesAtIndex(
            newIndex,
            isBackwards = byteCount < 0,
            isWord = false,
            count = startChars
        ) * (if (byteCount < 0) -1 else 1)
    }

    override fun getGraphemesBeforeCursor(count: Int): Int {
        return getGraphemesAtIndex(selection.min, isBackwards = true, isWord = false, count = count)
    }

    override fun getGraphemesAfterCursor(count: Int): Int {
        return getGraphemesAtIndex(
            selection.max,
            isBackwards = false,
            isWord = false,
            count = count
        )
    }

    override fun getWordsBeforeCursor(count: Int): CharSequence {
        return getGraphemesAtIndex(
            selection.max,
            isBackwards = true,
            isWord = true,
            count = count
        ).let {
            getCharsBeforeCursor(it).toString()
        }
    }

    abstract override fun addString(index: Int, string: String)
    abstract override fun delete(start: Int, end: Int)
    override fun getStringByIndex(start: Int, end: Int): String {
        if (start == end) {
            return ""
        }
        val min = minOf(start, end)
        val max = maxOf(start, end)
        // min|-----|max
        ///     | |
        val charsAfterCount = max - selection.max
        val charsAtCount = max - selection.min
        val charsBeforeCount = selection.min - min

        val builder = StringBuilder()
        if (charsBeforeCount > 0) {
            val before = getCharsBeforeCursor(charsBeforeCount)
            builder.append(before.substring(0, minOf(max - min, before.length)))
        }
        if (charsAtCount > 0) {
            val at = selectedText()
            builder.append(at.substring(0, minOf(max - selection.min, at.length)))
        }
        if (charsAfterCount > 0) {
            val after = getCharsAfterCursor(charsAfterCount)
            builder.append(
                after.substring(
                    maxOf(0, after.length - charsAfterCount),
                    minOf(max - selection.max, after.length)
                )
            )
        }
        return builder.toString()
    }
    override fun getCandidate(): CharSequence? {
        if (candidate.max < 0 || candidate.min < 0) {
            return null
        }

        if (candidate.max == candidate.min) {
            return ""
        }

        return getStringByIndex(candidate.min, candidate.max)
    }

    override fun getGraphemesAtIndex(
        startIndex: Int,
        isBackwards: Boolean,
        isWord: Boolean,
        count: Int
    ): Int {
        var charsToGet = count
        charsToGet = 100
        if (isBackwards) {
            charsToGet *= -1
        }

        var chars = getStringByIndex(startIndex, startIndex + charsToGet)

        var totalLength = chars.length
        var isLast = false;

        val iterator =
            if (isWord) BreakIterator.getWordInstance() else BreakIterator.getCharacterInstance()
        iterator.setText(chars)
        if (isBackwards) {
            iterator.last()
        } else {
            iterator.first()
        }
        var j = 0
        while (true) {
            for (i in j until count) {
                val it = if (isBackwards) iterator.previous() else iterator.next()

                if (it <= 0 || it >= chars.length) {
                    break
                }
                j++
            }

            if (j <= count) {
                return if (isBackwards) chars.length - iterator.current() else iterator.current()
            }

            if (isLast) {
                return chars.length
            }

            val oldLength = totalLength
            totalLength *= 2
            chars = getStringByIndex(
                startIndex,
                startIndex + totalLength * (if (isBackwards) -1 else 1)
            )
            if (chars.length < totalLength || totalLength == 0) {
                isLast = true
            }

            iterator.setText(chars)
            if (isBackwards) {
                iterator.following(oldLength)
            } else {
                iterator.preceding(oldLength)
            }
        }
    }

}

class NoBufferLazyString(
    selection: SimpleCursor,
    candidate: SimpleCursor,
    refresher: Refresher
) : BaseLazyString(selection, candidate, refresher){
    override fun getCharsBeforeCursor(count: Int): CharSequence {
        return refresher.beforeCursor(count) ?: ""
    }

    override fun getCharsAfterCursor(count: Int): CharSequence {
        return refresher.afterCursor(count) ?: ""
    }

    override fun selectedText(): CharSequence {
        return refresher.atCursor() ?: ""
    }

    override fun addString(index: Int, string: String)
    {
        if (!selection.isNegative()) {
            selection.addLength(index, string.length)
        }
        if (!candidate.isNegative()) {
            candidate.addLength(index, string.length)
        }

        selection.zeroIfNegative()
        candidate.zeroIfNegative()
    }

    override fun delete(start: Int, end: Int) {
        if (!selection.isNegative()) {
            selection.deleteLength(start, end - start)
        }
        if (!candidate.isNegative()) {
            candidate.deleteLength(start, end - start)
        }
    }
}

class LazyStringRope(
    selection: SimpleCursor,
    candidate: SimpleCursor,
    initialTextBefore: CharSequence?,
    initialSelection: CharSequence?,
    initialTextAfter: CharSequence?,
    refresher: Refresher,
) :
    LazyString, BaseLazyString(selection, candidate, refresher) {
    private val rope = Rope()

    init {
        initialTextBefore?.let { rope.insert(selection.min - it.length, it) }
        initialSelection?.let { rope.insert(selection.min, it) }
        initialTextAfter?.let { rope.insert(selection.max, it) }
    }

    override fun getCharsBeforeCursor(count: Int): CharSequence {
        val safe = minOf(count, selection.min)
        return rope.get(selection.min - safe, selection.min) ?: requestCharsBeforeCursor(count)
    }

    override fun getCharsAfterCursor(count: Int): CharSequence {
        val safe = maxOf(count, 0)
        return rope.get(selection.max, selection.max + safe) ?: requestCharsAfterCursor(count)
    }

    override fun selectedText(): CharSequence {
        return rope.get(selection.min, selection.max) ?: requestSelection()
    }

    private fun requestCharsBeforeCursor(count: Int): CharSequence {
        val chars = refresher.beforeCursor(minOf(count, selection.min))
        chars?.let { rope.insert(selection.min, it) }
        return chars ?: ""
    }

    private fun requestCharsAfterCursor(count: Int): CharSequence {
        val chars = refresher.afterCursor(maxOf(count, 0))
        chars?.let { rope.insert(selection.max, it) }
        return chars ?: ""
    }

    private fun requestSelection(): CharSequence {
        val chars = refresher.atCursor()
        chars?.let { rope.insert(selection.min, it) }
        return chars ?: ""
    }

    override fun addString(index: Int, string: String) {
        rope.insert(index, string)
        fixCursorWithoutChangingSize(selection, index, string.length, false)
        fixCursorWithoutChangingSize(candidate, index, string.length, false)
    }

    override fun delete(start: Int, end: Int) {
        rope.delete(start, end)
        // change selection to match
        fixCursorWithoutChangingSize(selection, start, end - start, true)
        fixCursorWithoutChangingSize(candidate, start, end - start, true)
    }

    private fun fixCursorWithoutChangingSize(
        cursor: SimpleCursor,
        start: Int,
        count: Int,
        delete: Boolean
    ) {
        val sign = if (delete) -1 else 1
        if (cursor.start == -1 || cursor.end == -1) {
            return
        }
        if (cursor.max < start) {
            return
        }
        val afterCursor = if (delete) maxOf(start + count - cursor.max, 0) else 0
        cursor.move(sign * (count - afterCursor), sign * (count - afterCursor))

    }


    override fun toString(): String {
        return "LazyStringRope(selection=$selection, candidate=$candidate, length=${rope.length()})"
    }
}
