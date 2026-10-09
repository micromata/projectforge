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

import org.projectforge.framework.utils.DiskUsage
import org.projectforge.framework.utils.NumberFormatter
import org.projectforge.framework.utils.NumberHelper
import java.io.File

class SystemStatisticsData {
  /**
   * Fill level of an entry (memory, disk, connections), shown as a bar by the frontend.
   */
  class Gauge(
    val used: Long,
    val max: Long,
    val unit: Unit = Unit.BYTES,
  ) {
    enum class Unit { BYTES, COUNT }
  }

  class Entry(
    val id: String,
    /**
     * For grouping entries.
     */
    val group: String,
    val title: String,
    val value: Any?,
    val gauge: Gauge? = null,
  ) {
    fun valueAsString(): String {
      return value?.toString() ?: "--"
    }
  }

  val entries = mutableListOf<Entry>()

  val groups
    get() = entries.map { it.group }.distinct()

  fun filterEntries(group: String): List<Entry> {
    return entries.filter { it.group == group }
  }

  fun add(entry: Entry) {
    entries.add(entry)
  }

  fun add(id: String, group: String, title: String, value: Any?, gauge: Gauge? = null): Entry {
    val entry = Entry(id, group, title, value, gauge)
    add(entry)
    return entry
  }

  fun add(id: String, group: String, title: String, value: Number, scale: Int? = null) {
    add(id, group, title, format(value, scale))
  }

  fun add(id: String, group: String, title: String, value: MemoryStatistics) {
    add(id, group, title, value.toString(), if (value.max > 0) Gauge(value.used, value.max) else null)
  }

  fun addDiskUsage(id: String, group: String, title: String, file: File?) {
    addDiskUsage(id, group, title, DiskUsage(file))
  }

  fun addDiskUsage(id: String, group: String, title: String, file: String?) {
    addDiskUsage(id, group, title, DiskUsage(file))
  }

  fun addDiskUsage(id: String, group: String, title: String, diskUsage: DiskUsage) {
    val path = diskUsage.path?.toFile()?.absolutePath ?: "---"
    if (diskUsage.totalSpace <= 0) {
      add(id, group, title, "${formatBytes(diskUsage.used)}: $path")
      return
    }
    val diskUsed = diskUsage.totalSpace - diskUsage.freeSpace
    // The bar shows the fill level of the whole disk, the value also the size of the directory on it.
    val value = "${formatBytes(diskUsage.used)}: $path (disk: ${formatBytes(diskUsed)} / ${
      formatBytes(diskUsage.totalSpace)
    } used, ${formatBytes(diskUsage.freeSpace)} free)"
    add(id, group, title, value, Gauge(diskUsed, diskUsage.totalSpace))
  }

  private fun format(number: Number, scale: Int? = null): String {
    return NumberFormatter.format(number, scale)
  }

  private fun formatBytes(number: Long): String {
    return NumberHelper.formatBytes(number)
  }
}
