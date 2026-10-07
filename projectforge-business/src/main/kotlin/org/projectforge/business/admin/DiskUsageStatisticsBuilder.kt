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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.common.extensions.format
import org.projectforge.common.extensions.formatBytes
import org.projectforge.framework.persistence.database.DatabaseBackupPurgeJob
import org.projectforge.jcr.RepoBackupService
import org.projectforge.jcr.RepoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class DiskUsageStatisticsBuilder : SystemsStatisticsBuilderInterface {

  @Autowired
  private lateinit var databaseBackupPurgeJob: DatabaseBackupPurgeJob

  @Autowired
  private lateinit var repoService: RepoService

  @Autowired
  private lateinit var repoBackupService: RepoBackupService


  override fun addStatisticsEntries(stats: SystemStatisticsData) {
    stats.addDiskUsage("jcrDiskUsage", "disk usage", "'JCR storage", repoService.fileStoreLocation)
    repoService.fileStore?.let { fileStore ->
      fileStore.fileSystemDirs.forEachIndexed { index, dir ->
        stats.addDiskUsage("fileStoreDiskUsage$index", "disk usage", "'File store (${dir.name})", dir)
      }
      try {
        val info = fileStore.getStatistics().joinToString(", ") {
          "${it.storage}: ${it.count.format()} files (${it.size.formatBytes()}, stored ${it.storedSize.formatBytes()})"
        }
        stats.add("fileStoreStatistics", "disk usage", "'File store", info)
      } catch (ex: Exception) {
        log.error(ex) { "Can't get statistics of file store: ${ex.message}" }
      }
    }
    stats.addDiskUsage(
      "jcrBackupDiskUsage", "disk usage", "'JCR backup storage",
      repoBackupService.backupDirectory
    )
    stats.addDiskUsage(
      "backupDirDiskUsage", "disk usage", "'Backup storage",
      databaseBackupPurgeJob.dbBackupDir
    )
  }
}
