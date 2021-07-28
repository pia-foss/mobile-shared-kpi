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

import android.content.Context
import com.privateinternetaccess.kpi.internals.KPI.Companion.AGGREGATED_ID_PERSISTENCY_KEY
import com.privateinternetaccess.kpi.internals.KPI.Companion.EVENTS_BATCH_PERSISTENCY_KEY
import com.privateinternetaccess.kpi.internals.KPI.Companion.EVENTS_SAMPLE_PERSISTENCY_KEY
import com.privateinternetaccess.kpi.internals.KPI.Companion.json
import com.privateinternetaccess.kpi.internals.model.KPIEvent
import com.privateinternetaccess.kpi.internals.model.KPIEventIdentifier
import com.privateinternetaccess.kpi.internals.model.KPIPersistedEvents


internal actual object KPIPersistency {

    private const val SHARED_PREFS_NAME = "kpi_shared_preferences"

    actual fun persistIdentifier(identifier: KPIEventIdentifier) {
        val context = KPIContextProvider.applicationContext ?: return

        val sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(
            AGGREGATED_ID_PERSISTENCY_KEY,
            json.encodeToString(
                KPIEventIdentifier.serializer(),
                identifier
            )
        ).apply()
    }

    actual fun identifier(): KPIEventIdentifier? {
        val context = KPIContextProvider.applicationContext ?: return null
        val sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(AGGREGATED_ID_PERSISTENCY_KEY, null)?.let {
            json.decodeFromString(KPIEventIdentifier.serializer(), it)
        }
    }

    actual fun persistEvent(event: KPIEvent) {
        val context = KPIContextProvider.applicationContext ?: return

        // Update batched events.
        val batchedEvents = events().toMutableList()
        batchedEvents.add(event)
        val sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(
            EVENTS_BATCH_PERSISTENCY_KEY,
            json.encodeToString(
                KPIPersistedEvents.serializer(),
                KPIPersistedEvents(batchedEvents)
            )
        ).apply()

        // Update sample events.
        val recentEvents = sampleEvents().toMutableList()
        if (recentEvents.size > KPI.EVENTS_HISTORY_SIZE) {
            recentEvents.removeFirst()
        }
        recentEvents.add(event)
        sharedPreferences.edit().putString(
            EVENTS_SAMPLE_PERSISTENCY_KEY,
            json.encodeToString(
                KPIPersistedEvents.serializer(),
                KPIPersistedEvents(recentEvents)
            )
        ).apply()
    }

    actual fun events(): List<KPIEvent> {
        return persistedEventsForKey(EVENTS_BATCH_PERSISTENCY_KEY)
    }

    actual fun sampleEvents(): List<KPIEvent> {
        return persistedEventsForKey(EVENTS_SAMPLE_PERSISTENCY_KEY)
    }

    actual fun clearBatchedEvents() {
        val context = KPIContextProvider.applicationContext ?: return
        val sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().remove(EVENTS_BATCH_PERSISTENCY_KEY).apply()
    }

    actual fun clearAll() {
        val context = KPIContextProvider.applicationContext ?: return
        val sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
    }

    // region private
    private fun persistedEventsForKey(key: String): List<KPIEvent> {
        val context = KPIContextProvider.applicationContext ?: return emptyList()
        val sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        val encodedEvents = sharedPreferences.getString(key, "") ?: ""
        if (encodedEvents.isEmpty()) {
            return emptyList()
        }

        return json.decodeFromString(KPIPersistedEvents.serializer(), encodedEvents).events
    }
    // endregion
}