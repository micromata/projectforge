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

package org.projectforge.setup.wizard

import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * The flames of the ProjectForge logo for a terminal: pixels in half blocks (`▀`, `▄`), the upper pixel as foreground
 * and the lower as background color. The pixels are rendered from the logo master by `dev-docs/logo/render-logos.py`
 * (`setup/terminal-flames.txt`): one line per two pixel rows, one cell per column as `rrggbb/rrggbb` (upper/lower pixel),
 * `-` for a transparent pixel.
 */
object TerminalLogo {
  private const val RESOURCE = "/setup/terminal-flames.txt"

  /** A cell of two pixels as 0xRRGGBB, null if transparent. */
  class Cell(val upper: Int?, val lower: Int?)

  private val flames: List<List<Cell>>? by lazy {
    try {
      TerminalLogo::class.java.getResourceAsStream(RESOURCE)?.bufferedReader(Charsets.UTF_8)?.use { parse(it.readText()) }
    } catch (ex: Exception) {
      log.warn { "Can't read the flames of the logo: ${ex.message}" }
      null
    }
  }

  /** @return null if the terminal can't show them (no ANSI, no unicode, no colors) or there are none. */
  fun flames(terminal: TerminalInfo): List<String>? {
    val grid = flames ?: return null
    return render(grid, terminal)
  }

  fun parse(text: String): List<List<Cell>> =
    text.lines().filter { it.isNotBlank() }.map { line ->
      line.trim().split(Regex("\\s+")).map { cell ->
        val parts = cell.split('/')
        Cell(pixel(parts.getOrNull(0)), pixel(parts.getOrNull(1)))
      }
    }

  private fun pixel(text: String?): Int? = text?.takeIf { it.length == 6 }?.toIntOrNull(16)

  fun render(grid: List<List<Cell>>, terminal: TerminalInfo): List<String>? {
    if (!terminal.ansi || !terminal.unicode || terminal.colors == 0) {
      return null
    }
    return grid.map { row -> row.joinToString("") { cell(it, terminal) } }
  }

  private fun cell(cell: Cell, terminal: TerminalInfo): String {
    val esc = TerminalInfo.ESC
    return when {
      cell.upper == null && cell.lower == null -> " "
      cell.lower == null -> "$esc[${fg(cell.upper!!, terminal)}m▀$esc[0m"
      cell.upper == null -> "$esc[${fg(cell.lower, terminal)}m▄$esc[0m"
      else -> "$esc[${fg(cell.upper, terminal)};${bg(cell.lower, terminal)}m▀$esc[0m"
    }
  }

  private fun fg(rgb: Int, terminal: TerminalInfo): String = when {
    terminal.trueColor -> "38;2;${rgb shr 16 and 255};${rgb shr 8 and 255};${rgb and 255}"
    terminal.colors >= 256 -> "38;5;${index256(rgb)}"
    else -> "${30 + index8(rgb)}"
  }

  private fun bg(rgb: Int, terminal: TerminalInfo): String = when {
    terminal.trueColor -> "48;2;${rgb shr 16 and 255};${rgb shr 8 and 255};${rgb and 255}"
    terminal.colors >= 256 -> "48;5;${index256(rgb)}"
    else -> "${40 + index8(rgb)}"
  }

  /** The nearest color of the 6x6x6 cube of the 256 colors. */
  fun index256(rgb: Int): Int {
    fun level(value: Int) = (value * 5 + 127) / 255
    return 16 + 36 * level(rgb shr 16 and 255) + 6 * level(rgb shr 8 and 255) + level(rgb and 255)
  }

  /** Red or yellow (orange flames), the 8 colors have nothing in between: 1 = red, 3 = yellow. */
  fun index8(rgb: Int): Int {
    val r = rgb shr 16 and 255
    val g = rgb shr 8 and 255
    return if (g * 100 < r * 45) 1 else 3
  }
}
