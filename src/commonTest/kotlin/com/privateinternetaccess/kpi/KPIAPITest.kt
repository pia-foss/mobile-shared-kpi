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

import com.privateinternetaccess.kpi.internals.util.MockClientStateProvider
import kotlin.test.Test
import kotlin.test.assertFailsWith


class KPIAPITest {

    @Test
    fun `Test undefined mandatory dependencies`() {
        assertFailsWith(Exception::class) {
            KPIBuilder()
                .build()
        }
    }

    @Test
    fun `Test undefined app version`() {
        assertFailsWith(Exception::class) {
            KPIBuilder()
                .setCertificate("test-certificate")
                .setKPIFlushEventMode(KPISendEventsMode.PER_BATCH)
                .setKPIClientStateProvider(MockClientStateProvider())
                .build()
        }
    }

    @Test
    fun `Test undefined flush mode`() {
        assertFailsWith(Exception::class) {
            KPIBuilder()
                .setAppVersion("test-app-version")
                .setCertificate("test-certificate")
                .setKPIClientStateProvider(MockClientStateProvider())
                .build()
        }
    }

    @Test
    fun `Test undefined client state provider`() {
        assertFailsWith(Exception::class) {
            KPIBuilder()
                .setAppVersion("test-app-version")
                .setCertificate("test-certificate")
                .setKPIFlushEventMode(KPISendEventsMode.PER_BATCH)
                .build()
        }
    }

    @Test
    fun `Test defined dependencies`() {
        // We are testing the lack of exceptions being thrown here.
        KPIBuilder()
            .setAppVersion("test-app-version")
            .setCertificate("test-certificate")
            .setKPIFlushEventMode(KPISendEventsMode.PER_BATCH)
            .setKPIClientStateProvider(MockClientStateProvider())
            .build()
    }
}
