package com.privateinternetaccess.kpi.internals.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ElasticEvent(
    @SerialName("event")
    val event: String,
    @SerialName("properties")
    val properties: Map<String, String>
)