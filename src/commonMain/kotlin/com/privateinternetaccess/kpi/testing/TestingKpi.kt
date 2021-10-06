@file:Suppress("REDUNDANT_ELSE_IN_WHEN", "unused")

package com.privateinternetaccess.kpi.testing

import com.privateinternetaccess.kpi.KPIClientEvent
import com.privateinternetaccess.kpi.KPIClientStateProvider
import com.privateinternetaccess.kpi.KPIRequestFormat
import com.privateinternetaccess.kpi.internals.model.ElasticEvent
import com.privateinternetaccess.kpi.internals.model.KPIEvent
import com.privateinternetaccess.kpi.internals.utils.KPIEventUtils
import com.privateinternetaccess.kpi.internals.utils.KPIPlatformUtils
import com.privateinternetaccess.kpi.internals.utils.KTimeUnit
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class TestingKpi internal constructor(
    private val kpiClientStateProvider: KPIClientStateProvider,
    private val kpiEventUtils: KPIEventUtils,
    private val eventTimeRoundGranularity: KTimeUnit,
    private val eventTimeSendGranularity: KTimeUnit
) {
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val jsonPretty: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun encodeEventListForCache(
        listEvents: List<KPIClientEvent>,
        pretty: Boolean = false
    ): String {
        val listKpiEvent: List<KPIEvent> = listEvents.map { ev -> kpiEventUtils.adaptEvent(ev) }
        return (if (pretty) jsonPretty else json).encodeToString(
            ListSerializer(KPIEvent.serializer()),
            listKpiEvent
        )
    }

    fun encodeEventListForSend(
        listEvents: List<KPIClientEvent>,
        format: KPIRequestFormat,
        pretty: Boolean = false
    ): String {
        val json: Json = (if (pretty) jsonPretty else json)
        when (format) {
            KPIRequestFormat.KAPE -> {
                val listKpiEvent: List<KPIEvent> = listEvents.map { ev ->
                    val ev2 = kpiEventUtils.adaptEvent(ev)
                    return@map KPIEvent(
                        aggregatedId = ev2.aggregatedId,
                        uniqueId = ev2.uniqueId,
                        eventCountry = ev2.eventCountry,
                        eventName = ev2.eventName,
                        eventProperties = ev2.eventProperties,
                        eventTime = KPIPlatformUtils.adjustSendTime(
                            eventTime = ev2.eventTime,
                            eventRoundTimeGranularity = eventTimeRoundGranularity,
                            eventSendTimeGranularity = eventTimeSendGranularity
                        )
                    )
                }
                return json.encodeToString(
                    ListSerializer(KPIEvent.serializer()),
                    listKpiEvent
                )
            }
            KPIRequestFormat.ELASTIC -> {
                val elasticEvents: List<ElasticEvent> = listEvents.map { ev ->
                    val ev2 = kpiEventUtils.adaptEvent(ev)
                    return@map ElasticEvent(
                        ev2.eventName,
                        KPIPlatformUtils.createElasticProperties(
                            event = ev2,
                            token = kpiClientStateProvider.projectToken()!!,
                            eventRoundTimeGranularity = eventTimeRoundGranularity,
                            eventSendTimeGranularity = eventTimeSendGranularity
                        )
                    )
                }
                return json.encodeToString(ListSerializer(ElasticEvent.serializer()), elasticEvents)
            }
            else -> throw UnsupportedOperationException("unknown format")
        }
    }
}