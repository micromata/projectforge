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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.projectforge.setup.wizard.ConsoleSetupWizard
import org.projectforge.setup.wizard.SetupConsole
import org.projectforge.setup.wizard.TerminalInfo
import org.projectforge.setup.wizard.Texts
import java.io.File

class ConsoleSetupWizardTest {
  @TempDir
  lateinit var dir: File

  /** Answers the questions in the given order; "" accepts the default. */
  private class ScriptedConsole(
    answers: List<String>,
    override val terminal: TerminalInfo = TerminalInfo.PLAIN,
  ) : SetupConsole {
    private val remaining = answers.toMutableList()
    val questions = mutableListOf<String>()
    val output = mutableListOf<String>()
    var clears = 0

    override fun clear() {
      clears++
      output.add("<clear>")
    }

    override fun println(text: String) {
      output.add(text)
    }

    override fun readAnswer(question: String, timeoutSeconds: Long): String {
      questions.add(question)
      return if (remaining.isEmpty()) "" else remaining.removeAt(0)
    }

    override fun askSecret(question: String): String {
      questions.add(question)
      return if (remaining.isEmpty()) "" else remaining.removeAt(0)
    }
  }

  private fun setupContext(docker: SetupContext.DockerMode? = null, embedded: Boolean = true) =
    SetupContext().also {
      it.dockerMode = docker
      it.embeddedDatabaseSupported = embedded
    }

  private fun run(
    answers: List<String>,
    context: SetupContext = setupContext(),
    preset: File? = File(dir, "ProjectForge"),
    connectionTest: (String, String, String) -> String? = { _, _, _ -> Texts.JDBC_TESTRESULT_OK },
    terminal: TerminalInfo = TerminalInfo.PLAIN,
  ): Pair<org.projectforge.setup.SetupData?, ScriptedConsole> {
    val console = ScriptedConsole(answers, terminal)
    return Pair(ConsoleSetupWizard(preset, context, console, connectionTest).run(), console)
  }

  @Test
  fun `return on the main page saves the defaults with the embedded database`() {
    val (data, console) = run(emptyList())
    assertNotNull(data)
    assertEquals(File(dir, "ProjectForge").canonicalFile, data!!.applicationHomeDir!!.canonicalFile)
    assertEquals(8080, data.serverPort)
    assertEquals("http://localhost:8080", data.domain)
    assertTrue(data.useEmbeddedDatabase)
    assertFalse(data.developmentMode)
    assertTrue(data.startServer)
    assertTrue(console.output.any { it.contains("Home directory") && it.contains("[h]") })
  }

  @Test
  fun `the main page lists the values and an entry is chosen by number or letter`() {
    // 2 = port by number, p = port by letter (upper case), then save.
    val (data, console) = run(listOf("2", "9090", "P", "9191", "S"))
    assertEquals(9191, data!!.serverPort)
    assertEquals(3, console.output.count { it.contains("Port") && it.contains("[p]") })
  }

  @Test
  fun `the logo is shown once, not again with every return to the main page`() {
    val (_, console) = run(listOf("p", "9090", "c", "y", "b", "b", "s"))
    assertEquals(1, console.output.count { it.contains("|_|   |_|  \\___// |") })
    assertEquals(4, console.output.count { it.startsWith("ProjectForge setup") }, "The main page itself is shown each time.")
  }

  @Test
  fun `the domain follows the port until it was entered`() {
    assertEquals("http://localhost:9090", run(listOf("p", "9090", "s")).first!!.domain)
    val (data, _) = run(listOf("d", "https://pf.example.org/pf/", "p", "9090", "s"))
    assertEquals("https://pf.example.org/pf", data!!.domain)
    assertEquals(9090, data.serverPort)
  }

  @Test
  fun `invalid values are asked again and keep the old value on return`() {
    val (data, console) = run(listOf("p", "99999", "abc", "9090", "d", "ftp://x", "", "s"))
    assertEquals(9090, data!!.serverPort)
    assertEquals("http://localhost:9090", data.domain)
    assertTrue(console.output.any { it.contains("between 1 and 65535") })
    assertTrue(console.output.any { it.contains("http://") })
  }

  @Test
  fun `the source code repository is refused as home directory`() {
    val repo = File(dir, "repo")
    listOf("projectforge-application", "projectforge-business", "projectforge-common").forEach { File(repo, it).mkdirs() }
    val good = File(dir, "good")
    val (data, console) = run(listOf("h", repo.path, good.path, "s"))
    assertEquals(good.canonicalFile, data!!.applicationHomeDir!!.canonicalFile)
    assertTrue(console.output.any { it.contains("source code repository") })
  }

