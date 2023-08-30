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
import com.privateinternetaccess.kpi.internals.KPI.Companion.AGGREGATED_ID_PERSISTENCY_KEY
import com.privateinternetaccess.kpi.internals.KPI.Companion.EVENTS_BATCH_PERSISTENCY_KEY
import com.privateinternetaccess.kpi.internals.KPI.Companion.EVENTS_SAMPLE_PERSISTENCY_KEY
import com.privateinternetaccess.kpi.internals.KPI.Companion.json
import com.privateinternetaccess.kpi.internals.model.KPIEvent
import com.privateinternetaccess.kpi.internals.model.KPIEventIdentifier
import kotlinx.serialization.builtins.ListSerializer


internal class KPIPersistencyImpl internal constructor(
    private val kpiProvider: KPIPlatformProvider
): IKPIPersistency {
    private val TAG: String = KPIPersistencyImpl::class.simpleName!!

    override fun persistIdentifier(identifier: KPIEventIdentifier) {
        try {
            val kpiPreferences: KPIPreferences? = kpiProvider.preferences
            if (kpiPreferences?.isValid != true) {
                kpiProvider.logger.logDebug(tag = TAG, message = "KPIPreferences are invalid")
                return
            }
            kpiPreferences.putString(
                AGGREGATED_ID_PERSISTENCY_KEY,
                json.encodeToString(
                    KPIEventIdentifier.serializer(),
                    identifier
                )
            )
        } catch (t: Throwable) {
            kpiProvider.logger.logError(tag = TAG, message = t.stackTraceToString())
        }
    }

    override fun identifier(): KPIEventIdentifier? {
        try {
            val kpiPreferences: KPIPreferences? = kpiProvider.preferences
            if(kpiPreferences?.isValid != true) {
                kpiProvider.logger.logDebug(tag = TAG, message = "KPIPreferences are invalid")
                return null
            }

            return try {
                val value = kpiPreferences.getString(key = AGGREGATED_ID_PERSISTENCY_KEY, default = null) ?: return null
                json.decodeFromString(KPIEventIdentifier.serializer(), value)
            } catch (t: Throwable) {
                kpiPreferences.remove(AGGREGATED_ID_PERSISTENCY_KEY)
                null
            }
        } catch (t: Throwable) {
            kpiProvider.logger.logError(tag = TAG, message = t.stackTraceToString())
            return null
        }
    }

    override fun persistEvent(event: KPIEvent, eventHistorySize: Int) {
        val kpiPreferences: KPIPreferences? = kpiProvider.preferences
        if (kpiPreferences?.isValid != true) {
            kpiProvider.logger.logDebug(tag = TAG, message = "KPIPreferences are invalid")
            return
        }
        // Update batched events.
        val batchedEvents = events().toMutableList()
        batchedEvents.add(event)

        kpiPreferences.putString(
            EVENTS_BATCH_PERSISTENCY_KEY,
            json.encodeToString(
                ListSerializer(KPIEvent.serializer()),
                batchedEvents
            )
        )

        // Update sample events.
        val recentEvents = sampleEvents().toMutableList()
        if (recentEvents.size > eventHistorySize) {
            recentEvents.removeFirst()
        }
        recentEvents.add(event)
        val jsonString = json.encodeToString(
            ListSerializer(KPIEvent.serializer()),
            recentEvents
        )
        kpiPreferences.putString(
            EVENTS_SAMPLE_PERSISTENCY_KEY,
            jsonString
        )
    }

    override fun events(): List<KPIEvent> {
        return persistedEventsForKey(EVENTS_BATCH_PERSISTENCY_KEY)
    }

    override fun sampleEvents(): List<KPIEvent> {
        return persistedEventsForKey(EVENTS_SAMPLE_PERSISTENCY_KEY)
    }

    override fun clearBatchedEvents() {
        val kpiPreferences: KPIPreferences? = kpiProvider.preferences
        if (kpiPreferences?.isValid != true) {
            kpiProvider.logger.logDebug(tag = TAG, message = "KPIPreferences are invalid")
            return
        }
        kpiPreferences.remove(EVENTS_BATCH_PERSISTENCY_KEY)
    }

    override fun clearAll() {
        val kpiPreferences: KPIPreferences? = kpiProvider.preferences
        if (kpiPreferences?.isValid != true) {
            kpiProvider.logger.logDebug(tag = TAG, message = "KPIPreferences are invalid")
            return
        }
        kpiPreferences.remove(EVENTS_BATCH_PERSISTENCY_KEY)
        kpiPreferences.remove(EVENTS_SAMPLE_PERSISTENCY_KEY)
    }

    // region private
    private fun persistedEventsForKey(key: String): List<KPIEvent> {
        val kpiPreferences: KPIPreferences? = kpiProvider.preferences
        if (kpiPreferences?.isValid != true) {
            kpiProvider.logger.logDebug(tag = TAG, message = "KPIPreferences are invalid")
            return emptyList()
        }
        val encodedEvents: String = kpiPreferences.getString(key, "") ?: ""
        if (encodedEvents.isBlank()) {
            kpiProvider.logger.logDebug(tag = TAG, message = "KPIPreferences: no data for key '$key'")
            return emptyList()
        }
        return try {
            json.decodeFromString(ListSerializer(KPIEvent.serializer()), encodedEvents)
        } catch (t: Throwable) {
            kpiPreferences.remove(key)
            emptyList()
        }
    }
    // endregion
}