package com.privateinternetaccess.kpi.internals.utils

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

import com.privateinternetaccess.kpi.KPIClientEvent
import com.privateinternetaccess.kpi.internals.KPIIdentifier
import com.privateinternetaccess.kpi.internals.KPIPersistency
import com.privateinternetaccess.kpi.internals.model.KPIEvent
import com.privateinternetaccess.kpi.internals.model.KPIEventIdentifier
import kotlinx.datetime.*
import kotlin.time.ExperimentalTime


internal expect object KPIPlatformUtils {
    fun platformIdentifier(): String
}

internal object KPIEventUtils {

    internal fun adaptEvent(event: KPIClientEvent, appVersion: String): KPIEvent {
        val eventProperties = KPIEvent.EventProperties(
            event.eventProperties.connectionSource.value,
            event.eventProperties.data,
            KPIPlatformUtils.platformIdentifier(),
            event.eventProperties.preRelease,
            event.eventProperties.reason,
            event.eventProperties.serverIdentifier,
            event.eventProperties.userAgent,
            appVersion,
            event.eventProperties.vpnProtocol.value
        )
        val datetime = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val reducedDatetime = LocalDateTime(datetime.year, datetime.month, datetime.dayOfMonth, datetime.hour, 0, 0, 0)
        return KPIEvent(
            aggregatedIdentifier(),
            KPIIdentifier.uuid(),
            event.eventCountry,
            event.eventName.value,
            eventProperties,
            reducedDatetime.toInstant(TimeZone.UTC).epochSeconds,
            event.eventToken
        )
    }

    // region private
    @OptIn(ExperimentalTime::class)
    private fun aggregatedIdentifier(): String {
        var identifier =
            KPIPersistency.identifier() ?:
            newAggregatedIdentifier()

        if ((Clock.System.now().toLocalDateTime(TimeZone.UTC).toInstant(TimeZone.UTC) - Instant.parse(identifier.createdAt)).inDays > 1.0) {
            identifier = newAggregatedIdentifier()
            KPIPersistency.clearAll()
        }

        KPIPersistency.persistIdentifier(identifier)
        return identifier.aggregatedId
    }

    private fun newAggregatedIdentifier() =
        KPIEventIdentifier(
            KPIIdentifier.uuid(),
            Clock.System.now().toLocalDateTime(TimeZone.UTC).toInstant(TimeZone.UTC).toString()
        )
    // endregion
}