  @Test
  fun `the development mode (CORS filter) is toggled`() {
    assertTrue(run(listOf("c", "y", "s")).first!!.developmentMode)
    assertFalse(run(listOf("c", "n", "s")).first!!.developmentMode)
    val (_, console) = run(listOf("s"))
    assertTrue(console.output.any { it.contains("Development mode") && it.contains("off (CORS filter)") && it.contains("[c]") })
  }

  @Test
  fun `an unknown choice is explained`() {
    val (data, console) = run(listOf("x", "9", "s"))
    assertNotNull(data)
    assertEquals(2, console.output.count { it.contains("Please choose") })
  }

  @Test
  fun `quit needs a confirmation`() {
    assertNull(run(listOf("q", "y")).first)
    assertNotNull(run(listOf("q", "n", "s")).first)
    assertNotNull(run(listOf("q", "", "s")).first)
  }

  @Test
  fun `PostgreSQL is configured in its menu, tested and saved`() {
    var tested: Triple<String, String, String>? = null
    var tests = 0
    val (data, console) = run(
      listOf(
        "b", "2", // database menu, PostgreSQL
        "1", "jdbc:postgresql://db:5432/pf", "2", "pfuser", "3", "secret", "t", "b", // postgres menu
        "s",
      ),
      connectionTest = { url, user, password ->
        tested = Triple(url, user, password)
        tests++
        Texts.JDBC_TESTRESULT_OK
      },
    )
    assertFalse(data!!.useEmbeddedDatabase)
    assertEquals("jdbc:postgresql://db:5432/pf", data.jdbcSettings!!.jdbcUrl)
    assertEquals("pfuser", data.jdbcSettings!!.user)
    assertEquals("secret", data.jdbcSettings!!.password)
    assertEquals("org.postgresql.Driver", data.jdbcSettings!!.driverClass)
    assertEquals(Triple("jdbc:postgresql://db:5432/pf", "pfuser", "secret"), tested)
    assertEquals(4, tests, "After each of the three values and on demand.")
    assertTrue(console.output.any { it.contains("Connection test: ${Texts.JDBC_TESTRESULT_OK}") })
    assertTrue(console.output.any { it.contains("password set") && it.contains("test: ${Texts.JDBC_TESTRESULT_OK}") })
    assertTrue(console.questions.none { it.contains("Save anyway") })
  }

  @Test
  fun `saving PostgreSQL without a successful test asks once`() {
    val (data, console) = run(listOf("b", "2", "b", "s", "n", "s", "y"))
    // Back out of the postgres menu with "b", save -> warning, "n" -> main page, save again -> warning, "y".
    assertFalse(data!!.useEmbeddedDatabase)
    assertEquals(2, console.questions.count { it.contains("Save anyway") })
  }

  @Test
  fun `the test runs after a changed value and its status is shown below the parameters`() {
    var result = "Connection failed: nope"
    val (_, console) = run(
      listOf("b", "2", "t", "2", "other", "b", "s", "y"),
      connectionTest = { _, _, _ -> result.also { result = Texts.JDBC_TESTRESULT_OK } },
    )
    val status = console.output.filter { it.startsWith("Connection test:") }
    // Menu after entering (not done), after "t" (failed), after the user change (the second run succeeded).
    assertEquals(
      listOf("Connection test: not done", "Connection test: Connection failed: nope", "Connection test: ${Texts.JDBC_TESTRESULT_OK}"),
      status,
    )
    val lines = console.output
    assertTrue(lines.indexOf("  b  Back") < lines.indexOf("Connection test: not done"), "Status below the entries.")
  }

  @Test
  fun `the embedded database is not offered if PostgreSQL is forced`() {
    val (data, console) = run(listOf("b", "1", "b", "b", "s", "y"), setupContext(embedded = false))
    assertFalse(data!!.useEmbeddedDatabase)
    assertTrue(console.output.none { it.contains("Embedded (built-in") })
    assertTrue(console.output.any { it.contains("Please choose") })
  }

  @Test
  fun `docker container uses the preset directory and offers neither home directory, port nor development mode`() {
    val preset = File(dir, "docker-home")
    val (data, console) = run(emptyList(), setupContext(SetupContext.DockerMode.SINGLE), preset)
    assertEquals(preset, data!!.applicationHomeDir)
    assertEquals("0.0.0.0", data.serverAdress)
    assertTrue(data.startServer)
    assertTrue(console.output.none { it.contains("Home directory") || it.contains("Development mode") || it.contains("[p]") })
    assertTrue(console.output.any { it.contains("Domain") })
  }

