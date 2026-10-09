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

import java.lang.management.ManagementFactory
import java.lang.management.MemoryType

class MemoryStatisticsBuilder : SystemsStatisticsBuilderInterface {
  override fun addStatisticsEntries(stats: SystemStatisticsData) {
    // Second: run GC and measure memory consumption before getting database statistics.
    System.gc()
    val runtime = Runtime.getRuntime()
    stats.add(
      "heap", "memory", "'Heap (total)",
      MemoryStatistics(max = runtime.maxMemory(), used = runtime.totalMemory() - runtime.freeMemory(), committed = runtime.totalMemory(), init = 0)
    )
    ManagementFactory.getMemoryPoolMXBeans().sortedBy { it.type }.forEach { mpBean ->
      val usageBean = mpBean.usage
      val memoryStats = MemoryStatistics(
        max = usageBean.max,
        used = usageBean.used,
        committed = usageBean.committed,
        init = usageBean.init
      )
      if (mpBean.type == MemoryType.HEAP) {
        stats.add(mpBean.name, "memory", "'${mpBean.name}", memoryStats)
      } else {
        stats.add(mpBean.name, "memory (non-heap)", "'${mpBean.name}", memoryStats)
      }
    }
    ManagementFactory.getGarbageCollectorMXBeans().forEach { gcBean ->
      stats.add(
        "gc-${gcBean.name}", "garbage collection", "'${gcBean.name}",
        "runs=${format(gcBean.collectionCount)}, time=${format(gcBean.collectionTime)} ms"
      )
    }
  }
}
