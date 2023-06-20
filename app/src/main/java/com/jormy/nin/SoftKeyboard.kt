@file:Suppress("NAME_SHADOWING")

package com.jormy.nin

import android.content.Context
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
import com.jormy.nin.KotlinUtils.changeIf
import com.jormy.nin.NINLib.onChangeAppOrTextbox
import com.jormy.nin.NINLib.onExternalSelChange
import com.jormy.nin.NINLib.onTextSelection
import com.jormy.nin.NINLib.onWordDestruction
import com.jormy.nin.NINLib.processSoftKeyboardCursorMovementLeft
import com.jormy.nin.NINLib.processSoftKeyboardCursorMovementRight
import com.jormy.nin.Utils.prin
import com.lurebat.keyboard71.triggerBasicTaskerEvent
import java.nio.charset.StandardCharsets
import java.text.BreakIterator
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class SoftKeyboard : InputMethodService() {
    override fun onCreate() {
        super.onCreate()
        textopbuffer = ConcurrentLinkedQueue()
        textboxeventsbuffer = ConcurrentLinkedQueue()
        globalsoftkeyboard = this
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
            !(updatesel_byroenflag || System.currentTimeMillis() - updatesel_byroen_lasttimemilli < 55)
        prin("candstart " + currentCandidateStart + " -> " + currentCandidateEnd + " || " + currentSelectionStart + " -> " + currentSelectionEnd)
        changeSelection(
            currentInputConnection,
            newSelStart,
            newSelEnd,
            candidatesStart,
            candidatesEnd,
            if (shouldSignal) "external" else null
        )
        if (!shouldSignal) {
            updatesel_byroenflag = false
        }
    }

    internal class PerformTextOpsTask : Runnable {
        override fun run() {
            processTextOps()
        }
    }

    override fun onCreateInputView(): View {
        globalcontext = this

        val view = theopenglview
        if (view == null) {
            theopenglview = NINView(this).apply {
                setZOrderOnTop(true)
            }
        } else {
            (view.parent as ViewGroup?)?.removeView(view)
        }

        return theopenglview!!
    }

    override fun onStartInputView(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInputView(attribute, restarting)
        val printStream = System.out
        printStream.println("------------ jormoust Editor Info : " + attribute.packageName + " | " + attribute.fieldName + "|" + attribute.inputType)
        var typemode = ""
        when (attribute.inputType and 15) {
            1 -> {
                val variation = attribute.inputType and 4080
                typemode = "uri"
                if (variation == 128) {
                    typemode = "passwd"
                }
            }
        }

        doTextEvent(
            TextEvent.AppFieldChange(attribute.packageName, attribute.fieldName, typemode)
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            textAfterCursor = attribute.getInitialTextAfterCursor(1000, 0)
            textBeforeCursor = attribute.getInitialTextBeforeCursor(1000, 0)
            if (textAfterCursor == null) {
                textAfterCursor = ""
            }
            if (textBeforeCursor == null) {
                textBeforeCursor = ""
            }
        }

        signalCursorCandidacyResult(globalsoftkeyboard!!.currentInputConnection, "startInputView")
    }

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

    private fun commitTyped(inputConnection: InputConnection) {
        inputConnection.commitText("haha!", "haha!".length)
    }

    companion object {
        var globalcontext: Context? = null
        var globalsoftkeyboard: SoftKeyboard? = null
        var textboxeventsbuffer: ConcurrentLinkedQueue<TextEvent>? = null
        var textopbuffer: ConcurrentLinkedQueue<TextOp>? = null
        var theopenglview: NINView? = null
        var currentSelectionStart = 0
        var currentSelectionEnd = 0
        var currentCandidateStart = 0
        var currentCandidateEnd = 0
        var updatesel_byroenflag = false
        var updatesel_byroen_lasttimemilli: Long = 0
        private var textAfterCursor: CharSequence? = ""
        private var textBeforeCursor: CharSequence? = ""
        private fun changeSelection(
            ic: InputConnection,
            selStart: Int,
            selEnd: Int,
            candidatesStart: Int,
            candidatesEnd: Int,
            signal: String?
        ) {
            adjustCursorText(selEnd)
            if (textAfterCursor == "" && textBeforeCursor == "") {
                fillText(ic, 100, false);
            }

            currentSelectionStart = selStart
            currentSelectionEnd = selEnd
            currentCandidateStart = candidatesStart
            currentCandidateEnd = candidatesEnd
            if (signal != null) {
                signalCursorCandidacyResult(ic, signal)
            }
        }

        private fun adjustCursorText(selEnd: Int) {
            val selectionEndMovement = selEnd - currentSelectionEnd
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
            selectStartBytes: Int,
            selectEndBytes: Int,
            fromStart: Boolean,
            ic: InputConnection
        ) {
            if (selectStartBytes == 0 && selectEndBytes == 0 && !fromStart) {
                // Select nothing - just stay in place
                val endPoint = currentCandidateEnd
                ic.setComposingRegion(endPoint, endPoint)
                ic.setSelection(endPoint, endPoint)
                changeSelection(ic, endPoint, endPoint, endPoint, endPoint, null)
                return
            }
            var baseStart = currentSelectionStart
            var baseEnd = currentSelectionEnd
            val selectionMin = min(baseStart, baseEnd)
            val selectionMax = max(baseStart, baseEnd)
            fillText(ic, 200, false)

            if (currentCandidateEnd != currentCandidateStart) {
                val candidateMin = min(currentCandidateStart, currentCandidateEnd)
                val candidateMax = max(currentCandidateStart, currentCandidateEnd)
                if (candidateMin >= selectionMax) {
                    baseEnd += candidateMax - candidateMin
                    baseStart += candidateMax - candidateMin
                } else if (candidateMax <= selectionMin) {
                    baseStart -= candidateMax - candidateMin
                    baseEnd -= candidateMax - candidateMin
                } else {
                    baseStart = min(candidateMin, selectionMin)
                    baseEnd = max(candidateMax, selectionMax)
                }
            }

            val newStart = if (selectStartBytes == 0) 0 else max(
                0,
                baseStart + getUnicodeMovementForIndex(ic, selectStartBytes, true)
            )
            val newEnd = if (selectEndBytes == 0) 0 else max(
                0,
                baseEnd + getUnicodeMovementForIndex(ic, selectEndBytes, true)
            )
            ic.setComposingRegion(newStart, newEnd)
            ic.setSelection(newEnd, newEnd)
            changeSelection(ic, newEnd, newEnd, newStart, newEnd, null)
        }

        private fun getUnicodeMovementForIndex(currentChars: CharSequence?, count: Int, inBytes: Boolean): Int {
            val isBackwards = count < 0
            val abs = abs(count)
            if (inBytes) {
                val bytes = currentChars.toString().toByteArray(StandardCharsets.UTF_8)
                return if (!isBackwards) {
                    String(bytes, 0, min(count, bytes.size), StandardCharsets.UTF_8).length
                } else {
                    -String(bytes, max(bytes.size + count, 0), min(-count, bytes.size), StandardCharsets.UTF_8).length
                }
            }

            val iterator = BreakIterator.getCharacterInstance()
            iterator.setText(currentChars.toString())
            var finalVar = 0
            var i = 0
            if (isBackwards) iterator.last() else iterator.first()
            while (i < abs) {
                var result = 0
                result = if (isBackwards) {
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

        private fun getUnicodeMovementForIndex(ic: InputConnection, count: Int, inBytes: Boolean): Int {
            val factor = 10
            val abs = abs(count)
            val isBackwards = count < 0
            val amount = abs * factor
            val currentChars = fillText(ic, amount, isBackwards)
            return getUnicodeMovementForIndex(currentChars, count, inBytes)
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

        fun performBackReplacement(
            rawBackIndex: Int,
            original: String,
            replacement: String?,
            ic: InputConnection
        ) {
            var startOfOriginalWordOffsetBytes = rawBackIndex
            val candidateLength = currentCandidateEnd - currentCandidateStart
            val originalUnicodeLen = original.length

            fillText(ic, abs(currentSelectionEnd) + originalUnicodeLen, false)
            val totalText = textBeforeCursor.toString() + textAfterCursor.toString()
            val cursorIndexBytes = textBeforeCursor.toString()
                .toByteArray(StandardCharsets.UTF_8).size

            val bytes = totalText.toByteArray(StandardCharsets.UTF_8)

            if (candidateLength > 0 && currentCandidateStart > 0) {
                val candidate = totalText.substring(
                    currentCandidateStart,
                    currentCandidateStart + candidateLength
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
                currentSelectionStart + positionShift,
                currentSelectionEnd + positionShift,
                currentCandidateStart + positionShift,
                currentCandidateEnd + positionShift,
                null
            )
            ic.setComposingRegion(currentCandidateStart, currentCandidateEnd)
            ic.setSelection(currentSelectionStart, currentSelectionEnd)
            if (wordOverriding) {
                signalCursorCandidacyResult(ic, "backrepl")
            }
        }

        fun performCursorMovement(
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
                    var usedcurpos = currentSelectionStart
                    val i = currentCandidateEnd
                    if (i == currentSelectionStart) {
                        usedcurpos = currentCandidateStart
                        val thedifference = i - currentCandidateStart
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
                    if (currentCandidateEnd != currentCandidateStart) {
                        laterpoint = currentCandidateEnd
                        later2 = laterpoint
                    } else {
                        val usedcurpos2 = currentSelectionStart
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

        @Api
        @JvmStatic
        fun signalWordDestruction(leword: String?, lestring: String?) {
            doTextEvent(TextEvent.WordDestruction(leword, lestring))
        }

        fun signalCursorCandidacyResult(ic: InputConnection?, mode: String?) {
            if (ic == null) {
                doTextEvent(TextEvent.Reset)
                return
            }

            val candidateLength = currentCandidateEnd - currentCandidateStart
            val nullCandidate = candidateLength == 0

            if (!nullCandidate && currentCandidateStart != currentSelectionEnd && currentCandidateEnd != currentSelectionEnd) {
                ic.finishComposingText()
            }

            fillText(ic, 200, false)
            var curword: String? = null
            var pretext = textBeforeCursor.toString()
            var posttext = textAfterCursor.toString()
            if (nullCandidate || currentCandidateStart == currentSelectionStart) {
                val length = min(textAfterCursor!!.length, candidateLength)
                posttext = posttext.substring(length)
            } else if (currentCandidateEnd == currentSelectionStart) {
                val length = min(textBeforeCursor!!.length, candidateLength)
                curword = pretext.substring(pretext.length - length)
                pretext = pretext.substring(0, pretext.length - length)
            }
            doTextEvent(TextEvent.Selection(curword, pretext, posttext, mode))
        }

        @JvmStatic
        fun relayDelayedEvents() {
            while (true) {
                val event = textboxeventsbuffer!!.poll()
                if (event != null) {
                    //Log.d("jormoust", "Relaying delayed event: $event")
                }
                when (event) {
                    is TextEvent.AppFieldChange -> onChangeAppOrTextbox(
                        event.packageName,
                        event.field,
                        event.mode
                    )

                    TextEvent.Reset -> onExternalSelChange()

                    is TextEvent.Selection -> onTextSelection(
                        event.textBefore,
                        event.currentWord,
                        event.textAfter,
                        event.mode
                    )

                    is TextEvent.WordDestruction -> onWordDestruction(
                        event.destroyedWord,
                        event.destroyedString
                    )

                    null -> break
                }
            }
        }

        fun performMUCommand(
            cmd: String?,
            a1: String?,
            a2: String?,
            a3: String?,
            ic: InputConnection
        ) {
            if (cmd == "retypebksp") {
                ic.setComposingText("", 0)
                changeSelection(ic, currentSelectionStart, currentSelectionStart, currentSelectionStart, currentSelectionStart, null)
            }
        }

        fun performBackspacing(mode: String?, singleCharacterMode: Boolean, ic: InputConnection) {
            val hasCandidate = currentCandidateEnd != currentCandidateStart
            val hasSelection = currentSelectionStart != currentSelectionEnd
            val start: Int
            val end: Int
            if (hasSelection) {
                start = currentSelectionStart
                end = currentSelectionEnd
            } else if (hasCandidate) {
                start = currentCandidateStart
                end = currentCandidateEnd
            } else if (mode != null && mode.startsWith("X:")) {
                start = currentSelectionStart - mode.substring(2).toInt()
                end = currentSelectionEnd
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
                changeSelection(ic, index, index, index, index, "setselle")

                return;
            }
            ic.setComposingRegion(start, start)
            ic.setSelection(start, start)
            ic.deleteSurroundingText(0, end - start)
            changeSelection(ic, start, start, start, start, "setselle")
        }

        fun processTextOps() {
            val buffer = textopbuffer ?: return
            val bufferSize = buffer.size
            if (bufferSize == 0) return

            val ic = globalsoftkeyboard?.currentInputConnection ?: return

            ic.beginBatchEdit()

            try {
                for (i in 0 until bufferSize) {
                    val op = textopbuffer!!.poll() ?: break
                    val next = textopbuffer!!.peek()
                    processOperation(op, next, ic)
                }
            } finally {
                updatesel_byroenflag = true
                updatesel_byroen_lasttimemilli = System.currentTimeMillis()
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
                                currentSelectionStart,
                                currentSelectionEnd,
                                currentCandidateStart,
                                currentCandidateEnd,
                                "external"
                            )
                        }

                        op.newString == "\n" -> {
                            var action = 1
                            if (globalsoftkeyboard!!.currentInputEditorInfo.imeOptions and 1073741824 == 0) {
                                action =
                                    globalsoftkeyboard!!.currentInputEditorInfo.imeOptions and 255
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
                                    currentSelectionStart,
                                    currentSelectionEnd,
                                    currentCandidateStart,
                                    currentCandidateEnd,
                                    "external"
                                )
                                callSpecialOperation(inner)
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

                't' -> globalcontext!!.triggerBasicTaskerEvent(rest)
            }
        }

        fun callSpecialOperation(args: String) {
            doTextOp(TextOp.Special(args))
        }

        @Api
        @JvmStatic
        fun callMUCommand(command: String, arg1: String?, arg2: String?, arg3: String?) {
            doTextOp(TextOp.MuCommand(command, arg1, arg2, arg3))
        }

        @Api
        @JvmStatic
        fun callRequestSel() {
            doTextOp(TextOp.RequestSelection)
        }

        @Api
        @JvmStatic
        fun callSetSel(start: Int, end: Int, fromStart: Boolean, dontSignal: Boolean) {
            doTextOp(TextOp.SetSelection(start, end, fromStart, !dontSignal))
        }

        @Api
        @JvmStatic
        fun callDragCursorUp(releasedDirection: Int) {
            doTextOp(TextOp.DragCursorUp(releasedDirection))
        }

        @Api
        @JvmStatic
        fun callDragCursorMove(xMovement: Int, yMovement: Int, selectionMode: Boolean) {
            doTextOp(TextOp.DragCursorMove(xMovement, yMovement, selectionMode))
        }

        @Api
        @JvmStatic
        fun callSimpleBackspace(singleCharacterMode: Boolean) {
            doTextOp(TextOp.SimpleBackspace(singleCharacterMode))
        }

        @Api
        @JvmStatic
        fun callBackReplacement(
            backIndexFromCursorBytes: Int,
            oldString: String,
            newString: String
        ) {
            doTextOp(TextOp.BackspaceReplacement(backIndexFromCursorBytes, oldString, newString))
        }

        @Api
        @JvmStatic
        fun callBackspaceModed(string: String) {
            doTextOp(TextOp.BackspaceModed(string))
        }

        @Api
        @JvmStatic
        fun callMarkLiquid(string: String) {
            doTextOp(TextOp.MarkLiquid(string))
        }

        @Api
        @JvmStatic
        fun callSolidify(string: String) {
            doTextOp(TextOp.Solidify(string))
        }

        fun doTextOp(op: TextOp) = textopbuffer?.let {
            it.add(op)
            Handler(Looper.getMainLooper()).post(PerformTextOpsTask())
        }

        fun doTextEvent(event: TextEvent) = textboxeventsbuffer?.add(event)

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


object WordHelper {

    private val keyCodesMap =
        KeyEvent::class.java.fields.asSequence().filter { it.name.startsWith("KEYCODE_") }
            .associateTo(mutableMapOf()) { Pair(it.name.substring(8), it.getInt(null)) }
    private val modifiersMap = KeyEvent::class.java.fields.asSequence()
        .filter { it.name.startsWith("META_") && it.name.endsWith("_ON") }
        .associateTo(mutableMapOf()) {
            Pair(
                it.name.substring(5, it.name.indexOf("_ON")),
                it.getInt(null)
            )
        }

    fun parseKeyCode(keyCode: String): Int = keyCodesMap[keyCode.uppercase()] ?: keyCode.toInt()
    fun parseModifiers(modifiers: String): Int {
        if (modifiers.all { it.isDigit() }) return modifiers.toInt()
        return modifiers.split(",").map { it.trim() }
            .map { modifiersMap[it.uppercase()] ?: it.toInt() }.reduce { acc, i -> acc or i }
    }

    fun lastWordBreak(text: String): Int {
        val iterator = android.icu.text.BreakIterator.getWordInstance()
        iterator.setText(text)
        return try {
            iterator.last()
            iterator.previous().changeIf(BreakIterator.DONE, 0)
        } catch (e: Exception) {
            Log.e("WordHelper", "wordBreakForwards", e)
            0
        }
    }
}
