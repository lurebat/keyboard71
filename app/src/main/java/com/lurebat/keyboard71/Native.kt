package com.lurebat.keyboard71

import com.jormy.nin.Api

class Native {
    companion object
    {
        @JvmStatic
        @Api
        external fun getUnicodeBackIndex(str: String?, i: Int): Int

        @JvmStatic
        @Api
        external fun getUnicodeFrontIndex(str: String?, i: Int): Int

        @JvmStatic
        @Api
        external fun init(i: Int, i2: Int, i3: Int, i4: Int)

        @JvmStatic
        @Api
        external fun memTestStep()

        @JvmStatic
        @Api
        external fun onChangeAppOrTextbox(str: String?, str2: String?, str3: String?)

        @JvmStatic
        @Api
        external fun onEditorChangeTypeClass(str: String?, str2: String?)

        @JvmStatic
        @Api
        external fun onExternalSelChange()

        @JvmStatic
        @Api
        external fun onTextSelection(str: String?, str2: String?, str3: String?, str4: String?)

        @JvmStatic
        @Api
        external fun onTouchEvent(
            i: Int,
            i2: Int,
            f: Float,
            f2: Float,
            f3: Float,
            f4: Float,
            j: Long
        )

        @JvmStatic
        @Api
        external fun onWordDestruction(str: String?, str2: String?)

        @JvmStatic
        @Api
        external fun processBackspaceAllowance(str: String?, str2: String?, i: Int): Int

        @JvmStatic
        @Api
        external fun processSoftKeyboardCursorMovementLeft(str: String?): Long

        @JvmStatic
        @Api
        external fun processSoftKeyboardCursorMovementRight(str: String?): Long

        @JvmStatic
        @Api
        external fun step()

        @JvmStatic
        @Api
        external fun syncTiming(j: Long): Long

        @JvmStatic
        @Api
        external fun runShortcut(category: Char, action: String)

        @JvmStatic
        @Api
        external fun getBackup(): ByteArray

        @JvmStatic
        @Api
        external fun setBackup(backup: ByteArray)

        @JvmStatic
        @Api
        external fun backToAlphaBoard()

        init {
            System.loadLibrary("keyboard71");
        }
    }
}
