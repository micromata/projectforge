/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.common.logging

import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.projectforge.common.i18n.MessageParam
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.io.IOException

private val log = KotlinLogging.logger {}

class LogSubscriptionTestMain {
  private val threads = ArrayList<Thread>()

  private val testLogger2: Logger = LoggerFactory.getLogger("org.projectforge.common.i18n.MessageParam")

  fun test() {
    val subscription1 = LoggerMemoryAppender.getInstance()
      .register(LogSubscription("subscription1", "user", LogEventLoggerNameMatcher("org.projectforge.common.i18n"), 10000))
    val subscription2 = LoggerMemoryAppender.getInstance()
      .register(LogSubscription("subscription2", "user", LogEventLoggerNameMatcher("org.projectforge.common.logging"), 10000))
    val subscription3 = LoggerMemoryAppender.getInstance()
      .register(LogSubscription("subscription2", "user", LogEventLoggerNameMatcher("org.projectforge.common.logging|Thread #42"), 10000))
    for (i in 1..NUMBER_OF_THREADS) {
      startProducerThread(i)
    }
    for (thread in threads) {
      try {
        thread.join()
      } catch (ex: InterruptedException) {
      }
    }
    Assertions.assertEquals(
      2 * NUMBER_OF_THREADS * NUMBER_OF_MESSAGES_PER_THREAD,
      LoggerMemoryAppender.getInstance().size
    )
    Assertions.assertEquals(
      NUMBER_OF_THREADS * NUMBER_OF_MESSAGES_PER_THREAD,
      subscription1.size
    )
    Assertions.assertEquals(
      NUMBER_OF_THREADS * NUMBER_OF_MESSAGES_PER_THREAD,
      subscription2.size
    )
    Assertions.assertEquals(
      NUMBER_OF_MESSAGES_PER_THREAD,
      subscription3.size
    )
  }

  private fun startProducerThread(nr: Int) {
    val thread: Thread = object : Thread() {
      override fun run() {
        MDC.put(MDC_USER, "user")
        for (i in 1..NUMBER_OF_MESSAGES_PER_THREAD) {
          log.error { "Thread #$nr, message #$i" }
          testLogger2.error("Thread #$nr, message #$i")
        }
      }
    }
    thread.start()
    threads.add(thread)
  }

  companion object {
    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
      LogSubscriptionTestMain().test()
    }

    private const val NUMBER_OF_THREADS = 50
    private const val NUMBER_OF_MESSAGES_PER_THREAD = 100
  }
}
