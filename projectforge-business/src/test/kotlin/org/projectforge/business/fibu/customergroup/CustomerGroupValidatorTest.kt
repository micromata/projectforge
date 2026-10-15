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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.customergroup.CustomerGroupConfigTest.Companion.businessUnit
import org.projectforge.business.fibu.customergroup.CustomerGroupConfigTest.Companion.group
import org.projectforge.business.fibu.customergroup.CustomerGroupConfigTest.Companion.sample
import org.projectforge.business.fibu.customergroup.CustomerGroupValidator.Companion as V

/** One test per rule of [CustomerGroupValidator]: which error, on which field. */
class CustomerGroupValidatorTest {
    private val validator = CustomerGroupValidator(
        CustomerDirectory(
            customers = mapOf(4L to "Four", 5L to "Five", 101L to "ACME Germany", 102L to "ACME Logistics", 310L to "Kühne Spedition"),
            freeTexts = listOf("ACME Freight Logistics", "Unrelated Ltd."),
            // Task 1 is the root; 10 ⊃ 11, and 20 beside them.
            taskPath = { mapOf(1L to listOf(1L), 10L to listOf(1L, 10L), 11L to listOf(1L, 10L, 11L), 20L to listOf(1L, 20L))[it] },
            taskTitle = { "Task $it" },
        )
    )

    @Test
    fun `a valid config passes`() {
        assertTrue(validator.validate(sample()).isEmpty())
        assertTrue(validator.validate(CustomerGroupConfig()).isEmpty())
    }

    @Test
    fun `names are required and unique per list`() {
        assertErrors(
            config(groups = listOf(group("aaaa", " "), group("bbbb", "ACME"), group("cccc", "acme"))),
            "groups[0].name" to V.ERROR_NAME_REQUIRED,
            "groups[1].name" to V.ERROR_DUPLICATE_NAME,
            "groups[2].name" to V.ERROR_DUPLICATE_NAME,
        )
        // A group and a business unit may share a name:
        assertTrue(validator.validate(config(listOf(group("aaaa", "X")), listOf(businessUnit("bbbb", "X")))).isEmpty())
    }

    @Test
    fun `keys must have the server's format and be unique`() {
        assertErrors(
            config(groups = listOf(group("ab", "A"), group("Abcd", "B"), group("abcd", "C"), group("abcd", "D"))),
            "groups[0].name" to V.ERROR_INVALID_KEY,
            "groups[1].name" to V.ERROR_INVALID_KEY,
            "groups[3].name" to V.ERROR_INVALID_KEY,
        )
    }

    @Test
    fun `customers must exist and texts be valid patterns`() {
        assertErrors(
            config(groups = listOf(group("aaaa", "A", listOf(5, 4711), listOf("ok", "A*B")))),
            "groups[0].customers" to V.ERROR_UNKNOWN_CUSTOMER,
            "groups[0].texts" to V.ERROR_INVALID_PATTERN,
        )
    }

    @Test
    fun `a customer or text belongs to at most one group`() {
        assertErrors(
            config(groups = listOf(group("aaaa", "A", listOf(5), listOf("ACME")), group("bbbb", "B", listOf(5), listOf("acme")))),
            "groups[0].customers" to V.ERROR_CUSTOMER_IN_TWO_GROUPS,
            "groups[1].customers" to V.ERROR_CUSTOMER_IN_TWO_GROUPS,
            "groups[0].texts" to V.ERROR_PATTERN_OVERLAP,
            "groups[1].texts" to V.ERROR_PATTERN_OVERLAP,
        )
    }

