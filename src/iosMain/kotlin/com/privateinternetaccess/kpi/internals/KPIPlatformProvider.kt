package com.privateinternetaccess.kpi.internals

import com.privateinternetaccess.kpi.KPIHttpLogLevel
import com.privateinternetaccess.kpi.internals.utils.KPILogger


actual class KPIPlatformProvider {

    internal actual val defaultPreferenceName: String = "kpi_shared_preferences"

    private var mKpiPreferences: KPIPreferences? = null
    private var mUserAgent: String? = null
    private var mKpiHttpLogLevel = KPIHttpLogLevel.NONE
    private var mLoggingEnabled = false
    private val mKPILogger: KPILogger = KPILogger { mLoggingEnabled }

    internal actual val userAgent: String?
        get() {
            return mUserAgent
        }

    internal actual val kpiPreferences: KPIPreferences?
        get() {
            return mKpiPreferences
        }

    internal actual val kpiLogger: KPILogger
        get() {
            return mKPILogger
        }

    internal actual val kpiHttpLogLevel: KPIHttpLogLevel
        get() {
            return mKpiHttpLogLevel
        }

    internal actual fun preference(name: String): Boolean {
        mKpiPreferences = KPIPreferences(provider = this, context = null, name = name)
        return kpiPreferences?.isValid ?: false
    }

    internal actual fun userAgent(userAgent: String?): Boolean {
        mUserAgent = userAgent
        return true
    }

    internal actual fun loggingEnabled(enabled: Boolean) {
        mLoggingEnabled = enabled
    }

    internal actual fun kpiLogLevel(logLevel: KPIHttpLogLevel): Boolean {
        mKpiHttpLogLevel = logLevel
        return true
    }
}