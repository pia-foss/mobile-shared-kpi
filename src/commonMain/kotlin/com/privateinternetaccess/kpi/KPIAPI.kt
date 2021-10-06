@file:Suppress("RedundantVisibilityModifier", "unused")

package com.privateinternetaccess.kpi

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

import com.privateinternetaccess.kpi.internals.KPI
import com.privateinternetaccess.kpi.internals.KPIPlatformProvider
import com.privateinternetaccess.kpi.internals.utils.KTimeUnit
import com.privateinternetaccess.kpi.testing.TestingKpi
import kotlinx.datetime.Instant
import kotlin.math.max


/**
 * Enum defining the supported flush modes.
 */
public enum class KPISendEventsMode {
    PER_EVENT,
    PER_BATCH
}

/**
 * Enum defining how the events are encoded in the request
 */
public enum class KPIRequestFormat {
    ELASTIC,
    KAPE
}

public enum class KPIHttpLogLevel {
    ALL, HEADERS, BODY, INFO, NONE
}

/**
 * Interface defining the API to be offered by the kpi module.
 */
public interface KPIAPI {
    /**
     * Provides methods for testing purposes.
     */
    val testingKpi: TestingKpi

    /**
     * It enables the module for the sending of events to the API.
     */
    fun start()

    /**
     * It disables the module from sending events to the API. Clearing any persisted information.
     */
    fun stop(callback: (error: KPIError?) -> Unit)

    /**
     * It submits an event for processing. Depending on the module configuration it will submit the event immediately
     * or once a predefined number of events is queued. @see `KPISendEventsMode`.
     * The module has to be started in order to send those events. @see `start`
     *
     * @param event `KPIClientEvent`.
     * @param callback `(error: KPIError?) -> Unit`.
     */
    fun submit(event: KPIClientEvent, callback: (error: KPIError?) -> Unit)

    /**
     * It sends all the events that are currently being batched. Regardless of the batch size.
     * The module has to be started in order to send those events. @see `start`
     *
     * @param callback `(error: KPIError?) -> Unit`.
     */
    fun flush(callback: (error: KPIError?) -> Unit)

    /**
     * It returns a sample of the most recent events reported. Keep in mind events are only available once the service
     * is started. And, cleared once stopped.
     *
     * @param callback `(events: List<String>) -> Unit`.
     */
    fun recentEvents(callback: (events: List<String>) -> Unit)
}

/**
 * Interface defining the client's state provider.
 */
public interface KPIClientStateProvider {

    /**
     * It returns the list of endpoints to try to reach when performing a request. Order is relevant.
     *
     * @return `List<KPIEndpoint>`
     */
    fun kpiEndpoints(): List<KPIEndpoint>

    /**
     * It returns the authentication token to be used as part of the event's request being sent to the endpoint.
     *
     * @return `String`
     */
    fun kpiAuthToken(): String?

    /**
     * Specifies the project the events belong to.
     *
     * @return `String`
     */
    fun projectToken(): String?
}

/**
 * Builder class responsible for creating an instance of an object conforming to
 * the `KPIAPI` interface.
 */
@Suppress("MemberVisibilityCanBePrivate")
public class KPIBuilder {
    val kpiProvider = KPIPlatformProvider()
    private var userAgent: String? = null
    private var kpiClientStateProvider: KPIClientStateProvider? = null
    private var kpiSendEventMode: KPISendEventsMode? = null
    private var certificate: String? = null
    private var format: KPIRequestFormat = KPIRequestFormat.KAPE
    private var eventTimeRoundGranularity: KTimeUnit = KTimeUnit.MILLISECONDS
    private var eventTimeSendGranularity: KTimeUnit = KTimeUnit.MILLISECONDS
    private var preferenceName: String? = kpiProvider.defaultPreferenceName
    private var isLoggingEnabled: Boolean = false
    private var kpiHttpLogLevel: KPIHttpLogLevel = KPIHttpLogLevel.NONE
    private var eventsBatchSize: Int = DEFAULT_EVENTS_BATCH_SIZE
    private var eventsHistorySize: Int = DEFAULT_EVENTS_HISTORY_SIZE
    private var requestTimeoutMs: Long = DEFAULT_REQUEST_TIMEOUT_MS

    /**
     * Specifies the 'user-agent' header, that the KPIAPI client is using for its requests.
     *
     * @param userAgent `KPIClientStateProvider`.
     */
    fun setUserAgent(userAgent: String?): KPIBuilder =
        apply { this.userAgent = userAgent }

    /**
     * It sets the instance responsible to provide client side information.
     *
     * @param kpiClientStateProvider `KPIClientStateProvider`.
     */
    fun setKPIClientStateProvider(kpiClientStateProvider: KPIClientStateProvider): KPIBuilder =
        apply { this.kpiClientStateProvider = kpiClientStateProvider }

    /**
     * It sets mode to use when sending events.
     *
     * @param kpiSendEventMode `KPISendEventsMode`.
     */
    fun setKPIFlushEventMode(kpiSendEventMode: KPISendEventsMode): KPIBuilder =
        apply { this.kpiSendEventMode = kpiSendEventMode }

