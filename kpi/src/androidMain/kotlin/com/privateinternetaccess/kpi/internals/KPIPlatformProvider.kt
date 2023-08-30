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

import com.privateinternetaccess.kpi.KPIHttpLogLevel
import com.privateinternetaccess.kpi.internals.utils.KPILogger


internal actual class KPIPlatformProvider actual constructor(
    preferenceName: String,
    actual val userAgent: String?,
    loggingEnabled: Boolean,
    actual val httpLogLevel: KPIHttpLogLevel
)  {

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