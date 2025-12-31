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

package org.projectforge.security

import mu.KLogger
import org.projectforge.common.EmphasizedLogSupport
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.SpringApplication
import kotlin.system.exitProcess

/**
 * If any mis-configuration is detected which may result in an undesired state or vulnerable state
 * of ProjectForge, ProjectForge should be immediately be stopped with hints to fix any problems before
 * restart.
 */
object SecurityShutdown {
  private class ShutdownCause(val log: KLogger, val logLines: Array<out String>) {
    fun logItNow() {
      val logSupport = EmphasizedLogSupport(log, EmphasizedLogSupport.Priority.VERY_IMPORTANT)
        .setLogLevel(EmphasizedLogSupport.LogLevel.ERROR)
        .log("SECURITY SHUTDOWN:")
        .log("")
      logLines.forEach { line ->
        logSupport.log(line)
      }
      logSupport.logEnd()
    }
  }
  /**
   * If called, ProjectForge will be shutdowned gracefully.
   */
  fun shutdownSystemOnFatalError(log: KLogger, vararg logLines: String) {
    shutdownCause = ShutdownCause(log, logLines) // Persist for ProjectForgeApplication, for repeating this cause instead of general error message at the end.
    shutdownCause?.logItNow()
    val exitCode = SpringApplication.exit(ApplicationContextProvider.getApplicationContext(), ExitCodeGenerator { 0 })
    exitProcess(exitCode)
  }

  @JvmStatic
  fun logShutDownCause(): Boolean {
    shutdownCause?.logItNow() ?: return false
    return true
  }

  private var shutdownCause: ShutdownCause? = null
}
