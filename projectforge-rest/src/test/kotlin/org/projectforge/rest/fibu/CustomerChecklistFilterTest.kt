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

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.kost.Kost2DO

/**
 * The customer checklist matches a row by its customer entity or, for a row without one, by its free-text
 * customer — the two kinds of keys the frontend sends.
 */
class CustomerChecklistFilterTest {
    private val filter = CustomerChecklistFilter("order/customerFilterValues")

    @Test
    fun `entity keys match the order's customer`() {
        val predicate = filter.buildPredicate(arrayOf("k:473", "k:12"))!!
        assertTrue(predicate.match(order(kundeId = 473)))
        assertFalse(predicate.match(order(kundeId = 99)))
        assertFalse(predicate.match(order(kundeText = "473")))
    }

    @Test
    fun `text keys match only orders without a customer entity`() {
        val predicate = filter.buildPredicate(arrayOf("t:ACME"))!!
        assertTrue(predicate.match(order(kundeText = "ACME")))
        assertFalse(predicate.match(order(kundeText = "Other")))
        // The cell shows the entity then, so does the filter.
        assertFalse(predicate.match(order(kundeId = 5, kundeText = "ACME")))
    }

    @Test
    fun `both kinds combine as alternatives`() {
        val predicate = filter.buildPredicate(arrayOf("k:473", "t:ACME"))!!
        assertTrue(predicate.match(order(kundeId = 473)))
        assertTrue(predicate.match(order(kundeText = "ACME")))
        assertFalse(predicate.match(order(kundeId = 1)))
    }

    @Test
    fun `no usable key filters nothing`() {
        assertNull(filter.buildPredicate(null))
        assertNull(filter.buildPredicate(arrayOf()))
        assertNull(filter.buildPredicate(arrayOf("k:abc", "unknown")))
    }

    @Test
    fun `without a free-text path text keys are ignored`() {
        val projects = CustomerChecklistFilter("project/customerFilterValues", kundeTextPath = null)
        assertNull(projects.buildPredicate(arrayOf("t:ACME")))
        val predicate = projects.buildPredicate(arrayOf("k:473", "t:ACME"))!!
        assertTrue(predicate.match(ProjektDO().also { it.kunde = kunde(473) }))
        assertFalse(predicate.match(ProjektDO()))
    }

    @Test
    fun `a nested path matches the customer of the cost 2's project`() {
        val kost2s = CustomerChecklistFilter("cost2/customerFilterValues", kundePath = "projekt.kunde", kundeTextPath = null)
        val predicate = kost2s.buildPredicate(arrayOf("k:473"))!!
        assertTrue(predicate.match(kost2(kunde(473))))
        assertFalse(predicate.match(kost2(kunde(12))))
    }

    private fun kunde(id: Long) = KundeDO().also { it.nummer = id }

    private fun kost2(kunde: KundeDO) = Kost2DO().also { it.projekt = ProjektDO().also { p -> p.kunde = kunde } }

    private fun order(kundeId: Long? = null, kundeText: String? = null) = AuftragDO().also { order ->
        order.kunde = kundeId?.let { kunde(it) }
        order.kundeText = kundeText
    }
}
