package com.lurebat.keyboard71

import com.jormy.nin.Api

class Native {


    companion object
    {
        @JvmStatic
        @Api
        external fun echo(j: String): String

        init {
            System.loadLibrary("keyboard71");
        }
    }
}
