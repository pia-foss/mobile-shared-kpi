package com.privateinternetaccess.kpi

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


class KPIContextProvider {

    companion object {

        private var application: Application? = null

        /**
         * Specifies an Android context, that will be used by this library.
         *
         * Note:
         * The library always stores the Application context to avoid memory leaks.
         *
         * @param context instance of Android context.
         */
        public fun setApplicationContext(context: Context) {

            if (context.applicationContext !is Application) {
                throw IllegalArgumentException("Invalid context. Application's Context expected.")
            }
            application = context.applicationContext as Application
        }

        /**
         * @return `Context`. Application's Context.
         */
        internal fun applicationContext(): Context =
            application?.applicationContext ?: throw IllegalStateException("Invalid State. Context not available.")
    }
}