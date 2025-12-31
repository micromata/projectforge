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

package org.projectforge.core

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.business.fibu.EingangsrechnungDao
import org.projectforge.business.fibu.EingangsrechnungsPositionDO
import org.projectforge.business.fibu.PaymentType
import org.projectforge.business.orga.ContractDO
import org.projectforge.business.orga.ContractDao
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month

class BaseDOTest : AbstractTestBase() {
  @Autowired
  private lateinit var contractDao: ContractDao

  @Autowired
  private lateinit var eingangsrechnungDao: EingangsrechnungDao

  @Test
  fun modificationStatusTest() {
    val contractA = ContractDO()
    contractA.title = "Test contract"
    val contractB = ContractDO()
    contractB.title = "Test contract"
    Assertions.assertEquals(EntityCopyStatus.NONE, contractA.copyValuesFrom(contractB))
    contractB.title = "Changed title"
    Assertions.assertEquals(EntityCopyStatus.MAJOR, contractA.copyValuesFrom(contractB))

    logon(TEST_FINANCE_USER)
    contractDao.insert(contractA)
    Assertions.assertEquals(EntityCopyStatus.NONE, contractDao.update(contractA))
    contractA.title = "Something new"
    Assertions.assertEquals(EntityCopyStatus.MAJOR, contractDao.update(contractA))

    val invoice = EingangsrechnungDO()
    invoice.datum = LocalDate.of(2022, Month.FEBRUARY, 17)
    invoice.betreff = "subject"
    invoice.bemerkung = "comment"
    invoice.faelligkeit = invoice.datum!!.plusDays(30)
    invoice.bezahlDatum = invoice.datum!!.plusDays(7)
    invoice.zahlBetrag = BigDecimal("1.96")
    // invoice.konto
    invoice.discountPercent = BigDecimal("2.00")
    invoice.discountMaturity = invoice.datum!!.plusDays(7)
    invoice.uiStatusAsXml = "<rechnungUIStatus><closedPositions/></rechnungUIStatus>"
    invoice.receiver = "ACME"
    invoice.iban = "DE1234567890"
    invoice.bic = "ABCDEFGHIJ"
    invoice.referenz = "AB2214"
    invoice.kreditor = "ACME"
    invoice.paymentType = PaymentType.BANK_TRANSFER
    val pos = EingangsrechnungsPositionDO()
    pos.number = 1
    pos.menge = BigDecimal.ONE
    pos.vat = BigDecimal.ZERO
    pos.einzelNetto = BigDecimal("2.00")
    pos.eingangsrechnung = invoice
    invoice.ensureAndGetPositionen()
    invoice.positionen!!.add(pos)
    eingangsrechnungDao.insert(invoice)
    // Doesn't work due to zahlungsZiel: Assertions.assertEquals(ModificationStatus.NONE, eingangsrechnungDao.update(invoice))
  }
}
