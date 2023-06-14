package com.jormy.nin

enum class TextEventType {
    SELECTION,
    RESET,
    APP_FIELD_CHANGE,
    WORD_DESTRUCTION,
}

sealed class TextEvent {
    object Reset : TextEvent()
    data class Selection(
        val currentWord: String?,
        val textBefore: String,
        val textAfter: String,
        val mode: String?
    ) : TextEvent()

    data class AppFieldChange(val packageName: String?, val field: String?, val mode: String?) :
        TextEvent()

    data class WordDestruction(var destroyedWord: String?, var destroyedString: String?) :
        TextEvent()

    companion object {
        fun fromType(type: TextEventType, vararg args: String?): TextEvent {
            return when (type) {
                TextEventType.SELECTION -> Selection(args[0], args[1]!!, args[2]!!, args[3])
                TextEventType.RESET -> Reset
                TextEventType.APP_FIELD_CHANGE -> AppFieldChange(args[0], args[1], args[2])
                TextEventType.WORD_DESTRUCTION -> WordDestruction(args[0], args[1])
            }
        }
    }
}
