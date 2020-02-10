package com.github.pavlospt.kotlin.logger

import android.util.Log

object AndroidRxFileLogger : RxFileLogger {
    override fun log(message: String) {
        Log.d(this::class.java.simpleName, message)
    }

    override fun log(throwable: Throwable) {
        Log.e(this::class.java.simpleName, "", throwable)
    }
}
