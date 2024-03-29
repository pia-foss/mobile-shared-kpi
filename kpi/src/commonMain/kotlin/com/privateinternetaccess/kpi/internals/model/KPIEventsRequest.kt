package com.privateinternetaccess.kpi.internals.model.request

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

import com.privateinternetaccess.kpi.internals.model.KPIEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * Data class defining the list of events to be submitted to the api.
 *
 * @param events `List<KPIEvent>`.
 */
@Serializable
internal class KPIEventsRequest(
    @SerialName("events")
    val events: List<KPIEvent>
)
