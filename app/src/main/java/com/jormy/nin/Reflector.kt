package com.jormy.nin

@Api
internal object Reflector {

    @JvmStatic
    @Api
    fun analyzeClass(className: String): String {
        return try {
            val theClass = Class.forName(className)
            val builder = StringBuilder()
            for (method in theClass.declaredMethods) {
                builder.append(method.toGenericString())
                builder.append("\n")
            }
            builder.toString()
        } catch (e: ClassNotFoundException) {
            "-- class not found!"
        } catch (e2: Exception) {
            "-- exception : $e2"
        }
    }
}

annotation class Api
