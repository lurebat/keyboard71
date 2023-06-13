package com.jormy.nin;

/* loaded from: classes.dex */
public class NINLib {
    public static native int getUnicodeBackIndex(String str, int i);

    public static native int getUnicodeFrontIndex(String str, int i);

    public static native void init(int i, int i2, int i3, int i4);

    public static native void memTestStep();

    public static native void onChangeAppOrTextbox(String str, String str2, String str3);

    public static native void onEditorChangeTypeClass(String str, String str2);

    public static native void onExternalSelChange();

    public static native void onTextSelection(String str, String str2, String str3, String str4);

    public static native void onTouchEvent(int i, int i2, float f, float f2, float f3, float f4, long j);

    public static native void onWordDestruction(String str, String str2);

    public static native int processBackspaceAllowance(String str, String str2, int i);

    public static native long processSoftKeyboardCursorMovementLeft(String str);

    public static native long processSoftKeyboardCursorMovementRight(String str);

    public static native void step();

    public static native long syncTiming(long j);

    static {
        System.loadLibrary("gl2jni");
    }
}
