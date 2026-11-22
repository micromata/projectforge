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
import org.projectforge.common.CanonicalFileUtils
import org.projectforge.setup.SetupContext
import org.projectforge.setup.SetupData
import org.projectforge.start.ProjectForgeHomeFinder
import java.io.File

private val log = KotlinLogging.logger {}

/**
 * A small menu for the settings that are needed before ProjectForge starts: the home directory (where
 * projectforge.properties is written), the server port and domain, the database and, for developers, the development mode (CORS filter).
 * The main page shows all values; an entry is chosen by its number or letter, changed, and the main page is shown again.
 * Return on the main page saves and continues, so without a console (nobody answers) the defaults are used.
 *
 * Everything else (currency, locale, admin user, ...) is set on the setup page in the browser after the start.
 */
class ConsoleSetupWizard(
  private val presetAppHomeDir: File? = null,
  private val setupContext: SetupContext = SetupContext(),
  private val console: SetupConsole = SystemSetupConsole(),
  private val connectionTest: (jdbcUrl: String, user: String, password: String) -> String? =
    JdbcConnectionTest::testConnection,
) {
  private class State(
    var home: File,
    var port: Int = 8080,
    var domain: String = "http://localhost:8080",
    /** The domain follows the port until it was entered. */
    var domainCustomized: Boolean = false,
    var embedded: Boolean = true,
    var jdbcUrl: String = JdbcConnectionTest.defaultJdbcUrl,
    var jdbcUser: String = "projectforge",
    var jdbcPassword: String = "",
    var lastConnectionTest: String? = null,
    var cors: Boolean = false,
  )

  private class Entry(
    val letter: String,
    val label: String,
    val value: () -> String,
    /** An optional second line below the value. */
    val detail: () -> String? = { null },
    val edit: () -> Unit,
  )

  private val docker = setupContext.runAsDockerContainer

  private val terminal = console.terminal

  /** Shown below the next page (an input loop prints its hints directly), see [notice]. */
  private var pendingNotice: String? = null

  /**
   * The preset of the home finder, unless it lies in the source code repository (started there, the finder suggests a
   * sub directory of the working directory): then the standard directory in the user's home.
   */
  private fun defaultHomeDir(): File {
    val standard = File(System.getProperty("user.home"), "ProjectForge")
    val preset = presetAppHomeDir ?: return standard
    if (docker) return preset
    val inSourceRepository = generateSequence(CanonicalFileUtils.absolute(preset)) { it.parentFile }
      .any { ProjectForgeHomeFinder.isProjectForgeSourceCodeRepository(it) }
    return if (inSourceRepository) standard else preset
  }

  /**
   * @return The user settings or null, if the user quit the wizard.
   */
  fun run(): SetupData? {
    if (!setupContext.embeddedDatabaseSupported) {
      JdbcConnectionTest.defaultJdbcUrl = "jdbc:postgresql://projectforge-db:5432/projectforge"
    }
    val state = State(
      home = defaultHomeDir(),
      embedded = setupContext.embeddedDatabaseSupported,
      jdbcUrl = JdbcConnectionTest.defaultJdbcUrl,
    )
    val entries = buildEntries(state)
    if (!terminal.ansi) {
      console.println()
      LOGO.lines().forEach { console.println(it) }
    }
    while (true) {
      page(null)
      render(state, entries)
      val choice = console.ask("Choice", "s").lowercase()
      val entry = entries.getOrNull((choice.toIntOrNull() ?: 0) - 1) ?: entries.firstOrNull { it.letter == choice }
      when {
        entry != null -> entry.edit()
        choice == "s" -> if (confirmSave(state)) return toSetupData(state)
        choice == "q" -> if (console.askYesNo("Quit without saving?", false)) {
          log.info { "Setup wizard canceled by user." }
          return null
        }

        else -> notice("Please choose one of the numbers or letters shown.")
      }
    }
  }

  private fun buildEntries(state: State): List<Entry> = buildList {
    if (!docker) {
      add(Entry("h", "Home directory", { "${CanonicalFileUtils.absolutePath(state.home)} (${FinalizeScreenSupport.getDirText(state.home)})" }) {
        askHomeDir(state)
      })
      add(Entry("p", "Port", { state.port.toString() }) { askPort(state) })
    }
    add(Entry("d", "Domain", { state.domain }) { askDomain(state) })
    add(Entry("b", "Database", { databaseText(state) }, { databaseDetail(state) }) { databaseMenu(state) })
    if (!docker) {
      add(Entry("c", "Development mode", { if (state.cors) terminal.style("on (CORS filter)", TerminalInfo.YELLOW) else "off (CORS filter)" }) {
        state.cors = console.askYesNo("Enable the development mode (CORS filter, for development only, never on productive systems)?", state.cors)
      })
    }
  }

  /**
   * Starts a page: with ANSI the screen is cleared and the header (the logo, if the terminal is big enough) shown, so
   * the page always starts at the top; plain text just continues.
   */
  private fun page(subtitle: String?) {
    if (!terminal.ansi) {
      return
    }
    console.clear()
    val flames = if (terminal.width >= FLAMES_MIN_WIDTH && terminal.height >= 26) TerminalLogo.flames(terminal) else null
    if (flames != null) {
      // The flames left of the lettering, which is aligned to their bottom.
      val letters = LOGO.lines()
      val offset = flames.size - letters.size
      flames.forEachIndexed { index, row ->
        val text = letters.getOrNull(index - offset)?.let { "  " + terminal.style(it, TerminalInfo.RED) } ?: ""
        console.println(row + text)
      }
      console.println()
    } else if (terminal.width >= 60 && terminal.height >= 24) {
      LOGO.lines().forEach { console.println(terminal.style(it, TerminalInfo.RED)) }
      console.println()
    }
    val title = "ProjectForge setup" + (subtitle?.let { " > $it" } ?: "")
    console.println(terminal.style(title, TerminalInfo.BOLD))
    console.println(terminal.style((if (terminal.unicode) "\u2500" else "-").repeat(minOf(terminal.width, 80)), TerminalInfo.DIM))
  }

  /** A hint for the user after the next page change; plain text prints it at once (nothing is cleared). */
  private fun notice(text: String) {
    if (terminal.ansi) {
      pendingNotice = text
    } else {
      console.println(text)
    }
  }

  private fun showNotice() {
    pendingNotice?.let {
      console.println()
      console.println(terminal.style(it, TerminalInfo.YELLOW))
    }
    pendingNotice = null
  }

  private fun key(text: String) = terminal.style(text, TerminalInfo.YELLOW)

  private fun render(state: State, entries: List<Entry>) {
    console.println()
    if (!terminal.ansi) {
      console.println("ProjectForge setup (Return = save and continue)")
      console.println()
    }
    entries.forEachIndexed { index, entry ->
      console.println(
        "  ${key("${index + 1}")}  ${terminal.style(entry.label.padEnd(LABEL_WIDTH), TerminalInfo.BOLD)} ${entry.value()}  " +
            terminal.style("[${entry.letter}]", TerminalInfo.DIM)
      )
      entry.detail()?.let { console.println(" ".repeat(DETAIL_INDENT) + terminal.style(it, TerminalInfo.DIM)) }
    }
    console.println()
    console.println("  ${key("s")}  Save and continue (Return)     ${key("q")}  Quit")
    showNotice()
    console.println()
  }

  private fun databaseText(state: State): String =
    if (state.embedded) "embedded" else "PostgreSQL ${state.jdbcUrl}"

  /** The user, the password and the result of the connection test, below the database line. */
  private fun databaseDetail(state: State): String? {
    if (state.embedded) return null
    val password = if (state.jdbcPassword.isBlank()) "password not set" else "password set"
    return "user ${state.jdbcUser}, $password, connection test: ${testStatus(state)}"
  }

  /** Warns once if the PostgreSQL settings look incomplete. */
  private fun confirmSave(state: State): Boolean {
    if (state.embedded || (state.jdbcPassword.isNotBlank() && state.lastConnectionTest == Texts.JDBC_TESTRESULT_OK)) {
      return true
    }
    console.println(terminal.style("The PostgreSQL connection is not tested successfully or has no password.", TerminalInfo.YELLOW))
    return console.askYesNo("Save anyway?", true)
  }

  private fun askHomeDir(state: State) {
    while (true) {
      val answer = console.ask("Home directory of ProjectForge", CanonicalFileUtils.absolutePath(state.home))
      val dir = CanonicalFileUtils.absolute(File(expandHome(answer)))
      when {
        dir.exists() && !dir.isDirectory -> console.println("'${dir.path}' isn't a directory.")
        ProjectForgeHomeFinder.isProjectForgeSourceCodeRepository(dir) ->
          console.println("'${dir.path}' seems to be ProjectForge's source code repository, please choose another.")

        else -> {
          state.home = dir
          return
        }
      }
    }
  }

  private fun askPort(state: State) {
    while (true) {
      val port = console.ask("Port of the server", state.port.toString()).toIntOrNull()
      if (port != null && port in 1..65535) {
        state.port = port
        if (!state.domainCustomized) {
          state.domain = "http://localhost:$port"
        }
        return
      }
      console.println("Please enter a port between 1 and 65535.")
    }
  }

  private fun askDomain(state: State) {
    while (true) {
      val domain = console.ask(
        "Domain, the external url of the users, with servlet context if any, e.g. https://acme.com/pf",
        state.domain,
      ).trim().trimEnd('/')
      if ((domain.startsWith("http://") || domain.startsWith("https://")) && domain.none { it.isWhitespace() }) {
        state.domainCustomized = state.domainCustomized || domain != state.domain
        state.domain = domain
        return
      }
      console.println("Please enter an url starting with http:// or https://.")
    }
  }

  private fun databaseMenu(state: State) {
    while (true) {
      page("Database")
      console.println()
      if (setupContext.embeddedDatabaseSupported) {
        console.println("  ${key("1")}  Embedded (built-in, fine for tests and small instances)")
      }
      console.println("  ${key("2")}  PostgreSQL (recommended for productive use)")
      console.println("  ${key("b")}  Back (Return)")
      showNotice()
      console.println()
      when (console.ask("Choice", "b").lowercase()) {
        "1" -> if (setupContext.embeddedDatabaseSupported) {
          state.embedded = true
          return
        } else {
          notice("Please choose one of the numbers or letters shown.")
        }

        "2" -> {
          state.embedded = false
          postgresMenu(state)
          return
        }

        "b" -> return
        else -> notice("Please choose one of the numbers or letters shown.")
      }
    }
  }

  private fun postgresMenu(state: State) {
    while (true) {
      page("Database > PostgreSQL")
      console.println()
      console.println("  ${key("1")}  JDBC url   ${state.jdbcUrl}")
      console.println("  ${key("2")}  User       ${state.jdbcUser}")
      console.println("  ${key("3")}  Password   ${if (state.jdbcPassword.isBlank()) "(not set)" else "****"}")
      console.println("  ${key("t")}  Test connection")
      console.println("  ${key("b")}  Back (Return)")
      console.println()
      console.println("Connection test: ${testStatus(state)}")
      if (setupContext.dockerMode == SetupContext.DockerMode.STACK) {
        console.println("Please wait for the database of docker compose (try to retest the connection).")
      }
      showNotice()
      console.println()
      when (console.ask("Choice", "b").lowercase()) {
        "1" -> {
          state.jdbcUrl = console.ask("JDBC url of PostgreSQL", state.jdbcUrl).trim()
          testConnection(state)
        }

        "2" -> {
          state.jdbcUser = console.ask("Database user", state.jdbcUser).trim()
          testConnection(state)
        }

        "3" -> {
          state.jdbcPassword = console.askSecret("Database password")
          testConnection(state)
        }

        "t" -> testConnection(state)
        "b" -> return
        else -> notice("Please choose one of the numbers or letters shown.")
      }
    }
  }

  /** The result of the last test, green if it was successful, red otherwise. */
  private fun testStatus(state: State): String {
    val result = state.lastConnectionTest ?: return terminal.style("not done", TerminalInfo.DIM)
    return terminal.style(result, if (result == Texts.JDBC_TESTRESULT_OK) TerminalInfo.GREEN else TerminalInfo.RED)
  }

  /** Done after every change of a parameter and on demand; the result is shown below the parameters. */
  private fun testConnection(state: State) {
    console.println("Testing the connection...")
    state.lastConnectionTest = connectionTest(state.jdbcUrl, state.jdbcUser, state.jdbcPassword)
  }

  private fun toSetupData(state: State): SetupData {
    val data = SetupData()
    data.applicationHomeDir = if (docker) presetAppHomeDir else state.home
    data.serverPort = state.port
    data.domain = state.domain
    data.developmentMode = state.cors
    data.useEmbeddedDatabase = state.embedded
    if (!state.embedded) {
      data.jdbcSettings = SetupData.JdbcSettings(state.jdbcUrl, state.jdbcUser, state.jdbcPassword, "org.postgresql.Driver")
    }
    if (docker) {
      // In a docker container the bind address must be 0.0.0.0 for exporting the port to the host:
      data.serverAdress = "0.0.0.0"
      // Don't autostart on docker-compose (server.address=0.0.0.0 not yet available, port 8080 doesn't yet work).
      data.startServer = setupContext.dockerMode != SetupContext.DockerMode.STACK
    }
    console.println()
    console.println("Then open http://localhost:${data.serverPort} in your browser to finish the setup.")
    return data
  }

  private fun expandHome(path: String): String {
    val home = System.getProperty("user.home")
    return when {
      path == "~" -> home
      path.startsWith("~/") || path.startsWith("~\\") -> home + path.substring(1)
      else -> path
    }
  }

  companion object {
    private const val LABEL_WIDTH = 16

    /** The column of the values on the main page: `  1  ` plus the label plus a blank. */
    private const val DETAIL_INDENT = 2 + 1 + 2 + LABEL_WIDTH + 1

    /** The flames (16 columns), two blanks and the lettering (58 columns). */
    private const val FLAMES_MIN_WIDTH = 78

    /** `figlet ProjectForge` */
    private val LOGO = """
 ____            _           _   _____
|  _ \ _ __ ___ (_) ___  ___| |_|  ___|__  _ __ __ _  ___
| |_) | '__/ _ \| |/ _ \/ __| __| |_ / _ \| '__/ _` |/ _ \
|  __/| | | (_) | |  __/ (__| |_|  _| (_) | | | (_| |  __/
|_|   |_|  \___// |\___|\___|\__|_|  \___/|_|  \__, |\___|
              |__/                             |___/
""".trimIndent().trim('\n')

    /**
     * @param appHomeDir Preset for the home directory; used as is in a docker container (no entry).
     * @param console Should be the one that asked before, so that all questions read the same input.
     * @return The user settings or null, if the user quit the wizard.
     */
    @JvmStatic
    fun run(
      appHomeDir: File?,
      setupContext: SetupContext = SetupContext(),
      console: SetupConsole = SystemSetupConsole(),
    ): SetupData? {
      log.info { "Starting console wizard..." }
      return ConsoleSetupWizard(appHomeDir, setupContext, console).run()
    }
  }
}
