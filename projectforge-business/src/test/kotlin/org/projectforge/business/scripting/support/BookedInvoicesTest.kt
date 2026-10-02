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

package org.projectforge.business.scripting.support

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.KontoDO
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungTyp
import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.framework.time.PFDay
import java.time.LocalDate

class BookedInvoicesTest {
  @Test
  fun `tokens are extracted from the document number`() {
    assertEquals(listOf("17209"), BookedInvoices.tokens("17209"))
    assertEquals(listOf("17209"), BookedInvoices.tokens("RE17209"))
    assertEquals(listOf("17209", "1"), BookedInvoices.tokens("17209/1"))
    assertEquals(listOf("17209-S"), BookedInvoices.tokens("17209-s"))
    assertEquals(listOf("15917-S"), BookedInvoices.tokens("15917S")) // Older cancellations
    assertEquals(emptyList<String>(), BookedInvoices.tokens(null))
    assertEquals(emptyList<String>(), BookedInvoices.tokens("Gehalt"))
  }

  @Test
  fun `invoice booked into an earlier month is found with its booking date`() {
    val booked = BookedInvoices(listOf(record("17209", 4400, 2026, 7, 31)))
    val invoice = invoice(17209)
    assertTrue(booked.isBooked(invoice))
    assertEquals(PFDay.of(2026, 7, 31), booked.bookedDate(invoice))
    assertFalse(booked.isBooked(invoice(17315)))
  }

  @Test
  fun `records outside the revenue accounts are ignored`() {
    val booked = BookedInvoices(listOf(record("17209", 1200, 2026, 7, 31), record("17209", 6300, 2026, 7, 31)))
    assertFalse(booked.isBooked(invoice(17209)))
    assertEquals(0, booked.size)
  }

  @Test
  fun `the earliest booking date wins`() {
    val booked = BookedInvoices(
      listOf(
        record("17209", 4400, 2026, 8, 31),
        record("RE 17209", 4400, 2026, 7, 31),
        record("17209", 4400, 2026, 9, 30),
      )
    )
    assertEquals(PFDay.of(2026, 7, 31), booked.bookedDate(invoice(17209)))
  }

  @Test
  fun `cancellation matches only its own number`() {
    val original = invoice(17209)
    val cancellation = RechnungDO().also {
      it.typ = RechnungTyp.CANCELLATION
      it.originalRechnung = original
    }
    val onlyCancellation = BookedInvoices(listOf(record("17209-S", 4400, 2026, 8, 31)))
    assertTrue(onlyCancellation.isBooked(cancellation))
    assertFalse(onlyCancellation.isBooked(original))

    val onlyOriginal = BookedInvoices(listOf(record("17209", 4400, 2026, 7, 31)))
    assertTrue(onlyOriginal.isBooked(original))
    assertFalse(onlyOriginal.isBooked(cancellation))
  }

  @Test
  fun `invoice without number is never booked`() {
    val booked = BookedInvoices(listOf(record("17209", 4400, 2026, 7, 31)))
    assertNull(booked.bookedDate(RechnungDO()))
  }

  private fun invoice(nummer: Int): RechnungDO = RechnungDO().also {
    it.nummer = nummer
    it.typ = RechnungTyp.RECHNUNG
  }

  private fun record(beleg: String, account: Int, year: Int, month: Int, day: Int): BuchungssatzDO =
    BuchungssatzDO().also {
      it.beleg = beleg
      it.konto = KontoDO().also { konto -> konto.nummer = account }
      it.datum = LocalDate.of(year, month, day)
    }
}
