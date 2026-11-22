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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.setup.wizard.TerminalInfo

class TerminalInfoTest {
  private fun detect(
    env: Map<String, String> = emptyMap(),
    props: Map<String, String> = emptyMap(),
    hasConsole: Boolean = true,
    size: Pair<Int, Int>? = Pair(100, 40),
  ) = TerminalInfo.detect(env, { props[it] }, hasConsole) { size }

  @Test
  fun `a terminal with TERM is ansi with the size asked from the terminal`() {
    val info = detect(mapOf("TERM" to "xterm", "LANG" to "de_DE.UTF-8"))
    assertTrue(info.ansi)
    assertEquals(8, info.colors)
    assertTrue(info.unicode)
    assertEquals(100, info.width)
    assertEquals(40, info.height)
  }

  @Test
  fun `256 colors are detected by TERM or COLORTERM`() {
    assertEquals(256, detect(mapOf("TERM" to "xterm-256color")).colors)
    assertEquals(256, detect(mapOf("TERM" to "xterm", "COLORTERM" to "truecolor")).colors)
    assertEquals(256, detect(mapOf("TERM" to "xterm", "COLORTERM" to "24bit")).colors)
  }

  @Test
  fun `no terminal, a dumb terminal and NO_COLOR are plain`() {
    assertFalse(detect(mapOf("TERM" to "xterm"), hasConsole = false).ansi)
    assertFalse(detect(mapOf("TERM" to "dumb")).ansi)
    assertFalse(detect(emptyMap()).ansi)
    assertFalse(detect(mapOf("TERM" to "xterm", "NO_COLOR" to "1")).ansi)
    assertEquals(TerminalInfo.PLAIN, detect(mapOf("TERM" to "xterm", "NO_COLOR" to "1")))
  }

  @Test
  fun `the switch overrides the detection, a property wins over the environment`() {
    assertTrue(detect(mapOf("TERM" to "dumb"), props = mapOf(TerminalInfo.PROPERTY to "ansi"), hasConsole = false).ansi)
    assertTrue(detect(mapOf(TerminalInfo.ENV to "ansi"), hasConsole = false).ansi)
    assertFalse(detect(mapOf("TERM" to "xterm", TerminalInfo.ENV to "plain")).ansi)
    assertFalse(detect(mapOf(TerminalInfo.ENV to "ansi"), props = mapOf(TerminalInfo.PROPERTY to "plain")).ansi)
  }

  @Test
  fun `Windows needs a known terminal`() {
    val windows = mapOf("os.name" to "Windows 11")
    assertFalse(detect(mapOf("TERM" to "xterm"), props = windows).ansi)
    assertTrue(detect(mapOf("WT_SESSION" to "x"), props = windows).ansi)
  }

  @Test
  fun `the size falls back to the environment and then to 80x24`() {
    val fromEnv = detect(mapOf("TERM" to "xterm", "COLUMNS" to "120", "LINES" to "50"), size = null)
    assertEquals(120, fromEnv.width)
    assertEquals(50, fromEnv.height)
    val default = detect(mapOf("TERM" to "xterm"), size = null)
    assertEquals(80, default.width)
    assertEquals(24, default.height)
  }

  @Test
  fun `unicode follows the encoding`() {
    assertTrue(detect(mapOf("TERM" to "xterm", "LC_ALL" to "en_US.UTF-8")).unicode)
    assertTrue(detect(mapOf("TERM" to "xterm"), props = mapOf("file.encoding" to "UTF-8")).unicode)
    assertFalse(detect(mapOf("TERM" to "xterm", "LANG" to "C")).unicode)
  }

  @Test
  fun `a style is always reset and absent without ansi`() {
    val ansi = detect(mapOf("TERM" to "xterm"))
    assertEquals("\u001B[1;31mtext\u001B[0m", ansi.style("text", TerminalInfo.BOLD, TerminalInfo.RED))
    assertEquals("\u001B[2J\u001B[H", ansi.clearScreen())
    assertEquals("text", TerminalInfo.PLAIN.style("text", TerminalInfo.RED))
    assertEquals("", TerminalInfo.PLAIN.clearScreen())
  }
}