    @Test
    fun `a pattern must not cover or be covered by another group's pattern`() {
        // Whether a customer matching both exists or not:
        assertErrors(
            config(groups = listOf(group("aaaa", "A", texts = listOf("dhl*")), group("bbbb", "B", texts = listOf("dh*")))),
            "groups[0].texts" to V.ERROR_PATTERN_OVERLAP,
            "groups[1].texts" to V.ERROR_PATTERN_OVERLAP,
        )
        assertErrors(
            config(groups = listOf(group("aaaa", "A", texts = listOf("ACME*")), group("bbbb", "B", texts = listOf("ACME Holding")))),
            "groups[0].texts" to V.ERROR_PATTERN_OVERLAP,
            "groups[1].texts" to V.ERROR_PATTERN_OVERLAP,
        )
        // Within the same group it is merely redundant:
        assertTrue(validator.validate(config(groups = listOf(group("aaaa", "A", texts = listOf("ACME*", "ACME AG"))))).isEmpty())
    }

    @Test
    fun `patterns of different kinds claiming the same customer are found by the data`() {
        val errors = validator.validate(
            config(groups = listOf(group("aaaa", "A", texts = listOf("ACME*")), group("bbbb", "B", texts = listOf("*Logistics"))))
        )
        // On both groups, each naming the other; the customer entity (by its name) and the free text, both caught
        // by both patterns:
        assertEquals(
            listOf(
                "groups[0].texts" to listOf("102 ACME Logistics, ACME Freight Logistics", "B", 2),
                "groups[1].texts" to listOf("102 ACME Logistics, ACME Freight Logistics", "A", 2),
            ),
            errors.filter { it.messageKey == V.ERROR_CLAIMED_TWICE }.map { it.fieldId to it.params }.sortedBy { it.first },
        )
        assertEquals(2, errors.size)
        // Nothing caught twice, nothing reported:
        assertTrue(
            validator.validate(config(groups = listOf(group("aaaa", "A", texts = listOf("ACME*")), group("bbbb", "B", texts = listOf("*Ltd."))))).isEmpty()
        )
    }

    @Test
    fun `an explicit customer must not be caught by another group's pattern`() {
        assertErrors(
            config(groups = listOf(group("aaaa", "A", listOf(102)), group("bbbb", "B", texts = listOf("ACME*")))),
            "groups[0].customers" to V.ERROR_CLAIMED_TWICE,
            "groups[1].texts" to V.ERROR_CLAIMED_TWICE,
        )
        assertErrors(
            config(groups = listOf(group("aaaa", "A", texts = listOf("ACME*")), group("bbbb", "B", listOf(102)))),
            "groups[0].texts" to V.ERROR_CLAIMED_TWICE,
            "groups[1].customers" to V.ERROR_CLAIMED_TWICE,
        )
        // In its own group's pattern as well: redundant only.
        assertTrue(validator.validate(config(groups = listOf(group("aaaa", "A", listOf(102), listOf("ACME*"))))).isEmpty())
    }

    @Test
    fun `a business unit refers to known groups, each in one business unit only`() {
        assertErrors(
            config(
                groups = listOf(group("aaaa", "A")),
                businessUnits = listOf(businessUnit("bu01", "X", listOf("aaaa", "gone")), businessUnit("bu02", "Y", listOf("aaaa"))),
            ),
            "businessUnits[0].groups" to V.ERROR_UNKNOWN_GROUP,
            "businessUnits[0].groups" to V.ERROR_GROUP_IN_TWO_BUSINESS_UNITS,
            "businessUnits[1].groups" to V.ERROR_GROUP_IN_TWO_BUSINESS_UNITS,
        )
    }

    @Test
    fun `a member belongs to at most one business unit`() {
        assertErrors(
            config(businessUnits = listOf(businessUnit("bu01", "X", customers = listOf(5)), businessUnit("bu02", "Y", customers = listOf(5)))),
            "businessUnits[0].customers" to V.ERROR_MEMBER_IN_TWO_BUSINESS_UNITS,
            "businessUnits[1].customers" to V.ERROR_MEMBER_IN_TWO_BUSINESS_UNITS,
        )
    }

