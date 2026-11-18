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

package org.projectforge.rest.fibu

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.projectforge.rest.dto.Auftrag
import org.projectforge.rest.dto.PostData
import org.springframework.beans.factory.annotation.Autowired

/** The account's contact data is handed out only to those who may create orders. */
class OrderCustomerContactAccessTest : AbstractTestBase() {
    @Autowired
    private lateinit var orderEntityRest: OrderEntityRest

    private fun post() = PostData(Auftrag(), watchFieldsTriggered = null, serverData = null)

    @Test
    fun `an ordinary user is refused`() {
        logon(TEST_USER)
        assertThrows<AccessException> { orderEntityRest.customerContactFromAccount(post()) }
    }

    @Test
    fun `the finance user gets an answer, empty without a customer`() {
        logon(TEST_FINANCE_USER)
        assertNull(orderEntityRest.customerContactFromAccount(post()).customerContactPerson)
    }
}
