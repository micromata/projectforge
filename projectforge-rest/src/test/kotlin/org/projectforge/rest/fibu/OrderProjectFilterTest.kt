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

package org.projectforge.rest.fibu

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.ProjektDO

/** The project filter of the order book matches an order by the id of its project. */
class OrderProjectFilterTest {

    @Test
    fun `ids match the order's project`() {
        val predicate = OrderProjectFilter.buildPredicate(arrayOf("17", "42"))!!
        assertTrue(predicate.match(order(projektId = 17)))
        assertTrue(predicate.match(order(projektId = 42)))
        assertFalse(predicate.match(order(projektId = 99)))
        assertFalse(predicate.match(order(projektId = null)))
    }

    @Test
    fun `no usable key filters nothing`() {
        assertNull(OrderProjectFilter.buildPredicate(null))
        assertNull(OrderProjectFilter.buildPredicate(arrayOf()))
        assertNull(OrderProjectFilter.buildPredicate(arrayOf("abc")))
    }

    private fun order(projektId: Long?) = AuftragDO().also { order ->
        order.projekt = projektId?.let { id -> ProjektDO().also { it.id = id } }
    }
}
