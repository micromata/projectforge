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

package org.projectforge.business.admin

import mu.KotlinLogging
import org.projectforge.ProjectForgeVersion
import org.projectforge.framework.calendar.DurationUtils
import org.projectforge.framework.i18n.TimeAgo
import org.projectforge.framework.time.PFDateTime
import java.lang.management.ManagementFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

private val log = KotlinLogging.logger {}

class SystemsStatisticsBuilder : SystemsStatisticsBuilderInterface {
  override fun addStatisticsEntries(stats: SystemStatisticsData) {
    // First: Get the system load average (don't measure gc run ;-)
    val osBean = ManagementFactory.getOperatingSystemMXBean()
    val systemLoadAverage = BigDecimal(osBean.systemLoadAverage).setScale(2, RoundingMode.HALF_UP)
    val processUptime = ManagementFactory.getRuntimeMXBean().uptime
    val processStartTime = PFDateTime.from(ManagementFactory.getRuntimeMXBean().startTime)
    val numberOfActiveThreads = Thread.activeCount()
    log.info(
      "System load average: $systemLoadAverage, active threads: $numberOfActiveThreads, process start time: ${processStartTime.isoString}, process uptime: ${
        DurationUtils.getFormattedDaysHoursAndMinutes(
          processUptime
        )
      } [h:mm], ${TimeAgo.getMessage(processStartTime.utilDate, Locale.ENGLISH)}"
    )
    stats.add(
      "version", "system", "'ProjectForge® version",
      "${ProjectForgeVersion.APP_ID} ${ProjectForgeVersion.VERSION_NUMBER}: build date=${ProjectForgeVersion.BUILD_TIMESTAMP}",
    )
    stats.add(
      "scm", "system", "'SCM",
      "${ProjectForgeVersion.SCM}=${ProjectForgeVersion.SCM_ID}"
    )
    stats.add("systemLoadAverage", "system", "'System load average", format(systemLoadAverage))
    stats.add("activeThreads", "system", "'Number of active threads", format(numberOfActiveThreads))
    stats.add(
      "processStartTime", "system", "'Process start time",
      "${processStartTime.isoString} (UTC), ${TimeAgo.getMessage(processStartTime.utilDate)}"
    )
    @Suppress("unused")
    stats.add(
      "processUptime", "system", "'Process uptime",
      "${DurationUtils.getFormattedDaysHoursAndMinutes(processUptime)} [h:mm]"
    )
    stats.add(
      "java", "system", "'Java version",
      "${System.getProperty("java.vendor")} ${System.getProperty("java.version")}"
    )
  }
}
