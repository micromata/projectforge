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

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * What the terminal of the setup wizard can do: ANSI escape sequences (colors, clearing the screen), unicode, its size.
 * Without ANSI ([PLAIN]) the wizard writes plain text, as for a pipe, a log or the run window of an IDE.
 */
class TerminalInfo(
  val ansi: Boolean,
  /** 0 (none), 8 or 256. */
  val colors: Int,
  val unicode: Boolean,
  val width: Int,
  val height: Int,
  /** 24 bit colors (`COLORTERM=truecolor`). */
  val trueColor: Boolean = false,
) {
  /** [text] in the given SGR codes (see [BOLD] etc.), always reset at its end; unchanged without ANSI. */
  fun style(text: String, vararg codes: Int): String =
    if (!ansi || colors == 0 || codes.isEmpty()) text else "$ESC[${codes.joinToString(";")}m$text$ESC[0m"

  /** Clears the screen and puts the cursor to the top left; nothing without ANSI. */
  fun clearScreen(): String = if (ansi) "$ESC[2J$ESC[H" else ""

  companion object {
    const val ESC = "\u001B"
    const val BOLD = 1
    const val DIM = 2
    const val RED = 31
    const val GREEN = 32
    const val YELLOW = 33
    const val CYAN = 36

    /** The property and the environment variable of the switch: `ansi`, `plain` or `auto` (default). */
    const val PROPERTY = "projectforge.setup.terminal"
    const val ENV = "PF_SETUP_TERMINAL"

    val PLAIN = TerminalInfo(ansi = false, colors = 0, unicode = false, width = 80, height = 24)

    /**
     * @param hasConsole Java knows a console only if stdin and stdout are a terminal. Under Gradle (`bootRun`) they
     * aren't, so the switch [PROPERTY] is the way to get colors there (e. g. `-Dprojectforge.setup.terminal=ansi`).
     */
    fun detect(
      env: Map<String, String> = System.getenv(),
      property: (String) -> String? = { System.getProperty(it) },
      hasConsole: Boolean = System.console() != null,
      sizeProvider: () -> Pair<Int, Int>? = ::queryTerminalSize,
    ): TerminalInfo {
      val mode = (property(PROPERTY) ?: env[ENV] ?: "auto").trim().lowercase()
      val term = env["TERM"]?.lowercase() ?: ""
      val windows = property("os.name")?.startsWith("Windows", ignoreCase = true) == true
      val ansi = when (mode) {
        "plain" -> false
        "ansi" -> true
        else -> hasConsole && env["NO_COLOR"].isNullOrEmpty() && term != "dumb" &&
            (windows && (env.containsKey("WT_SESSION") || env.containsKey("ANSICON") || env.containsKey("TERM_PROGRAM"))
                || !windows && term.isNotEmpty())
      }
      if (!ansi) {
        return PLAIN
      }
      val colorTerm = env["COLORTERM"]?.lowercase() ?: ""
      val trueColor = colorTerm == "truecolor" || colorTerm == "24bit"
      val colors = if (trueColor || term.contains("256color")) 256 else 8
      val locale = listOf("LC_ALL", "LC_CTYPE", "LANG").mapNotNull { env[it] }.firstOrNull { it.isNotEmpty() } ?: ""
      val encoding = (property("stdout.encoding") ?: property("file.encoding") ?: "").uppercase()
      val unicode = listOf(locale.uppercase(), encoding).any { it.contains("UTF-8") || it.contains("UTF8") }
      val size = sizeProvider()
        ?: Pair(env["COLUMNS"]?.toIntOrNull() ?: 80, env["LINES"]?.toIntOrNull() ?: 24)
      return TerminalInfo(true, colors, unicode, size.first.coerceAtLeast(20), size.second.coerceAtLeast(5), trueColor)
    }

    /** The size (columns, rows) of the controlling terminal by `stty`, null if unknown (e. g. on Windows). */
    fun queryTerminalSize(): Pair<Int, Int>? {
      return try {
        val process = ProcessBuilder("sh", "-c", "stty size < /dev/tty")
          .redirectErrorStream(true)
          .redirectInput(ProcessBuilder.Redirect.from(File("/dev/null")))
          .start()
        if (!process.waitFor(1, TimeUnit.SECONDS)) {
          process.destroyForcibly()
          return null
        }
        val parts = process.inputStream.bufferedReader().readText().trim().split(' ')
        val rows = parts.getOrNull(0)?.toIntOrNull()
        val columns = parts.getOrNull(1)?.toIntOrNull()
        if (rows != null && columns != null && rows > 0 && columns > 0) Pair(columns, rows) else null
      } catch (ex: Exception) {
        null
      }
    }
  }
}
