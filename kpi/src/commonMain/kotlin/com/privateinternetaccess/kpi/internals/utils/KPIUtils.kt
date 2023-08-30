package com.privateinternetaccess.kpi.internals.utils

import com.privateinternetaccess.kpi.KPIClientEvent
import com.privateinternetaccess.kpi.internals.model.KPIEvent

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


object KPIUtils {
    internal fun isErrorStatusCode(code: Int): Boolean {
        when (code) {
            in 300..399 ->
                // Redirect response
                return true
            in 400..499 ->
                // Client error response
                return true
            in 500..599 ->
                // Server error response
                return true
        }

        if (code >= 600) {
            // Unknown error response
            return true
        }
        return false
    }

    internal fun String.snakeToPascalCase(): String = split("_").joinToString("") { value -> value.capitalized().trim() }

    internal fun String.capitalized() = replaceFirst(first(), first().uppercaseChar())
}