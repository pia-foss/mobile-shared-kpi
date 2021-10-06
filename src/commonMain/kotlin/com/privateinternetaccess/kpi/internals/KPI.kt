@file:Suppress("DeferredResultUnused")

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

import com.privateinternetaccess.kpi.*
import com.privateinternetaccess.kpi.internals.model.KPIEvent
import com.privateinternetaccess.kpi.internals.utils.*
import com.privateinternetaccess.kpi.internals.utils.KPIEventUtils
import com.privateinternetaccess.kpi.internals.utils.KPIPlatformUtils
import com.privateinternetaccess.kpi.testing.TestingKpi
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext


internal expect object KPIHttpClient {
    fun client(
        kpiHttpLogLevel: KPIHttpLogLevel = KPIHttpLogLevel.NONE,
        userAgent: String? = null,
        certificate: String? = null,
        pinnedEndpoint: Pair<String, String>? = null,
        requestTimeoutMs: Long
    ): HttpClient
}

expect class KPIPlatformProvider internal constructor() {

    internal val defaultPreferenceName: String

    internal val userAgent: String?

    internal val kpiPreferences: KPIPreferences?

    internal val kpiLogger: KPILogger

    internal val kpiHttpLogLevel: KPIHttpLogLevel

    internal fun preference(name: String): Boolean

    internal fun userAgent(userAgent: String?): Boolean

    internal fun loggingEnabled(enabled: Boolean)

    internal fun kpiLogLevel(logLevel: KPIHttpLogLevel): Boolean
}

internal expect class KPIPreferences(
    provider: KPIPlatformProvider,
    context: Any?,
    name: String
) {
    val isValid: Boolean

    fun getString(key: String, default: String? = null): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
    fun clear()
}

internal expect object KPIIdentifier {
    fun uuid(): String
}

