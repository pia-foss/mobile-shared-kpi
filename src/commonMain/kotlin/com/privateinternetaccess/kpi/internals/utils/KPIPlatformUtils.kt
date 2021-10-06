package com.privateinternetaccess.kpi.internals.utils

import com.privateinternetaccess.kpi.internals.KPI
import com.privateinternetaccess.kpi.internals.model.ElasticEvent
import com.privateinternetaccess.kpi.internals.model.KPIEvent
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.builtins.ListSerializer

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

/**
 * Returns the base64-encoded string representation of srcString encoded in UTF-8.
 */
internal expect fun encodeToUtf8Base64(srcString: String): String

internal object KPIPlatformUtils {
    fun encodeWithKapeFormat(
        events: List<KPIEvent>,
        eventTimeRoundGranularity: KTimeUnit,
        eventTimeSendGranularity: KTimeUnit,
    ): String {
        val sendEvents: List<KPIEvent> = events.map { event ->
            return@map KPIEvent(
                aggregatedId = event.aggregatedId,
                uniqueId = event.uniqueId,
                eventCountry = event.eventCountry,
                eventName = event.eventName,
                eventProperties = event.eventProperties,
                eventTime = adjustSendTime(
                    eventTime = event.eventTime,
                    eventRoundTimeGranularity = eventTimeRoundGranularity,
                    eventSendTimeGranularity = eventTimeSendGranularity
                )
            )
        }
        return KPI.json.encodeToString(ListSerializer(KPIEvent.serializer()), sendEvents)
    }

    fun encodeWithElasticFormat(
        events: List<KPIEvent>,
        projectToken: String,
        eventRoundTimeGranularity: KTimeUnit,
        eventSendTimeGranularity: KTimeUnit
    ): String {
        val elasticEvents: List<ElasticEvent> = events.map { e -> ElasticEvent(e.eventName, createElasticProperties(
            event = e,
            token = projectToken,
            eventRoundTimeGranularity = eventRoundTimeGranularity,
            eventSendTimeGranularity = eventSendTimeGranularity
        )) }
        return "data=${encodeToUtf8Base64(KPI.json.encodeToString(ListSerializer(ElasticEvent.serializer()), elasticEvents))}"
    }

    internal fun createElasticProperties(
        event: KPIEvent,
        token: String,
        eventRoundTimeGranularity: KTimeUnit,
        eventSendTimeGranularity: KTimeUnit
    ): Map<String, String> {
        val properties: MutableMap<String, String> = event.eventProperties.toMutableMap()
        properties["time"] = adjustSendTime(
            eventTime = event.eventTime,
            eventRoundTimeGranularity = eventRoundTimeGranularity,
            eventSendTimeGranularity = eventSendTimeGranularity
        ).toString(10)
        properties["token"] = token
        val country = event.eventCountry
        if (country != null) {
            properties["country"] = event.eventCountry
        }
        return properties.toMap()
    }

    internal fun adjustSendTime(
        eventTime: Long,
        eventRoundTimeGranularity: KTimeUnit,
        eventSendTimeGranularity: KTimeUnit
    ): Long {
        val timeInMilliSeconds: Long = Instant.fromEpochMilliseconds(eventTime).toLocalDateTime(TimeZone.UTC).toInstant(TimeZone.UTC).toEpochMilliseconds()
        return eventSendTimeGranularity.convert(eventRoundTimeGranularity.convert(timeInMilliSeconds, KTimeUnit.MILLISECONDS), eventRoundTimeGranularity)
    }
}
