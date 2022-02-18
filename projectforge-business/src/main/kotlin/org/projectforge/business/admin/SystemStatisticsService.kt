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
import org.projectforge.framework.ToStringUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

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
  private lateinit var databaseStatisticsBuilder: DatabaseStatisticsBuilder

  @Autowired
  private lateinit var diskUsageStatisticsBuilder: DiskUsageStatisticsBuilder

  private var statisticsBuilderRegistry = mutableSetOf<SystemsStatisticsBuilderInterface>()

  @PostConstruct
  private fun postConstruct() {
    // First get System statistics (for getting current CPU loads, not CPU loads after collecting all statistics ;-)
    registerStatisticsBuilder(SystemsStatisticsBuilder())
    registerStatisticsBuilder(databaseStatisticsBuilder)
    registerStatisticsBuilder(MemoryStatisticsBuilder())
    registerStatisticsBuilder(diskUsageStatisticsBuilder)
  }

  /**
   * Plugins may register statistics builder for appending there own statistics.
   */
  @Suppress("unused")
  fun registerStatisticsBuilder(statisticsBuilder: SystemsStatisticsBuilderInterface) {
    statisticsBuilderRegistry.add(statisticsBuilder)
  }

  /**
   * Builds all system statistics of registered builders, logs and returns the result.
   */
  fun getSystemStatistics(): SystemStatisticsData {
    val stats = SystemStatisticsData()

    statisticsBuilderRegistry.forEach {
      it.addStatisticsEntries(stats)
    }

    log.info("Statistics: ${ToStringUtil.toJsonString(stats)}")
    return stats
  }
}
