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
        return inputConnection()?.getTextBeforeCursor(1, 0)
    }

}

interface LazyString {
    var selection: SimpleCursor
    val candidate: SimpleCursor
    val refresher: Refresher
    fun moveSelection(deltaStart: Int, deltaEnd: Int)
    fun moveSelectionAndCandidate(deltaStart: Int, deltaEnd: Int, deltaCandidateStart: Int, deltaCandidateEnd: Int)
    fun setSelection(start: Int, end: Int)
    fun setSelectionAndCandidate(start: Int?, end: Int?, candidateStart: Int?, candidateEnd: Int?)
    fun getCharsBeforeCursor(count: Int): CharSequence
    fun getCharsAfterCursor(count: Int): CharSequence
    fun getGraphemesBeforeCursor(count: Int): Int
    fun getWordBeforeCursor(): CharSequence
    fun getStringByBytesBeforeCursor(byteCount: Int): String
    fun overrideString(index: Int, string: String)
    fun delete(start: Int, end: Int)
    fun getStringByIndex(start: Int, end: Int): String
    fun getGraphemesAfterCursor(count: Int): Int
    fun getGraphemesAtIndex(startIndex: Int, isBackwards: Boolean, isWord: Boolean, count: Int): Int
    fun byteOffsetToGraphemeOffset(index: Int, byteCount: Int): Int
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



class LazyStringRope(override var selection: SimpleCursor, override val candidate: SimpleCursor, initialTextBefore: CharSequence?, initialSelection: CharSequence?, initialTextAfter: CharSequence?, override val refresher: Refresher) :
    LazyString {
    private val rope = Rope()

    init {
        initialTextBefore?.let { rope.insert(selection.min-it.length, it) }
        initialSelection?.let { rope.insert(selection.min, it) }
        initialTextAfter?.let { rope.insert(selection.max, it) }
    }

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

    override fun getCharsBeforeCursor(count: Int): CharSequence {
        val safe = minOf(count, selection.min)
        return rope.get(selection.min - safe, selection.min) ?: requestCharsBeforeCursor(count)
    }

    override fun getCharsAfterCursor(count: Int): CharSequence {
        val safe = maxOf(count, 0)
        return rope.get(selection.max, selection.max + safe) ?: requestCharsAfterCursor(count)
    }

    fun getSelection(): CharSequence {
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

    override fun getStringByBytesBeforeCursor(byteCount: Int): String {
        getCharsBeforeCursor(byteCount * 2).toString().toByteArray().takeLast(byteCount).toByteArray().let {
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
        val startChars = String(startBytes, 0, minOf(abs(byteCount), startBytes.size) , Charsets.UTF_8).length
        return getGraphemesAtIndex(newIndex, isBackwards = byteCount < 0, isWord = false, count = startChars) * (if (byteCount < 0) -1 else 1)
    }

    override fun getGraphemesBeforeCursor(count: Int): Int {
        return getGraphemesAtIndex(selection.min, isBackwards = true, isWord = false, count = count)
    }

    override fun getGraphemesAfterCursor(count: Int): Int {
        return getGraphemesAtIndex(selection.max, isBackwards = false, isWord = false, count = count)
    }

    override fun getWordBeforeCursor(): CharSequence {
        return getGraphemesAtIndex(selection.max, isBackwards = true, isWord = true, count = 1).let {
            getCharsBeforeCursor(it).toString()
        }
    }

    override fun overrideString(index: Int, string: String) {
        rope.insert(index, string)
    }

    override fun delete(start: Int, end: Int) {
        rope.delete(start, end)
        // change selection to match
        if (selection.max < start) {
            return
        }
        if (selection.min >= end) {
            selection.move(-(end - start), -(end - start))
            return
        }

        val countBeforeSelection = selection.min - start
        val countInsideSelection = end - selection.min

        moveSelection(-countBeforeSelection, -countBeforeSelection + -(countInsideSelection))
    }

    override fun getStringByIndex(start: Int, end: Int): String {
        if (start == end) {
            return ""
        }
        val min = minOf(start, end)
        val max = maxOf(start, end)
        // min|-----|max
        ///     | |
        val charsAfterCount = max - maxOf(min, selection.max)
        val charsAtCount = minOf(max, selection.max) - maxOf(min, selection.min)
        val charsBeforeCount = minOf(max, selection.min) - min

        val builder = StringBuilder()
        if (charsBeforeCount > 0) {
            builder.append(getCharsBeforeCursor(charsBeforeCount))
        }
        if (charsAtCount > 0) {
            builder.append(getSelection().substring(0, minOf(charsAtCount, selection.length())))
        }
        if (charsAfterCount > 0) {
            builder.append(getCharsAfterCursor(charsAfterCount))
        }
        return builder.toString()
    }

    override fun getGraphemesAtIndex(startIndex: Int, isBackwards: Boolean, isWord: Boolean, count: Int): Int {
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

            if (j == count) {
                return if (isBackwards) chars.length - iterator.current() else iterator.current()
            }

            if (isLast) {
                return chars.length
            }

            val oldLength = totalLength
            totalLength *= 2
            chars = getStringByIndex(startIndex, startIndex + totalLength * (if (isBackwards) -1 else 1))
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

    override fun toString(): String {
        return "LazyStringRope(selection=$selection, candidate=$candidate, length=${rope.length()})"
    }
}
