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

package org.projectforge.start

import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.time.Duration
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.system.exitProcess

private val log = KotlinLogging.logger {}

/**
 * For reading a system console input (with timeout).
 */
open class ConsoleTimeoutReader
/**
 * @param question
 * @param defaultAnswer The answer if the user hits simply return.
 */
@JvmOverloads
constructor(
  private val question: String,
  private val defaultAnswer: String? = null,
  var timeOutSeconds: Int = CONSOLE_INPUT_TIMEOUT
) {
  private val streamReader = InputStreamReader(System.`in`)
  private val bufferedReader = BufferedReader(streamReader)

  fun ask(): String? {
    log.info("ProjectForge is waiting $timeOutSeconds seconds for your input on console (if running without console, ProjectForge will continue anyway): $question")
    println()
    println(StringUtils.center(" QUESTION ", 120, "?"))
    println()
    var answer: String?
    do {
      answer = readConsoleAnswerWithTimeout()
      if (answer == null) return null
      val result = if (defaultAnswer != null && answer.isEmpty()) {
        defaultAnswer
      } else {
        answer.trim { it <= ' ' }.lowercase()
      }
      if (answerValid(result)) return result
    } while (true)
  }

  /**
   * If the user hits simply return, the parameter answer will be the configured default answer or an empty string,
   * if no default value is defined.
   *
   * @param answer Answer given from console input ("" or input to lower case).
   * @return true if answer starts with 'y' or 'n', otherwise false.
   */
  protected open fun answerValid(answer: String): Boolean {
    return answer.startsWith("y") || answer.startsWith("n")
  }

  private fun readConsoleAnswerWithTimeout(): String? {
    val timeout = Duration.ofSeconds(timeOutSeconds.toLong())
    val executor = Executors.newSingleThreadExecutor()
    val handler = executor.submit<String> { readConsoleAnswer(question) }
    var answer: String? = null
    try {
      answer = handler[timeout.toMillis(), TimeUnit.MILLISECONDS]
    } catch (ex: TimeoutException) {
      log.info("Timeout of console input exceeded (>" + timeOutSeconds + "s). Aborting.")
      handler.cancel(true)
    } catch (ex: InterruptedException) {
      log.info("Timeout of console input exceeded (>" + timeOutSeconds + "s). Aborting.")
      handler.cancel(true)
    } catch (ex: ExecutionException) {
      log.info("Timeout of console input exceeded (>" + timeOutSeconds + "s). Aborting.")
      handler.cancel(true)
    }
    executor.shutdownNow()
    return answer
  }

  @Throws(IOException::class)
  private fun readConsoleAnswer(question: String): String {
    println(question)
    return bufferedReader.readLine() ?: ""
  }

  companion object {
    private const val CONSOLE_INPUT_TIMEOUT = 30

    @JvmStatic
    fun main(args: Array<String>) {
      println("The answer is: " + ConsoleTimeoutReader("Question", timeOutSeconds = 5).ask())
      exitProcess(0)
    }
  }
}
