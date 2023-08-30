package com.privateinternetaccess.kpi.internals

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.privateinternetaccess.kpi.KPIContextProvider


internal actual class KPIPreferences actual constructor(
    private val provider: KPIPlatformProvider,
    preferenceName: String
) {
    private val sharedPreferences: SharedPreferences? = try {
        when (val context = KPIContextProvider.applicationContext()) {
            !is Application -> throw IllegalArgumentException()
            else -> context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
        }
    } catch (t: Throwable) {
        provider.logger.logError(tag = TAG, message = "Cannot create SharedPreference instance: ${t.stackTraceToString()}")
        null
    }

    actual val isValid: Boolean = sharedPreferences != null

    actual fun getString(key: String, default: String?): String? {
        return logErrors {
            val sharedPreferences = sharedPreferences ?: return@logErrors null
            return@logErrors sharedPreferences.getString(key, null) ?: default
        }
    }

    actual fun putString(key: String, value: String) {
        logErrors<Unit> {
            val sharedPreferences = sharedPreferences ?: return@logErrors
            sharedPreferences.edit().putString(key, value).apply()
        }
    }

    actual fun remove(key: String) {
        logErrors<Unit> {
            val sharedPreferences = sharedPreferences ?: return@logErrors
            sharedPreferences.edit().remove(key).apply()
        }
    }

    actual fun clear() {
        logErrors<Unit> {
            val sharedPreferences = sharedPreferences ?: return@logErrors
            sharedPreferences.edit().clear().apply()
        }
    }

    @Throws(Throwable::class)
    private fun <T> logErrors(toRun: () -> T): T {
        sharedPreferences ?: throw RuntimeException("invalid state")
        return try {
            toRun()
        } catch (t: Throwable) {
            provider.logger.logError(tag = TAG, message = t.stackTraceToString())
            throw t
        }
    }

    companion object {
        private val TAG: String = KPIPreferences::class.simpleName!!
    }
}
