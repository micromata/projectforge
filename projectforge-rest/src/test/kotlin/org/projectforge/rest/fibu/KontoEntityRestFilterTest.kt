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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.KontoDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.persistence.api.QueryFilter
import org.springframework.beans.factory.annotation.Autowired

/**
 * The account type pill of the list, from the filter entry the client sends to the result filter. Which number
 * falls into which type is [KontoTypeFilterTest]'s business; here the installation has no ranges configured, so
 * every account is a general ledger account.
 */
class KontoEntityRestFilterTest : AbstractTestBase() {
    @Autowired
    private lateinit var kontoEntityRest: KontoEntityRest

    private val konto = KontoDO().also { it.nummer = 1200 }

    private fun typeFilter(vararg types: String) = MagicFilter(
        entries = mutableListOf(
            MagicFilterEntry("accountType").also { it.value.values = arrayOf(*types) }
        )
    )

    @Test
    fun `the pill is offered with its three types`() {
        val elements = mutableListOf<org.projectforge.ui.UILabelledElement>()
        kontoEntityRest.addMagicFilterElements(elements)
        val values = (elements.single() as org.projectforge.ui.filter.UIFilterListElement).values!!.map { it.id }
        assertEquals(listOf("DEBITOR", "CREDITOR", "GENERAL_LEDGER"), values)
    }

    @Test
    fun `the chosen types narrow the list and the entry is left to this filter`() {
        val general = typeFilter("GENERAL_LEDGER")
        val filters = kontoEntityRest.preProcessMagicFilter(QueryFilter(), general)!!
        assertTrue(general.entries.single().synthetic == true, "not applied again as a property of the account")
        assertTrue(filters.single().match(mutableListOf(), konto))

        val debitors = kontoEntityRest.preProcessMagicFilter(QueryFilter(), typeFilter("DEBITOR"))!!
        assertFalse(debitors.single().match(mutableListOf(), konto))

        val both = kontoEntityRest.preProcessMagicFilter(QueryFilter(), typeFilter("DEBITOR", "GENERAL_LEDGER"))!!
        assertTrue(both.single().match(mutableListOf(), konto))
    }

    @Test
    fun `no choice or an unknown one filters nothing`() {
        assertNull(kontoEntityRest.preProcessMagicFilter(QueryFilter(), MagicFilter()))
        assertNull(kontoEntityRest.preProcessMagicFilter(QueryFilter(), typeFilter()))
        assertNull(kontoEntityRest.preProcessMagicFilter(QueryFilter(), typeFilter("SOMETHING")))
    }
}
