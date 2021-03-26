/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import org.projectforge.business.task.TaskDO
import org.projectforge.business.tasktree.TaskTreeHelper
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.calendar.DurationUtils
import org.projectforge.framework.i18n.TimeAgo
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.framework.persistence.database.DatabaseBackupPurgeJob
import org.projectforge.framework.persistence.history.entities.PfHistoryMasterDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberFormatter
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.jcr.RepoBackupService
import org.projectforge.jcr.RepoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.lang.management.ManagementFactory
import java.lang.management.MemoryType
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import javax.sql.DataSource

private val log = KotlinLogging.logger {}

@Service
class SystemStatistics {
  class DatabasePoolStatistics(
    val total: Int,
    val idle: Int,
    val active: Int,
    val threadsAwaitingConnection: Int
  )

  @Autowired
  private lateinit var dataSource: DataSource

  @Autowired
  private lateinit var databaseBackupPurgeJob: DatabaseBackupPurgeJob

  @Autowired
  private lateinit var repoService: RepoService

  @Autowired
  private lateinit var repoBackupService: RepoBackupService

  fun getSystemStatistics(): SystemStatisticsData {
    // First: Get the system load average (don't measure gc run ;-)
    val osBean = ManagementFactory.getOperatingSystemMXBean()
    val systemLoadAverage = BigDecimal(osBean.systemLoadAverage).setScale(2, RoundingMode.HALF_UP)
    val processUptime = ManagementFactory.getRuntimeMXBean().uptime
    val processStartTime = PFDateTime.from(ManagementFactory.getRuntimeMXBean().startTime)
    log.info(
      "System load average: $systemLoadAverage, process start time: ${processStartTime.isoString}, process uptime: ${
        DurationUtils.getFormattedDaysHoursAndMinutes(
          processUptime
        )
      } [h:mm], ${TimeAgo.getMessage(processStartTime.utilDate, Locale.ENGLISH)}"
    )

    val memoryStats = mutableMapOf<String, MemoryStatistics>()
    // Second: run GC and measure memory consumption before getting database statistics.
    System.gc()
    ManagementFactory.getMemoryPoolMXBeans().filter { it.type == MemoryType.HEAP }.forEach { mpBean ->
      val usageBean = mpBean.usage
      memoryStats[mpBean.name] = MemoryStatistics(
        max = usageBean.max,
        used = usageBean.used,
        committed = usageBean.committed,
        init = usageBean.init
      )
    }

    // Finally, the database statistics.
    val jdbc = JdbcTemplate(dataSource)
    val taskTree = TaskTreeHelper.getTaskTree()
    val totalDuration = taskTree.rootTaskNode.getDuration(taskTree, true)
    var totalPersonDays = BigDecimal(totalDuration).divide(DateHelper.SECONDS_PER_WORKING_DAY, 2, RoundingMode.HALF_UP)
    totalPersonDays = NumberHelper.setDefaultScale(totalPersonDays)!!

    val hikariDataSource = dataSource as HikariDataSource
    val hikariPoolMXBean = hikariDataSource.hikariPoolMXBean
    val databaseStatistics = try {
      DatabasePoolStatistics(
        total = hikariPoolMXBean.totalConnections,
        idle = hikariPoolMXBean.idleConnections,
        active = hikariPoolMXBean.activeConnections,
        threadsAwaitingConnection = hikariPoolMXBean.threadsAwaitingConnection
      )
    } catch (ex: Exception) {
      log.error("Can't get HikariDataSource: '${ex.message}'.", ex)
      DatabasePoolStatistics(
        total = -1,
        idle = -1,
        active = -1,
        threadsAwaitingConnection = -1
      )
    }

    val stats = SystemStatisticsData()
    stats.add(
      "totalNumberOfTimesheets", "data base", "system.statistics.totalNumberOfTimesheets",
      getTableCount(jdbc, TimesheetDO::class.java)
    )
    stats.add("totalTimesheetDurations", "data base", "system.statistics.totalTimesheetDurations", totalPersonDays)
    stats.add(
      "totalNumberOfUsers", "data base", "system.statistics.totalNumberOfUsers",
      getTableCount(jdbc, PFUserDO::class.java)
    )
    stats.add(
      "totalNumberOfTasks", "data base", "system.statistics.totalNumberOfTasks",
      getTableCount(jdbc, TaskDO::class.java)
    )
    val totalNumberOfHistoryEntries = getTableCount(jdbc, PfHistoryMasterDO::class.java) + getTableCount(
      jdbc,
      PfHistoryMasterDO::class.java
    )
    stats.add(
      "totalNumberOfHistoryEntries", "data base", "system.statistics.totalNumberOfHistoryEntries",
      totalNumberOfHistoryEntries
    )

    stats.add(
      "databasePool", "data base", "system.statistics.databasePool",
      "total=${databaseStatistics.total}, active=${databaseStatistics.active}, idle=${databaseStatistics.idle}, threadsAwaitingConnection=${databaseStatistics.threadsAwaitingConnection}"
    )

    stats.add("systemLoadAverage", "system", "'System load average", format(systemLoadAverage))
    stats.add(
      "processStartTime", "system", "'Process start time",
      "${processStartTime.isoString} (UTC), ${TimeAgo.getMessage(processStartTime.utilDate)}"
    )
    stats.add(
      "processUptime", "system", "'Process uptime",
      "${DurationUtils.getFormattedDaysHoursAndMinutes(processUptime)} [h:mm]"
    )

    stats.addDiskUsage("jcrDiskUsage", "disk usage", "'JCR storage", repoService.fileStoreLocation)
    stats.addDiskUsage(
      "jcrBackupDiskUsage", "disk usage", "'JCR backup storage",
      repoBackupService.backupDirectory
    )
    stats.addDiskUsage(
      "backupDirDiskUsage", "disk usage", "'Backup storage",
      databaseBackupPurgeJob.dbBackupDir
    )

    memoryStats.forEach { (key, value) ->
      stats.add("$key", "memory", "'$key", value)
    }

    log.info("Statistics: ${ToStringUtil.toJsonString(stats)}")
    return stats
  }

  private fun getTableCount(jdbc: JdbcTemplate, entity: Class<*>): Int {
    try {
      return jdbc.queryForObject("SELECT COUNT(*) FROM " + HibernateUtils.getDBTableName(entity), Int::class.java)!!
    } catch (ex: Exception) {
      log.error(ex.message, ex)
      return 0
    }

  }

  private fun format(number: Number, scale: Int? = null): String {
    return NumberFormatter.format(number, scale)
  }
}
