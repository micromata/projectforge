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
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungTyp

class OutgoingInvoiceNumberSortTest {
    @Test
    fun `a cancellation sorts right behind the invoice it cancels, numbers numerically`() {
        val original = invoice(9999)
        val cancellation = RechnungDO().also {
            it.typ = RechnungTyp.CANCELLATION
            it.originalRechnung = original
        }
        val planned = RechnungDO() // No number yet: ranks as blank.
        val sorted = listOf(invoice(10000), cancellation, planned, invoice(9998), original)
            .sortedWith(compareBy(nullsFirst()) { OutgoingInvoiceEntityRest.numberSortKey(it) })
            .map { it.belegNummer }
        assertEquals(listOf(null, "9998", "9999", "9999-S", "10000"), sorted)
    }

    private fun invoice(nummer: Int) = RechnungDO().also {
        it.typ = RechnungTyp.RECHNUNG
        it.nummer = nummer
    }
}
