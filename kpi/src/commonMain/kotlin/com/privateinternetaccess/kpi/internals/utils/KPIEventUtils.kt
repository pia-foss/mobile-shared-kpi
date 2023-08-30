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
import com.privateinternetaccess.kpi.internals.IKPIPersistency
import com.privateinternetaccess.kpi.internals.KPIIdentifier
import com.privateinternetaccess.kpi.internals.model.KPIEvent
import com.privateinternetaccess.kpi.internals.model.KPIEventIdentifier
import kotlinx.datetime.*
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime


internal class KPIEventUtils(
    private val kpiPersistency: IKPIPersistency,
    private val projectToken: String
) {
    internal fun adaptEvent(event: KPIClientEvent): KPIEvent {
        val eventProperties = event.eventProperties.toMutableMap()
        return KPIEvent(
            aggregatedId = aggregatedIdentifier(),
            uniqueId = KPIIdentifier.uuid(),
            eventCountry = event.eventCountry,
            eventName = event.eventName,
            eventProperties = eventProperties,
            eventTime = event.eventInstant.toLocalDateTime(TimeZone.UTC).toInstant(TimeZone.UTC).toEpochMilliseconds(),
            eventToken = projectToken
        )
    }

    // region private
    @OptIn(ExperimentalTime::class)
    private fun aggregatedIdentifier(): String {
        var identifier =
            kpiPersistency.identifier() ?:
            newAggregatedIdentifier()

        if ((Clock.System.now().toLocalDateTime(TimeZone.UTC).toInstant(TimeZone.UTC) - Instant.parse(identifier.createdAt)).toDouble(DurationUnit.DAYS) > 1.0) {
            identifier = newAggregatedIdentifier()
            kpiPersistency.clearAll()
        }

        kpiPersistency.persistIdentifier(identifier)
        return identifier.aggregatedId
    }

    private fun newAggregatedIdentifier() =
        KPIEventIdentifier(
            KPIIdentifier.uuid(),
            Clock.System.now().toLocalDateTime(TimeZone.UTC).toInstant(TimeZone.UTC).toString()
        )
    // endregion
}