internal class KPI(
    private val kpiProvider: KPIPlatformProvider,
    private val kpiClientStateProvider: KPIClientStateProvider,
    private val kpiSendEventMode: KPISendEventsMode,
    private val certificate: String?,
    private val format: KPIRequestFormat,
    private val eventTimeRoundGranularity: KTimeUnit,
    private val eventTimeSendGranularity: KTimeUnit,
    private val eventsBatchSize: Int,
    private val eventsHistorySize: Int,
    private val requestTimeoutMs: Long
) : CoroutineScope, KPIAPI {
    companion object {
        private val TAG: String = KPI::class.simpleName!!

        internal const val AGGREGATED_ID_PERSISTENCY_KEY = "AGGREGATED_ID_PERSISTENCY_KEY"
        internal const val EVENTS_BATCH_PERSISTENCY_KEY = "EVENTS_BATCH_PERSISTENCY_KEY"
        internal const val EVENTS_SAMPLE_PERSISTENCY_KEY = "EVENTS_SAMPLE_PERSISTENCY_KEY"

        internal val json = Json {
            // do not fail, if json objects with unknown key names are being deserialized
            ignoreUnknownKeys = true

            // default values of Kotlin are encoded
            encodeDefaults = true
        }
    }

    internal var batchedEvents = mutableListOf<KPIEvent>()
    internal var sampleEvents = mutableListOf<KPIEvent>()

    private val kpiPersistency: IKPIPersistency = KPIPersistencyImpl(kpiProvider)
    private val kpiEventUtils: KPIEventUtils = KPIEventUtils(kpiPersistency)
    private var started = false

    // region CoroutineScope
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main
    // endregion

    // region KPIAPI
    override val testingKpi: TestingKpi = TestingKpi(
        kpiClientStateProvider = kpiClientStateProvider,
        kpiEventUtils = kpiEventUtils,
        eventTimeRoundGranularity = eventTimeRoundGranularity,
        eventTimeSendGranularity = eventTimeSendGranularity,
    )

    override fun start() {
        launch {
            startAsync()
        }
    }

    override fun stop(callback: (error: KPIError?) -> Unit) {
        launch {
            stopAsync(callback)
        }
    }

    override fun submit(event: KPIClientEvent, callback: (error: KPIError?) -> Unit) {
        launch {
            submitAsync(event, kpiClientStateProvider.kpiEndpoints(), callback)
        }
    }

    override fun flush(callback: (error: KPIError?) -> Unit) {
        launch {
            flushAsync(kpiClientStateProvider.kpiEndpoints(), callback)
        }
    }

    override fun recentEvents(callback: (events: List<String>) -> Unit) {
        launch {
            recentEventsAsync(callback)
        }
    }
    // endregion

    // region private
    private fun startAsync() {
        batchedEvents = kpiPersistency.events().toMutableList()
        sampleEvents = kpiPersistency.sampleEvents().toMutableList()
        started = true
    }

    private fun stopAsync(callback: (error: KPIError?) -> Unit) {
        var error: KPIError? = null
        try {
            batchedEvents = mutableListOf()
            sampleEvents = mutableListOf()
            kpiPersistency.clearAll()
            started = false
        } catch (t: Throwable) {
            val stacktrace = t.stackTraceToString()
            kpiProvider.kpiLogger.logError(tag = TAG, message = stacktrace)
            error = KPIError(description = stacktrace)
        }
        callback(error)
    }

    private suspend fun submitAsync(
        event: KPIClientEvent,
        endpoints: List<KPIEndpoint>,
        callback: (error: KPIError?) -> Unit
    ) {
        var error: KPIError? = null
        if (endpoints.isEmpty()) {
            error = KPIError("No available endpoints to perform the request.")
        }

        if (!started) {
            error = KPIError("KPI has not been started. Event discarded.")
        }

        if (error == null) {
            try {
                // Queue in memory and persist events
                persistAndQueueEvent(kpiEventUtils.adaptEvent(event))

                // Evaluate whether it's time to flush events according to the set mode.
                val shouldSendEvents = when (kpiSendEventMode) {
                    KPISendEventsMode.PER_EVENT -> true
                    KPISendEventsMode.PER_BATCH -> batchedEvents.size >= eventsBatchSize
                }

                // If we need to flush events. Get those that fulfill the set mode.
                if (shouldSendEvents) {
                    error = sendEvents(endpoints)

                    // If there were no errors sending events. Clear them according to the set mode.
                    if (error == null) {
                        when (kpiSendEventMode) {
                            KPISendEventsMode.PER_EVENT,
                            KPISendEventsMode.PER_BATCH -> clearPersistedAndQueuedEvents()
                        }
                    }
                }
            } catch (t: Throwable) {
                val stacktrace = t.stackTraceToString()
                kpiProvider.kpiLogger.logDebug(tag = TAG, message = stacktrace)
                error = KPIError(description = stacktrace)
            }
        }

        withContext(Dispatchers.Main) {
            callback(error)
        }
    }

    private suspend fun flushAsync(
        endpoints: List<KPIEndpoint>,
        callback: (error: KPIError?) -> Unit
    ) {
        var error = sendEvents(endpoints)

        // If there were no errors flushing events. Clear them.
        if (error == null) {
            try {
                clearPersistedAndQueuedEvents()
            } catch (t: Throwable) {
                val stacktrace = t.stackTraceToString()
                kpiProvider.kpiLogger.logDebug(tag = TAG, message = stacktrace)
                error = KPIError(description = stacktrace)
            }
        }

        withContext(Dispatchers.Main) {
            callback(error)
        }
    }

    private suspend fun sendEvents(
        endpoints: List<KPIEndpoint>
    ): KPIError? {
        var error: KPIError? = null
        if (endpoints.isEmpty()) {
            error = KPIError("No available endpoints to perform the request.")
        }

        if (!started) {
            error = KPIError("KPI has not been started. Event discarded.")
        }

        if (error == null) {

            // Get those events that fulfill the set mode.
            val events = when (kpiSendEventMode) {
                KPISendEventsMode.PER_EVENT,
                KPISendEventsMode.PER_BATCH -> {
                    val eventsToBeSent = batchedEvents
                    batchedEvents = mutableListOf()
                    eventsToBeSent
                }
            }

            for (endpoint in endpoints) {
                if (endpoint.usePinnedCertificate && certificate.isNullOrEmpty()) {
                    error = KPIError("No available certificate for pinning purposes")
                    continue
                }

                error = null
                val client = if (endpoint.usePinnedCertificate) {
                    KPIHttpClient.client(
                        kpiHttpLogLevel = kpiProvider.kpiHttpLogLevel,
                        userAgent = kpiProvider.userAgent,
                        certificate = certificate,
                        pinnedEndpoint = Pair(endpoint.endpoint, endpoint.certificateCommonName!!),
                        requestTimeoutMs = requestTimeoutMs
                    )
                } else {
                    KPIHttpClient.client(
                        kpiHttpLogLevel = kpiProvider.kpiHttpLogLevel,
                        userAgent = kpiProvider.userAgent,
                        requestTimeoutMs = requestTimeoutMs
                    )
                }
                val projectToken = kpiClientStateProvider.projectToken()
                if (format == KPIRequestFormat.ELASTIC && projectToken == null) {
                    return KPIError("project token must not be null")
                }
                val response: Pair<HttpResponse?, Throwable?> = when (format) {
                    KPIRequestFormat.KAPE -> client.postCatching {
                        url("https://${endpoint.endpoint}")
                        header("Authorization", "Token ${kpiClientStateProvider.kpiAuthToken()}")
                        contentType(ContentType.Application.Json)
                        body = KPIPlatformUtils.encodeWithKapeFormat(
                            events = events,
                            eventTimeRoundGranularity = eventTimeRoundGranularity,
                            eventTimeSendGranularity = eventTimeSendGranularity
                        )
                    }
                    KPIRequestFormat.ELASTIC -> client.postCatching {
                        url("https://${endpoint.endpoint}")
                        contentType(ContentType.Application.FormUrlEncoded)
                        body = KPIPlatformUtils.encodeWithElasticFormat(
                            events = events,
                            projectToken = projectToken!!,
                            eventRoundTimeGranularity = eventTimeRoundGranularity,
                            eventSendTimeGranularity = eventTimeSendGranularity
                        )
                    }
                }

                response.first?.let {
                    if (KPIUtils.isErrorStatusCode(it.status.value)) {
                        error = KPIError("${it.status.description} (${it.status.value})")
                    }
                }
                response.second?.let {
                    error = KPIError("${it.message} (600)")
                }

                // If there were no errors in the request for the current endpoint.
                // No need to try the next endpoint.
                if (error == null) {
                    break
                }
            }

            // If we failed to submit events. Add them back to the queue.
            if (error != null) {
                batchedEvents.addAll(events)
            }
        }
        return error
    }

    private suspend fun recentEventsAsync(callback: (events: List<String>) -> Unit) {
        val result = mutableListOf<String>()
        for (event in sampleEvents) {
            result.add("" +
                    "EventName: ${event.eventName} " +
                    "EventToken: ${kpiClientStateProvider.projectToken()} " +
                    "EventProperties.Platform: ${event.eventProperties["platform"]} " +
                    "EventProperties.UserAgent: ${event.eventProperties["user_agent"]} " +
                    "EventProperties.Version: ${event.eventProperties["version"]} " +
                    "EventProperties.VpnProtocol: ${event.eventProperties["vpn_protocol"]} " +
                    "EventProperties.ConnectionSource: ${event.eventProperties["connection_source"]} " +
                    "")
        }

        withContext(Dispatchers.Main) {
            callback(result)
        }
    }

    private fun persistAndQueueEvent(event: KPIEvent) {
        // Persist all events. In case of request failure, they'll be re-submitted the next time we flush events.
        kpiPersistency.persistEvent(event, eventsHistorySize)

        // Update batched events. The batching size is to indicate when to trigger the request,
        // not max numbers of events to batch. Thus, add all events to its queue.
        batchedEvents.add(event)

        // Update sample events. If we have more than the max of sample events supported.
        // Clear the oldest one before adding the new one.
        if (sampleEvents.size >= eventsHistorySize) {
            sampleEvents.removeFirst()
        }
        sampleEvents.add(event)
    }

    private fun clearPersistedAndQueuedEvents() {
        // Clear only batched events. We keep a max of historical events `EVENTS_HISTORY_SIZE` to display to the user.
        kpiPersistency.clearBatchedEvents()

        // Update in-memory queue
        batchedEvents = mutableListOf()
    }

    private suspend inline fun HttpClient.postCatching(
        block: HttpRequestBuilder.() -> Unit = {}
    ): Pair<HttpResponse?, Throwable?> = request {
        var throwable: Throwable? = null
        var response: HttpResponse? = null
        try {
            response = request {
                method = HttpMethod.Post
                apply(block)
            }
        } catch (t: Throwable) {
            throwable = t
        }
        return Pair(response, throwable)
    }

    // endregion
}