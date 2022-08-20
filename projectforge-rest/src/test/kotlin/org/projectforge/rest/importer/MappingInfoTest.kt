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

package org.projectforge.rest.importer

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.rest.importer.MappingInfoEntryTest.Companion.checkMapping
import org.projectforge.rest.importer.MappingInfoEntryTest.Companion.checkValues
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month

class MappingInfoTest {
  @Test
  fun mappingSetValuesTest() {
    checkValues("", emptyArray(), emptyArray())
    checkValues("alias ", arrayOf("alias"), emptyArray())
    checkValues(":dd.MM.yyyy | | ", emptyArray(), arrayOf("dd.MM.yyyy"))
    checkValues(":dd.MM.yyyy | alias| :dd.MM.yy|alias2 ", arrayOf("alias", "alias2"), arrayOf("dd.MM.yyyy", "dd.MM.yy"))
  }

  @Test
  fun parseMappingInfoTest() {
    val mappingString = """
date=buchungstag|:dd.MM.yyyy|:dd.MM.yy
valueDate=valuta*|:dd.MM.yyyy|:dd.MM.yy
amount=betrag*|:#.##0,0#|:#0,0#
type=buchungstext*
debteeId=gläub*|glaeu*
# Empty line and comment

subject=verwendung*
mandateReference=Mandat*
customerReference=Kundenref*
collectionReference=sammler*
receiverSender=*beguen*|*zahlungspflicht*
iban=*iban*
bic=*bic*|*swift*
currency=waehrung|währung
info=info
    """.trimMargin()
    val info = MappingInfo.parseMappingInfo(mappingString)
    Assertions.assertEquals(14, info.entries.size)
    val dateMapping = info.entries.find { it.property == "date" }!!
    val amountMapping = info.entries.find { it.property == "amount" }!!
    checkMapping(dateMapping, arrayOf("buchungstag"), arrayOf("dd.MM.yyyy", "dd.MM.yy"))
    checkMapping(amountMapping, arrayOf("betrag*"), arrayOf("#.##0,0#", "#0,0#"))
    Assertions.assertEquals(LocalDate.of(2000, Month.MAY, 18), dateMapping.parseLocalDate("18.05.2000"))
    Assertions.assertEquals(LocalDate.of(2000, Month.MAY, 18), dateMapping.parseLocalDate("18.05.00"))
    Assertions.assertEquals(BigDecimal("1000.12"), amountMapping.parseBigDecimal("1.000,12"))
    Assertions.assertEquals(BigDecimal("1000.12"), amountMapping.parseBigDecimal("1000,12"))
    Assertions.assertEquals(BigDecimal("1000"), amountMapping.parseBigDecimal("1000"))
    Assertions.assertEquals(BigDecimal("1000.2"), amountMapping.parseBigDecimal("1000,2"))
  }
}
