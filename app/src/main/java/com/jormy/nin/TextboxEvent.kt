package com.jormy.nin

class TextboxEvent @JvmOverloads constructor(
    val type: TextboxEventType,
    val mainarg: String? = null,
    val arg1: String? = null,
    val arg2: String? = null,
    val codemode: String? = null
)
