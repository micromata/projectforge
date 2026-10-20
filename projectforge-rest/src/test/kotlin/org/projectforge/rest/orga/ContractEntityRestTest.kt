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

package org.projectforge.rest.orga

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.orga.ContractDO
import org.projectforge.business.orga.ContractDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.configuration.ConfigurationJsonValues
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.rest.dto.Contract
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.ValidationError
import org.projectforge.ui.filter.UIFilterElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession
import java.time.LocalDate

/**
 * The contracts of projectforge-next ([ContractEntityRest]): the types the form offers, the check of the date, the
 * clone and the year filter of the former React list.
 */
class ContractEntityRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var contractDao: ContractDao

    @Autowired
    private lateinit var contractEntityRest: ContractEntityRest

    /** The types are the configured ones, for users with access to the contracts only. */
    @Test
    fun `the contract types are offered to users with access to the contracts only`() {
        try {
            ConfigurationJsonValues.internalSetForTest(
                ConfigurationParam.CONTRACT_TYPES,
                """[{"value":"nda","label":"Non-disclosure agreement"},{"value":"lease"},{"value":" "}]""",
            )
            logon(TEST_USER)
            assertThrows<AccessException> { contractEntityRest.getTypes() }
            logon(TEST_FULL_ACCESS_USER)
            val types = contractEntityRest.getTypes()
            assertEquals(listOf("nda", "lease"), types.map { it.value })
            // Without a label, the value is the label.
            assertEquals(listOf("Non-disclosure agreement", "lease"), types.map { it.label })
        } finally {
            ConfigurationJsonValues.internalSetForTest(ConfigurationParam.CONTRACT_TYPES, null)
        }
    }

    /** A contract can't be dated in the future. */
    @Test
    fun `a date in the future is refused`() {
        logon(TEST_FULL_ACCESS_USER)
        assertTrue(validate(LocalDate.now()).isEmpty())
        assertTrue(validate(null).isEmpty())
        assertEquals(listOf("date"), validate(LocalDate.now().plusDays(1)).map { it.fieldId })
    }

    /** A clone gets a number of its own on saving. */
    @Test
    fun `a clone has no number`() {
        logon(TEST_FULL_ACCESS_USER)
        val clone = contractEntityRest.prepareClone(Contract(id = 42, number = 1000, title = "Lease"))
        assertNull(clone.number)
        assertEquals("Lease", clone.title)
    }

    /** A year stored by the former React list still narrows the list, and is offered, so it can be seen and removed. */
    @Test
    fun `a stored year filter is offered and applied`() {
        val elements = mutableListOf<UILabelledElement>()
        contractEntityRest.addMagicFilterElements(elements)
        assertEquals(listOf("year"), elements.map { (it as UIFilterElement).id })

        logon(TEST_FINANCE_USER) // Writes contracts (the full access user only reads them).
        val title = "Year filter ${System.nanoTime()}"
        listOf(2020, 2021).forEach { year ->
            contractDao.insert(ContractDO().also {
                it.title = title
                it.date = LocalDate.of(year, 6, 1)
            })
        }
        // Picked by title rather than searched for: a fresh insert is not yet in the full text index.
        val filter = MagicFilter().also { it.entries.add(MagicFilterEntry("year", "2020")) }
        val request = MockHttpServletRequest().also { it.setSession(MockHttpSession()) }
        val dates = contractEntityRest.getList(request, filter, doNotStore = true).resultSet
            .map { it as Contract }.filter { it.title == title }.map { it.date }
        assertEquals(listOf(LocalDate.of(2020, 6, 1)), dates)
    }

    private fun validate(date: LocalDate?): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        contractEntityRest.validate(errors, Contract(date = date))
        return errors
    }
}
