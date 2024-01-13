package com.lurebat.keyboard71

enum class TextEventType {
    SELECTION,
    RESET,
    APP_FIELD_CHANGE,
    WORD_DESTRUCTION,
}

sealed class TextBoxEvent {
    data object Reset : TextBoxEvent()
    data class Selection(
        val currentWord: String?,
        val textBefore: String,
        val textAfter: String,
        val mode: String?
    ) : TextBoxEvent()

    data class AppFieldChange(val packageName: String?, val field: String?, val mode: String?) :
        TextBoxEvent()

    data class WordDestruction(val word: String) : TextBoxEvent()


}
