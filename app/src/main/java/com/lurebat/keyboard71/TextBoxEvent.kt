package com.lurebat.keyboard71

enum class TextEventType {
    SELECTION,
    RESET,
    APP_FIELD_CHANGE,
    WORD_DESTRUCTION,
}

sealed class TextBoxEvent {
    object Reset : TextBoxEvent()
    data class Selection(
        val currentWord: String?,
        val textBefore: String,
        val textAfter: String,
        val mode: String?
    ) : TextBoxEvent()

    data class AppFieldChange(val packageName: String?, val field: String?, val mode: String?, val switchToNumpad: Boolean = false) :
        TextBoxEvent()

    data class WordDestruction(var destroyedWord: String?, var destroyedString: String?) :
        TextBoxEvent()

    // TODO - make proper enums
    data class Shortcut(
        val category: Char,
        val action: String,
    ) : TextBoxEvent()

    companion object {
        fun fromType(type: TextEventType, vararg args: String?): TextBoxEvent {
            return when (type) {
                TextEventType.SELECTION -> Selection(args[0], args[1]!!, args[2]!!, args[3])
                TextEventType.RESET -> Reset
                TextEventType.APP_FIELD_CHANGE -> AppFieldChange(args[0], args[1], args[2])
                TextEventType.WORD_DESTRUCTION -> WordDestruction(args[0], args[1])
            }
        }
    }
}
