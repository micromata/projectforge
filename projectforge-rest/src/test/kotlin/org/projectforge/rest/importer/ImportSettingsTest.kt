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
import org.projectforge.business.fibu.PaymentScheduleDO
import org.projectforge.business.task.TaskDO
import org.projectforge.rest.importer.ImportFieldSettingsTest.Companion.checkFieldSettings
import org.projectforge.rest.importer.ImportFieldSettingsTest.Companion.checkValues
import org.projectforge.test.AbstractTestBase
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.Month

class ImportSettingsTest: AbstractTestBase() { // AbstractTestBase needed for getting configured date-formats.
  @Test
  fun parseFieldSettingsTest() {
    checkValues("", emptyArray(), emptyArray())
    checkValues("alias ", arrayOf("alias"), emptyArray())
    checkValues(":dd.MM.yyyy | | ", emptyArray(), arrayOf("dd.MM.yyyy"))
    checkValues(":dd.MM.yyyy | alias| :dd.MM.yy|alias2 ", arrayOf("alias", "alias2"), arrayOf("dd.MM.yyyy", "dd.MM.yy"))

    var settings = ImportSettings().parseSettings("", PaymentScheduleDO::class.java)
    Assertions.assertEquals(
      "Date|:MM/dd/yyyy|:MM/dd/yy",
      settings.getFieldSettings("scheduleDate")!!.getSettingsAsString(true),
    )
    Assertions.assertEquals(
      "Amount|:#,##0.0#|:#0.0#",
      settings.getFieldSettings("amount")!!.getSettingsAsString(true),
    )
    settings = ImportSettings().parseSettings("", TaskDO::class.java)
    Assertions.assertEquals(
      "Maximum hours|:#,##0|:#0",
      settings.getFieldSettings("maxHours")!!.getSettingsAsString(true),
    )
  }

  @Test
  fun parseSettingsTest() {
    val settingsString = """
encoding=iso-8859-1
date=buchungstag|:dd.MM.yyyy|:dd.MM.yy
amount=betrag*|:#.##0,0#|:#0,0#
type=buchungstext*
debteeId=gläub*|glaeu*
# Empty line and comment

subject=verwendung*
    """.trimMargin()
    val settings = ImportSettings()
    settings.parseSettings(settingsString)
    Assertions.assertEquals(5, settings.fieldSettings.size)
    val dateMapping = settings.fieldSettings.find { it.property == "date" }!!
    val amountMapping = settings.fieldSettings.find { it.property == "amount" }!!
    checkFieldSettings(dateMapping, arrayOf("buchungstag"), arrayOf("dd.MM.yyyy", "dd.MM.yy"))
    checkFieldSettings(amountMapping, arrayOf("betrag*"), arrayOf("#.##0,0#", "#0,0#"))
    Assertions.assertEquals("iso-8859-1", settings.encoding)
    Assertions.assertEquals(StandardCharsets.ISO_8859_1, settings.charSet)
    Assertions.assertEquals(LocalDate.of(2000, Month.MAY, 18), dateMapping.parseLocalDate("18.05.2000"))
    Assertions.assertEquals(LocalDate.of(2000, Month.MAY, 18), dateMapping.parseLocalDate("18.05.00"))
    Assertions.assertEquals(BigDecimal("1000.12"), amountMapping.parseBigDecimal("1.000,12"))
    Assertions.assertEquals(BigDecimal("1000.12"), amountMapping.parseBigDecimal("1000,12"))
    Assertions.assertEquals(BigDecimal("1000"), amountMapping.parseBigDecimal("1000"))
    Assertions.assertEquals(BigDecimal("1000.2"), amountMapping.parseBigDecimal("1000,2"))
  }
}
