/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.banking

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class BankAccountRecordTest {
  @Test
  fun matchScoreTest() {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val record = BankAccountRecord(date = today, amount = BigDecimal.TEN, subject = "abc")
    val dbRecord = createDBRecord(yesterday, BigDecimal.TEN, "abc")
    Assertions.assertEquals(-1, record.matchScore(dbRecord))
    dbRecord.date = today
    Assertions.assertEquals(2, record.matchScore(dbRecord))
    dbRecord.iban = " DE12 3456 7890 1234 56 /)***"
    Assertions.assertEquals(2, record.matchScore(dbRecord))
    record.iban = "de1234567890123456"
    Assertions.assertEquals(3, record.matchScore(dbRecord))
    dbRecord.iban = null
    Assertions.assertEquals(2, record.matchScore(dbRecord))

    dbRecord.amount = BigDecimal("10.9")
    Assertions.assertEquals(1, record.matchScore(dbRecord))
  }

  private fun createDBRecord(
    date: LocalDate?,
    amount: BigDecimal?,
    subject: String?,
    iban: String? = null
  ): BankAccountRecordDO {
    val result = BankAccountRecordDO()
    result.date = date
    result.amount = amount
    result.subject = subject
    result.iban = iban
    return result
  }
}