    @Test
    fun `a member must not be in one business unit directly and in another via its group`() {
        assertErrors(
            config(
                groups = listOf(group("aaaa", "A", listOf(5), listOf("ACME*"))),
                businessUnits = listOf(
                    businessUnit("bu01", "X", listOf("aaaa")),
                    businessUnit("bu02", "Y", customers = listOf(5), texts = listOf("ACME AG")),
                ),
            ),
            "businessUnits[0].groups" to V.ERROR_MEMBER_IN_TWO_BUSINESS_UNITS,
            "businessUnits[0].groups" to V.ERROR_PATTERN_OVERLAP,
            "businessUnits[1].customers" to V.ERROR_MEMBER_IN_TWO_BUSINESS_UNITS,
            "businessUnits[1].texts" to V.ERROR_PATTERN_OVERLAP,
        )
        // The other way round, the business unit holding the group shows it on its groups:
        assertErrors(
            config(
                groups = listOf(group("aaaa", "A", listOf(5))),
                businessUnits = listOf(businessUnit("bu01", "X", customers = listOf(5)), businessUnit("bu02", "Y", listOf("aaaa"))),
            ),
            "businessUnits[0].customers" to V.ERROR_MEMBER_IN_TWO_BUSINESS_UNITS,
            "businessUnits[1].groups" to V.ERROR_MEMBER_IN_TWO_BUSINESS_UNITS,
        )
        // Directly in the business unit its group is in, too: redundant, not a conflict.
        assertTrue(
            validator.validate(
                config(listOf(group("aaaa", "A", listOf(5))), listOf(businessUnit("bu01", "X", listOf("aaaa"), listOf(5))))
            ).isEmpty()
        )
    }

    @Test
    fun `tasks must exist and not lie in another business unit's task`() {
        assertErrors(config(businessUnits = listOf(withTasks("bu01", "X", 10, 99))), "businessUnits[0].tasks" to V.ERROR_UNKNOWN_TASK)
        // The same task, and a nested one in either order: on both business units.
        assertErrors(
            config(businessUnits = listOf(withTasks("bu01", "X", 20), withTasks("bu02", "Y", 20))),
            "businessUnits[0].tasks" to V.ERROR_TASK_OVERLAP,
            "businessUnits[1].tasks" to V.ERROR_TASK_OVERLAP,
        )
        assertErrors(
            config(businessUnits = listOf(withTasks("bu01", "X", 10), withTasks("bu02", "Y", 11))),
            "businessUnits[0].tasks" to V.ERROR_TASK_CONTAINS,
            "businessUnits[1].tasks" to V.ERROR_TASK_OVERLAP,
        )
        assertErrors(
            config(businessUnits = listOf(withTasks("bu01", "X", 11), withTasks("bu02", "Y", 10))),
            "businessUnits[0].tasks" to V.ERROR_TASK_OVERLAP,
            "businessUnits[1].tasks" to V.ERROR_TASK_CONTAINS,
        )
        // Side by side, or nested within one business unit: fine.
        assertTrue(validator.validate(config(businessUnits = listOf(withTasks("bu01", "X", 10, 11), withTasks("bu02", "Y", 20)))).isEmpty())
    }

    private fun withTasks(key: String, name: String, vararg tasks: Long) =
        businessUnit(key, name).also { it.tasks = tasks.toMutableList() }

    private fun config(groups: List<CustomerGroup> = emptyList(), businessUnits: List<BusinessUnit> = emptyList()) =
        CustomerGroupConfig(groups = groups.toMutableList(), businessUnits = businessUnits.toMutableList())

    /** In any order: what matters is which field shows which error. */
    private fun assertErrors(config: CustomerGroupConfig, vararg expected: Pair<String, String>) {
        val order = compareBy<Pair<String, String>>({ it.first }, { it.second })
        assertEquals(expected.sortedWith(order), validator.validate(config).map { it.fieldId to it.messageKey }.sortedWith(order))
    }
}
