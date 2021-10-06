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
import com.privateinternetaccess.kpi.internals.model.KPIEventIdentifier
import com.privateinternetaccess.kpi.internals.model.request.KPIEventsRequest
import com.privateinternetaccess.kpi.internals.utils.KPIEventUtils
import com.privateinternetaccess.kpi.internals.utils.KPIUtils
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext


internal expect object KPIHttpClient {
    fun client(certificate: String? = null, pinnedEndpoint: Pair<String, String>? = null): HttpClient
}

internal expect object KPIPersistency {
    fun persistIdentifier(identifier: KPIEventIdentifier)
    fun identifier(): KPIEventIdentifier?
    fun persistEvent(event: KPIEvent)
    fun events(): List<KPIEvent>
    fun sampleEvents(): List<KPIEvent>
    fun clearBatchedEvents()
    fun clearAll()
}

internal expect object KPIIdentifier {
    fun uuid(): String
}

internal class KPI(
    private val kpiClientStateProvider: KPIClientStateProvider,
    private val kpiSendEventMode: KPISendEventsMode,
    private val certificate: String?,
    private val appVersion: String,
) : CoroutineScope, KPIAPI {

    companion object {
        private const val EVENTS_BATCH_SIZE = 20
        internal const val EVENTS_HISTORY_SIZE = 50
        internal const val REQUEST_TIMEOUT_MS = 3000L
        internal const val AGGREGATED_ID_PERSISTENCY_KEY = "AGGREGATED_ID_PERSISTENCY_KEY"
        internal const val EVENTS_BATCH_PERSISTENCY_KEY = "EVENTS_BATCH_PERSISTENCY_KEY"
        internal const val EVENTS_SAMPLE_PERSISTENCY_KEY = "EVENTS_SAMPLE_PERSISTENCY_KEY"
        internal val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    }

    private enum class Endpoint(val url: String) {
        KPI("/api/client/v2/service-quality")
    }

    internal var batchedEvents = mutableListOf<KPIEvent>()
    internal var sampleEvents = mutableListOf<KPIEvent>()
    private var started = false

    // region CoroutineScope
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main
    // endregion

    // region KPIAPI
    override fun start() {
        launch {
            startAsync()
        }
    }

    override fun stop() {
        launch {
            stopAsync()
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
        batchedEvents = KPIPersistency.events().toMutableList()
        sampleEvents = KPIPersistency.sampleEvents().toMutableList()
        started = true
    }

    private fun stopAsync() {
        batchedEvents = mutableListOf()
        sampleEvents = mutableListOf()
        KPIPersistency.clearAll()
        started = false
    }

    private suspend fun submitAsync(
        event: KPIClientEvent,
        endpoints: List<KPIEndpoint>,
        callback: (error: KPIError?) -> Unit
    ) {
        var error: KPIError? = null
        if (endpoints.isNullOrEmpty()) {
            error= KPIError("No available endpoints to perform the request.")
        }

        if (!started) {
            error = KPIError("KPI has not been started. Event discarded.")
        }

        if (error == null) {
            // Queue in memory and persist events
            persistAndQueueEvent(KPIEventUtils.adaptEvent(event, appVersion))

            // Evaluate whether it's time to flush events according to the set mode.
            val shouldSendEvents = when (kpiSendEventMode) {
                KPISendEventsMode.PER_EVENT -> true
                KPISendEventsMode.PER_BATCH -> batchedEvents.size >= EVENTS_BATCH_SIZE
            }

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
        }

        withContext(Dispatchers.Main) {
            callback(error)
        }
    }

    private suspend fun flushAsync(
        endpoints: List<KPIEndpoint>,
        callback: (error: KPIError?) -> Unit
    ) {
        val error = sendEvents(endpoints)

        // If there were no errors flushing events. Clear them.
        if (error == null) {
            clearPersistedAndQueuedEvents()
        }

        withContext(Dispatchers.Main) {
            callback(error)
        }
    }

    private suspend fun sendEvents(
        endpoints: List<KPIEndpoint>
    ): KPIError? {
        var error: KPIError? = null
        if (endpoints.isNullOrEmpty()) {
            error= KPIError("No available endpoints to perform the request.")
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
                    KPIHttpClient.client(certificate, Pair(endpoint.endpoint, endpoint.certificateCommonName!!))
                } else {
                    KPIHttpClient.client()
                }
                val response = client.postCatching<Pair<HttpResponse?, Exception?>> {
                    url("https://${endpoint.endpoint}${Endpoint.KPI.url}")
                    header("Authorization", "Token ${kpiClientStateProvider.kpiAuthToken()}")
                    contentType(ContentType.Application.Json)
                    body = json.encodeToString(KPIEventsRequest.serializer(), KPIEventsRequest(events))
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
                    "EventToken: ${event.eventToken} " +
                    "EventProperties.Platform: ${event.eventProperties.platform} " +
                    "EventProperties.UserAgent: ${event.eventProperties.userAgent} " +
                    "EventProperties.Version: ${event.eventProperties.version} " +
                    "EventProperties.VpnProtocol: ${event.eventProperties.vpnProtocol} " +
                    "EventProperties.ConnectionSource: ${event.eventProperties.connectionSource} " +
                    "")
        }

        withContext(Dispatchers.Main) {
            callback(result)
        }
    }

    private fun persistAndQueueEvent(event: KPIEvent) {
        // Persist all events. In case of request failure, they'll be re-submitted the next time we flush events.
        KPIPersistency.persistEvent(event)

        // Update batched events. The batching size is to indicate when to trigger the request,
        // not max numbers of events to batch. Thus, add all events to its queue.
        batchedEvents.add(event)

        // Update sample events. If we have more than the max of sample events supported.
        // Clear the oldest one before adding the new one.
        if (sampleEvents.size >= EVENTS_HISTORY_SIZE) {
            sampleEvents.removeFirst()
        }
        sampleEvents.add(event)
    }

    private fun clearPersistedAndQueuedEvents() {
        // Clear only batched events. We keep a max of historical events `EVENTS_HISTORY_SIZE` to display to the user.
        KPIPersistency.clearBatchedEvents()

        // Update in-memory queue
        batchedEvents = mutableListOf()
    }

    private suspend inline fun <reified T> HttpClient.postCatching(
        block: HttpRequestBuilder.() -> Unit = {}
    ): Pair<HttpResponse?, Exception?> = request {
        var exception: Exception? = null
        var response: HttpResponse? = null
        try {
            response = request {
                method = HttpMethod.Post
                apply(block)
            }
        } catch (e: Exception) {
            exception = e
        }
        return Pair(response, exception)
    }
    // endregion
}