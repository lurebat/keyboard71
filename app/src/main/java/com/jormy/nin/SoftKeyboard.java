package com.jormy.nin;

import android.content.Context;
import android.graphics.Rect;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodSubtype;

import androidx.annotation.Nullable;

import com.jormy.Sistm;
import com.lurebat.keyboard71.TaskerPluginEventKt;

import java.io.PrintStream;
import java.text.BreakIterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/* loaded from: classes.dex */
public class SoftKeyboard extends InputMethodService {
    public static Context globalcontext;
    public static SoftKeyboard globalsoftkeyboard;
    public static ConcurrentLinkedQueue<TextboxEvent> textboxeventsbuffer;
    public static ConcurrentLinkedQueue<TextOp> textopbuffer;
    public static NINView theopenglview;
    public static ConcurrentLinkedQueue<WordDestructionInfo> worddestructionbuffer;
    static int currentSelectionStart = 0;
    static int currentSelectionEnd = 0;
    static int currentCandidateStart = 0;
    static int currentCandidateEnd = 0;
    static boolean updatesel_byroenflag = false;
    static long updatesel_byroen_lasttimemilli = 0;
    private static String textAfterCursor = "";
    private static String textBeforeCursor = "";

    @Override // android.inputmethodservice.InputMethodService, android.app.Service
    public void onCreate() {
        super.onCreate();
        textopbuffer = new ConcurrentLinkedQueue<>();
        textboxeventsbuffer = new ConcurrentLinkedQueue<>();
        worddestructionbuffer = new ConcurrentLinkedQueue<>();
        globalsoftkeyboard = this;
    }

    @Override // android.inputmethodservice.InputMethodService
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        var shouldSignal =!(updatesel_byroenflag || System.currentTimeMillis() - updatesel_byroen_lasttimemilli < 55);
        Utils.prin("candstart " + currentCandidateStart + " -> " + currentCandidateEnd + " || " + currentSelectionStart + " -> " + currentSelectionEnd);

        changeSelection(getCurrentInputConnection(), newSelStart, newSelEnd, candidatesStart, candidatesEnd, shouldSignal ? "external" : null);