  @Test
  fun `docker compose stack does not start the server after the setup`() {
    val (data, _) = run(listOf("b", "2", "b", "s", "y"), setupContext(SetupContext.DockerMode.STACK, embedded = false))
    assertFalse(data!!.startServer)
  }

  private val ansiTerminal = TerminalInfo(ansi = true, colors = 8, unicode = true, width = 100, height = 40)

  @Test
  fun `with ansi every page clears the screen and shows the logo, colored and reset`() {
    val (_, console) = run(listOf("p", "9090", "b", "b", "s"), terminal = ansiTerminal)
    // Main page, main page (after the port), main page (after b/b), database menu: 4 pages in total.
    assertEquals(console.clears, console.output.count { it == "<clear>" })
    assertTrue(console.clears >= 4)
    assertEquals(console.clears, console.output.count { it.contains("|_|   |_|  \\___// |") })
    val colored = console.output.filter { it.contains("\u001B[") }
    assertTrue(colored.isNotEmpty())
    assertTrue(colored.all { it.endsWith("\u001B[0m") || it.contains("\u001B[0m") }, "Every style is reset.")
    assertTrue(console.output.any { it.contains("\u001B[1mHome directory") }, "Labels are bold.")
  }

  @Test
  fun `with ansi a hint stays visible after the screen was cleared`() {
    val (_, console) = run(listOf("x", "s"), terminal = ansiTerminal)
    val lastClear = console.output.lastIndexOf("<clear>")
    val hint = console.output.indexOfFirst { it.contains("Please choose") }
    assertTrue(hint > lastClear - 1 && hint >= 0, "The hint follows the page it belongs to.")
    assertTrue(console.output.subList(0, hint).count { it == "<clear>" } == 2)
  }

  @Test
  fun `a small terminal gets a one line header instead of the logo`() {
    val small = TerminalInfo(ansi = true, colors = 8, unicode = false, width = 50, height = 20)
    val (_, console) = run(listOf("s"), terminal = small)
    assertTrue(console.output.none { it.contains("|_|   |_|") })
    assertTrue(console.output.any { it.contains("ProjectForge setup") })
  }

  @Test
  fun `plain text has neither escape sequences nor clearing`() {
    val (_, console) = run(listOf("x", "b", "2", "t", "b", "s", "y"))
    assertEquals(0, console.clears)
    assertTrue(console.output.none { it.contains("\u001B") })
  }

  @Test
  fun `the default home directory is not a sub directory of the source code repository`() {
    val repo = File(dir, "repo")
    listOf("projectforge-application", "projectforge-business", "projectforge-common").forEach { File(repo, it).mkdirs() }
    val (data, _) = run(emptyList(), preset = File(repo, "ProjectForge"))
    assertEquals(File(System.getProperty("user.home"), "ProjectForge").canonicalFile, data!!.applicationHomeDir!!.canonicalFile)
  }

  @Test
  fun `the database line is short and its status stands below it`() {
    val (_, console) = run(listOf("b", "2", "3", "secret", "b", "q", "y"))
    val line = console.output.indexOfLast { it.contains("Database") && it.contains("PostgreSQL jdbc:") }
    assertTrue(line >= 0)
    assertFalse(console.output[line].contains("password"), "Only the url on the line itself.")
    assertTrue(console.output[line + 1].trim().startsWith("user projectforge, password set, connection test:"))
  }

  @Test
  fun `a blank line separates the prompt from the page and the logo is red`() {
    val (_, console) = run(listOf("s"), terminal = ansiTerminal)
    val prompt = console.questions.indexOf("Choice [s]")
    assertTrue(prompt >= 0)
    val footer = console.output.indexOfFirst { it.contains("Save and continue") }
    assertEquals("", console.output[footer + 1], "The page ends with a blank line before the prompt.")
    assertTrue(console.output.any { it.contains("\u001B[31m|_|   |_|") }, "The logo is red.")
  }

  @Test
  fun `a big terminal shows the flames left of the lettering, a medium one only the lettering`() {
    val big = TerminalInfo(ansi = true, colors = 256, unicode = true, width = 100, height = 40, trueColor = true)
    val (_, bigConsole) = run(listOf("s"), terminal = big)
    assertTrue(bigConsole.output.any { it.contains("▀") && it.contains("|_|   |_|") }, "Flames and lettering in one line.")
    val medium = TerminalInfo(ansi = true, colors = 256, unicode = true, width = 70, height = 40)
    val (_, mediumConsole) = run(listOf("s"), terminal = medium)
    assertTrue(mediumConsole.output.none { it.contains("▀") })
    assertTrue(mediumConsole.output.any { it.contains("|_|   |_|") })
  }
}
