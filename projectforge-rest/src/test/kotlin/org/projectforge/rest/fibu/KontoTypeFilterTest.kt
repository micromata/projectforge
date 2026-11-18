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

/** The kinds of accounts are told apart by the configured number ranges. */
class KontoTypeFilterTest {
    private val debitors = KontoType.rangesOf("10000-69999")
    private val creditors = KontoType.rangesOf("70000-79999")

    @Test
    fun `numbers are classified by the configured ranges`() {
        assertEquals(KontoType.DEBITOR, KontoType.of(10001, debitors, creditors))
        assertEquals(KontoType.CREDITOR, KontoType.of(70500, debitors, creditors))
        assertEquals(KontoType.GENERAL_LEDGER, KontoType.of(1200, debitors, creditors))
        assertEquals(KontoType.GENERAL_LEDGER, KontoType.of(80000, debitors, creditors))
        assertNull(KontoType.of(null, debitors, creditors))
    }

    @Test
    fun `without configured ranges every account is a general ledger account`() {
        assertNull(KontoType.rangesOf(null))
        assertNull(KontoType.rangesOf(" "))
        assertEquals(KontoType.GENERAL_LEDGER, KontoType.of(10001, null, null))
    }

    @Test
    fun `the filter keeps the accounts of any chosen type`() {
        val filter = KontoTypeFilter(setOf(KontoType.DEBITOR, KontoType.CREDITOR), debitors, creditors)
        assertTrue(filter.match(mutableListOf(), account(10001)))
        assertTrue(filter.match(mutableListOf(), account(70500)))
        assertFalse(filter.match(mutableListOf(), account(1200)))
    }

    private fun account(nummer: Int) = KontoDO().also { it.nummer = nummer }
}
