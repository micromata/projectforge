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


package org.projectforge.business.fibu.customergroup

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.customergroup.CustomerGroupConfigTest.Companion.businessUnit
import org.projectforge.business.fibu.customergroup.CustomerGroupConfigTest.Companion.grant
import org.projectforge.business.fibu.customergroup.CustomerGroupConfigTest.Companion.group

/** Which orders the order access grants give whom, see [OrderAccessGrants]. */
class OrderAccessGrantsTest {
    private val directory = CustomerDirectory(
        customers = mapOf(101L to "ACME Germany", 200L to "Internal", 300L to "Kühne Spedition", 400L to "Single"),
        // Project 1 of the internal customer below task 10 (in BU Blue), project 2 below no business unit's task.
        projects = mapOf(1L to ProjectRef(200, 11), 2L to ProjectRef(200, 30)),
        taskPath = { mapOf(10L to listOf(1L, 10L), 11L to listOf(1L, 10L, 11L), 30L to listOf(1L, 30L))[it] },
    )

    private val config = CustomerGroupConfig(
        groups = mutableListOf(group("acmeg1", "ACME", texts = listOf("ACME*"))),
        businessUnits = mutableListOf(
            businessUnit("blue01", "Blue", texts = listOf("*Spedition")).also { it.tasks = mutableListOf(10) },
        ),
        orderAccess = mutableListOf(
            grant("grant1", "Single customer", userIds = listOf(PM), customers = listOf(400)),
            grant("grant2", "ACME", groupIds = listOf(TEAM), groups = listOf("acmeg1")),
            grant("grant3", "Blue", userIds = listOf(ASSISTANT, OUTSIDER), businessUnits = listOf("blue01")),
        ),
    )

    private val index = CustomerGroupIndex(config, directory)

    private val memberships = mapOf(
        PM to setOf(PM_GROUP, TEAM),
        ASSISTANT to setOf(PA_GROUP),
        OUTSIDER to setOf(TEAM),
    )

    private val grants = OrderAccessGrants(index, memberships, setOf(PM_GROUP, PA_GROUP))

    @Test
    fun `a customer grant covers the orders of that customer entity only`() {
        assertTrue(grants.isGranted(PM, 400, null, null))
        assertFalse(grants.isGranted(PM, null, "Single", null), "Not a free text of the same name.")
        assertFalse(grants.isGranted(PM, 200, null, 1))
    }

    @Test
    fun `a group grant reaches its members through a group of users, by entity and by free text`() {
        assertTrue(grants.isGranted(PM, 101, null, null))
        assertTrue(grants.isGranted(PM, null, "ACME Holding", null))
        assertFalse(grants.isGranted(PM, 300, null, null))
    }

    @Test
    fun `a business unit grant follows the customer, else the project's task`() {
        assertTrue(grants.isGranted(ASSISTANT, 300, null, null)) // by the customer's name
        assertTrue(grants.isGranted(ASSISTANT, null, "Müller Spedition", null)) // by free text
        assertTrue(grants.isGranted(ASSISTANT, 200, null, 1)) // the project's task
        assertFalse(grants.isGranted(ASSISTANT, 200, null, 2))
        // ACME's group has no business unit, so its order on project 1 counts by the task:
        assertTrue(grants.isGranted(ASSISTANT, 101, null, 1))
        assertFalse(grants.isGranted(ASSISTANT, 101, null, 2))
        assertFalse(grants.isGranted(ASSISTANT, 400, null, null))
    }

    @Test
    fun `only project managers and assistants are granted`() {
        assertFalse(grants.isEligible(OUTSIDER))
        assertTrue(grants.isNamed(config.orderAccess[2], OUTSIDER))
        assertFalse(grants.isGranted(OUTSIDER, 300, null, null))
        assertFalse(grants.isGranted(OUTSIDER, 101, null, null))
        assertFalse(grants.isGranted(UNKNOWN, 300, null, null))
        assertFalse(grants.isGranted(null, 300, null, null))
    }

    @Test
    fun `a membership left or a grant removed revokes the access with the next snapshot`() {
        val left = OrderAccessGrants(index, memberships + (PM to setOf(TEAM)), setOf(PM_GROUP, PA_GROUP))
        assertFalse(left.isGranted(PM, 400, null, null))
        val removed = CustomerGroupConfig(
            groups = config.groups, businessUnits = config.businessUnits, orderAccess = config.orderAccess.drop(1).toMutableList(),
        )
        val withoutGrant = OrderAccessGrants(CustomerGroupIndex(removed, directory), memberships, setOf(PM_GROUP, PA_GROUP))
        assertFalse(withoutGrant.isGranted(PM, 400, null, null))
        assertTrue(withoutGrant.isGranted(PM, 101, null, null))
        assertFalse(OrderAccessGrants.EMPTY.isGranted(PM, 400, null, null))
    }

    companion object {
        private const val PM = 1L
        private const val ASSISTANT = 2L
        private const val OUTSIDER = 3L
        private const val UNKNOWN = 4L
        private const val PM_GROUP = 11L
        private const val PA_GROUP = 12L
        private const val TEAM = 13L
    }
}
