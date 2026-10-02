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

/**
 * The customer filter of the order book matches an order by its customer entity or, for an order without
 * one, by its free-text customer — the two kinds of keys the frontend sends.
 */
class OrderCustomerFilterTest {

    @Test
    fun `entity keys match the order's customer`() {
        val predicate = OrderCustomerFilter.buildPredicate(arrayOf("k:473", "k:12"))!!
        assertTrue(predicate.match(order(kundeId = 473)))
        assertFalse(predicate.match(order(kundeId = 99)))
        assertFalse(predicate.match(order(kundeText = "473")))
    }

    @Test
    fun `text keys match only orders without a customer entity`() {
        val predicate = OrderCustomerFilter.buildPredicate(arrayOf("t:ACME"))!!
        assertTrue(predicate.match(order(kundeText = "ACME")))
        assertFalse(predicate.match(order(kundeText = "Other")))
        // The cell shows the entity then, so does the filter.
        assertFalse(predicate.match(order(kundeId = 5, kundeText = "ACME")))
    }

    @Test
    fun `both kinds combine as alternatives`() {
        val predicate = OrderCustomerFilter.buildPredicate(arrayOf("k:473", "t:ACME"))!!
        assertTrue(predicate.match(order(kundeId = 473)))
        assertTrue(predicate.match(order(kundeText = "ACME")))
        assertFalse(predicate.match(order(kundeId = 1)))
    }

    @Test
    fun `no usable key filters nothing`() {
        assertNull(OrderCustomerFilter.buildPredicate(null))
        assertNull(OrderCustomerFilter.buildPredicate(arrayOf()))
        assertNull(OrderCustomerFilter.buildPredicate(arrayOf("k:abc", "unknown")))
    }

    private fun order(kundeId: Long? = null, kundeText: String? = null) = AuftragDO().also { order ->
        order.kunde = kundeId?.let { id -> KundeDO().also { it.nummer = id } }
        order.kundeText = kundeText
    }
}
