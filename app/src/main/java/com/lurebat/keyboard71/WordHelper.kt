package com.lurebat.keyboard71

import android.util.Log
import android.view.KeyEvent
import com.lurebat.keyboard71.KotlinUtils.changeIf
import java.text.BreakIterator

object WordHelper {

    private val keyCodesMap =
        KeyEvent::class.java.fields.asSequence().filter { it.name.startsWith("KEYCODE_") }
            .associateTo(mutableMapOf()) { Pair(it.name.substring(8), it.getInt(null)) }.apply {
                this["CTRL"] = this["CTRL_LEFT"]!!
                this["SHIFT"] = this["SHIFT_LEFT"]!!
                this["ALT"] = this["ALT_LEFT"]!!
                this["META"] = this["ALT_LEFT"]!!
                this["RIGHT"] = this["DPAD_RIGHT"]!!
                this["LEFT"] = this["DPAD_LEFT"]!!
                this["UP"] = this["DPAD_UP"]!!
                this["DOWN"] = this["DPAD_DOWN"]!!
            }
    private val modifiersMap = KeyEvent::class.java.fields.asSequence()
        .filter { it.name.startsWith("META_") && it.name.endsWith("_ON") }
        .associateTo(mutableMapOf()) {
            Pair(
                it.name.substring(5, it.name.indexOf("_ON")),
                it.getInt(null)
            )
        }

    fun parseKeyCode(keyCode: String): Int = keyCodesMap[keyCode.uppercase()] ?: keyCode.toInt()
    fun parseModifiers(modifiers: String): Int {
        if (modifiers.all { it.isDigit() }) return modifiers.toInt()
        return modifiers.split(",").map { it.trim() }
            .map { modifiersMap[it.uppercase()] ?: it.toInt() }.reduce { acc, i -> acc or i }
    }

    fun lastWordBreak(text: String): Int {
        val iterator = android.icu.text.BreakIterator.getWordInstance()
        iterator.setText(text)
        return try {
            iterator.last()
            iterator.previous().changeIf(BreakIterator.DONE, 0)
        } catch (e: Exception) {
            Log.e("WordHelper", "wordBreakForwards", e)
            0
        }
    }
}
