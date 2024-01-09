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

package org.projectforge.business.admin

import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskTree
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.framework.persistence.history.entities.PfHistoryMasterDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.utils.NumberHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import javax.sql.DataSource

private val log = KotlinLogging.logger {}

@Service
class DatabaseStatisticsBuilder : SystemsStatisticsBuilderInterface {
  @Autowired
  private lateinit var dataSource: DataSource

  @Autowired
  private lateinit var taskTree: TaskTree

  override fun addStatisticsEntries(stats: SystemStatisticsData) {
    val jdbc = JdbcTemplate(dataSource)
    val totalDuration = taskTree.rootTaskNode.getDuration(taskTree, true)
    var totalPersonDays = BigDecimal(totalDuration).divide(DateHelper.SECONDS_PER_WORKING_DAY, 2, RoundingMode.HALF_UP)
    totalPersonDays = NumberHelper.setDefaultScale(totalPersonDays)!!

    val hikariDataSource = dataSource as HikariDataSource
    val hikariPoolMXBean = hikariDataSource.hikariPoolMXBean
    val databaseStatistics = try {
      SystemStatistics.DatabasePoolStatistics(
        total = hikariPoolMXBean.totalConnections,
        idle = hikariPoolMXBean.idleConnections,
        active = hikariPoolMXBean.activeConnections,
        threadsAwaitingConnection = hikariPoolMXBean.threadsAwaitingConnection
      )
    } catch (ex: Exception) {
      log.error("Can't get HikariDataSource: '${ex.message}'.", ex)
      SystemStatistics.DatabasePoolStatistics(
        total = -1,
        idle = -1,
        active = -1,
        threadsAwaitingConnection = -1
      )
    }
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
  }

  private fun getTableCount(jdbc: JdbcTemplate, entity: Class<*>): Int {
    return try {
      jdbc.queryForObject("SELECT COUNT(*) FROM " + HibernateUtils.getDBTableName(entity), Int::class.java)!!
    } catch (ex: Exception) {
      log.error(ex.message, ex)
      0
    }
  }
}
