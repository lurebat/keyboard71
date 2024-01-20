package com.lurebat.keyboard71

object Utils {
    fun <T> T.changeIf(check: T, alt: T): T {
        return if (this == check) alt else this
    }
    fun isDebug(): Boolean {
        @Suppress("KotlinConstantConditions")
        return BuildConfig.BUILD_TYPE == "debug"
    }
}
