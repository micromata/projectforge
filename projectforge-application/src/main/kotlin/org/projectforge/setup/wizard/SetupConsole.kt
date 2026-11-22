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
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

private val log = KotlinLogging.logger {}

/**
 * The console of the setup: all questions before ProjectForge starts (the wizard question of the
 * [org.projectforge.start.ProjectForgeHomeFinder] as well as the ones of the [ConsoleSetupWizard]) share one instance,
 * so that they read the same input.
 *
 * Without a console (input closed) an answer is empty, so the defaults apply. Only a timeout (nobody answers although
 * the input is open) results in no answer at all.
 */
interface SetupConsole {
  fun println(text: String = "")

  /** What the terminal can do; plain text unless it is known to understand ANSI. */
  val terminal: TerminalInfo
    get() = TerminalInfo.PLAIN

  /** Clears the screen (only with ANSI), so every page of the wizard starts at the top. */
  fun clear() {}

  /**
   * Prints the question and reads one line.
   *
   * @return The line as typed (not trimmed, case is kept, e. g. for paths), an empty string if the input is closed, null
   * on timeout.
   */
  fun readAnswer(question: String, timeoutSeconds: Long): String?

  /** Like [readAnswer], but without echo if the system has a console. */
  fun askSecret(question: String): String

  /**
   * @param default Used for an empty answer, a timeout and a closed input.
   */
  fun ask(question: String, default: String): String =
    readAnswer("$question [$default]", DEFAULT_TIMEOUT_SECONDS)?.trim()?.ifEmpty { default } ?: default

  /**
   * @return The answer, until it is a yes or no; the default for an empty answer or a closed input; null on timeout.
   */
  fun askYesNoOrNull(question: String, default: Boolean, timeoutSeconds: Long): Boolean? {
    val prompt = "$question (${if (default) "Y/n" else "y/N"})"
    while (true) {
      val answer = readAnswer(prompt, timeoutSeconds)?.trim()?.lowercase() ?: return null
      when {
        answer.isEmpty() -> return default
        answer.startsWith("y") -> return true
        answer.startsWith("n") -> return false
      }
      println("Please answer with 'y' or 'n'.")
    }
  }

  fun askYesNo(question: String, default: Boolean): Boolean =
    askYesNoOrNull(question, default, DEFAULT_TIMEOUT_SECONDS) ?: default

  companion object {
    const val DEFAULT_TIMEOUT_SECONDS = 300L
  }
}

/**
 * Reads one line at a time in a daemon thread, only while a question is open: a read that timed out stays pending and
 * answers the next question (it is not lost), but between the questions nobody reads ahead. So [askSecret] may read from
 * the system console itself without a second reader taking the first line.
 */
class SystemSetupConsole(private val input: InputStream = System.`in`) : SetupConsole {
  /** Only the real input is a terminal (a test input isn't). */
  override val terminal: TerminalInfo by lazy {
    if (input === System.`in`) TerminalInfo.detect() else TerminalInfo.PLAIN
  }

  override fun clear() {
    val sequence = terminal.clearScreen()
    if (sequence.isNotEmpty()) {
      print(sequence)
      System.out.flush()
    }
  }

  private val reader by lazy { BufferedReader(InputStreamReader(input)) }
  private val executor = Executors.newSingleThreadExecutor { runnable ->
    Thread(runnable, "pf-setup-console").also { it.isDaemon = true }
  }
  private var pending: Future<String>? = null
  private var closed = false
  private var closedReported = false

  override fun println(text: String) {
    kotlin.io.println(text)
  }

  override fun readAnswer(question: String, timeoutSeconds: Long): String? {
    log.debug { "Waiting $timeoutSeconds seconds for the answer to: $question" }
    kotlin.io.println(question)
    return readLine(timeoutSeconds)
  }

  override fun askSecret(question: String): String {
    // Only the real input has a system console (a test input is no terminal).
    val console = if (input === System.`in`) System.console() else null
    if (console != null && pending == null && !closed) {
      return console.readPassword("$question: ")?.let { String(it) } ?: ""
    }
    return readAnswer("$question:", SetupConsole.DEFAULT_TIMEOUT_SECONDS)?.trim() ?: ""
  }

  /** @return "" if the input is closed, null on timeout. */
  private fun readLine(timeoutSeconds: Long): String? {
    if (closed) return closedAnswer()
    val read = pending ?: executor.submit<String> { reader.readLine() ?: EOF }.also { pending = it }
    val line = try {
      read.get(timeoutSeconds, TimeUnit.SECONDS)
    } catch (ex: TimeoutException) {
      return null // Stays pending for the next question.
    } catch (ex: Exception) {
      log.debug { "Console input failed: ${ex.message}" }
      EOF
    }
    pending = null
    if (line == EOF) {
      closed = true
      return closedAnswer()
    }
    return line
  }

  private fun closedAnswer(): String {
    if (!closedReported) {
      closedReported = true
      kotlin.io.println(
        "No console input available, the defaults are used. Start ProjectForge in a terminal to choose them."
      )
    }
    return ""
  }

  private companion object {
    const val EOF = "\u0000EOF"
  }
}
