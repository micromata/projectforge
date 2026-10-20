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

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.springframework.beans.factory.annotation.Autowired

/** The order access grants of the customer-groups editor ([CustomerGroupPageRest]). */
class CustomerGroupPageRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var customerGroupPageRest: CustomerGroupPageRest

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `the users a grant has no effect for are named, with the reason`() {
        val pm = getUser(TEST_PROJECT_MANAGER_USER)
        val other = getUser(TEST_USER)
        val data = read(
            """{"orderAccess":[{"key":"grant1","name":"Test","users":[
              {"id":${pm.id},"displayName":"PM","deactivated":false},{"id":${other.id},"displayName":"Other"}],
              "customers":[],"groups":[],"businessUnits":[]}]}"""
        )
        logon(TEST_USER)
        assertThrows<AccessException> { customerGroupPageRest.orderAccessInfo(data) }

        logon(TEST_FINANCE_USER)
        val info = customerGroupPageRest.orderAccessInfo(data)
        assertEquals(setOf("grant1"), info.keys)
        val ineffective = info["grant1"]!!.single()
        assertEquals(other.getFullname(), ineffective.name)
        assertEquals(CustomerGroupPageRest.IneffectiveReason.NO_PROJECT_MANAGER, ineffective.reason)
        // Nothing granted: the only error, the project manager isn't refused.
        assertEquals(
            listOf("orderAccess[0].customers"),
            customerGroupPageRest.validate(data).map { it.fieldId },
        )
    }

    /** As the frontend posts it: the references carry flags of their own, which must not fail the request. */
    private fun read(json: String): CustomerGroupPageRest.CustomerGroupsData {
        val strict = objectMapper.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
        return strict.readValue(json, CustomerGroupPageRest.CustomerGroupsData::class.java).also {
            assertTrue(it.orderAccess.single().users.size == 2)
        }
    }
}
