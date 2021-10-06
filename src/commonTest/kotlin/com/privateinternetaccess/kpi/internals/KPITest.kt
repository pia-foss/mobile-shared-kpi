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
import com.privateinternetaccess.kpi.internals.KPI.Companion.EVENTS_HISTORY_SIZE
import com.privateinternetaccess.kpi.internals.util.MockClientStateProvider
import com.privateinternetaccess.kpi.internals.util.TestUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.test.*


class KPITest {

    companion object {
        private const val TEST_TIMEOUT = 5000L
    }

    private val testUtils = TestUtils()
    private val mockClientStateProvider = MockClientStateProvider()
    private val event = KPIClientEvent(
        eventCountry = "",
        eventName = KPIConnectionEvent.VPN_CONNECTION_ATTEMPT,
        KPIClientEvent.EventProperties(
            connectionSource = KPIConnectionSource.MANUAL,
            data = null,
            preRelease = false,
            reason = null,
            serverIdentifier = null,
            userAgent = "",
            vpnProtocol = KPIVpnProtocol.OPENVPN
        ),
        eventToken = ""
    )

    @BeforeTest
    fun before() {
        testUtils.bootstrap()
    }

    @AfterTest
    fun after() {
        testUtils.teardown()
    }

    @Test
    fun `Test error as a result of no endpoints being provided`() = testUtils.runBlockingTest {
        val kpi = givenAMockKpiApi()
        kpi.start()

        val completable = CompletableDeferred<KPIError?>()
        kpi.submit(event) { error ->
            completable.complete(error)
        }
        val error: KPIError? = withTimeout(TEST_TIMEOUT) {
            completable.await()
        }

        assertNotNull(error)
        assertEquals(error.description, "No available endpoints to perform the request.")
    }

    @Test
    fun `Test error as a result of submitting before starting the module`() = testUtils.runBlockingTest {
        val kpi = givenAMockKpiApi()

        val completable = CompletableDeferred<KPIError?>()
        kpi.submit(event) { error ->
            completable.complete(error)
        }
        val error: KPIError? = withTimeout(TEST_TIMEOUT) {
            completable.await()
        }

        assertNotNull(error)
        assertEquals(error.description, "KPI has not been started. Event discarded.")
    }

    @Test
    fun `Test error as a result of stopping prior submission`() = testUtils.runBlockingTest {
        val kpi = givenAMockKpiApi()
        kpi.start()
        kpi.stop()

        val completable = CompletableDeferred<KPIError?>()
        kpi.submit(event) { error ->
            completable.complete(error)
        }
        val error: KPIError? = withTimeout(TEST_TIMEOUT) {
            completable.await()
        }

        assertNotNull(error)
        assertEquals(error.description, "KPI has not been started. Event discarded.")
    }

    @Test
    fun `Test clear of events after stopping the module`() = testUtils.runBlockingTest {
        mockClientStateProvider.setEndpoints(
            listOf(
                KPIEndpoint(
                    endpoint = "192.168.0.1",
                    isProxy = false,
                    usePinnedCertificate = false,
                    certificateCommonName = null
                )
            )
        )
        val kpi = givenAMockKpiApi() as KPI

        // confirm batching after starting the module
        kpi.start()
        val completableStart = CompletableDeferred<Unit>()
        kpi.submit(event) { }
        kpi.submit(event) { }
        kpi.submit(event) {
            completableStart.complete(Unit)
        }
        withTimeout(TEST_TIMEOUT) {
            completableStart.await()
        }
        assertEquals(kpi.batchedEvents.size, 3)

        // confirm clearing after stopping the module
        kpi.stop()
        delay(20L) // To be updated with the `stop` callback being introduced as part of MR#13
        assertEquals(kpi.batchedEvents.size, 0)
        assertEquals(kpi.sampleEvents.size, 0)
    }

    @Test
    fun `Test submission of multiple events`() = testUtils.runBlockingTest {
        mockClientStateProvider.setEndpoints(
            listOf(
                KPIEndpoint(
                    endpoint = "192.168.0.1",
                    isProxy = false,
                    usePinnedCertificate = false,
                    certificateCommonName = null
                )
            )
        )
        val kpi = givenAMockKpiApi() as KPI
        kpi.start()

        val completable = CompletableDeferred<Unit>()
        kpi.submit(event) { }
        kpi.submit(event) { }
        kpi.submit(event) { }
        kpi.submit(event) { }
        kpi.submit(event) {
            completable.complete(Unit)
        }
        withTimeout(TEST_TIMEOUT) {
            completable.await()
        }

        assertEquals(kpi.batchedEvents.size, 5)
    }

    @Test
    fun `Test max sample events being kept`() = testUtils.runBlockingTest {
        mockClientStateProvider.setEndpoints(
            listOf(
                KPIEndpoint(
                    endpoint = "192.168.0.1",
                    isProxy = false,
                    usePinnedCertificate = false,
                    certificateCommonName = null
                )
            )
        )
        val kpi = givenAMockKpiApi() as KPI
        kpi.start()

        // Test adding the max events to the history
        var completable = CompletableDeferred<Unit>()
        for (idx in 0 until EVENTS_HISTORY_SIZE) {
            if (idx == EVENTS_HISTORY_SIZE - 1) {
                kpi.submit(event) { completable.complete(Unit) }
            } else {
                kpi.submit(event) { }
            }
        }
        withTimeout(TEST_TIMEOUT) {
            completable.await()
        }
        assertEquals(kpi.sampleEvents.size, EVENTS_HISTORY_SIZE)

        // Test adding an extra one doesn't go beyond the max
        completable = CompletableDeferred<Unit>()
        kpi.submit(event) {
            completable.complete(Unit)
        }
        withTimeout(TEST_TIMEOUT) {
            completable.await()
        }
        assertEquals(kpi.sampleEvents.size, EVENTS_HISTORY_SIZE)
    }

    // region private
    private fun givenAMockKpiApi() =
        KPIBuilder()
            .setAppVersion("test-app-version")
            .setCertificate("test-certificate")
            .setKPIFlushEventMode(KPISendEventsMode.PER_BATCH)
            .setKPIClientStateProvider(mockClientStateProvider)
            .build()
    // endregion
}