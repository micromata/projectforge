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

package org.projectforge.business.admin

import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskTree
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.framework.persistence.history.HistoryEntryAttrDO
import org.projectforge.framework.persistence.history.HistoryEntryDO
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

  override val adminOnly = false

  override fun addStatisticsEntries(stats: SystemStatisticsData) {
    val jdbc = JdbcTemplate(dataSource)
    val totalDuration = taskTree.rootTaskNode.getDuration(taskTree, true)
    var totalPersonDays = BigDecimal(totalDuration).divide(DateHelper.SECONDS_PER_WORKING_DAY, 2, RoundingMode.HALF_UP)
    totalPersonDays = NumberHelper.setDefaultScale(totalPersonDays)!!

    stats.add(
      "totalNumberOfTimesheets", GROUP, "system.statistics.totalNumberOfTimesheets",
      getTableCount(jdbc, TimesheetDO::class.java)
    )
    stats.add("totalTimesheetDurations", GROUP, "system.statistics.totalTimesheetDurations", totalPersonDays)
    stats.add(
      "totalNumberOfUsers", GROUP, "system.statistics.totalNumberOfUsers",
      getTableCount(jdbc, PFUserDO::class.java)
    )
    stats.add(
      "totalNumberOfTasks", GROUP, "system.statistics.totalNumberOfTasks",
      getTableCount(jdbc, TaskDO::class.java)
    )
    stats.add(
      "totalNumberOfHistoryEntries", GROUP, "system.statistics.totalNumberOfHistoryEntries",
      getTableCount(jdbc, HistoryEntryDO::class.java)
    )
    stats.add(
      "totalNumberOfHistoryAttrs", GROUP, "'Number of history attributes",
      getTableCount(jdbc, HistoryEntryAttrDO::class.java)
    )
    addPoolStatistics(stats)
    addServerStatistics(stats, jdbc)
  }

  private fun addPoolStatistics(stats: SystemStatisticsData) {
    val hikariDataSource = dataSource as? HikariDataSource ?: return
    try {
      val pool = hikariDataSource.hikariPoolMXBean
      val max = hikariDataSource.maximumPoolSize
      stats.add(
        "databasePool", GROUP, "system.statistics.databasePool",
        "total=${pool.totalConnections}, active=${pool.activeConnections}, idle=${pool.idleConnections}, threadsAwaitingConnection=${pool.threadsAwaitingConnection}"
      )
      stats.add(
        "databasePoolActive", GROUP, "'Pool: active connections",
        "${format(pool.activeConnections)} / ${format(max)}",
        SystemStatisticsData.Gauge(pool.activeConnections.toLong(), max.toLong(), SystemStatisticsData.Gauge.Unit.COUNT),
      )
      stats.add(
        "databasePoolOpen", GROUP, "'Pool: open connections",
        "${format(pool.totalConnections)} / ${format(max)} (idle=${format(pool.idleConnections)})",
        SystemStatisticsData.Gauge(pool.totalConnections.toLong(), max.toLong(), SystemStatisticsData.Gauge.Unit.COUNT),
      )
      stats.add(
        "databasePoolAwaiting", GROUP, "'Pool: threads awaiting connection",
        format(pool.threadsAwaitingConnection)
      )
      stats.add(
        "databasePoolConfig", GROUP, "'Pool: configuration",
        "name=${hikariDataSource.poolName}, maximumPoolSize=$max, minimumIdle=${hikariDataSource.minimumIdle}, connectionTimeout=${hikariDataSource.connectionTimeout} ms, idleTimeout=${hikariDataSource.idleTimeout} ms, maxLifetime=${hikariDataSource.maxLifetime} ms"
      )
    } catch (ex: Exception) {
      log.error(ex) { "Can't get statistics of HikariDataSource: '${ex.message}'." }
    }
  }

  private fun addServerStatistics(stats: SystemStatisticsData, jdbc: JdbcTemplate) {
    val productName = try {
      dataSource.connection.use { connection ->
        val meta = connection.metaData
        stats.add(
          "databaseProduct", GROUP, "'Database",
          "${meta.databaseProductName} ${meta.databaseProductVersion} (driver ${meta.driverName} ${meta.driverVersion})"
        )
        meta.databaseProductName
      }
    } catch (ex: Exception) {
      log.warn { "Can't get the database meta data: ${ex.message}" }
      return
    }
    if (!productName.contains("PostgreSQL", ignoreCase = true)) {
      return
    }
    try {
      val size = jdbc.queryForObject("SELECT pg_database_size(current_database())", Long::class.java)
      stats.add("databaseSize", GROUP, "'Database size", formatBytes(size))
    } catch (ex: Exception) {
      log.warn { "Can't get the size of the database: ${ex.message}" }
    }
    try {
      val maxConnections = jdbc.queryForObject("SHOW max_connections", String::class.java)!!.toLong()
      val connections = jdbc.queryForObject("SELECT COUNT(*) FROM pg_stat_activity", Long::class.java)!!
      val ownConnections = jdbc.queryForObject(
        "SELECT COUNT(*) FROM pg_stat_activity WHERE datname = current_database()", Long::class.java
      )!!
      stats.add(
        "databaseServerConnections", GROUP, "'Database server: connections",
        "${format(connections)} / ${format(maxConnections)} (this database: ${format(ownConnections)})",
        SystemStatisticsData.Gauge(connections, maxConnections, SystemStatisticsData.Gauge.Unit.COUNT),
      )
    } catch (ex: Exception) {
      log.warn { "Can't get the connections of the database server: ${ex.message}" }
    }
  }

  private fun getTableCount(jdbc: JdbcTemplate, entity: Class<*>): Int {
    return try {
      jdbc.queryForObject("SELECT COUNT(*) FROM " + HibernateUtils.getDBTableName(entity), Int::class.java)!!
    } catch (ex: Exception) {
      log.error(ex) { ex.message }
      0
    }
  }

  companion object {
    const val GROUP = "database"
  }
}
