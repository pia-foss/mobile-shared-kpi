package com.privateinternetaccess.kpi.internals

import platform.CoreFoundation.CFAllocatorGetDefault
import platform.CoreFoundation.CFUUIDCreate
import platform.CoreFoundation.CFUUIDCreateString
import platform.Foundation.CFBridgingRelease
import platform.Foundation.NSUUID

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
 
internal actual object KPIIdentifier {

    actual fun uuid(): String {
        return NSUUID.UUID().UUIDString
    }
}