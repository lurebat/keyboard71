package com.lurebat.keyboard71

object KotlinUtils {
    fun <T> T.changeIf(check: T, alt: T): T {
        return if (this == check) alt else this
    }
}
