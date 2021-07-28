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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


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

    private var started = false

    // region CoroutineScope
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main
    // endregion

    // region KPIAPI
    override fun start() {
        startAsync()
    }

    override fun stop() {
        stopAsync()
    }

    override fun submit(event: KPIClientEvent, callback: (error: KPIError?) -> Unit) {
        submitAsync(event, kpiClientStateProvider.kpiEndpoints(), callback)
    }

    override fun flush(callback: (error: KPIError?) -> Unit) {
        flushAsync(kpiClientStateProvider.kpiEndpoints(), KPIPersistency.events(), callback)
    }

    override fun recentEvents(callback: (events: List<String>) -> Unit) {
        recentEventsAsync(callback)
    }
    // endregion

    // region private
    private fun startAsync() = async {
        started = true
    }

    private fun stopAsync() = async {
        KPIPersistency.clearAll()
        started = false
    }

    private fun submitAsync(
        event: KPIClientEvent,
        endpoints: List<KPIEndpoint>,
        callback: (error: KPIError?) -> Unit
    ) = async {
        var error: KPIError? = null
        if (endpoints.isNullOrEmpty()) {
            error= KPIError("No available endpoints to perform the request.")
        }

        if (!started) {
            error = KPIError("KPI has not been started. Event discarded.")
        }

        if (error == null) {
            // Persist all events. In case of request failure, they'll be re-submitted the next time we flush events.
            KPIPersistency.persistEvent(KPIEventUtils.adaptEvent(event, appVersion))

            // Evaluate whether it's time to flush events according to the set mode.
            val shouldSendEvents = when (kpiSendEventMode) {
                KPISendEventsMode.PER_EVENT -> true
                KPISendEventsMode.PER_BATCH -> KPIPersistency.events().size >= EVENTS_BATCH_SIZE
            }

            // If we need to flush events. Get those that fulfill the set mode.
            if (shouldSendEvents) {
                val events = when (kpiSendEventMode) {
                    KPISendEventsMode.PER_EVENT,
                    KPISendEventsMode.PER_BATCH -> KPIPersistency.events()
                }
                error = sendEvents(endpoints, events)

                // If there were no errors sending events. Clear them according to the set mode.
                if (error == null) {
                    when (kpiSendEventMode) {
                        KPISendEventsMode.PER_EVENT,
                        KPISendEventsMode.PER_BATCH -> KPIPersistency.clearBatchedEvents()
                    }
                }
            }
        }

        withContext(Dispatchers.Main) {
            callback(error)
        }
    }

    private fun flushAsync(
        endpoints: List<KPIEndpoint>,
        events: List<KPIEvent>,
        callback: (error: KPIError?) -> Unit
    ) = async {
        val error = sendEvents(endpoints, events)

        // If there were no errors flushing events. Clear them.
        if (error == null) {
            KPIPersistency.clearBatchedEvents()
        }

        withContext(Dispatchers.Main) {
            callback(error)
        }
    }

    private suspend fun sendEvents(
        endpoints: List<KPIEndpoint>,
        events: List<KPIEvent>,
    ): KPIError? {
        var error: KPIError? = null
        if (endpoints.isNullOrEmpty()) {
            error= KPIError("No available endpoints to perform the request.")
        }

        if (!started) {
            error = KPIError("KPI has not been started. Event discarded.")
        }

        if (error == null) {
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
        }
        return error
    }

    private fun recentEventsAsync(callback: (events: List<String>) -> Unit) = async {
        val result = mutableListOf<String>()
        for (event in KPIPersistency.sampleEvents()) {
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