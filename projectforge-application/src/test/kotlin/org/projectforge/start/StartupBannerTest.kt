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

package org.projectforge.start

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.setup.wizard.TerminalInfo

class StartupBannerTest {
  private val ansi = TerminalInfo(ansi = true, colors = 8, unicode = true, width = 100, height = 40)
  private val url = "http://localhost:8080"

  private fun strip(text: String) = text.replace(Regex("\u001B\\[[0-9;]*m"), "")

  @Test
  fun `plain text has the address between two lines and no escape sequences`() {
    val lines = StartupBanner.lines(TerminalInfo.PLAIN, "9.2.14", url, url, firstStart = false)
    assertTrue(lines.first().all { it == '=' })
    assertEquals(lines.first(), lines.last())
    assertTrue(lines.any { it.trim() == url })
    assertTrue(lines.any { it.contains("ProjectForge 9.2.14 is running") })
    assertTrue(lines.none { it.contains("\u001B") })
    assertTrue(lines.none { it.contains("Domain:") }, "The domain equals the address.")
  }

  @Test
  fun `with ansi the frame is green and the lines have the same width`() {
    val lines = StartupBanner.lines(ansi, "9.2.14", url, null, firstStart = false)
    assertTrue(lines.first().startsWith("\u001B[32m╭"))
    assertTrue(lines.all { it.endsWith("\u001B[0m") })
    assertEquals(1, lines.map { strip(it).length }.toSet().size, "A rectangle.")
    assertTrue(lines.any { it.contains("\u001B[1m$url\u001B[0m") }, "The address is bold.")
  }

  @Test
  fun `without unicode the frame is ascii`() {
    val lines = StartupBanner.lines(
      TerminalInfo(ansi = true, colors = 8, unicode = false, width = 100, height = 40),
      "9.2.14", url, null, firstStart = false,
    )
    assertTrue(strip(lines.first()).startsWith("+-"))
    assertTrue(lines.none { strip(it).any { ch -> ch.code > 127 } })
  }

  @Test
  fun `a different domain and the first start are shown`() {
    val lines = StartupBanner.lines(TerminalInfo.PLAIN, "9.2.14", url, "https://pf.acme.com/", firstStart = true)
    assertTrue(lines.any { it.contains("Domain: https://pf.acme.com") })
    assertTrue(lines.any { it.contains("First start: open this address to finish the setup.") })
  }

  @Test
  fun `the banner waits until the log is quiet, but not forever`() {
    // Fake clock: every sleep advances 100 ms; a log event at 0, 400 and 900 ms.
    var now = 1000L
    val logs = mutableListOf(1000L, 1400L, 1900L)
    var slept = 0L
    StartupBanner.waitUntilQuiet(
      lastLogMillis = { logs.filter { it <= now }.max() },
      nowMillis = { now },
      sleep = { now += it; slept += it },
      quietMillis = 1500,
      maxWaitMillis = 20_000,
    )
    assertEquals(1900L + 1500L, now, "Returns 1.5 s after the last log event.")

    now = 0L
    slept = 0L
    var chatty = 0L
    StartupBanner.waitUntilQuiet(
      lastLogMillis = { chatty = now; chatty }, // Always just logged.
      nowMillis = { now },
      sleep = { now += it; slept += it },
      quietMillis = 1500,
      maxWaitMillis = 5000,
    )
    assertEquals(5000L, slept, "Gives up after the maximum.")
  }
}
