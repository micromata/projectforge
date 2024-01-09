/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
import org.testng.Assert

class ScriptExecutorTest {

  @Test
  fun createValidIdentifierTest() {
    Assert.assertEquals(ScriptExecutor.createValidIdentifier(null), "_null_")
    Assert.assertEquals(ScriptExecutor.createValidIdentifier(""), "_empty_")
    Assert.assertEquals(ScriptExecutor.createValidIdentifier("i"), "i")
    Assert.assertEquals(ScriptExecutor.createValidIdentifier("I"), "i")
    Assert.assertEquals(ScriptExecutor.createValidIdentifier("5"), "_")
    Assert.assertEquals(ScriptExecutor.createValidIdentifier("55"), "_5")
    Assert.assertEquals(ScriptExecutor.createValidIdentifier("Hello world!"), "hello_world_")
  }

  @Test
  fun createScriptExecutorTest() {
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

  class DummyScriptDao : AbstractScriptDao() {
    override fun loadByNameOrId(name: String): ScriptDO? {
      if (name == "33143255") {
        val script = ScriptDO()
        script.name = "33143255"
        script.scriptAsString = "// Do what to do"
        script.id = 33143255
        return script
      } else if (name == "script1") {
        val script = ScriptDO()
        script.name = "script1"
        script.scriptAsString = """#INCLUDE "33143255" // Basisfunctions
// Includes script1 and indirect script 33143255
"""
        script.id = 1
        return script
      } else if (name == "circular") {
        val script = ScriptDO()
        script.name = "circular"
        script.scriptAsString = """#INCLUDE "circular2" // Basisfunctions
// circular1
"""
        script.id = 2
        return script
      } else if (name == "circular2") {
        val script = ScriptDO()
        script.name = "circular2"
        script.scriptAsString = """#INCLUDE "circular" // Basisfunctions
// circular2
"""
        script.id = 3
        return script
      }
      return null
    }
  }

  @Test
  fun includingScriptsTest() {
    val script = ScriptDO()
    script.scriptAsString = SCRIPT1
    ScriptExecutor.setIncludingScripts(script, DummyScriptDao())
    Assertions.assertEquals(1, script.includes!!.size)
    Assertions.assertTrue(script.includes!!.first().scriptAsString!!.contains("// Do what to do"))
    Assertions.assertEquals(1, script.includesRecursive!!.size)
    Assertions.assertTrue(script.includesRecursive!!.first().scriptAsString!!.contains("// Do what to do"))


    script.scriptAsString = """#INCLUDE "script1"
// Includes script1 and indirect script 33143255
"""
    ScriptExecutor.setIncludingScripts(script, DummyScriptDao())
    Assertions.assertEquals(1, script.includes!!.size)
    Assertions.assertTrue(script.includes!!.filter { it.scriptAsString!!.contains("// Includes script1 and indirect script 33143255") }.size == 1)

    Assertions.assertEquals(2, script.includesRecursive!!.size)
    Assertions.assertTrue(script.includesRecursive!!.filter { it.scriptAsString!!.contains("// Do what to do") }.size == 1)
    Assertions.assertTrue(script.includesRecursive!!.filter { it.scriptAsString!!.contains("// Includes script1 and indirect script 33143255") }.size == 1)

    script.scriptAsString = """#INCLUDE "circular"
// Includes circular scripts
"""
    ScriptExecutor.setIncludingScripts(script, DummyScriptDao())
    Assertions.assertEquals(2, script.includesRecursive!!.size)
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
