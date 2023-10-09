package com.privateinternetaccess.kpi.internals

import com.privateinternetaccess.kpi.KPIHttpLogLevel
import com.privateinternetaccess.kpi.internals.utils.KPILogger


internal actual class KPIPlatformProvider actual constructor(
    preferenceName: String,
    actual val userAgent: String?,
    loggingEnabled: Boolean,
    actual val  httpLogLevel: KPIHttpLogLevel
) {

    private var mPreferences: KPIPreferences
    private val mLogger: KPILogger = KPILogger { loggingEnabled }

    init {
        mPreferences = KPIPreferences(provider = this, preferenceName = preferenceName)
    }

    internal actual val preferences: KPIPreferences
        get() {
            return mPreferences
        }

    internal actual val logger: KPILogger
        get() {
            return mLogger
        }
}