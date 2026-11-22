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

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import jakarta.annotation.PostConstruct
import org.projectforge.ProjectForgeVersion
import org.projectforge.SystemStatus
import org.projectforge.business.configuration.DomainService
import org.projectforge.setup.wizard.TerminalInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

/**
 * A banner on the console (System.out, no time stamp and logger in front) as the last output of the start: the log of
 * the start is long and other listeners, plugins and caches write after the "up and running" box of the log, so the
 * address of the new instance scrolled out of sight. The banner waits until the log has been quiet for a moment (at
 * most [MAX_WAIT_MILLIS]), so it really is the last thing on the screen.
 */
@Component
class StartupBanner {
  @Autowired
  private lateinit var environment: Environment

  @Autowired
  private lateinit var domainService: DomainService

  @Autowired
  private lateinit var systemStatus: SystemStatus

  private val lastLogMillis = AtomicLong(System.currentTimeMillis())

  private var logWatcher: AppenderBase<ILoggingEvent>? = null

  /** Notes the time of the last log event; started with the bean, so the logging of the start is seen. */
  @PostConstruct
  fun watchLog() {
    val context = LoggerFactory.getILoggerFactory() as? LoggerContext ?: return
    val watcher = object : AppenderBase<ILoggingEvent>() {
      override fun append(event: ILoggingEvent) {
        lastLogMillis.set(System.currentTimeMillis())
      }
    }
    watcher.context = context
    watcher.start()
    context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(watcher)
    logWatcher = watcher
  }

  @EventListener(ApplicationReadyEvent::class)
  fun onApplicationReady() {
    // Not in the thread of the start: it would delay the start and the listeners which log afterwards.
    Thread({
      waitUntilQuiet({ lastLogMillis.get() }, { System.currentTimeMillis() }, { Thread.sleep(it) })
      logWatcher?.let { (LoggerFactory.getILoggerFactory() as? LoggerContext)?.getLogger(Logger.ROOT_LOGGER_NAME)?.detachAppender(it) }
      printBanner()
    }, "pf-startup-banner").also { it.isDaemon = true }.start()
  }

  private fun printBanner() {
    val port = environment.getProperty("local.server.port") ?: environment.getProperty("server.port") ?: "8080"
    val address = environment.getProperty("server.address")
    val host = if (address.isNullOrBlank() || address == "0.0.0.0" || address == "::") "localhost" else address
    val lines = lines(
      TerminalInfo.detect(),
      ProjectForgeVersion.VERSION_NUMBER,
      "http://$host:$port",
      domainService.domainWithContextPath,
      systemStatus.setupRequiredFirst == true,
    )
    println()
    lines.forEach { println(it) }
    println()
    System.out.flush()
  }

  companion object {
    /** The log has to be quiet this long, ... */
    const val QUIET_MILLIS = 1500L

    /** ... but the banner doesn't wait longer than this (jobs which log regularly). */
    const val MAX_WAIT_MILLIS = 20_000L

    /**
     * Returns when nothing was logged for [quietMillis] or after [maxWaitMillis]; the clock and the sleep are
     * parameters for the test.
     */
    fun waitUntilQuiet(
      lastLogMillis: () -> Long,
      nowMillis: () -> Long,
      sleep: (Long) -> Unit,
      quietMillis: Long = QUIET_MILLIS,
      maxWaitMillis: Long = MAX_WAIT_MILLIS,
    ) {
      val start = nowMillis()
      while (nowMillis() - lastLogMillis() < quietMillis && nowMillis() - start < maxWaitMillis) {
        sleep(100)
      }
    }

    /**
     * @param domain The configured domain; only shown if it differs from the [url].
     * @param firstStart The setup page is still to be completed.
     */
    fun lines(terminal: TerminalInfo, version: String, url: String, domain: String?, firstStart: Boolean): List<String> {
      val content = mutableListOf("ProjectForge $version is running", "", url)
      val configured = domain?.trim()?.trimEnd('/')
      if (!configured.isNullOrEmpty() && configured != url) {
        content.add("Domain: $configured")
      }
      if (firstStart) {
        content.addAll(listOf("", "First start: open this address to finish the setup."))
      }
      val width = content.maxOf { it.length }
      fun styled(text: String) = when (text) {
        content[0] -> terminal.style(text, TerminalInfo.BOLD, TerminalInfo.GREEN)
        url -> terminal.style(text, TerminalInfo.BOLD)
        else -> text
      }
      if (!terminal.ansi) {
        val line = "=".repeat(width + 4)
        return listOf(line) + content.map { "  $it" } + line
      }
      val (topLeft, topRight, bottomLeft, bottomRight, horizontal, vertical) =
        if (terminal.unicode) Frame("╭", "╮", "╰", "╯", "─", "│")
        else Frame("+", "+", "+", "+", "-", "|")
      fun frame(text: String) = terminal.style(text, TerminalInfo.GREEN)
      return buildList {
        add(frame(topLeft + horizontal.repeat(width + 2) + topRight))
        content.forEach { add(frame(vertical) + " " + styled(it) + " ".repeat(width - it.length) + " " + frame(vertical)) }
        add(frame(bottomLeft + horizontal.repeat(width + 2) + bottomRight))
      }
    }

    private data class Frame(
      val topLeft: String, val topRight: String, val bottomLeft: String, val bottomRight: String,
      val horizontal: String, val vertical: String,
    )
  }
}
