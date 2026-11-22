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

package org.projectforge.setup

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.setup.wizard.SystemSetupConsole
import java.io.ByteArrayInputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

class SystemSetupConsoleTest {
  private fun console(input: String) = SystemSetupConsole(ByteArrayInputStream(input.toByteArray()))

  @Test
  fun `the case of an answer is kept and surrounding blanks are removed`() {
    val console = console("  /Home/Kai/ProjectForge \nhttps://Acme.com/PF\n")
    assertEquals("/Home/Kai/ProjectForge", console.ask("Directory", "x"))
    assertEquals("https://Acme.com/PF", console.ask("Domain", "x"))
  }

  @Test
  fun `successive questions read successive lines of one input`() {
    val console = console("1\n2\n3\n")
    assertEquals(listOf("1", "2", "3"), List(3) { console.ask("Question", "x") })
  }

  @Test
  fun `an empty line and a closed input result in the default`() {
    val console = console("\n")
    assertEquals("def", console.ask("Question", "def"))
    assertEquals("def", console.ask("Question", "def"), "Closed input.")
    assertEquals("def", console.ask("Question", "def"), "Stays closed.")
    assertTrue(console.askYesNo("Question", true))
    assertFalse(console.askYesNo("Question", false))
  }

  @Test
  fun `yes or no is recognized, anything else is asked again`() {
    val console = console("Y\nno\nmaybe\nyes\n")
    assertTrue(console.askYesNo("Question", false))
    assertFalse(console.askYesNo("Question", true))
    assertTrue(console.askYesNo("Question", false), "maybe is refused, yes follows.")
  }

  @Test
  fun `a timeout is no answer, the default applies only for the questions with a default`() {
    val pipe = PipedOutputStream()
    val console = SystemSetupConsole(PipedInputStream(pipe))
    assertNull(console.readAnswer("Question", 1))
    assertNull(console.askYesNoOrNull("Question", true, 1))
    pipe.write("late\n".toByteArray())
    pipe.flush()
    assertEquals("late", console.readAnswer("Question", 5), "The input still works after a timeout.")
  }

  @Test
  fun `a closed input is reported once`() {
    val out = java.io.ByteArrayOutputStream()
    val saved = System.out
    System.setOut(java.io.PrintStream(out, true))
    try {
      val console = console("")
      repeat(3) { console.ask("Question", "x") }
    } finally {
      System.setOut(saved)
    }
    assertEquals(1, Regex("No console input available").findAll(out.toString()).count())
  }

  @Test
  fun `a secret and the questions around it read successive lines`() {
    val console = console("first\nsecret\nlast\n")
    assertEquals("first", console.ask("Question", "x"))
    assertEquals("secret", console.askSecret("Password"))
    assertEquals("last", console.ask("Question", "x"))
  }
}
