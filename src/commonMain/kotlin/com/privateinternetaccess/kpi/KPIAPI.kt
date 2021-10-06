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


/**
 * Enum defining the supported connection related events.
 */
public enum class KPIConnectionEvent(val value: String) {
    VPN_CONNECTION_ATTEMPT("VPN_CONNECTION_ATTEMPT"),
    VPN_CONNECTION_CANCELLED("VPN_CONNECTION_CANCELLED"),
    VPN_CONNECTION_ESTABLISHED("VPN_CONNECTION_ESTABLISHED"),
}

/**
 * Enum defining the supported vpn protocols to report.
 */
public enum class KPIVpnProtocol(val value: String) {
    IPSEC("IPSec"),
    OPENVPN("OpenVPN"),
    WIREGUARD("WireGuard"),
}

/**
 * Enum defining the different connection sources. e.g. Manual for user-related actions, Automatic for reconnections, etc.
 */
public enum class KPIConnectionSource(val value: String) {
    AUTOMATIC("Automatic"),
    MANUAL("Manual"),
}

/**
 * Enum defining the supported flush modes.
 */
public enum class KPISendEventsMode {
    PER_EVENT,
    PER_BATCH
}

/**
 * Interface defining the API to be offered by the kpi module.
 */
public interface KPIAPI {

    /**
     * It enables the module for the sending of events to the API.
     */
    fun start()

    /**
     * It disables the module from sending events to the API. Clearing any persisted information.
     */
    fun stop()

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
    fun kpiAuthToken(): String
}

/**
 * Builder class responsible for creating an instance of an object conforming to
 * the `KPIAPI` interface.
 */
public class KPIBuilder {
    private var kpiClientStateProvider: KPIClientStateProvider? = null
    private var kpiSendEventMode: KPISendEventsMode? = null
    private var certificate: String? = null
    private var appVersion: String? = null

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
     * It sets the application version for which we are building the API.
     *
     * @param appVersion `String`.
     */
    fun setAppVersion(appVersion: String): KPIBuilder =
        apply { this.appVersion = appVersion }

    /**
     * @return `KPIAPI` instance.
     */
    fun build(): KPIAPI {
        val kpiClientStateProvider = kpiClientStateProvider
            ?: throw Exception("KPI client state provider missing.")
        val kpiSendEventMode = kpiSendEventMode
            ?: throw Exception("KPI events send mode missing.")
        val appVersion = this.appVersion
            ?: throw Exception("App version missing.")
        return KPI(kpiClientStateProvider, kpiSendEventMode, certificate, appVersion)
    }
}

/**
 * Data class defining the client's event information to be submitted.
 *
 * @param eventCountry `String?` Optional. Country code ISO 3166-1 alpha-2.
 * @param eventName `KPIConnectionEvent`. Type of event.
 * @param eventProperties `String`. Event properties specific to the product.
 * @param eventToken `String`. Token identifier for a particular product.
 */
data class KPIClientEvent(
    val eventCountry: String? = null,
    val eventName: KPIConnectionEvent,
    val eventProperties: EventProperties,
    val eventToken: String
) {

    /**
     * Data class defining the product's event properties.
     *
     * @param connectionSource `KPIConnectionSource`.
     * @param data `String?` Optional.
     * @param reason `String?` Optional.
     * @param serverIdentifier `Int?` Optional.
     * @param userAgent `String`.
     * @param vpnProtocol `KPIVpnProtocol`.
     */
    data class EventProperties(
        val connectionSource: KPIConnectionSource,
        val data: String? = null,
        val preRelease: Boolean,
        val reason: String? = null,
        val serverIdentifier: String? = null,
        val userAgent: String,
        val vpnProtocol: KPIVpnProtocol
    )
}

/**
 * Data class defining the endpoints data needed when performing a request on it.
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
