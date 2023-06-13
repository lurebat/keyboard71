package com.jormy.nin;

import android.content.Context;
import android.graphics.Rect;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodSubtype;
import com.jormy.Sistm;
import java.io.PrintStream;
import java.util.concurrent.ConcurrentLinkedQueue;

/* loaded from: classes.dex */
public class SoftKeyboard extends InputMethodService {
    public static Context globalcontext;
    public static SoftKeyboard globalsoftkeyboard;
    public static ConcurrentLinkedQueue<TextboxEvent> textboxeventsbuffer;
    public static ConcurrentLinkedQueue<TextOp> textopbuffer;
    public static NINView theopenglview;
    public static ConcurrentLinkedQueue<WordDestructionInfo> worddestructionbuffer;
    static int global_selstart = 0;
    static int global_selend = 0;
    static int global_candidatestart = 0;
    static int global_candidateend = 0;
    static boolean updatesel_byroenflag = false;
    static long updatesel_byroen_lasttimemilli = 0;

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
        global_selstart = newSelStart;
        global_selend = newSelEnd;
        global_candidatestart = candidatesStart;
        global_candidateend = candidatesEnd;
        long curtimemilli = System.currentTimeMillis();
        if (updatesel_byroenflag || curtimemilli - updatesel_byroen_lasttimemilli < 55) {
            updatesel_byroenflag = false;
        } else {
            signalCursorCandidacyResult(getCurrentInputConnection(), "external");
        }
    }

    public static void signalWordDestruction(String leword, String lestring) {
        worddestructionbuffer.add(new WordDestructionInfo(leword, lestring));
    }

    public static void performBackReplacement(int rawbackindex, int origstrunicodelen, String toreplacewith, InputConnection ic) {
        CharSequence charseq = ic.getTextBeforeCursor(120, 0);
        CharSequence nextseq = ic.getTextAfterCursor(60, 0);
        if (charseq != null) {
            String thebackbuffer = charseq.toString();
            nextseq.toString();
            int usedselstart = global_selstart;
            Utils.prin("-----------------------");
            Utils.prin("PREDOt " + global_candidatestart + " -> " + global_candidateend + " || " + global_selstart + " -> " + global_selend);
            int i = global_candidateend;
            int i2 = global_candidatestart;
            if (i != i2) {
                int candlength = i - i2;
                if (candlength < thebackbuffer.length()) {
                    thebackbuffer = thebackbuffer.substring(0, thebackbuffer.length() - candlength);
                    usedselstart -= candlength;
                } else {
                    return;
                }
            }
            int realbackindex = NINLib.getUnicodeBackIndex(thebackbuffer, rawbackindex);
            Utils.prin("realbackindex : " + realbackindex);
            boolean replacingfuture = realbackindex < origstrunicodelen;
            int replstart = usedselstart - realbackindex;
            if (replstart >= 0) {
                ic.setSelection(replstart, replstart);
                ic.setComposingRegion(replstart, replstart);
                ic.deleteSurroundingText(0, origstrunicodelen);
                ic.commitText(toreplacewith, 1);
                int posshift = toreplacewith.length() - origstrunicodelen;
                global_selstart += posshift;
                global_selend += posshift;
                global_candidatestart += posshift;
                global_candidateend += posshift;
                Utils.prin("candstart " + global_candidatestart + " -> " + global_candidateend + " || " + global_selstart + " -> " + global_selend);
                ic.setComposingRegion(global_candidatestart, global_candidateend);
                ic.setSelection(global_selstart, global_selend);
                if (replacingfuture) {
                    signalCursorCandidacyResult(ic, "backrepl");
                }
            }
        }
    }

    public static void performCursorMovement(int xmove, InputConnection ic) {
        int later2;
        int laterpoint;
        if (xmove < 0) {
            CharSequence charseq = ic.getTextBeforeCursor(120, 0);
            if (charseq != null) {
                String thestr = charseq.toString();
                int usedcurpos = global_selstart;
                int i = global_candidateend;
                if (i == global_selstart) {
                    usedcurpos = global_candidatestart;
                    int thedifference = i - global_candidatestart;
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
                global_selstart = earlpoint2;
                global_selend = earlpoint2;
                global_candidatestart = earlpoint2;
                global_candidateend = laterpoint2;
                signalCursorCandidacyResult(ic, "cursordrag");
                return;
            }
            System.out.println("jormoust :: No charsequence!");
        } else if (xmove > 0) {
            CharSequence charseq2 = ic.getTextAfterCursor(120, 0);
            if (charseq2 != null) {
                String thestr2 = charseq2.toString();
                if (global_candidateend != global_candidatestart) {
                    laterpoint = global_candidateend;
                    later2 = laterpoint;
                } else {
                    int usedcurpos2 = global_selstart;
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
                global_selstart = laterpoint;
                global_selend = laterpoint;
                global_candidatestart = later2;
                global_candidateend = laterpoint;
                signalCursorCandidacyResult(ic, "cursordrag");
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
        int i2 = global_selstart;
        if (i2 == global_selend) {
            int i3 = global_candidatestart;
            int i4 = global_candidateend;
            boolean nullcandidate = i3 == i4 && i3 == -1;
            if (i3 == 0 && i4 == 0) {
                nullcandidate = true;
            }
            if (i3 == i4) {
                nullcandidate = true;
            }
            if (i3 != i2 && i4 != i2 && !nullcandidate) {
                Utils.prin("softkeyboard going haywire!! : " + global_candidatestart + " -> " + global_candidateend + " :: " + global_selstart);
                return;
            }
            boolean nocand = nullcandidate;
            CharSequence pretext_seq = ic.getTextBeforeCursor(200, 0);
            CharSequence posttext_seq = ic.getTextAfterCursor(200, 0);
            String pretext = pretext_seq == null ? "" : pretext_seq.toString();
            String posttext = posttext_seq != null ? posttext_seq.toString() : "";
            String curword = null;
            int i5 = global_candidateend;
            int i6 = global_candidatestart;
            int canlength = i5 - i6;
            if (nocand) {
                canlength = 0;
            }
            if (nocand || i6 == (i = global_selstart)) {
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

    public static void performSetSel(int startingpos, int endpos, boolean fromstart, boolean dontsignal, InputConnection ic) {
        int i;
        int i2;
        int i3;
        int tocut;
        if (startingpos == 0 && endpos == 0 && !fromstart) {
            int laterpoint = global_candidateend;
            ic.setComposingRegion(laterpoint, laterpoint);
            ic.setSelection(laterpoint, laterpoint);
            global_selstart = laterpoint;
            global_selend = laterpoint;
            global_candidatestart = laterpoint;
            global_candidateend = laterpoint;
        } else if (fromstart && startingpos <= 0 && endpos <= 0 && ((i2 = global_candidateend) == (i3 = global_selstart) || i2 == -1)) {
            int usedstart = global_selstart;
            if (i2 == i3) {
                usedstart = global_candidatestart;
            }
            String pretext = ic.getTextBeforeCursor(130, 0).toString();
            int i4 = global_candidateend;
            int i5 = global_candidatestart;
            if (i4 != i5 && (tocut = i4 - i5) <= pretext.length()) {
                pretext = pretext.substring(0, pretext.length() - tocut);
            }
            int uniback = NINLib.getUnicodeBackIndex(pretext, -startingpos);
            int uniend = NINLib.getUnicodeBackIndex(pretext, -endpos);
            int newstart = usedstart - uniback;
            int newend = usedstart - uniend;
            if (newstart < 0) {
                newstart = 0;
            }
            if (newend < 0) {
                newend = 0;
            }
            ic.setComposingRegion(newstart, newend);
            ic.setSelection(newend, newend);
        } else {
            int usedstart2 = global_selend;
            if (fromstart && (i = global_candidatestart) != -1) {
                int shifter = i - global_selend;
                startingpos += shifter;
                endpos += shifter;
            }
            int unistart = 0;
            int uniend2 = 0;
            String pretext2 = null;
            String posttext = null;
            if (startingpos < 0 || endpos < 0) {
                pretext2 = ic.getTextBeforeCursor(130, 0).toString();
            }
            if (startingpos > 0 || endpos > 0) {
                posttext = ic.getTextAfterCursor(120, 0).toString();
            }
            if (startingpos < 0) {
                unistart = -NINLib.getUnicodeBackIndex(pretext2, -startingpos);
            } else if (startingpos > 0) {
                unistart = NINLib.getUnicodeFrontIndex(posttext, startingpos);
            }
            if (endpos < 0) {
                uniend2 = -NINLib.getUnicodeBackIndex(pretext2, -endpos);
            } else if (endpos > 0) {
                uniend2 = NINLib.getUnicodeFrontIndex(posttext, endpos);
            }
            int newstart2 = usedstart2 + unistart;
            int newend2 = usedstart2 + uniend2;
            if (newstart2 < 0) {
                newstart2 = 0;
            }
            if (newend2 < 0) {
                newend2 = 0;
            }
            ic.setComposingRegion(newstart2, newend2);
            ic.setSelection(newend2, newend2);
        }
        if (!dontsignal) {
            signalCursorCandidacyResult(ic, "setselle");
        }
    }

    public static void performMUCommand(String cmd, String a1, String a2, String a3, InputConnection ic) {
        if (cmd.equals("retypebksp")) {
            ic.setComposingText("", 0);
            int i = global_selstart;
            global_selend = i;
            global_candidatestart = i;
            global_candidateend = i;
        }
    }

    public static void performBackspacing(String mode, boolean singlecharmode, InputConnection ic) {
        int i = global_candidateend;
        int i2 = global_candidatestart;
        if (i != i2) {
            int charstodel = i - i2;
            int startpos = global_candidatestart;
            global_candidateend = startpos;
            global_selend = startpos;
            global_selstart = startpos;
            ic.setComposingRegion(startpos, startpos);
            ic.setSelection(startpos, startpos);
            ic.deleteSurroundingText(0, charstodel);
            return;
        }
        int i3 = global_selstart;
        int i4 = global_selend;
        if (i3 != i4) {
            int charstodel2 = i4 - i3;
            int startpos2 = global_selstart;
            global_selend = startpos2;
            global_candidatestart = startpos2;
            global_candidateend = startpos2;
            ic.setComposingRegion(startpos2, startpos2);
            ic.setSelection(startpos2, startpos2);
            ic.deleteSurroundingText(0, charstodel2);
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
                                keyDownUp(ic, 66);
                            }
                        } else {
                            ic.commitText(theop.strarg, 1);
                        }
                    } else if (theop.type == 'e') {
                        performSetSel(theop.intarg1, theop.intarg2, theop.boolarg, theop.boolarg2, ic);
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
                            performCursorMovement(theop.intarg1, ic);
                        }
                    }
                }
            }
            updatesel_byroenflag = true;
            updatesel_byroen_lasttimemilli = System.currentTimeMillis();
            ic.endBatchEdit();
        }
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
        textopbuffer.add(topush);
        new Handler(Looper.getMainLooper()).post(new PerformTextOpsTask());
    }

    public static void callRequestSel() {
        textopbuffer.add(new TextOp('!', 0, 0, false, false, null));
        new Handler(Looper.getMainLooper()).post(new PerformTextOpsTask());
    }

    public static void callSetSel(int startpoint, int endpoint, boolean fromstart, boolean dontsignal) {
        textopbuffer.add(new TextOp('e', startpoint, endpoint, fromstart, dontsignal, null));
        new Handler(Looper.getMainLooper()).post(new PerformTextOpsTask());
    }

    public static void callDragCursorUp(int releasedir) {
        textopbuffer.add(new TextOp('u', releasedir, 0, false, false, null));
        new Handler(Looper.getMainLooper()).post(new PerformTextOpsTask());
    }

    public static void callDragCursorMove(int xmove, int ymove, boolean selmode) {
        textopbuffer.add(new TextOp('m', xmove, ymove, selmode, false, null));
        new Handler(Looper.getMainLooper()).post(new PerformTextOpsTask());
    }

    public static void callSimpleBackspace(boolean simplecharmode) {
        textopbuffer.add(new TextOp('<', 0, 0, simplecharmode, false, null));
        new Handler(Looper.getMainLooper()).post(new PerformTextOpsTask());
    }

    public static void callBackReplacement(int rawbackindex, String oldstr, String lestr) {
        textopbuffer.add(new TextOp('r', rawbackindex, oldstr.length(), true, false, lestr));
        new Handler(Looper.getMainLooper()).post(new PerformTextOpsTask());
    }

    public static void callBackspaceModed(String lestr) {
        textopbuffer.add(new TextOp('b', 0, 0, true, false, lestr));
        new Handler(Looper.getMainLooper()).post(new PerformTextOpsTask());
    }

    public static void callMarkLiquid(String lestr) {
        textopbuffer.add(new TextOp('l', 0, 0, true, false, lestr));
        new Handler(Looper.getMainLooper()).post(new PerformTextOpsTask());
    }

    public static void callSolidify(String lestr) {
        textopbuffer.add(new TextOp('s', 0, 0, true, false, lestr));
        new Handler(Looper.getMainLooper()).post(new PerformTextOpsTask());
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
        printStream.println("------------ jormoust Editor Info : " + attribute.packageName + " | " + attribute.fieldName);
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
            case EXSurfaceView.DEBUG_LOG_GL_CALLS /* 2 */:
            case 3:
                typemode = "numbers";
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

    static void keyDownUp(InputConnection ic, int keyEventCode) {
        ic.sendKeyEvent(new KeyEvent(0, keyEventCode));
        ic.sendKeyEvent(new KeyEvent(1, keyEventCode));
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
