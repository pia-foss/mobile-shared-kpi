package com.privateinternetaccess.kpi.internals

/*
 *  Copyright (c) 2021 Private Internet Access, Inc.
 *
 *  This file is part of the Private Internet Access Mobile Client.
 *
 *  The Private Internet Access Mobile Client is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  The Private Internet Access Mobile Client is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with the Private
 *  Internet Access Mobile Client.  If not, see <https://www.gnu.org/licenses/>.
 */

import android.app.Application
import android.content.Context
import com.privateinternetaccess.kpi.KPIHttpLogLevel
import com.privateinternetaccess.kpi.internals.utils.KPILogger
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference


actual class KPIPlatformProvider {

    internal actual val defaultPreferenceName: String = "kpi_shared_preferences"

    private val applicationContext: AtomicReference<Application?> = AtomicReference(null)
    private val mKpiPreferences: AtomicReference<KPIPreferences?> = AtomicReference(null)
    private val mUserAgent: AtomicReference<String?> = AtomicReference(null)
    private val mKpiHttpLogLevel: AtomicReference<KPIHttpLogLevel> = AtomicReference(KPIHttpLogLevel.NONE)
    private val mLoggingEnabled: AtomicBoolean = AtomicBoolean(false)
    private val mKPILogger: KPILogger = KPILogger { mLoggingEnabled.get() }

    internal actual val userAgent: String?
        get() {
            return mUserAgent.get()
        }

    internal actual val kpiPreferences: KPIPreferences?
        get() {
            return mKpiPreferences.get()
        }

    internal actual val kpiLogger: KPILogger
        get() {
            return mKPILogger
        }

    internal actual val kpiHttpLogLevel: KPIHttpLogLevel
        get() {
            return mKpiHttpLogLevel.get()
        }

    /**
     * Android only (has no effect on other platforms):
     *
     * Specifies an Android context, that will be used by this library.
     *
     * Note:
     * The library always stores the Application context to avoid memory leaks.
     * The KPIAPI instance cannot be created, if the Application context does not exist.
     *
     * @param context instance of Android context.
     */
    fun setAndroidContext(context: Context): Boolean {
        val app: Context? = context.applicationContext
        if (app !is Application) {
            return false
        }
        applicationContext.set(app)
        return true
    }

    internal actual fun preference(name: String): Boolean {
        val app: Application = applicationContext.get() ?: return false
        val kpiPreferences = KPIPreferences(provider = this, context = app, name = name)
        this.mKpiPreferences.set(kpiPreferences)
        return kpiPreferences.isValid
    }

    internal actual fun userAgent(userAgent: String?): Boolean {
        mUserAgent.set(userAgent)
        return true
    }

    internal actual fun loggingEnabled(enabled: Boolean) {
        mLoggingEnabled.set(enabled)
    }

    internal actual fun kpiLogLevel(logLevel: KPIHttpLogLevel): Boolean {
        mKpiHttpLogLevel.set(logLevel)
        return true
    }
}