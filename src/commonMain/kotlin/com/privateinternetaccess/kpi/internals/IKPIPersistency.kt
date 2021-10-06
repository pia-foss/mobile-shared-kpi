package com.privateinternetaccess.kpi.internals

import com.privateinternetaccess.kpi.internals.model.KPIEvent
import com.privateinternetaccess.kpi.internals.model.KPIEventIdentifier

internal interface IKPIPersistency {
    fun persistIdentifier(identifier: KPIEventIdentifier)
    fun identifier(): KPIEventIdentifier?
    fun persistEvent(event: KPIEvent, eventHistorySize: Int)
    fun events(): List<KPIEvent>
    fun sampleEvents(): List<KPIEvent>
    fun clearBatchedEvents()
    fun clearAll()
}