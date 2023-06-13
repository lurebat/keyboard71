package com.jormy.nin

class TextboxEvent @JvmOverloads constructor(
    @JvmField val type: TextboxEventType,
    @JvmField val mainarg: String? = null,
    @JvmField val arg1: String? = null,
    @JvmField val arg2: String? = null,
    @JvmField val codemode: String? = null
)
