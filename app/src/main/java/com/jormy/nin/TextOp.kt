package com.jormy.nin

sealed class TextOp {
    data class MuCommand(val command: String, val arg1: String? = null, val arg2: String? = null, val arg3: String? = null) : TextOp()
    class RequestSelection() : TextOp()
    data class SetSelection(val start: Int, val end: Int, val fromStart: Boolean, val signal: Boolean) : TextOp()

    data class DragCursorUp(val releasedDirection: Int) : TextOp()

    data class DragCursorMove(val xMovement: Int, val yMovement: Int, val selectionMode: Boolean) : TextOp()
    data class SimpleBackspace(val singleCharacterMode: Boolean) : TextOp()
    data class BackspaceReplacement(val backIndexFromCursorBytes: Int, val oldString: String, val newString: String) : TextOp()
    data class BackspaceModed(val mode: String) : TextOp()
    data class MarkLiquid(val newString: String) : TextOp()
    data class Solidify(val newString: String) : TextOp()

    companion object {
        fun parse(type: Char, intArg1: Int, intArg2: Int, boolArg1: Boolean, boolArg2: Boolean, stringArg1: String?): TextOp {
            val stringArg1 = stringArg1 ?: ""
            return when(type) {
                's' -> Solidify(stringArg1)
                'm' -> MarkLiquid(stringArg1)
                'e' -> SetSelection(intArg1, intArg2, boolArg1, !boolArg2)
                'b' -> BackspaceModed(stringArg1)
                'r' -> BackspaceReplacement(intArg1, String(), stringArg1)
                'd' -> DragCursorMove(intArg1, intArg2, boolArg1)
                '!' -> RequestSelection()
                'u' -> DragCursorUp(intArg1)
                '<' -> SimpleBackspace(boolArg1)
                else -> MuCommand(stringArg1)
            }
        }
    }
}
