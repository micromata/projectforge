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

package org.projectforge.gateway

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GatewayPageControllerTest {
    @Test
    fun acceptsDataTransferReturnUrls() {
        assertEquals("/next/datatransfer", GatewayPageController.safeReturnUrl("/next/datatransfer"))
        assertEquals("/next/datatransfer/42/?q=1", GatewayPageController.safeReturnUrl("/next/datatransfer/42/?q=1"))
    }

    @Test
    fun addsBasePathToAppRoutes() {
        // The next client's AuthGuard names the route without the base path.
        assertEquals("/next/datatransfer/42", GatewayPageController.safeReturnUrl("/datatransfer/42"))
        assertEquals("/next/datatransfer/personal-box", GatewayPageController.safeReturnUrl("/datatransfer/personal-box"))
    }

    @Test
    fun rejectsForeignOrOtherReturnUrls() {
        assertNull(GatewayPageController.safeReturnUrl("https://evil.example.com/next/datatransfer"))
        assertNull(GatewayPageController.safeReturnUrl("//evil.example.com"))
        assertNull(GatewayPageController.safeReturnUrl("/next/datatransfer//evil.example.com"))
        assertNull(GatewayPageController.safeReturnUrl("/datatransfer\\evil.example.com"))
        assertNull(GatewayPageController.safeReturnUrl("/next/datatransferX"))
        assertNull(GatewayPageController.safeReturnUrl("/next/address"))
        assertNull(GatewayPageController.safeReturnUrl("/address"))
        assertNull(GatewayPageController.safeReturnUrl("/react/datatransfer"))
    }
}
