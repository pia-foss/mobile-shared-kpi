@file:Suppress("unused")
package com.privateinternetaccess.kpi.internals.utils

import io.github.aakira.napier.Napier

class KPILogger(
    private val isLoggingEnabled: () -> Boolean
) {
    fun logInfo(tag: String, message: String) {
        if(isLoggingEnabled()) {
            Napier.i(tag = tag, message = message)
        }
    }
    fun logDebug(tag: String, message: String) {
        if(isLoggingEnabled()) {
            Napier.d(tag = tag, message = message)
        }
    }
    fun logWarning(tag: String, message: String) {
        if(isLoggingEnabled()) {
            Napier.w(tag = tag, message = message)
        }
    }
    fun logError(tag: String, message: String) {
        if(isLoggingEnabled()) {
            Napier.e(tag = tag, message = message)
        }
    }
    fun logWtf(tag: String, message: String) {
        if(isLoggingEnabled()) {
            Napier.wtf(tag = tag, message = message)
        }
    }
}