    /**
     * It sets the certificate to use when using an endpoint with pinning enabled. Optional.
     *
     * @param certificate `String`.
     */
    fun setCertificate(certificate: String): KPIBuilder =
        apply { this.certificate = certificate }

    /**
     * It sets the format, how the events are encoded in the HTTP requests.
     *
     * @param format `KPIRequestFormat`.
     */
    fun setRequestFormat(format: KPIRequestFormat): KPIBuilder =
        apply { this.format = format }

    /**
     * Specifies the time unit event times are rounded to.
     *
     * @param eventTimeRoundGranularity `KPIEventTimeGranularity`.
     */
    fun setEventTimeRoundGranularity(eventTimeRoundGranularity: KTimeUnit): KPIBuilder =
        apply { this.eventTimeRoundGranularity = eventTimeRoundGranularity }

    /**
     * Specifies the time unit event times are sent to the api.
     *
     * @param eventSendTimeGranularity `KPIEventTimeGranularity`.
     */
    fun setEventTimeSendGranularity(eventSendTimeGranularity: KTimeUnit): KPIBuilder =
        apply { this.eventTimeSendGranularity = eventSendTimeGranularity }

    /**
     * Specifies the preference location, that the KPIAPI uses as its cache
     */
    fun setPreferenceName(name: String?): KPIBuilder =
        apply {
            this.preferenceName = when {
                name.isNullOrBlank().not() -> name?.trim()
                else -> kpiProvider.defaultPreferenceName
            }
        }

    /**
     * Enables/Disables logging.
     */
    fun setKpiLoggingEnabled(isEnabled: Boolean): KPIBuilder =
       apply { this.isLoggingEnabled = isEnabled }

    /**
     * Specifies the log level of the http client
     */
    fun setKpiHttpLogLevel(level: KPIHttpLogLevel): KPIBuilder =
        apply { this.kpiHttpLogLevel = level }

    /**
     * Specifies how many events have to be queued until they are sent as one batch to the KPI endpoints.
     */
    fun setEventsBatchSize(size: Int): KPIBuilder=
        apply { this.eventsBatchSize = max(size, 1) }

    /**
     * Specifies how many events are stored in the event history
     */
    fun setEventsHistorySize(size: Int): KPIBuilder=
        apply { this.eventsHistorySize = max(size, 1) }

    /**
     * Specifies, how long the http client is waiting for requests to finish until it considers them as failed, timed out requests.
     */
    fun setRequestTimeoutMs(timeout: Long): KPIBuilder=
        apply { this.requestTimeoutMs = max(timeout, 1L) }

    /**
     * @return `KPIAPI` instance.
     */
    fun build(): KPIAPI {
        val kpiClientStateProvider = kpiClientStateProvider
            ?: throw Exception("KPI client state provider missing.")
        val kpiSendEventMode = kpiSendEventMode
            ?: throw Exception("KPI events send mode missing.")
        val preferenceName = preferenceName
            ?: throw Exception("KPI preferences scope name missing.")
        if (kpiProvider.preference(preferenceName).not()) {
            throw Exception("preference could not be created")
        }
        kpiProvider.userAgent(userAgent)
        kpiProvider.loggingEnabled(isLoggingEnabled)
        kpiProvider.kpiLogLevel(kpiHttpLogLevel)
        return KPI(
            kpiProvider = kpiProvider,
            kpiClientStateProvider = kpiClientStateProvider,
            kpiSendEventMode = kpiSendEventMode,
            certificate = certificate,
            format = format,
            eventTimeRoundGranularity = eventTimeRoundGranularity,
            eventTimeSendGranularity = eventTimeSendGranularity,
            eventsBatchSize = eventsBatchSize,
            eventsHistorySize = eventsHistorySize,
            requestTimeoutMs =requestTimeoutMs,
        )
    }

    companion object {
        const val DEFAULT_EVENTS_BATCH_SIZE: Int = 20
        const val DEFAULT_EVENTS_HISTORY_SIZE: Int = 50
        const val DEFAULT_REQUEST_TIMEOUT_MS: Long = 3_000L
    }
}

/**
 * Data class defining the client's event information to be submitted.
 *
 * @param eventCountry `String?` Optional. Country code ISO 3166-1 alpha-2.
 * @param eventName `String`. Type of event.
 * @param eventProperties `Map<String, String>`. Event properties specific to the product.
 */
data class KPIClientEvent(
    val eventCountry: String? = null,
    val eventName: String,
    val eventProperties: Map<String, String>,
    val eventInstant: Instant
)

/**
 * Data class defining the endpoint's data needed when performing a request on it.
 */
public data class KPIEndpoint(
    val endpoint: String,
    val isProxy: Boolean,
    val usePinnedCertificate: Boolean = false,
    val certificateCommonName: String? = null
)

/**
 * KPI Error object.
 *
 * @param description `String`.
 */
public data class KPIError(val description: String)
