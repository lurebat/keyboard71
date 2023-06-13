package com.jormy.nin

object NINLib {
    external fun getUnicodeBackIndex(str: String?, i: Int): Int
    external fun getUnicodeFrontIndex(str: String?, i: Int): Int
    @JvmStatic
    external fun init(i: Int, i2: Int, i3: Int, i4: Int)
    external fun memTestStep()
    @JvmStatic
    external fun onChangeAppOrTextbox(str: String?, str2: String?, str3: String?)
    @JvmStatic
    external fun onEditorChangeTypeClass(str: String?, str2: String?)
    @JvmStatic
    external fun onExternalSelChange()
    @JvmStatic
    external fun onTextSelection(str: String?, str2: String?, str3: String?, str4: String?)
    @JvmStatic
    external fun onTouchEvent(i: Int, i2: Int, f: Float, f2: Float, f3: Float, f4: Float, j: Long)
    @JvmStatic
    external fun onWordDestruction(str: String?, str2: String?)
    external fun processBackspaceAllowance(str: String?, str2: String?, i: Int): Int
    @JvmStatic
    external fun processSoftKeyboardCursorMovementLeft(str: String?): Long
    @JvmStatic
    external fun processSoftKeyboardCursorMovementRight(str: String?): Long
    @JvmStatic
    external fun step()
    @JvmStatic
    external fun syncTiming(j: Long): Long

    init {
        System.loadLibrary("gl2jni")
    }
}
