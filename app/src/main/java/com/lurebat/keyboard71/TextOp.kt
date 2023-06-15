package com.lurebat.keyboard71

enum class TextOpType {
    SOLIDIFY,
    MARK_LIQUID,
    SET_SELECTION,
    BACKSPACE_MODED,
    BACKSPACE_REPLACEMENT,
    DRAG_CURSOR_MOVE,
    REQUEST_SELECTION,
    DRAG_CURSOR_UP,
    SIMPLE_BACKSPACE,
    MU_COMMAND,
}

sealed class TextOp {
    data class MuCommand(val command: String, val arg1: String? = null, val arg2: String? = null, val arg3: String? = null) : TextOp()
    object RequestSelection : TextOp()
    data class SetSelection(val start: Int, val end: Int, val fromStart: Boolean, val signal: Boolean) : TextOp()

    data class DragCursorUp(val releasedDirection: Int) : TextOp()

    data class DragCursorMove(val xMovement: Int, val yMovement: Int, val selectionMode: Boolean) : TextOp()
    data class SimpleBackspace(val singleCharacterMode: Boolean) : TextOp()
    data class BackspaceReplacement(val backIndexFromCursorBytes: Int, val oldString: String, val newString: String) : TextOp()
    data class BackspaceModed(val mode: String) : TextOp()
    data class MarkLiquid(val newString: String) : TextOp()
    data class Solidify(val newString: String) : TextOp()
    data class Special(val args: String) : TextOp()

    companion object {
        fun parse(type: TextOpType, intArg1: Int, intArg2: Int, boolArg1: Boolean, boolArg2: Boolean, stringArg1: String?, stringArg2: String?): TextOp {
            val str1 = stringArg1 ?: ""
            val str2 = stringArg2 ?: ""
            return when(type) {
                TextOpType.SOLIDIFY -> Solidify(str1)
                TextOpType.MARK_LIQUID -> MarkLiquid(str1)
                TextOpType.SET_SELECTION -> SetSelection(intArg1, intArg2, boolArg1, !boolArg2)
                TextOpType.BACKSPACE_MODED -> BackspaceModed(str1)
                TextOpType.BACKSPACE_REPLACEMENT -> BackspaceReplacement(intArg1, str1, str2)
                TextOpType.DRAG_CURSOR_MOVE -> DragCursorMove(intArg1, intArg2, boolArg1)
                TextOpType.REQUEST_SELECTION -> RequestSelection
                TextOpType.DRAG_CURSOR_UP -> DragCursorUp(intArg1)
                TextOpType.SIMPLE_BACKSPACE -> SimpleBackspace(boolArg1)
                TextOpType.MU_COMMAND -> MuCommand(str1)
            }
        }
    }
}
