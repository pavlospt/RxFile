package com.github.pavlospt.kotlin.logger

interface RxFileLogger {
    fun log(message: String)
    fun log(throwable: Throwable)
}
