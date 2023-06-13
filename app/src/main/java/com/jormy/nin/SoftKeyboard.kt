@file:Suppress("NAME_SHADOWING")

package com.jormy.nin

import android.R
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
import com.jormy.nin.NINLib.onChangeAppOrTextbox
import com.jormy.nin.NINLib.onEditorChangeTypeClass
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

/* loaded from: classes.dex */
class SoftKeyboard : InputMethodService() {
    // android.inputmethodservice.InputMethodService, android.app.Service
    override fun onCreate() {
        super.onCreate()
        textopbuffer = ConcurrentLinkedQueue()
        textboxeventsbuffer = ConcurrentLinkedQueue()
        worddestructionbuffer = ConcurrentLinkedQueue()
        globalsoftkeyboard = this
    }

    // android.inputmethodservice.InputMethodService
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

        return theopenglview?.let {
            (it.parent as ViewGroup).removeView(it)
            it
        } ?: NINView(this)
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
                val i = attribute.inputType and 65536
            }
        }
        val inf = TextboxEvent(
            TextboxEventType.APPFIELDCHANGE,
            attribute.packageName,
            attribute.fieldName,
            typemode
        )
        textboxeventsbuffer!!.add(inf)
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

    // android.inputmethodservice.InputMethodService
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
        var textboxeventsbuffer: ConcurrentLinkedQueue<TextboxEvent>? = null
        var textopbuffer: ConcurrentLinkedQueue<TextOp>? = null
        var theopenglview: NINView? = null
        var worddestructionbuffer: ConcurrentLinkedQueue<WordDestructionInfo>? = null
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
            currentSelectionStart = selStart
            currentSelectionEnd = selEnd
            currentCandidateStart = candidatesStart
            currentCandidateEnd = candidatesEnd
            if (signal != null) {
                signalCursorCandidacyResult(ic, signal)
            }
        }

        fun performSetSelection(
            selectStart: Int,
            selectEnd: Int,
            fromStart: Boolean,
            dontSignal: Boolean,
            ic: InputConnection
        ) {
            setSelectionHelper(selectStart, selectEnd, fromStart, ic)
            if (!dontSignal) {
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
                val endPoint = currentCandidateEnd
                ic.setComposingRegion(endPoint, endPoint)
                ic.setSelection(endPoint, endPoint)
                changeSelection(ic, endPoint, endPoint, endPoint, endPoint, null)
                return
            }
            var baseStart = currentSelectionStart
            val baseEnd = currentSelectionEnd
            if (fromStart) {
                if (selectStart <= 0 && selectEnd <= 0 && currentCandidateEnd == currentSelectionStart) {
                    baseStart = currentCandidateStart
                } else if (currentCandidateStart != -1) {
                    val shifter = currentCandidateStart - currentSelectionEnd
                    selectStart += shifter
                    selectEnd += shifter
                }
            }
            fillText(ic, 50, false)
            val newStart = if (selectStart == 0) 0 else Math.max(
                0,
                baseStart + getUnicodeMovementForIndex(ic, selectStart)
            )
            val newEnd = if (selectEnd == 0) 0 else Math.max(
                0,
                baseEnd + getUnicodeMovementForIndex(ic, selectEnd)
            )
            ic.setComposingRegion(newStart, newEnd)
            ic.setSelection(newEnd, newEnd)
        }

        private fun getUnicodeSumMovement(currentChars: CharSequence?): Int {
            val iterator = BreakIterator.getCharacterInstance()
            iterator.setText(currentChars.toString())
            var total = -1
            var current = iterator.first()
            while (current != BreakIterator.DONE) {
                total++
                current = iterator.next()
            }
            return Math.max(0, total)
        }

        private fun getUnicodeMovementForIndex(currentChars: CharSequence?, count: Int): Int {
            val isBackwards = count < 0
            val abs = Math.abs(count)
            val iterator = BreakIterator.getCharacterInstance()
            iterator.setText(currentChars.toString())
            var finalVar = 0
            var i = 0
            val _none = if (isBackwards) iterator.last() else iterator.first()
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

        private fun getUnicodeMovementForIndex(ic: InputConnection, count: Int): Int {
            val factor = 10
            val abs = Math.abs(count)
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

        fun performBackReplacement(
            rawBackIndex: Int,
            originalUnicodeLen: Int,
            replacement: String?,
            ic: InputConnection
        ) {
            var startOfOriginalWordOffsetBytes = rawBackIndex
            val candidateLength = currentCandidateEnd - currentCandidateStart

            fillText(ic, abs(currentSelectionEnd) + originalUnicodeLen, false)
            val totalText = textBeforeCursor.toString() + textAfterCursor.toString()
            val cursorIndexBytes =textBeforeCursor.toString()
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
            worddestructionbuffer!!.add(WordDestructionInfo(leword!!, lestring!!))
        }

        fun signalCursorCandidacyResult(ic: InputConnection?, mode: String?) {
            if (ic == null) {
                textboxeventsbuffer!!.add(TextboxEvent(TextboxEventType.RESET))
                return
            }
            val hasSelection = currentSelectionStart != currentSelectionEnd
            if (hasSelection) {
                return
            }
            val candidateLength = currentCandidateEnd - currentCandidateStart
            val nullCandidate = candidateLength == 0
            if (currentCandidateStart != currentSelectionStart && currentCandidateEnd != currentSelectionStart && !nullCandidate) {
                prin("softkeyboard going haywire!! : " + currentCandidateStart + " -> " + currentCandidateEnd + " :: " + currentSelectionStart)
                return
            }
            fillText(ic, 200, false)
            var curword: String? = null
            var pretext = textBeforeCursor.toString()
            var posttext = textAfterCursor.toString()
            if (nullCandidate || currentCandidateStart == currentSelectionStart) {
                val length = Math.min(textAfterCursor!!.length, candidateLength)
                posttext = posttext.substring(length)
            } else if (currentCandidateEnd == currentSelectionStart) {
                val length = Math.min(textBeforeCursor!!.length, candidateLength)
                curword = pretext.substring(pretext.length - length)
                pretext = pretext.substring(0, pretext.length - length)
            }
            val inf = TextboxEvent(TextboxEventType.SELECTION, curword, pretext, posttext, mode)
            textboxeventsbuffer!!.add(inf)
        }

        @JvmStatic
        fun relayDelayedEvents() {
            while (true) {
                val torel = textboxeventsbuffer!!.poll()
                if (torel != null) {
                    //Log.d("NIN", "relaying delayed event $torel")
                }
                if (torel == null) {
                    break
                } else if (torel.type === TextboxEventType.RESET) {
                    onExternalSelChange()
                } else if (torel.type === TextboxEventType.SELECTION) {
                    onTextSelection(torel.arg1, torel.mainarg, torel.arg2, torel.codemode)
                } else if (torel.type === TextboxEventType.APPFIELDCHANGE) {
                    onChangeAppOrTextbox(torel.mainarg, torel.arg1, torel.arg2)
                } else if (torel.type === TextboxEventType.FIELDTYPECLASSCHANGE) {
                    onEditorChangeTypeClass(torel.mainarg, torel.arg1)
                }
            }
            while (true) {
                val desu = worddestructionbuffer!!.poll()
                if (desu != null) {
                    onWordDestruction(desu.destructedword, desu.destructedstring)
                } else {
                    return
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
                val i = currentSelectionStart
                currentSelectionEnd = i
                currentCandidateStart = i
                currentCandidateEnd = i
            }
        }

        fun performBackspacing(mode: String?, singleCharacterMode: Boolean, ic: InputConnection) {
            val hasCandidate = currentCandidateEnd != currentCandidateStart
            val hasSelection = currentSelectionStart != currentSelectionEnd
            var start: Int
            var end: Int
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
                if (seq.subSequence(index, seq.length).matches(".*\\P{L}.*".toRegex())) {
                    changeSelection(ic, index, index, index, index, "external")
                }

                return;
            }
            ic.setComposingRegion(start, start)
            ic.setSelection(start, start)
            ic.deleteSurroundingText(0, end - start)
            changeSelection(ic, start, start, start, start, "external")
        }

        fun processTextOps() {
            var ic: InputConnection?
            val origbatchcount = textopbuffer!!.size
            if (origbatchcount != 0) {
                if (globalsoftkeyboard!!.currentInputConnection.also {
                        ic = it
                    } != null) {
                    ic!!.beginBatchEdit()
                    for (i in 0 until origbatchcount) {
                        val theop = textopbuffer!!.poll() ?: break
                        val thenext = textopbuffer!!.peek()
                        var skipit = false
                        // only if debug level
                        //Log.d("NIN", theop.toString() + " ---- " + (thenext?.toString() ?: "null"))
                        if (thenext != null && (thenext.type == 's' || thenext.type == 'l') && theop.type == 'l') {
                            skipit = true
                        }
                        if (!skipit && ic != null) {
                            if (theop.type == 's') {
                                if (theop.strarg == "\n") {
                                    var action = 1
                                    if (globalsoftkeyboard!!.currentInputEditorInfo.imeOptions and 1073741824 == 0) {
                                        action =
                                            globalsoftkeyboard!!.currentInputEditorInfo.imeOptions and 255
                                    }
                                    if (action != 1) {
                                        ic!!.performEditorAction(action)
                                    } else {
                                        keyDownUp(ic!!, 66, 0, 0, KeyEvent.FLAG_SOFT_KEYBOARD)
                                    }
                                } else {
                                    if (theop.strarg!!.startsWith("<{") && theop.strarg.endsWith("}>")) {
                                        val taskerstring =
                                            theop.strarg.substring(2, theop.strarg.length - 2)
                                        try {
                                            handleSpecialText(ic!!, theop, taskerstring)
                                        } catch (e: Exception) {
                                            Log.e(
                                                "NIN",
                                                "Error handling special text: " + e.message
                                            )
                                            ic!!.commitText(theop.strarg, 1)
                                        }
                                    } else {
                                        ic!!.commitText(theop.strarg, 1)
                                    }
                                }
                            } else if (theop.type == 'e') {
                                performSetSelection(
                                    theop.intarg1,
                                    theop.intarg2,
                                    theop.boolarg,
                                    theop.boolarg2,
                                    ic!!
                                )
                            } else if (theop.type == 'r') {
                                performBackReplacement(
                                    theop.intarg1,
                                    theop.intarg2,
                                    theop.strarg,
                                    ic!!
                                )
                            } else if (theop.type == 'l') {
                                ic!!.setComposingText(theop.strarg, 1)
                            } else if (theop.type == '<') {
                                performBackspacing(theop.strarg, theop.boolarg, ic!!)
                            } else if (theop.type == 'b') {
                                performBackspacing(theop.strarg, theop.boolarg, ic!!)
                            } else if (theop.type != 'u') {
                                if (theop.type == '!') {
                                    signalCursorCandidacyResult(ic, "requestsel")
                                } else if (theop.type == 'C') {
                                    performMUCommand(
                                        theop.strarg,
                                        theop.a1,
                                        theop.a2,
                                        theop.a2,
                                        ic!!
                                    )
                                } else if (theop.type == 'm') {
                                    performCursorMovement(
                                        theop.intarg1,
                                        theop.intarg2,
                                        theop.boolarg,
                                        ic!!
                                    )
                                }
                            }
                        }
                    }
                    updatesel_byroenflag = true
                    updatesel_byroen_lasttimemilli = System.currentTimeMillis()
                    ic!!.endBatchEdit()
                }
            }
        }

        private fun handleSpecialText(ic: InputConnection, theop: TextOp, taskerstring: String) {
            if (taskerstring.startsWith("k")) {
                val substring = taskerstring.substring(1)
                val parts =
                    substring.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                var keycode = 0
                keycode = if (parts.size < 1) {
                    substring.toInt()
                } else {
                    parts[0].toInt()
                }
                var modifiers = 0
                if (parts.size > 1) {
                    modifiers = parts[1].toInt()
                }
                var repeat = 0
                if (parts.size > 2) {
                    repeat = parts[2].toInt()
                }
                var flags = KeyEvent.FLAG_SOFT_KEYBOARD
                if (parts.size > 2) {
                    flags = parts[2].toInt()
                }
                keyDownUp(ic, keycode, modifiers, repeat, flags)
            } else if (taskerstring.startsWith("c")) {
                val substring = taskerstring.substring(1)
                val parts =
                    substring.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                var code = 0
                code = if (parts.size < 1) {
                    substring.toInt()
                } else {
                    parts[0].toInt()
                }
                ic.performContextMenuAction(
                    when (code) {
                        0 -> R.id.cut
                        1 -> R.id.copy
                        2 -> R.id.paste
                        3 -> R.id.selectAll
                        4 -> R.id.startSelectingText
                        5 -> R.id.stopSelectingText
                        6 -> R.id.switchInputMethod
                        else -> code
                    }
                )
            } else if (taskerstring.startsWith("t")) {
                globalcontext!!.triggerBasicTaskerEvent(theop.strarg!!)
            }
            changeSelection(
                ic,
                currentSelectionStart,
                currentSelectionEnd,
                currentCandidateStart,
                currentCandidateEnd,
                "external"
            )
        }

        @Api
        @JvmStatic
        fun callMUCommand(cmd: String?, a1: String?, a2: String?, a3: String?) {
            val topush = TextOp('C', 0, 0, false, false, cmd, a1, a2, a3)
            doTextOp(topush)
        }

        @Api
        @JvmStatic
        fun callRequestSel() {
            doTextOp(TextOp('!'))
        }

        @Api
        @JvmStatic
        fun callSetSel(startpoint: Int, endpoint: Int, fromstart: Boolean, dontsignal: Boolean) {
            doTextOp(TextOp('e', startpoint, endpoint, fromstart, dontsignal))
        }

        @Api
        @JvmStatic
        fun callDragCursorUp(releasedir: Int) {
            doTextOp(TextOp('u', releasedir))
        }

        @Api
        @JvmStatic
        fun callDragCursorMove(xmove: Int, ymove: Int, selmode: Boolean) {
            doTextOp(TextOp('m', xmove, ymove, selmode))
        }

        @Api
        @JvmStatic
        fun callSimpleBackspace(simplecharmode: Boolean) {
            doTextOp(TextOp('<', simplecharmode))
        }

        @Api
        @JvmStatic
        fun callBackReplacement(rawbackindex: Int, oldstr: String, lestr: String?) {
            doTextOp(TextOp('r', rawbackindex, oldstr.length, true, false, lestr))
        }

        @Api
        @JvmStatic
        fun callBackspaceModed(lestr: String?) {
            doTextOp(TextOp('b', lestr))
        }

        @Api
        @JvmStatic
        fun callMarkLiquid(lestr: String?) {
            doTextOp(TextOp('l', lestr))
        }

        @Api
        @JvmStatic
        fun callSolidify(lestr: String?) {
            val op = TextOp('s', lestr)
            doTextOp(op)
        }

        fun doTextOp(op: TextOp) {
            textopbuffer!!.add(op)
            Handler(Looper.getMainLooper()).post(PerformTextOpsTask())
        }

        fun doTextEvent(event: TextboxEvent) {
            textboxeventsbuffer!!.add(event)
        }

        fun keyDownUp(
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
    fun wordLen(text: String, start: Int): Int {
        val iterator = android.icu.text.BreakIterator.getWordInstance()
        iterator.setText(text)
        return try {
            iterator.following(start).let { if (it == BreakIterator.DONE) text.length else it }
        } catch (e: Exception) {
            Log.e("WordHelper", "wordBreakForwards", e)
            text.length
        } - start
    }

    fun lastWordBreak(text: String): Int {
        val iterator = android.icu.text.BreakIterator.getWordInstance()
        iterator.setText(text)
        return try {
            iterator.last()
            iterator.previous().let { if (it == BreakIterator.DONE) 0 else it }
        } catch (e: Exception) {
            Log.e("WordHelper", "wordBreakForwards", e)
            0
        }
    }
}
