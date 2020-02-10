package com.github.pavlospt.kotlin.logger

object NoOpRxFileLogger : RxFileLogger {
    override fun log(message: String) = Unit
    override fun log(throwable: Throwable) = Unit
}
