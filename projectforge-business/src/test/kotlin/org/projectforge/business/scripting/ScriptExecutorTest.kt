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

package org.projectforge.business.scripting

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ScriptExecutorTest {

  @Test
  fun createScriptExecutor() {
    val script = ScriptDO()
    script.scriptAsString = SCRIPT1
    Assertions.assertTrue(ScriptExecutor.createScriptExecutor(script) is KotlinScriptExecutor)
    script.scriptAsString = SCRIPT2
    Assertions.assertTrue(ScriptExecutor.createScriptExecutor(script) is KotlinScriptExecutor)
    script.scriptAsString = SCRIPT3
    Assertions.assertTrue(ScriptExecutor.createScriptExecutor(script) is KotlinScriptExecutor)
    script.scriptAsString = SCRIPT4
    Assertions.assertTrue(ScriptExecutor.createScriptExecutor(script) is GroovyScriptExecutor)
  }

  val SCRIPT1 = """#INCLUDE "33143255" // Basisfunctions

// Pauschalstundensatz MA (für Monate ohne Buchungssätze)
val stundensatz = BigDecimal(47.29) // Wird mit Zeitbuchungen zu Projektkosten berechnet.

val filter = BuchungssatzFilter()
filter.setFrom(vonGJ.year, vonGJ.monthValue)
filter.setTo(bisKJ.year, bisKJ.monthValue)
/"""

  val SCRIPT2 = """#INCLUDE "33143255" // Basisfunctions

// Pauschalstundensatz MA (für Monate ohne Buchungssätze)
  var stundensatz = BigDecimal(47.29) // Wird mit Zeitbuchungen zu Projektkosten berechnet.

  var filter = BuchungssatzFilter()
filter.setFrom(vonGJ.year, vonGJ.monthValue)
filter.setTo(bisKJ.year, bisKJ.monthValue)
/"""

  val SCRIPT3 = """#INCLUDE "33143255" // Basisfunctions
  fun test() {
  
/"""

  val SCRIPT4 = """#INCLUDE "33143255" // Basisfunctions

// Pauschalstundensatz MA (für Monate ohne Buchungssätze)
BigDecimal stundensatz = BigDecimal(47.29) // Wird mit Zeitbuchungen zu Projektkosten berechnet.

BuchungssatzFilter filter = BuchungssatzFilter()
filter.setFrom(vonGJ.year, vonGJ.monthValue)
filter.setTo(bisKJ.year, bisKJ.monthValue)
/"""
}
