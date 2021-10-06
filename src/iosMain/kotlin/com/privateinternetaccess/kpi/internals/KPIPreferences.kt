package com.privateinternetaccess.kpi.internals

import platform.Foundation.NSUserDefaults


internal actual class KPIPreferences actual constructor(
    private val provider: KPIPlatformProvider,
    context: Any?, // TODO(juan.docal): This shouldn't be here. Fix context injection.
    private val name: String
) {
    companion object {
        private val TAG: String = KPIPreferences::class.simpleName!!
    }

    private val userDefaults = NSUserDefaults(suiteName = name)

    actual val isValid = true

    actual fun getString(key: String, default: String?): String? {
        return logErrors {
            return@logErrors userDefaults.stringForKey(key) ?: default
        }
    }

    actual fun putString(key: String, value: String) {
        logErrors<Unit> {
            userDefaults.setObject(forKey = key, value = value)
            userDefaults.synchronize()
        }
    }

    actual fun remove(key: String) {
        logErrors<Unit> {
            userDefaults.removeObjectForKey(key)
            userDefaults.synchronize()
        }
    }

    actual fun clear() {
        logErrors<Unit> {
            userDefaults.removePersistentDomainForName(name)
            userDefaults.synchronize()
         }
    }

    private fun <T> logErrors(toRun: () -> T): T {
        return try {
            toRun()
        } catch (t: Throwable) {
            provider.kpiLogger.logError(tag = TAG, message = t.stackTraceToString())
            throw t
        }
    }
}
