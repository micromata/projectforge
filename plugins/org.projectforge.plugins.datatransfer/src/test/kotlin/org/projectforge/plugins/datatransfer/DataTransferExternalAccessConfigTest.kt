/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.plugins.datatransfer

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectforge.plugins.datatransfer.DataTransferExternalAccessConfig.Mode

class DataTransferExternalAccessConfigTest {
    @Test
    fun `mode depends on localAllowed and gateway push`() {
        assertEquals(Mode.LOCAL, config(push = false, localAllowed = null).mode)
        assertEquals(Mode.VIA_GATEWAY, config(push = true, localAllowed = null).mode)
        assertEquals(Mode.LOCAL, config(push = true, localAllowed = true).mode)
        assertEquals(Mode.NONE, config(push = true, localAllowed = false).mode)
        assertEquals(Mode.NONE, config(push = false, localAllowed = false).mode)
    }

    @Test
    fun `gateway url is the push url without the sync api path unless configured`() {
        assertEquals(
            "https://gw.example.com/pf",
            config(pushUrl = "https://gw.example.com/pf/api/gateway/sync/").gatewayPublicUrl,
        )
        assertEquals(
            "https://public.example.com",
            config(pushUrl = "http://10.0.0.1/api/gateway/sync", publicUrl = "https://public.example.com/").gatewayPublicUrl,
        )
    }

    @Test
    fun `only areas with external access are on the gateway, no personal boxes`() {
        val config = config(push = true)
        val area = DataTransferAreaDO().also { it.areaName = "Area"; it.adminIds = "1" }
        assertFalse(config.isOnGateway(area))
        area.externalUploadEnabled = true
        assertTrue(config.isOnGateway(area))
        assertFalse(config(push = true, localAllowed = true).isOnGateway(area))
        area.areaName = DataTransferAreaDO.PERSONAL_BOX_AREA_NAME
        assertFalse(config.isOnGateway(area))
    }

    private fun config(
        push: Boolean = true,
        localAllowed: Boolean? = null,
        pushUrl: String = "https://gw.example.com/api/gateway/sync",
        publicUrl: String = "",
    ) = DataTransferExternalAccessConfig(
        gatewayPushEnabled = push,
        gatewayPushUrl = pushUrl,
        gatewayPublicUrlConfig = publicUrl,
        localAllowed = localAllowed,
    )
}
