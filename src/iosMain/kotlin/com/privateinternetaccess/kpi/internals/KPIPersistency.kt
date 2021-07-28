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

import com.privateinternetaccess.kpi.internals.model.KPIEvent
import com.privateinternetaccess.kpi.internals.model.KPIEventIdentifier
import com.privateinternetaccess.kpi.internals.model.KPIPersistedEvents
import platform.Foundation.NSUserDefaults


internal actual object KPIPersistency {

    actual fun persistIdentifier(identifier: KPIEventIdentifier) {
        NSUserDefaults.standardUserDefaults.setObject(
            KPI.json.encodeToString(
                KPIEventIdentifier.serializer(),
                identifier
            ),
            KPI.AGGREGATED_ID_PERSISTENCY_KEY
        )
    }

    actual fun identifier(): KPIEventIdentifier? {
        return NSUserDefaults.standardUserDefaults.stringForKey(KPI.AGGREGATED_ID_PERSISTENCY_KEY)?.let {
            KPI.json.decodeFromString(KPIEventIdentifier.serializer(), it)
        }
    }

    actual fun persistEvent(event: KPIEvent) {

        // Update batched events.
        val batchedEvents = events().toMutableList()
        batchedEvents.add(event)
        NSUserDefaults.standardUserDefaults.setObject(
            KPI.json.encodeToString(
                KPIPersistedEvents.serializer(),
                KPIPersistedEvents(batchedEvents)
            ),
            KPI.EVENTS_BATCH_PERSISTENCY_KEY
        )

        // Update recent events.
        val recentEvents = sampleEvents().toMutableList()
        if (recentEvents.size > KPI.EVENTS_HISTORY_SIZE) {
            recentEvents.removeFirst()
        }
        recentEvents.add(event)
        NSUserDefaults.standardUserDefaults.setObject(
            KPI.json.encodeToString(
                KPIPersistedEvents.serializer(),
                KPIPersistedEvents(recentEvents)
            ),
            KPI.EVENTS_SAMPLE_PERSISTENCY_KEY
        )
    }

    actual fun events(): List<KPIEvent> {
        return persistedEventsForKey(KPI.EVENTS_BATCH_PERSISTENCY_KEY)
    }

    actual fun sampleEvents(): List<KPIEvent> {
        return persistedEventsForKey(KPI.EVENTS_SAMPLE_PERSISTENCY_KEY)
    }

    actual fun clearBatchedEvents() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(KPI.EVENTS_BATCH_PERSISTENCY_KEY)
    }

    actual fun clearAll() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(KPI.EVENTS_BATCH_PERSISTENCY_KEY)
        NSUserDefaults.standardUserDefaults.removeObjectForKey(KPI.EVENTS_SAMPLE_PERSISTENCY_KEY)
    }

    // region private
    private fun persistedEventsForKey(key: String): List<KPIEvent> {
        val encodedEvents = NSUserDefaults.standardUserDefaults.stringForKey(key) ?: ""
        if (encodedEvents.isEmpty()) {
            return emptyList()
        }

        return KPI.json.decodeFromString(KPIPersistedEvents.serializer(), encodedEvents).events
    }
    // endregion
}