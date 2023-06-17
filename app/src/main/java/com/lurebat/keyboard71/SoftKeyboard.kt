@file:Suppress("NAME_SHADOWING")

package com.lurebat.keyboard71

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.jormy.nin.NINLib.onChangeAppOrTextbox
import com.jormy.nin.NINLib.onExternalSelChange
import com.jormy.nin.NINLib.onTextSelection
import com.jormy.nin.NINLib.onWordDestruction
import com.jormy.nin.NINLib.processSoftKeyboardCursorMovementLeft
import com.jormy.nin.NINLib.processSoftKeyboardCursorMovementRight
import com.lurebat.keyboard71.tasker.triggerBasicTaskerEvent
import java.nio.charset.StandardCharsets
import java.text.BreakIterator
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class SoftKeyboard : InputMethodService() {
    val textBoxEventQueue: ConcurrentLinkedQueue<TextBoxEvent> = ConcurrentLinkedQueue()
    val textOpQueue: ConcurrentLinkedQueue<TextOp> = ConcurrentLinkedQueue()
    private var ninView: NINView? = null
    private var selectionStart = 0
    private var selectionEnd = 0
    private var candidateStart = 0
    private var candidateEnd = 0
    private var didProcessTextOps = false
    private var lastTextOpTimeMillis: Long = 0
    private var textAfterCursor: CharSequence? = ""
    private var textBeforeCursor: CharSequence? = ""

    override fun onCreate() {
        super.onCreate()
        keyboard = this
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        val shouldSignal =
            !didProcessTextOps && System.currentTimeMillis() - lastTextOpTimeMillis >= 55
        Log.d("SoftKeyboard",
            "candstart $candidateStart -> $candidateEnd || $selectionStart -> $selectionEnd"
        )
        changeSelection(
            currentInputConnection,
            newSelStart,
            newSelEnd,
            candidatesStart,
            candidatesEnd,
            if (shouldSignal) "external" else null
        )
        if (!shouldSignal) {
            didProcessTextOps = false
        }
    }

    override fun onCreateInputView(): View {
        val view = ninView
        if (view == null) {
            ninView = NINView(this).apply {
                setZOrderOnTop(true)
            }
        } else {
            (view.parent as ViewGroup?)?.removeView(view)
        }

        return ninView!!
    }

    override fun onStartInputView(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInputView(attribute, restarting)
        Log.d("SoftKeyboard",
            "------------ jormoust Editor Info : ${attribute.packageName} | ${attribute.fieldName}|${attribute.inputType}"
        )
        var keyboardType = ""
        when (attribute.inputType and EditorInfo.TYPE_MASK_CLASS) {
            EditorInfo.TYPE_CLASS_TEXT -> {
                val variation = attribute.inputType and EditorInfo.TYPE_MASK_VARIATION
                keyboardType = "uri"
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD) {
                    keyboardType = "passwd"
                }
            }
        }

        doTextEvent(
            TextBoxEvent.AppFieldChange(attribute.packageName, attribute.fieldName, keyboardType)
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            textAfterCursor = attribute.getInitialTextAfterCursor(1000, 0) ?: ""
            textBeforeCursor = attribute.getInitialTextBeforeCursor(1000, 0) ?: ""
        } else {
            textAfterCursor = currentInputConnection.getTextAfterCursor(1000, 0) ?: ""
            textBeforeCursor = currentInputConnection.getTextBeforeCursor(1000, 0) ?: ""
        }

        signalCursorCandidacyResult(currentInputConnection, "startInputView")
    }

    @Deprecated("Deprecated in Java")
    override fun onViewClicked(focusChanged: Boolean) {
        val curconn = currentInputConnection
        if (curconn != null) {
            curconn.setComposingRegion(-1, -1)
            signalCursorCandidacyResult(curconn, "onViewClicked")
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return super.onKeyUp(keyCode, event)
    }
        private fun changeSelection(
            ic: InputConnection,
            selStart: Int,
            selEnd: Int,
            candidatesStart: Int,
            candidatesEnd: Int,
            signal: String?
        ) {
            adjustCursorText(selEnd)
            selectionStart = selStart
            selectionEnd = selEnd
            candidateStart = candidatesStart
            candidateEnd = candidatesEnd
            if (signal != null) {
                signalCursorCandidacyResult(ic, signal)
            }
        }

        private fun adjustCursorText(selEnd: Int) {
            val selectionEndMovement = selEnd - selectionEnd
            if (selEnd < 0) {
                textAfterCursor = ""
                textBeforeCursor = ""
            } else {
                // adjust currentSelectionForwards and currentSelectionBackwards based on the new selection
                if (selectionEndMovement > 0) {
                    if (selEnd > textAfterCursor!!.length) {
                        textAfterCursor = ""
                        textBeforeCursor = ""
                    } else {
                        val start = textAfterCursor!!.subSequence(0, selEnd)
                        textAfterCursor =
                            textAfterCursor!!.subSequence(selEnd, textAfterCursor!!.length)
                        textBeforeCursor = textBeforeCursor.toString() + start.toString()
                    }
                } else if (selectionEndMovement < 0) {
                    if (selEnd > textBeforeCursor!!.length) {
                        textAfterCursor = ""
                        textBeforeCursor = ""
                    } else {
                        val start =
                            textBeforeCursor!!.subSequence(selEnd, textBeforeCursor!!.length)
                        textBeforeCursor = textBeforeCursor!!.subSequence(0, selEnd)
                        textAfterCursor = start.toString() + textAfterCursor.toString()
                    }
                }
            }
        }

        private fun performSetSelection(
            selectStart: Int,
            selectEnd: Int,
            fromStart: Boolean,
            signal: Boolean,
            ic: InputConnection
        ) {
            setSelectionHelper(selectStart, selectEnd, fromStart, ic)
            if (signal) {
                signalCursorCandidacyResult(ic, "setselle")
            }
        }

        private fun setSelectionHelper(
            selectStart: Int,
            selectEnd: Int,
            fromStart: Boolean,
            ic: InputConnection
        ) {
            var selectStart = selectStart
            var selectEnd = selectEnd
            if (selectStart == 0 && selectEnd == 0 && !fromStart) {
                // Select nothing - just stay in place
                val endPoint = candidateEnd
                ic.setComposingRegion(endPoint, endPoint)
                ic.setSelection(endPoint, endPoint)
                changeSelection(ic, endPoint, endPoint, endPoint, endPoint, null)
                return
            }
            var baseStart = selectionStart
            val baseEnd = selectionEnd
            if (fromStart) {
                if (selectStart <= 0 && selectEnd <= 0 && candidateEnd == selectionStart) {
                    baseStart = candidateStart
                } else if (candidateStart != -1) {
                    val shifter = candidateStart - selectionEnd
                    selectStart += shifter
                    selectEnd += shifter
                }
            }
            fillText(ic, 50, false)
            val newStart = if (selectStart == 0) 0 else max(
                0,
                baseStart + getUnicodeMovementForIndex(ic, selectStart)
            )
            val newEnd = if (selectEnd == 0) 0 else max(
                0,
                baseEnd + getUnicodeMovementForIndex(ic, selectEnd)
            )
            ic.setComposingRegion(newStart, newEnd)
            ic.setSelection(newEnd, newEnd)
        }

        private fun getUnicodeMovementForIndex(currentChars: CharSequence?, count: Int): Int {
            val isBackwards = count < 0
            val abs = abs(count)
            val iterator = BreakIterator.getCharacterInstance()
            iterator.setText(currentChars.toString())
            var finalVar = 0
            var i = 0
            if (isBackwards) iterator.last() else iterator.first()
            while (i < abs) {
                var result = if (isBackwards) {
                    iterator.previous()
                } else {
                    iterator.next()
                }
                if (result == BreakIterator.DONE) {
                    result = if (isBackwards) 0 else currentChars!!.length
                }
                finalVar = if (isBackwards) result - currentChars!!.length else result
                i++
            }
            return finalVar
        }

        private fun getUnicodeMovementForIndex(ic: InputConnection, count: Int): Int {
            val factor = 10
            val abs = abs(count)
            val isBackwards = count < 0
            val amount = abs * factor
            val currentChars = fillText(ic, amount, isBackwards)
            return getUnicodeMovementForIndex(currentChars, count)
        }

        private fun fillText(
            ic: InputConnection,
            amount: Int,
            isBackwards: Boolean
        ): CharSequence {
            if (textAfterCursor!!.length < amount) {
                val temp = ic.getTextAfterCursor(amount * 2, 0)
                textAfterCursor = temp?.toString() ?: ""
            }
            if (textBeforeCursor!!.length < amount) {
                val temp = ic.getTextBeforeCursor(amount * 2, 0)
                textBeforeCursor = temp?.toString() ?: ""
            }
            return if (isBackwards) {
                textBeforeCursor!!
            } else {
                textAfterCursor!!
            }
        }

        private fun performBackReplacement(
            rawBackIndex: Int,
            original: String,
            replacement: String?,
            ic: InputConnection
        ) {
            var startOfOriginalWordOffsetBytes = rawBackIndex
            val candidateLength = candidateEnd - candidateStart
            val originalUnicodeLen = original.length

            fillText(ic, abs(selectionEnd) + originalUnicodeLen, false)
            val totalText = textBeforeCursor.toString() + textAfterCursor.toString()
            val cursorIndexBytes = textBeforeCursor.toString()
                .toByteArray(StandardCharsets.UTF_8).size

            val bytes = totalText.toByteArray(StandardCharsets.UTF_8)

            if (candidateLength > 0 && candidateStart > 0) {
                val candidate = totalText.substring(
                    candidateStart,
                    candidateStart + candidateLength
                ).toByteArray(StandardCharsets.UTF_8)
                startOfOriginalWordOffsetBytes += candidate.size
            }

            val startIndex =
                String(bytes, 0, cursorIndexBytes - startOfOriginalWordOffsetBytes).length

            val wordOverriding = startIndex + originalUnicodeLen > (textBeforeCursor?.length ?: 0)

            // Delete the original text
            ic.setSelection(startIndex, startIndex)
            ic.setComposingRegion(startIndex, startIndex)
            ic.deleteSurroundingText(0, originalUnicodeLen)

            // Insert the replacement text
            ic.commitText(replacement, 1)
            val positionShift = (replacement ?: "").length - originalUnicodeLen
            changeSelection(
                ic,
                selectionStart + positionShift,
                selectionEnd + positionShift,
                candidateStart + positionShift,
                candidateEnd + positionShift,
                null
            )
            ic.setComposingRegion(candidateStart, candidateEnd)
            ic.setSelection(selectionStart, selectionEnd)
            if (wordOverriding) {
                signalCursorCandidacyResult(ic, "backrepl")
            }
        }

        @Suppress("UNUSED_PARAMETER")
        private fun performCursorMovement(
            xmove: Int,
            ymove: Int,
            selectionMode: Boolean,
            ic: InputConnection
        ) {
            var later2: Int
            var laterpoint: Int
            if (xmove < 0) {
                val charseq = ic.getTextBeforeCursor(120, 0)
                if (charseq != null) {
                    var thestr = charseq.toString()
                    var usedcurpos = selectionStart
                    val i = candidateEnd
                    if (i == selectionStart) {
                        usedcurpos = candidateStart
                        val thedifference = i - candidateStart
                        if (thedifference > 0) {
                            thestr = thestr.substring(0, thestr.length - thedifference)
                        }
                    }
                    val result = processSoftKeyboardCursorMovementLeft(thestr)
                    val earlier = (result shr 32).toInt()
                    val later = ((-1).toLong() and result).toInt()
                    var earlpoint2 = usedcurpos - earlier
                    var laterpoint2 = usedcurpos - later
                    if (earlpoint2 < 0) {
                        earlpoint2 = 0
                    }
                    if (laterpoint2 < 0) {
                        laterpoint2 = 0
                    }
                    ic.setComposingRegion(earlpoint2, laterpoint2)
                    ic.setSelection(earlpoint2, earlpoint2)
                    changeSelection(
                        ic,
                        earlpoint2,
                        earlpoint2,
                        earlpoint2,
                        laterpoint2,
                        "cursordrag"
                    )
                    return
                }
                println("jormoust :: No charsequence!")
            } else if (xmove > 0) {
                val charseq2 = ic.getTextAfterCursor(120, 0)
                if (charseq2 != null) {
                    val thestr2 = charseq2.toString()
                    if (candidateEnd != candidateStart) {
                        laterpoint = candidateEnd
                        later2 = laterpoint
                    } else {
                        val usedcurpos2 = selectionStart
                        val result2 = processSoftKeyboardCursorMovementRight(thestr2)
                        val earlier2 = (result2 shr 32).toInt()
                        val later22 = ((-1).toLong() and result2).toInt()
                        val earlpoint = usedcurpos2 + earlier2
                        val i2 = usedcurpos2 + later22
                        later2 = earlpoint
                        laterpoint = i2
                    }
                    if (later2 < 0) {
                        later2 = 0
                    }
                    if (laterpoint < 0) {
                        laterpoint = 0
                    }
                    ic.setComposingRegion(later2, laterpoint)
                    ic.setSelection(laterpoint, laterpoint)
                    changeSelection(ic, laterpoint, laterpoint, later2, laterpoint, "cursordrag")
                    return
                }
                println("jormoust :: No charsequence!")
            }
        }

        private fun signalCursorCandidacyResult(ic: InputConnection?, mode: String?) {
            if (ic == null) {
                doTextEvent(TextBoxEvent.Reset)
                return
            }
            val hasSelection = selectionStart != selectionEnd
            if (hasSelection) {
                return
            }
            val candidateLength = candidateEnd - candidateStart
            val nullCandidate = candidateLength == 0
            if (candidateStart != selectionStart && candidateEnd != selectionStart && !nullCandidate) {
                Log.e("SoftKeyboard",
                    "softkeyboard going haywire!! : $candidateStart -> $candidateEnd :: $selectionStart"
                )
                return
            }
            fillText(ic, 200, false)
            var curword: String? = null
            var pretext = textBeforeCursor.toString()
            var posttext = textAfterCursor.toString()
            if (nullCandidate || candidateStart == selectionStart) {
                val length = min(textAfterCursor!!.length, candidateLength)
                posttext = posttext.substring(length)
            } else if (candidateEnd == selectionStart) {
                val length = min(textBeforeCursor!!.length, candidateLength)
                curword = pretext.substring(pretext.length - length)
                pretext = pretext.substring(0, pretext.length - length)
            }
            doTextEvent(TextBoxEvent.Selection(curword, pretext, posttext, mode))
        }

        fun relayDelayedEvents() {
            while (true) {
                when (val event = textBoxEventQueue.poll()) {
                    is TextBoxEvent.AppFieldChange -> onChangeAppOrTextbox(
                        event.packageName,
                        event.field,
                        event.mode
                    )

                    TextBoxEvent.Reset -> onExternalSelChange()

                    is TextBoxEvent.Selection -> onTextSelection(
                        event.textBefore,
                        event.currentWord,
                        event.textAfter,
                        event.mode
                    )

                    is TextBoxEvent.WordDestruction -> onWordDestruction(
                        event.destroyedWord,
                        event.destroyedString
                    )

                    null -> break
                }
            }
        }

        @Suppress("UNUSED_PARAMETER")
        private fun performMUCommand(
            cmd: String?,
            a1: String?,
            a2: String?,
            a3: String?,
            ic: InputConnection
        ) {
            if (cmd == "retypebksp") {
                ic.setComposingText("", 0)
                val i = selectionStart
                selectionEnd = i
                candidateStart = i
                candidateEnd = i
            }
        }

        private fun performBackspacing(mode: String?, singleCharacterMode: Boolean, ic: InputConnection) {
            val hasCandidate = candidateEnd != candidateStart
            val hasSelection = selectionStart != selectionEnd
            val start: Int
            val end: Int
            if (hasSelection) {
                start = selectionStart
                end = selectionEnd
            } else if (hasCandidate) {
                start = candidateStart
                end = candidateEnd
            } else if (mode != null && mode.startsWith("X:")) {
                start = selectionStart - mode.substring(2).toInt()
                end = selectionEnd
            } else {
                val seq = fillText(ic, 300, true)
                val index = if (singleCharacterMode) {
                    val iterator = BreakIterator.getCharacterInstance()
                    iterator.setText(seq.toString())
                    iterator.last()
                    var index = iterator.previous()
                    if (index == BreakIterator.DONE) {
                        index = 0
                    }
                    index
                } else {
                    WordHelper.lastWordBreak(seq.toString())
                }
                ic.deleteSurroundingText(seq.length - index, 0)
                if (seq.subSequence(index, seq.length).matches(".*\\P{L}.*".toRegex())) {
                    changeSelection(ic, index, index, index, index, "external")
                }

                return
            }
            ic.setComposingRegion(start, start)
            ic.setSelection(start, start)
            ic.deleteSurroundingText(0, end - start)
            changeSelection(ic, start, start, start, start, "external")
        }

        fun processTextOps() {
            val buffer = textOpQueue
            val bufferSize = buffer.size
            if (bufferSize == 0) return

            val ic = keyboard?.currentInputConnection ?: return

            ic.beginBatchEdit()

            try {
                for (i in 0 until bufferSize) {
                    val op = textOpQueue.poll() ?: break
                    val next = textOpQueue.peek()
                    processOperation(op, next, ic)
                }
            } finally {
                didProcessTextOps = true
                lastTextOpTimeMillis = System.currentTimeMillis()
                ic.endBatchEdit()
            }
        }

        private fun processOperation(
            op: TextOp,
            next: TextOp?,
            ic: InputConnection
        ) {

            //Log.d("NIN", "processOperation: $op, next: $next")
            when (op) {
                is TextOp.MarkLiquid -> {
                    if (next !is TextOp.MarkLiquid && next !is TextOp.Solidify) {
                        ic.setComposingText(op.newString, 1)
                    }
                }

                is TextOp.Solidify -> {
                    when {
                        next != null && next is TextOp.Solidify && op.newString.trim()
                            .isEmpty() && next.newString.startsWith("<{") && next.newString.endsWith(
                            "}>"
                        ) -> {
                            changeSelection(
                                ic,
                                selectionStart,
                                selectionEnd,
                                candidateStart,
                                candidateEnd,
                                "external"
                            )
                        }

                        op.newString == "\n" -> {
                            var action = 1
                            if (currentInputEditorInfo.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION == 0) {
                                action =
                                    currentInputEditorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
                            }
                            if (action != 1) {
                                ic.performEditorAction(action)
                            } else {
                                keyDownUp(ic, 66, 0, 0, KeyEvent.FLAG_SOFT_KEYBOARD)
                            }
                        }

                        op.newString.startsWith("<{") && op.newString.endsWith("}>") -> {
                            val inner = op.newString.substring(2, op.newString.length - 2)
                            if (inner.isNotEmpty()) {
                                changeSelection(
                                    ic,
                                    selectionStart,
                                    selectionEnd,
                                    candidateStart,
                                    candidateEnd,
                                    "external"
                                )
                                doTextOp(TextOp.Special(inner))
                            }
                        }

                        else -> {
                            ic.commitText(op.newString, 1)
                        }
                    }
                }

                is TextOp.SetSelection -> performSetSelection(
                    op.start,
                    op.end,
                    op.fromStart,
                    op.signal,
                    ic
                )

                is TextOp.BackspaceReplacement -> performBackReplacement(
                    op.backIndexFromCursorBytes,
                    op.oldString,
                    op.newString,
                    ic
                )

                is TextOp.SimpleBackspace -> performBackspacing(
                    null,
                    op.singleCharacterMode,
                    ic
                )

                is TextOp.BackspaceModed -> performBackspacing(
                    op.mode,
                    true,
                    ic
                )

                is TextOp.DragCursorUp -> {}
                is TextOp.RequestSelection -> signalCursorCandidacyResult(
                    ic,
                    "requestsel"
                )

                is TextOp.MuCommand -> performMUCommand(
                    op.command,
                    op.arg1,
                    op.arg2,
                    op.arg3,
                    ic
                )

                is TextOp.DragCursorMove -> performCursorMovement(
                    op.xMovement,
                    op.xMovement,
                    op.selectionMode,
                    ic
                )

                is TextOp.Special -> parseSpecialText(ic, op.args)
            }
        }

        private fun parseSpecialText(ic: InputConnection, args: String) {
            val first = args[0]
            val rest = args.substring(1).trim()
            val parts = rest.let {
                val temp = it.split("\\|".toRegex()).toTypedArray()
                if (temp.isEmpty()) {
                    arrayOf(it)
                } else {
                    temp
                }
            }

            when (first) {
                'k' -> {
                    val code = WordHelper.parseKeyCode(parts[0])
                    val modifiers = if (parts.size > 1) WordHelper.parseModifiers(parts[1]) else 0
                    val repeat = if (parts.size > 2) parts[2].toInt() else 0
                    val flags =
                        if (parts.size > 3) parts[3].toInt() else KeyEvent.FLAG_SOFT_KEYBOARD
                    keyDownUp(ic, code, modifiers, repeat, flags)
                }

                'c' -> ic.performContextMenuAction(
                    when (parts[0].uppercase()) {
                        "0", "CUT" -> android.R.id.cut
                        "1", "COPY" -> android.R.id.copy
                        "2", "PASTE" -> android.R.id.paste
                        "3", "SELECT_ALL" -> android.R.id.selectAll
                        "4", "START_SELECT" -> android.R.id.startSelectingText
                        "5", "STOP_SELECT" -> android.R.id.stopSelectingText
                        "6", "SWITCH_KEYBOARD" -> android.R.id.switchInputMethod
                        else -> parts[0].toInt()
                    }
                )

                't' -> this.triggerBasicTaskerEvent(rest)
            }
        }

    companion object {
        var keyboard: SoftKeyboard? = null

        fun doTextOp(op: TextOp) = keyboard?.let{ k ->
            k.textOpQueue.let {
                it.add(op)
                Handler(Looper.getMainLooper()).post{ k.processTextOps()}
            }
        }

        fun doTextEvent(event: TextBoxEvent) = keyboard?.textBoxEventQueue?.add(event)

        private fun keyDownUp(
            ic: InputConnection,
            keyEventCode: Int,
            modifiers: Int,
            repeat: Int,
            flags: Int
        ) {
            ic.sendKeyEvent(
                KeyEvent(
                    0,
                    0,
                    KeyEvent.ACTION_DOWN,
                    keyEventCode,
                    repeat,
                    modifiers,
                    -1,
                    0,
                    flags
                )
            )
            ic.sendKeyEvent(
                KeyEvent(
                    0,
                    0,
                    KeyEvent.ACTION_UP,
                    keyEventCode,
                    repeat,
                    modifiers,
                    -1,
                    0,
                    flags
                )
            )
        }
    }
}
