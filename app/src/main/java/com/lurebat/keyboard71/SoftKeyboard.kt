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
import com.lurebat.keyboard71.tasker.triggerBasicTaskerEvent
import java.util.concurrent.ConcurrentLinkedQueue

class SoftKeyboard : InputMethodService() {
    val textBoxEventQueue: ConcurrentLinkedQueue<TextBoxEvent> = ConcurrentLinkedQueue()
    val textOpQueue: ConcurrentLinkedQueue<TextOp> = ConcurrentLinkedQueue()
    private var ninView: NINView? = null
    private lateinit var lazyString: LazyString
    private var didProcessTextOps = false
    private var lastTextOpTimeMillis: Long = 0
    private var selectionBeforeRetype: SimpleCursor? = null
    private var candidateBeforeRetype: SimpleCursor? = null
    private var afterRetypeCounter = 0

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
            "candstart $lazyString"
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

        lazyString = LazyStringRope(
            SimpleCursor(attribute.initialSelStart, attribute.initialSelEnd),
            SimpleCursor(-1, -1),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) attribute.getInitialTextBeforeCursor(1000, 0) else currentInputConnection.getTextBeforeCursor(1000, 0),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) attribute.getInitialSelectedText(0) else currentInputConnection.getSelectedText(0),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) attribute.getInitialTextAfterCursor(1000, 0) else currentInputConnection.getTextAfterCursor(1000, 0),
            InputConnectionRefresher(){ currentInputConnection }
        )

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
            selStart: Int? = null,
            selEnd: Int? = null,
            candidatesStart: Int? = null,
            candidatesEnd: Int? = null,
            signal: String? = null
        ) {
            lazyString.setSelectionAndCandidate(selStart, selEnd, candidatesStart, candidatesEnd)
            if (signal != null) {
                signalCursorCandidacyResult(ic, signal)
            }
        }

        private fun performSetSelection(
            selectStart: Int,
            selectEnd: Int,
            fromStart: Boolean,
            signal: Boolean,
            ic: InputConnection,
            isStartOfRetype: Boolean
        ) {
            var start = selectStart
            var end = selectEnd
            val candidate = lazyString.getCandidate()
            if (candidate != null && fromStart) {
                val candidateBeforeSelection = lazyString.selection.min - lazyString.candidate.min
                if (candidateBeforeSelection > 0) {
                    start -= candidateBeforeSelection
                    end -= candidateBeforeSelection
                }
            }

            var finalSelectionStart = lazyString.byteOffsetToGraphemeOffset(lazyString.selection.start, start)
            var finalSelectionEnd = lazyString.byteOffsetToGraphemeOffset(lazyString.selection.start, end)
            var finalCandidateStart = 0
            var finalCandidateEnd = 0

            if (isStartOfRetype) {
                selectionBeforeRetype = SimpleCursor(lazyString.selection.start, lazyString.selection.end)
                candidateBeforeRetype = SimpleCursor(lazyString.candidate.start, lazyString.candidate.end)
            }
            lazyString.moveSelectionAndCandidate(finalSelectionStart, finalSelectionEnd, finalCandidateStart, finalCandidateEnd)
            ic.setComposingRegion(lazyString.candidate.end, lazyString.candidate.end)
            ic.setSelection(lazyString.selection.start, lazyString.selection.end)
            signalCursorCandidacyResult(ic, "setselle")

            val isAfterRetype = selectStart == 5000 && selectEnd == 5000 && !fromStart && !signal
            if (!isAfterRetype) {
                return
            }

            afterRetypeCounter += 1
            if (afterRetypeCounter < 3) {
                return
            }
            afterRetypeCounter = 0

            val deltaSelection = selectionBeforeRetype?.let {
                selectionBeforeRetype = null
                SimpleCursor(lazyString.selection.start - it.start, lazyString.selection.end - it.end)
            } ?: SimpleCursor(0, 0)
            val deltaCandidate = candidateBeforeRetype?.let {
            candidateBeforeRetype = null
            if (it.start == -1 || it.end == -1) {
                SimpleCursor(0, 0)
            } else {
                if (lazyString.candidate.start == -1 || lazyString.candidate.end == -1) {
                    SimpleCursor(deltaSelection.start + it.start + 1, deltaSelection.end + it.end + 1)
                } else {
                    SimpleCursor(deltaSelection.start + lazyString.candidate.start - it.start, deltaSelection.end + lazyString.candidate.end - it.end)
                }
            }
            } ?: SimpleCursor(0, 0)

            finalSelectionStart = deltaSelection.start
            finalSelectionEnd = deltaSelection.end
            finalCandidateStart = deltaCandidate.start
            finalCandidateEnd = deltaCandidate.end

            lazyString.moveSelectionAndCandidate(finalSelectionStart, finalSelectionEnd, finalCandidateStart, finalCandidateEnd)
            ic.setComposingRegion(lazyString.candidate.start, lazyString.candidate.end)
            ic.setSelection(lazyString.selection.start, lazyString.selection.end)
            signalCursorCandidacyResult(ic, "setselle")
        }

        private fun performBackReplacement(
            rawBackIndex: Int,
            original: String,
            replacement: String?,
            ic: InputConnection
        ) {
            val stringUntilCursor = lazyString.getStringByBytesBeforeCursor(rawBackIndex)
            val replacement = replacement ?: ""
            val overriding = replacement.length > original.length
            var startIndex = lazyString.selection.min - stringUntilCursor.length

            //todo handle candidate
            val candidate = lazyString.getCandidate()
            var candidateBeforeSelection = 0
            if (candidate != null) {
                val candidateBeforeSelection = lazyString.selection.min - lazyString.candidate.min
                if (candidateBeforeSelection > 0) {
                    startIndex -= candidateBeforeSelection
                }
            }

            lazyString.delete(startIndex, startIndex + original.length)
            lazyString.addString(startIndex, replacement)
            lazyString.moveSelectionAndCandidate(candidateBeforeSelection, candidateBeforeSelection, candidateBeforeSelection, candidateBeforeSelection)

            // Delete the original text
            ic.setSelection(startIndex, startIndex)
            ic.setComposingRegion(startIndex, startIndex)
            ic.deleteSurroundingText(0, original.length)

            // Insert the replacement text
            ic.commitText(replacement, 1)
            ic.setComposingRegion(lazyString.candidate.start, lazyString.candidate.end)
            ic.setSelection(lazyString.selection.start, lazyString.selection.end)
            if (overriding) {
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
            //todo handle candidate

            lazyString.moveSelection(xmove, xmove)
            if (!selectionMode) {
                lazyString.setSelection(lazyString.selection.end, lazyString.selection.end)
            }

            ic.setComposingRegion(lazyString.selection.start, lazyString.selection.end)
            ic.setSelection(lazyString.selection.start, lazyString.selection.end)
            changeSelection(ic, lazyString.selection.start, lazyString.selection.end, lazyString.candidate.start, lazyString.candidate.end, "cursordrag")
        }

        private fun signalCursorCandidacyResult(ic: InputConnection?, mode: String?) {
            if (ic == null) {
                doTextEvent(TextBoxEvent.Reset)
                return
            }
            if (lazyString.candidate.isNotEmpty() && (lazyString.selection.max < lazyString.candidate.min || lazyString.selection.min > lazyString.candidate.max)) {
                // we jumped out of the candidate range - reset it
                ic.finishComposingText()
                lazyString.candidate.start = -1
                lazyString.candidate.end = -1
            }

            val charCount = 100
            val candidate = lazyString.getCandidate()?.toString()
            val beforeCandidate = if (candidate != null) lazyString.getStringByIndex(maxOf(0, lazyString.candidate.min - charCount), lazyString.candidate.min) else lazyString.getCharsBeforeCursor(charCount).toString()
            val afterCandidate = if (candidate != null) lazyString.getStringByIndex(lazyString.candidate.max, lazyString.candidate.max + charCount) else lazyString.getCharsAfterCursor(charCount).toString()

            doTextEvent(TextBoxEvent.Selection(candidate, beforeCandidate, afterCandidate, mode))
        }

        fun relayDelayedEvents() {
            while (true) {
                val event = textBoxEventQueue.poll()
                if (event != null) {
                    Log.d("relayDelayedEvents", "Relaying delayed event: $event")
                }
                when (event) {
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
                changeSelection(ic, lazyString.selection.start, lazyString.selection.start, lazyString.selection.start, lazyString.selection.start, null)

            }
        }

        private fun performBackspacing(mode: String?, singleCharacterMode: Boolean, ic: InputConnection) {

            if (lazyString.selection.isNotEmpty()) {
                lazyString.delete(lazyString.selection.min, lazyString.selection.max)
            }

            if (lazyString.candidate.isNotEmpty()) {
                lazyString.delete(lazyString.candidate.min, lazyString.candidate.max)
            }

            val toDelete = if (singleCharacterMode) {
                lazyString.getCharsBeforeCursor(1)
            }
            else {
                lazyString.getWordBeforeCursor()
            }

            if (toDelete.isNotEmpty()) {
                lazyString.delete(lazyString.selection.min - toDelete.length, lazyString.selection.min)
            }

            ic.setComposingRegion(lazyString.selection.min, lazyString.selection.min)
            ic.setSelection(lazyString.selection.min, lazyString.selection.min)
            ic.deleteSurroundingText(0, toDelete.length)
            changeSelection(ic, lazyString.selection.min, lazyString.selection.min, lazyString.candidate.min, lazyString.candidate.min, mode ?: "setselle")
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

            Log.d("NIN", "processOperation: $op, next: $next")
            when (op) {
                is TextOp.MarkLiquid -> {
                    if (next !is TextOp.MarkLiquid && next !is TextOp.Solidify) {
                        ic.setComposingText(op.newString, 1)
                    }
                }

                is TextOp.Solidify -> {
                    when {
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
                                    signal="external"
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
                    ic,
                    next is TextOp.MuCommand && next.command == "retypebksp"
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
