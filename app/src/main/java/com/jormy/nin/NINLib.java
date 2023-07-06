package com.jormy.nin;

import com.lurebat.keyboard71.Native;

@Api
public class NINLib {
    @Api
    public static int getUnicodeBackIndex(String str, int i) {
        return Native.getUnicodeBackIndex(str, i);
    }

    @Api
    public static int getUnicodeFrontIndex(String str, int i) {
        return Native.getUnicodeFrontIndex(str, i);
    }

    @Api
    public static void init(int i, int i2, int i3, int i4) {
        Native.init(i, i2, i3, i4);
    }

    @Api
    public static void memTestStep() {
        Native.memTestStep();
    }

    @Api
    public static void onChangeAppOrTextbox(String str, String str2, String str3) {
        Native.onChangeAppOrTextbox(str, str2, str3);
    }

    @Api
    public static void onEditorChangeTypeClass(String str, String str2) {
        Native.onEditorChangeTypeClass(str, str2);
    }

    @Api
    public static void onExternalSelChange() {
        Native.onExternalSelChange();
    }

    @Api
    public static void onTextSelection(String str, String str2, String str3, String str4) {
        Native.onTextSelection(str, str2, str3, str4);
    }

    @Api
    public static void onTouchEvent(int i, int i2, float f, float f2, float f3, float f4, long j) {
        Native.onTouchEvent(i, i2, f, f2, f3, f4, j);
    }

    @Api
    public static void onWordDestruction(String str, String str2) {
        Native.onWordDestruction(str, str2);
    }

    @Api
    public static int processBackspaceAllowance(String str, String str2, int i) {
        return Native.processBackspaceAllowance(str, str2, i);
    }

    @Api
    public static long processSoftKeyboardCursorMovementLeft(String str) {
        return Native.processSoftKeyboardCursorMovementLeft(str);
    }

    @Api
    public static long processSoftKeyboardCursorMovementRight(String str) {
        return Native.processSoftKeyboardCursorMovementRight(str);
    }

    @Api
    public static void step() {
        Native.step();
    }

    @Api
    public static long syncTiming(long j) {
        return Native.syncTiming(j);
    }
}
