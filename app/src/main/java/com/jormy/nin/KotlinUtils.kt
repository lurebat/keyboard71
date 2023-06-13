package com.jormy.nin

object KotlinUtils {
    fun <T> T.changeIf(check: T, alt: T): T {
        return if (this == check) alt else this
    }
}