        if (!shouldSignal) {
            updatesel_byroenflag = false;
        }
    }

    private static void changeSelection(InputConnection ic, int selStart, int selEnd, int candidatesStart, int candidatesEnd, @Nullable String signal) {

        var selectionEndMovement = selEnd - currentSelectionEnd;
        // adjust currentSelectionForwards and currentSelectionBackwards based on the new selection
        if (selectionEndMovement > 0) {
            if (selectionEndMovement > textAfterCursor.length()) {
                textAfterCursor = "";
                textBeforeCursor = "";
            }
            else {
                var start = textAfterCursor.substring(0, selectionEndMovement);
                textAfterCursor = textAfterCursor.substring(selectionEndMovement);
                textBeforeCursor = textBeforeCursor + start;
            }
        }
        else if (selectionEndMovement < 0) {
            if (-selectionEndMovement > textBeforeCursor.length()) {
                textAfterCursor = "";
                textBeforeCursor = "";
            }
            else {
                var start = textBeforeCursor.substring(0, -selectionEndMovement);
                var end = textBeforeCursor.substring(-selectionEndMovement);
                textBeforeCursor = start;
                textAfterCursor = end + textAfterCursor;
            }
        }

        currentSelectionStart = selStart;
        currentSelectionEnd = selEnd;
        currentCandidateStart = candidatesStart;
        currentCandidateEnd = candidatesEnd;

        if (signal != null) {
            signalCursorCandidacyResult(ic, signal);
        }
    }

    public static void signalWordDestruction(String leword, String lestring) {
        worddestructionbuffer.add(new WordDestructionInfo(leword, lestring));
    }

    public static void performBackReplacement(int rawBackIndex, int originalUnicodeLen, String replacement, InputConnection ic) {
        var moveIndex = getUnicodeMovementForIndex(ic, rawBackIndex);

        var forwards = fillText(ic, 50, false);

        var endOfWordIndex = -1;
        for (int i = 0; i < forwards.length(); i++) {
            if (Character.isWhitespace(forwards.charAt(i))) {
                endOfWordIndex = i;
                break;
            }
        }

        if (endOfWordIndex == -1) {
            endOfWordIndex = forwards.length();
        }

        boolean inMiddleOfWord = moveIndex < endOfWordIndex;
        var candidateLength = currentCandidateEnd - currentCandidateStart;
        var startPoint = currentSelectionStart;
        if (candidateLength != 0) {
            startPoint -= candidateLength;
        }

        int replaceStartPoint = startPoint - moveIndex;
        if (replaceStartPoint < 0) {
            return;
        }

        // Delete the original text
        ic.setSelection(replaceStartPoint, replaceStartPoint);
        ic.setComposingRegion(replaceStartPoint, replaceStartPoint);
        ic.deleteSurroundingText(0, endOfWordIndex);

        // Insert the replacement text
        ic.commitText(replacement, 1);

        var positionShift = replacement.length() - endOfWordIndex;
        changeSelection(ic, currentSelectionStart + positionShift, currentSelectionEnd + positionShift, currentCandidateStart + positionShift, currentCandidateEnd + positionShift, null);

        ic.setComposingRegion(currentSelectionStart, currentCandidateEnd);
        ic.setSelection(currentSelectionStart, currentSelectionEnd);
        if (inMiddleOfWord) {
            signalCursorCandidacyResult(ic, "backrepl");
        }
    }

    public static void performCursorMovement(int xmove, int ymove, boolean selectionMode, InputConnection ic) {
        int later2;
        int laterpoint;
        if (xmove < 0) {
            CharSequence charseq = ic.getTextBeforeCursor(120, 0);
            if (charseq != null) {
                String thestr = charseq.toString();
                int usedcurpos = currentSelectionStart;
                int i = currentCandidateEnd;
                if (i == currentSelectionStart) {
                    usedcurpos = currentCandidateStart;
                    int thedifference = i - currentCandidateStart;
                    if (thedifference > 0) {
                        thestr = thestr.substring(0, thestr.length() - thedifference);
                    }
                }
                long result = NINLib.processSoftKeyboardCursorMovementLeft(thestr);
                int earlier = (int) (result >> 32);
                int later = (int) ((-1) & result);
                int earlpoint2 = usedcurpos - earlier;
                int laterpoint2 = usedcurpos - later;
                if (earlpoint2 < 0) {
                    earlpoint2 = 0;
                }
                if (laterpoint2 < 0) {
                    laterpoint2 = 0;
                }
                ic.setComposingRegion(earlpoint2, laterpoint2);
                ic.setSelection(earlpoint2, earlpoint2);
                changeSelection(ic, earlpoint2, earlpoint2, earlpoint2, laterpoint2, "cursordrag");
                return;
            }
            System.out.println("jormoust :: No charsequence!");
        } else if (xmove > 0) {
            CharSequence charseq2 = ic.getTextAfterCursor(120, 0);
            if (charseq2 != null) {
                String thestr2 = charseq2.toString();
                if (currentCandidateEnd != currentCandidateStart) {
                    laterpoint = currentCandidateEnd;
                    later2 = laterpoint;
                } else {
                    int usedcurpos2 = currentSelectionStart;
                    long result2 = NINLib.processSoftKeyboardCursorMovementRight(thestr2);
                    int earlier2 = (int) (result2 >> 32);
                    int later22 = (int) ((-1) & result2);
                    int earlpoint = usedcurpos2 + earlier2;
                    int i2 = usedcurpos2 + later22;
                    later2 = earlpoint;
                    laterpoint = i2;
                }
                if (later2 < 0) {
                    later2 = 0;
                }
                if (laterpoint < 0) {
                    laterpoint = 0;
                }
                ic.setComposingRegion(later2, laterpoint);
                ic.setSelection(laterpoint, laterpoint);

                changeSelection(ic, laterpoint, laterpoint, later2, laterpoint, "cursordrag");
                return;
            }
            System.out.println("jormoust :: No charsequence!");
        }
    }

    public static void signalCursorCandidacyResult(InputConnection ic, String themode) {
        int i;
        if (ic == null) {
            textboxeventsbuffer.add(new TextboxEvent(TextboxEventType.RESET, null, null, null));
            return;
        }
        int i2 = currentSelectionStart;
        if (i2 == currentSelectionEnd) {
            int i3 = currentCandidateStart;
            int i4 = currentCandidateEnd;
            boolean nullcandidate = i3 == i4 && i3 == -1;
            if (i3 == 0 && i4 == 0) {
                nullcandidate = true;
            }
            if (i3 == i4) {
                nullcandidate = true;
            }
            if (i3 != i2 && i4 != i2 && !nullcandidate) {
                Utils.prin("softkeyboard going haywire!! : " + currentCandidateStart + " -> " + currentCandidateEnd + " :: " + currentSelectionStart);
                return;
            }
            boolean nocand = nullcandidate;
            CharSequence pretext_seq = ic.getTextBeforeCursor(200, 0);
            CharSequence posttext_seq = ic.getTextAfterCursor(200, 0);
            String pretext = pretext_seq == null ? "" : pretext_seq.toString();
            String posttext = posttext_seq != null ? posttext_seq.toString() : "";
            String curword = null;
            int i5 = currentCandidateEnd;
            int i6 = currentCandidateStart;
            int canlength = i5 - i6;
            if (nocand) {
                canlength = 0;
            }
            if (nocand || i6 == (i = currentSelectionStart)) {
                int lentouse = canlength;
                if (lentouse > posttext.length()) {
                    lentouse = posttext.length();
                }
                curword = posttext.substring(0, lentouse);
                posttext = posttext.substring(lentouse);
            } else if (i5 == i) {
                int lentouse2 = canlength;
                if (lentouse2 > pretext.length()) {
                    lentouse2 = pretext.length();
                }
                curword = pretext.substring(pretext.length() - lentouse2);
                pretext = pretext.substring(0, pretext.length() - lentouse2);
            }
            TextboxEvent inf = new TextboxEvent(TextboxEventType.SELECTION, curword, pretext, posttext);
            inf.codemode = themode;
            textboxeventsbuffer.add(inf);
        }
    }

    public static void relayDelayedEvents() {
        while (true) {
            TextboxEvent torel = textboxeventsbuffer.poll();
            if (torel != null) {
                Log.i("NIN", "relaying delayed event " + torel);
            }
            if (torel == null) {
                break;
            } else if (torel.type == TextboxEventType.RESET) {
                NINLib.onExternalSelChange();
            } else if (torel.type == TextboxEventType.SELECTION) {
                NINLib.onTextSelection(torel.arg1, torel.mainarg, torel.arg2, torel.codemode);
            } else if (torel.type == TextboxEventType.APPFIELDCHANGE) {
                NINLib.onChangeAppOrTextbox(torel.mainarg, torel.arg1, torel.arg2);
            } else if (torel.type == TextboxEventType.FIELDTYPECLASSCHANGE) {
                NINLib.onEditorChangeTypeClass(torel.mainarg, torel.arg1);
            }
        }
        while (true) {
            WordDestructionInfo desu = worddestructionbuffer.poll();
            if (desu != null) {
                NINLib.onWordDestruction(desu.destructedword, desu.destructedstring);
            } else {
                return;
            }
        }
    }

    public static void performSetSelection(int selectStart, int selectEnd, boolean fromStart, boolean dontSignal, InputConnection ic) {
        setSelectionHelper(selectStart, selectEnd, fromStart, ic);
        if (!dontSignal) {
            signalCursorCandidacyResult(ic, "setselle");
        }
    }

    private static void setSelectionHelper(int selectStart, int selectEnd, boolean fromStart, InputConnection ic) {
        if (selectStart == 0 && selectEnd == 0 && !fromStart) {
            // Select nothing - just stay in place
            int endPoint = currentCandidateEnd;
            ic.setComposingRegion(endPoint, endPoint);
            ic.setSelection(endPoint, endPoint);
            changeSelection(ic, endPoint, endPoint, endPoint, endPoint, null);
            return;
        }

        int baseStart = currentSelectionStart;
        int baseEnd = currentSelectionEnd;

        if (fromStart) {
            if (selectStart <= 0 && selectEnd <= 0 && currentCandidateEnd == currentSelectionStart) {
                baseStart = currentCandidateStart;
            }
            else if (currentCandidateStart != -1) {
                int shifter = currentCandidateStart - currentSelectionEnd;
                selectStart += shifter;
                selectEnd += shifter;
            }
        }

        fillText(ic, 50, null);


        var newStart = (selectStart == 0) ? 0 : Math.max(0, baseStart + getUnicodeMovementForIndex(ic, selectStart));
        var newEnd = (selectEnd == 0) ? 0 : Math.max(0, baseEnd + getUnicodeMovementForIndex(ic, selectEnd));
        ic.setComposingRegion(newStart, newEnd);
        ic.setSelection(newEnd, newEnd);

    }

    private static int getUnicodeMovementForIndex(InputConnection ic, int count) {
        var factor = 2;
        int abs = Math.abs(count);
        int amount = (abs * factor);
        boolean isBackwards = count < 0;

        fillText(ic, amount, isBackwards);

        String currentChars = isBackwards ? textBeforeCursor : textAfterCursor;
        if (currentChars == null) {
            return 0;
        }

        BreakIterator iterator = BreakIterator.getCharacterInstance();

        iterator.setText(currentChars);

        var finalVar = 0;
        int i = 0;

        while (true) {
            int _none = isBackwards ? iterator.last() : iterator.first();
            for (; i < abs; i++) {
                var result = 0;
                if (isBackwards) {
                    result = iterator.previous();
                } else {
                    result = iterator.next();
                }

                if (result == BreakIterator.DONE ) {
                    break;
                }
                finalVar = count > 0 ? result : currentChars.length() - result;
            }
            if ((i + 1) < abs) {
                if (currentChars.length() < amount) {
                    break;
                }

                amount *= 2;

                fillText(ic, amount, isBackwards);
                currentChars = isBackwards ? textBeforeCursor : textAfterCursor;
                if (currentChars == null) {
                    return 0;
                }

                if (currentChars.length() < amount) {
                    break;
                }

                iterator.setText(currentChars.substring(finalVar));
            } else {
                break;
            }
        }

        return isBackwards ? -finalVar : finalVar;
    }

    private static String fillText(InputConnection ic, int amount, Boolean isBackwards) {
        if (textAfterCursor.length() < amount && (isBackwards == null || isBackwards.equals(false))) {
            var temp = ic.getTextAfterCursor(amount, 0);
            textAfterCursor = temp == null ? "" : temp.toString();
        }

        if (textBeforeCursor.length() < amount && (isBackwards == null || isBackwards.equals(true))) {
            var temp = ic.getTextBeforeCursor(amount, 0);
            textBeforeCursor = temp == null ? "" : temp.toString();
        }

        if (isBackwards == null) {
            return null;
        } else if (isBackwards.equals(true)) {
            return textBeforeCursor;
        } else {
            return textAfterCursor;
        }
    }

    public static void performMUCommand(String cmd, String a1, String a2, String a3, InputConnection ic) {
        if (cmd.equals("retypebksp")) {
            ic.setComposingText("", 0);
            int i = currentSelectionStart;
            currentSelectionEnd = i;
            currentCandidateStart = i;
            currentCandidateEnd = i;
        }
    }

    public static void performBackspacing(String mode, boolean singlecharmode, InputConnection ic) {
        if (currentCandidateEnd != currentCandidateStart) {
            changeSelection(ic, currentCandidateStart, currentCandidateStart, currentCandidateStart, currentCandidateStart, null);
            ic.setComposingRegion(currentCandidateStart, currentCandidateStart);
            ic.setSelection(currentCandidateStart, currentCandidateStart);
            ic.deleteSurroundingText(0, currentCandidateEnd - currentCandidateStart);
            return;
        }
        if (currentSelectionStart != currentSelectionEnd) {
            changeSelection(ic, currentSelectionStart, currentSelectionStart, currentSelectionStart, currentSelectionStart, null);
            ic.setComposingRegion(currentSelectionStart, currentSelectionStart);
            ic.setSelection(currentSelectionStart, currentSelectionStart);
            ic.deleteSurroundingText(0, currentSelectionEnd - currentSelectionStart);
            return;
        }

        CharSequence charseq = ic.getTextBeforeCursor(120, 0);
        if (charseq != null) {
            String backbuf = charseq.toString();
            int delcount = NINLib.processBackspaceAllowance(backbuf, mode, singlecharmode ? 1 : 0);
            ic.deleteSurroundingText(delcount, 0);
        }
    }

    public static void processTextOps() {
        InputConnection ic;
        TextOp theop;
        int origbatchcount = textopbuffer.size();
        if (origbatchcount != 0 && (ic = globalsoftkeyboard.getCurrentInputConnection()) != null) {
            ic.beginBatchEdit();
            for (int i = 0; i < origbatchcount && (theop = textopbuffer.poll()) != null; i++) {
                TextOp thenext = textopbuffer.peek();
                boolean skipit = false;
                Log.d("NIN", theop + " ---- " + (thenext == null ? "null" : thenext.toString()));
                if (thenext != null && ((thenext.type == 's' || thenext.type == 'l') && theop.type == 'l')) {
                    skipit = true;
                }
                if (!skipit && ic != null) {
                    if (theop.type == 's') {
                        if (theop.strarg.equals("\n")) {
                            int action = 1;
                            if ((globalsoftkeyboard.getCurrentInputEditorInfo().imeOptions & 1073741824) == 0) {
                                action = globalsoftkeyboard.getCurrentInputEditorInfo().imeOptions & 255;
                            }
                            if (action != 1) {
                                ic.performEditorAction(action);
                            } else {
                                keyDownUp(ic, 66, 0, 0, KeyEvent.FLAG_SOFT_KEYBOARD);
                            }
                        } else {
                            if (theop.strarg.startsWith("<{") && theop.strarg.endsWith("}>")) {
                                String taskerstring = theop.strarg.substring(2, theop.strarg.length() - 2);
                                if (HandleSpecialText(ic, theop, taskerstring)) return;
                            } else {
                                ic.commitText(theop.strarg, 1);
                            }
                        }
                    } else if (theop.type == 'e') {
                        performSetSelection(theop.intarg1, theop.intarg2, theop.boolarg, theop.boolarg2, ic);
                    } else if (theop.type == 'r') {
                        performBackReplacement(theop.intarg1, theop.intarg2, theop.strarg, ic);
                    } else if (theop.type == 'l') {
                        ic.setComposingText(theop.strarg, 1);
                    } else if (theop.type == '<') {
                        performBackspacing(theop.strarg, theop.boolarg, ic);
                    } else if (theop.type == 'b') {
                        performBackspacing(theop.strarg, theop.boolarg, ic);
                    } else if (theop.type != 'u') {
                        if (theop.type == '!') {
                            signalCursorCandidacyResult(ic, "requestsel");
                        } else if (theop.type == 'C') {
                            performMUCommand(theop.strarg, theop.a1, theop.a2, theop.a2, ic);
                        } else if (theop.type == 'm') {
                            performCursorMovement(theop.intarg1, theop.intarg2, theop.boolarg,  ic);
                        }
                    }
                }
            }
            updatesel_byroenflag = true;
            updatesel_byroen_lasttimemilli = System.currentTimeMillis();
            ic.endBatchEdit();
        }
    }

    private static boolean HandleSpecialText(InputConnection ic, TextOp theop, String taskerstring) {
        if (taskerstring.startsWith("k")) {
            String substring = taskerstring.substring(1);
            String[] parts = substring.split("\\|");
            if (parts.length < 1) {
                return true;
            }
            int keycode = Integer.parseInt(parts[0]);

            int modifiers = 0;
            if (parts.length > 1) {
                modifiers = Integer.parseInt(parts[1]);
            }
            int repeat = 0;
            if (parts.length > 2) {
                repeat = Integer.parseInt(parts[2]);
            }

            int flags = KeyEvent.FLAG_SOFT_KEYBOARD;
            if (parts.length > 2) {
                flags = Integer.parseInt(parts[2]);
            }

            keyDownUp(ic, keycode, modifiers, repeat, flags);
        }
        else {
            TaskerPluginEventKt.triggerBasicTaskerEvent(globalcontext, theop.strarg);
            ic.commitText(theop.strarg, 1);
        }
        return false;
    }

    /* loaded from: classes.dex */
    static class PerformTextOpsTask implements Runnable {
        PerformTextOpsTask() {
        }

        @Override // java.lang.Runnable
        public void run() {
            SoftKeyboard softKeyboard = SoftKeyboard.globalsoftkeyboard;
            SoftKeyboard.processTextOps();
        }
    }

    public static void callMUCommand(String cmd, String a1, String a2, String a3) {
        TextOp topush = new TextOp('C', 0, 0, false, false, null);
        topush.strarg = cmd;
        topush.a1 = a1;
        topush.a2 = a2;
        topush.a3 = a3;
        doTextOp(topush);
    }

    public static void callRequestSel() {
        doTextOp(new TextOp('!', 0, 0, false, false, null));
    }

    public static void callSetSel(int startpoint, int endpoint, boolean fromstart, boolean dontsignal) {
        doTextOp(new TextOp('e', startpoint, endpoint, fromstart, dontsignal, null));
    }

    public static void callDragCursorUp(int releasedir) {
        doTextOp(new TextOp('u', releasedir, 0, false, false, null));
    }

    public static void callDragCursorMove(int xmove, int ymove, boolean selmode) {
        doTextOp(new TextOp('m', xmove, ymove, selmode, false, null));
    }

    public static void callSimpleBackspace(boolean simplecharmode) {
        doTextOp(new TextOp('<', 0, 0, simplecharmode, false, null));
    }

    public static void callBackReplacement(int rawbackindex, String oldstr, String lestr) {
        doTextOp(new TextOp('r', rawbackindex, oldstr.length(), true, false, lestr));
    }

    public static void callBackspaceModed(String lestr) {
        doTextOp(new TextOp('b', 0, 0, true, false, lestr));
    }

    public static void callMarkLiquid(String lestr) {
        doTextOp(new TextOp('l', 0, 0, true, false, lestr));
    }

    public static void callSolidify(String lestr) {
        TextOp op = new TextOp('s', 0, 0, true, false, lestr);
        doTextOp(op);
    }

    public static void doTextOp(TextOp op) {
        textopbuffer.add(op);
        new Handler(Looper.getMainLooper()).post(new PerformTextOpsTask());
    }

    public static void doTextEvent(TextboxEvent event) {
        textboxeventsbuffer.add(event);
    }

    @Override // android.inputmethodservice.InputMethodService
    public View onCreateInputView() {
        globalcontext = this;
        Sistm.assignAppContext(this);
        NINView nINView = theopenglview;
        if (nINView == null) {
            theopenglview = new NINView(this);
        } else {
            ViewGroup thepar = (ViewGroup) nINView.getParent();
            if (thepar != null) {
                thepar.removeView(theopenglview);
            }
        }
        System.out.println("jormoust ---- ONCREATEINPUTVIEW!!!");
        return theopenglview;
    }

    @Override // android.inputmethodservice.InputMethodService
    public View onCreateCandidatesView() {
        return null;
    }

    @Override // android.inputmethodservice.InputMethodService
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        PrintStream printStream = System.out;
        printStream.println("------------ jormoust Editor Info : " + attribute.packageName + " | " + attribute.fieldName + "|" + attribute.inputType);
        String typemode = "";
        switch (attribute.inputType & 15) {
            case 1:
                int variation = attribute.inputType & 4080;
                typemode = "uri";
                if (variation == 128) {
                    typemode = "passwd";
                }
                int i = attribute.inputType & 65536;
                break;
        }
        TextboxEvent inf = new TextboxEvent(TextboxEventType.APPFIELDCHANGE, attribute.packageName, attribute.fieldName, typemode);
        textboxeventsbuffer.add(inf);
        signalCursorCandidacyResult(globalsoftkeyboard.getCurrentInputConnection(), "startInputView");
    }

    @Override // android.inputmethodservice.InputMethodService
    public void onFinishInput() {
        super.onFinishInput();
    }

    @Override // android.inputmethodservice.InputMethodService
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype letype) {
    }

    @Override // android.inputmethodservice.InputMethodService
    public void onUnbindInput() {
        Utils.prin("----------onUnbindInput!");
    }

    @Override // android.inputmethodservice.InputMethodService
    public void onViewClicked(boolean focusChanged) {
        InputConnection curconn = getCurrentInputConnection();
        if (curconn != null) {
            curconn.setComposingRegion(-1, -1);
            signalCursorCandidacyResult(curconn, "onViewClicked");
        }
    }

    @Override // android.inputmethodservice.InputMethodService
    public void onUpdateCursor(Rect newCursor) {
    }

    @Override // android.inputmethodservice.InputMethodService
    public void onDisplayCompletions(CompletionInfo[] completions) {
    }

    private boolean translateKeyDown(int keyCode, KeyEvent event) {
        return true;
    }

    static void keyDownUp(InputConnection ic, int keyEventCode, int modifiers, int repeat, int flags) {
        ic.sendKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyEventCode, repeat, modifiers, -1, 0, flags));
        ic.sendKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_UP, keyEventCode, repeat, modifiers, -1, 0, flags));
    }

    @Override // android.inputmethodservice.InputMethodService, android.view.KeyEvent.Callback
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    @Override // android.inputmethodservice.InputMethodService, android.view.KeyEvent.Callback
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }

    private void commitTyped(InputConnection inputConnection) {
        inputConnection.commitText("haha!", "haha!".length());
    }

    private void updateShiftKeyState(EditorInfo attr) {
        if (attr != null) {
            getCurrentInputConnection().getCursorCapsMode(attr.inputType);
        }
    }

    private void handleClose() {
        commitTyped(getCurrentInputConnection());
    }
}
