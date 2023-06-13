package com.jormy.nin

data class TextOp @JvmOverloads constructor(
    @JvmField val type: Char,
    @JvmField val intarg1: Int = 0,
    @JvmField val intarg2: Int = 0,
    @JvmField val boolarg: Boolean = false,
    @JvmField val boolarg2: Boolean = false,
    @JvmField val strarg: String? = null,
    @JvmField val a1: String? = null,
    @JvmField val a2: String? = null,
    @JvmField val a3: String? = null
) {
    constructor(type: Char, boolarg: Boolean) : this(type, 0, 0, boolarg)
    constructor(type: Char, strarg: String?) : this(type, 0, 0, false, false, strarg)
}
