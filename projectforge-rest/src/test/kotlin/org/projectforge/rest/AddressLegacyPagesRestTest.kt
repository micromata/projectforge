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

package org.projectforge.rest

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.rest.dto.Address
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.TargetType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession

/**
 * The React form of an address, left for the address campaigns, which open it as a modal.
 */
class AddressLegacyPagesRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var addressLegacyPagesRest: AddressLegacyPagesRest

    @Test
    fun `the form opened as modal remembers it and every button closes the modal`() {
        logon(TEST_USER)
        val request = MockHttpServletRequest().also {
            it.setSession(MockHttpSession())
            it.addParameter("modal", "true")
        }
        val formLayoutData = addressLegacyPagesRest.getItemAndLayout(request, null, null).body!!
        val serverData = formLayoutData.serverData!!
        assertEquals("true", serverData.returnToCallerParams?.get("modal"))

        val postData = PostData(
            data = formLayoutData.data as Address,
            watchFieldsTriggered = null,
            serverData = serverData,
        )
        assertEquals(TargetType.CLOSE_MODAL, addressLegacyPagesRest.onCancelEdit(request, postData).targetType)
    }

    @Test
    fun `without the modal the user lands on the address list of projectforge-next`() {
        logon(TEST_USER)
        val request = MockHttpServletRequest().also { it.setSession(MockHttpSession()) }
        val formLayoutData = addressLegacyPagesRest.getItemAndLayout(request, null, null).body!!
        val postData = PostData(
            data = formLayoutData.data as Address,
            watchFieldsTriggered = null,
            serverData = formLayoutData.serverData,
        )
        assertEquals("/next/address", addressLegacyPagesRest.onCancelEdit(request, postData).url)
    }
}
