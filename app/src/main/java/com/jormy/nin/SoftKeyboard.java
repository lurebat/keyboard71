package com.jormy.nin;

import com.lurebat.keyboard71.TextOp;

public class SoftKeyboard {
    @Api
    public static void callMUCommand(String command, String arg1, String arg2, String arg3) {
        com.lurebat.keyboard71.SoftKeyboard.Companion.doTextOp(new TextOp.MuCommand(command, arg1, arg2, arg3));
    }

    @Api
    public static void callRequestSel() {
        com.lurebat.keyboard71.SoftKeyboard.Companion.doTextOp(TextOp.RequestSelection.INSTANCE);
    }

    @Api
    public static void callSetSel(int start, int end, boolean fromStart, boolean dontSignal) {
        com.lurebat.keyboard71.SoftKeyboard.Companion.doTextOp(new TextOp.SetSelection(start, end, fromStart, dontSignal));
    }

    @Api
    public static void callDragCursorUp(int releasedDirection) {
        com.lurebat.keyboard71.SoftKeyboard.Companion.doTextOp(new TextOp.DragCursorUp(releasedDirection));
    }

    @Api
    public static void callDragCursorMove(int xMovement, int yMovement, boolean selectionMode) {
        com.lurebat.keyboard71.SoftKeyboard.Companion.doTextOp(new TextOp.DragCursorMove(xMovement, yMovement, selectionMode));
    }

    @Api
    public static void callSimpleBackspace(boolean singleCharacterMode) {
        com.lurebat.keyboard71.SoftKeyboard.Companion.doTextOp(new TextOp.SimpleBackspace(singleCharacterMode));
    }

    @Api
    public static void callBackReplacement(
            int backIndexFromCursorBytes,
            String oldString,
            String newString
    ) {
        com.lurebat.keyboard71.SoftKeyboard.Companion.doTextOp(new TextOp.BackspaceReplacement(backIndexFromCursorBytes, oldString, newString));
    }

    @Api
    public static void callBackspaceModed(String string) {
        com.lurebat.keyboard71.SoftKeyboard.Companion.doTextOp(new TextOp.BackspaceModed(string));
    }

    @Api
    public static void callMarkLiquid(String string) {
        com.lurebat.keyboard71.SoftKeyboard.Companion.doTextOp(new TextOp.MarkLiquid(string));
    }

    @Api
    public static void callSolidify(String string) {
        com.lurebat.keyboard71.SoftKeyboard.Companion.doTextOp(new TextOp.Solidify(string));
    }
}
