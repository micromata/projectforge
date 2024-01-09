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

import org.projectforge.framework.persistence.database.DatabaseBackupPurgeJob
import org.projectforge.jcr.RepoBackupService
import org.projectforge.jcr.RepoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

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
