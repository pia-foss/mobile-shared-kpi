package com.privateinternetaccess.kpi.internals.model

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

import com.privateinternetaccess.kpi.KPIConnectionEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * Data class defining the event's information to be submitted.
 *
 * @param aggregatedId `String`. Event's aggregated identifier.
 * @param uniqueId `String`. Event unique identifier.
 * @param eventCountry `String?` Optional. Country code ISO 3166-1 alpha-2.
 * @param eventName `String`. Type of event.
 * @param eventProperties `String`. Event properties specific to the product.
 * @param eventTime `Long`. UTC timestamp.
 * @param eventToken `String`. Token identifier for a particular product.
 */
@Serializable
internal data class KPIEvent(
    @SerialName("aggregated_id")
    val aggregatedId: String,
    @SerialName("event_unique_id")
    val uniqueId: String,
    @SerialName("event_country")
    val eventCountry: String? = null,
    @SerialName("event_name")
    val eventName: String,
    @SerialName("event_properties")
    val eventProperties: EventProperties,
    @SerialName("event_time")
    val eventTime: Long,
    @SerialName("event_token")
    val eventToken: String
) {

    /**
     * Data class defining the product's event properties.
     *
     * @param connectionSource `String`.
     * @param data `String?` Optional.
     * @param platform `String`.
     * @param reason `String?` Optional.
     * @param serverIdentifier `Int?` Optional.
     * @param userAgent `String`.
     * @param version `String`.
     * @param vpnProtocol `String`.
     */
    @Serializable
    data class EventProperties(
        @SerialName("connection_source")
        val connectionSource: String,
        @SerialName("data")
        val data: String? = null,
        @SerialName("platform")
        val platform: String,
        @SerialName("prerelease")
        val preRelease: Boolean,
        @SerialName("reason")
        val reason: String? = null,
        @SerialName("server_identifier")
        val serverIdentifier: String? = null,
        @SerialName("user_agent")
        val userAgent: String,
        @SerialName("version")
        val version: String,
        @SerialName("vpn_protocol")
        val vpnProtocol: String
    )
}
