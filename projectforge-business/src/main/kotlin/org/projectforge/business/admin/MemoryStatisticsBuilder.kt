/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import java.lang.management.ManagementFactory
import java.lang.management.MemoryType

class MemoryStatisticsBuilder : SystemsStatisticsBuilderInterface {
  override fun addStatisticsEntries(stats: SystemStatisticsData) {
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
    memoryStats.forEach { (key, value) ->
      stats.add("$key", "memory", "'$key", value)
    }
  }
}
