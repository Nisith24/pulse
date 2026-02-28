package com.pulse.core.domain.util

interface ILogger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun v(tag: String, message: String)
}

class AndroidLogger : ILogger {
    override fun d(tag: String, message: String) { android.util.Log.d(tag, message) }
    override fun e(tag: String, message: String, throwable: Throwable?) { android.util.Log.e(tag, message, throwable) }
    override fun v(tag: String, message: String) { android.util.Log.v(tag, message) }
}
