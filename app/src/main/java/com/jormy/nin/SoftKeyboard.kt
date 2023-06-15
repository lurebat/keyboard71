
import com.jormy.nin.Api

@Api
class SoftKeyboard {
    companion object {
        @Api
        @JvmStatic
        fun callMUCommand(command: String, arg1: String?, arg2: String?, arg3: String?) {
            com.lurebat.keyboard71.SoftKeyboard.callMUCommand(command, arg1, arg2, arg3)
        }

        @Api
        @JvmStatic
        fun callRequestSel() {
            com.lurebat.keyboard71.SoftKeyboard.callRequestSel()
        }

        @Api
        @JvmStatic
        fun callSetSel(start: Int, end: Int, fromStart: Boolean, dontSignal: Boolean) {
            com.lurebat.keyboard71.SoftKeyboard.callSetSel(start, end, fromStart, dontSignal)
        }

        @Api
        @JvmStatic
        fun callDragCursorUp(releasedDirection: Int) {
            com.lurebat.keyboard71.SoftKeyboard.callDragCursorUp(releasedDirection)
        }

        @Api
        @JvmStatic
        fun callDragCursorMove(xMovement: Int, yMovement: Int, selectionMode: Boolean) {
            com.lurebat.keyboard71.SoftKeyboard.callDragCursorMove(
                xMovement,
                yMovement,
                selectionMode
            )
        }

        @Api
        @JvmStatic
        fun callSimpleBackspace(singleCharacterMode: Boolean) {
            com.lurebat.keyboard71.SoftKeyboard.callSimpleBackspace(singleCharacterMode)
        }

        @Api
        @JvmStatic
        fun callBackReplacement(
            backIndexFromCursorBytes: Int,
            oldString: String,
            newString: String
        ) {
            com.lurebat.keyboard71.SoftKeyboard.callBackReplacement(
                backIndexFromCursorBytes,
                oldString,
                newString
            )
        }

        @Api
        @JvmStatic
        fun callBackspaceModed(string: String) {
            com.lurebat.keyboard71.SoftKeyboard.callBackspaceModed(string)
        }

        @Api
        @JvmStatic
        fun callMarkLiquid(string: String) {
            com.lurebat.keyboard71.SoftKeyboard.callMarkLiquid(string)
        }

        @Api
        @JvmStatic
        fun callSolidify(string: String) {
            com.lurebat.keyboard71.SoftKeyboard.callSolidify(string)
        }
    }
}
