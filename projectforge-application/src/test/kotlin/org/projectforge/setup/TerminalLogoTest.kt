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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.setup.wizard.TerminalInfo
import org.projectforge.setup.wizard.TerminalLogo

class TerminalLogoTest {
  private val grid = TerminalLogo.parse("ff8000/0000ff ff0000/- -/00ff00 -/-\n-/- -/- -/- -/-\n")

  private fun terminal(colors: Int, trueColor: Boolean = false, ansi: Boolean = true, unicode: Boolean = true) =
    TerminalInfo(ansi, colors, unicode, 100, 40, trueColor)

  @Test
  fun `the file format is parsed into cells of two pixels`() {
    assertEquals(2, grid.size)
    assertEquals(4, grid[0].size)
    assertEquals(0xff8000, grid[0][0].upper)
    assertEquals(0x0000ff, grid[0][0].lower)
    assertNull(grid[0][1].lower)
    assertNull(grid[0][2].upper)
    assertNull(grid[0][3].upper)
  }

  @Test
  fun `true color uses the 24 bit sequences with half blocks`() {
    val lines = TerminalLogo.render(grid, terminal(256, trueColor = true))!!
    assertEquals(2, lines.size)
    val esc = TerminalInfo.ESC
    assertEquals(
      "$esc[38;2;255;128;0;48;2;0;0;255m▀$esc[0m" + // both pixels
        "$esc[38;2;255;0;0m▀$esc[0m" + // upper only
        "$esc[38;2;0;255;0m▄$esc[0m" + // lower only
        " ", // transparent
      lines[0],
    )
    assertEquals("    ", lines[1])
  }

  @Test
  fun `256 colors use the color cube`() {
    val line = TerminalLogo.render(grid, terminal(256))!![0]
    assertTrue(line.contains("38;5;${TerminalLogo.index256(0xff8000)};48;5;${TerminalLogo.index256(0x0000ff)}m"))
    assertEquals(16 + 36 * 5 + 6 * 3 + 0, TerminalLogo.index256(0xff8000))
    assertEquals(196, TerminalLogo.index256(0xff0000))
  }

  @Test
  fun `8 colors show red and yellow flames`() {
    assertEquals(1, TerminalLogo.index8(0xbf102b)) // dark red
    assertEquals(1, TerminalLogo.index8(0xe51f2b))
    assertEquals(3, TerminalLogo.index8(0xff8b08)) // orange
    assertEquals(3, TerminalLogo.index8(0xffa600))
    val line = TerminalLogo.render(grid, terminal(8))!![0]
    assertTrue(line.contains("[33;43m"), "Orange (33) and, as it has no red at all, blue are yellow (43).")
  }

  @Test
  fun `there are no flames without ansi, unicode or colors`() {
    assertNull(TerminalLogo.render(grid, TerminalInfo.PLAIN))
    assertNull(TerminalLogo.render(grid, terminal(8, unicode = false)))
    assertNull(TerminalLogo.render(grid, terminal(0)))
  }

  @Test
  fun `the rendered flames of the logo have the expected form`() {
    val flames = TerminalLogo.flames(terminal(256, trueColor = true))
    assertNotNull(flames)
    assertEquals(8, flames!!.size)
    flames.forEach { line ->
      val plain = line.replace(Regex("\u001B\\[[0-9;]*m"), "")
      assertEquals(16, plain.length, "16 columns per line")
    }
    assertTrue(flames.any { it.contains("▀") })
  }
}
