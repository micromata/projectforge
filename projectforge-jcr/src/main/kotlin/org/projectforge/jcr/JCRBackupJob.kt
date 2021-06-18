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

package org.projectforge.jcr

import mu.KotlinLogging
import org.projectforge.common.BackupFilesPurging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipOutputStream

private val log = KotlinLogging.logger {}

@Component
class JCRBackupJob {
  @Autowired
  private lateinit var repoBackupService: RepoBackupService

  @Value("\${projectforge.jcr.cron.purgeBackupKeepDailyBackups}")
  private val keepDailyBackups: Long? = null

  @Value("\${projectforge.jcr.cron.purgeBackupKeepWeeklyBackups}")
  private val keepWeeklyBackups: Long? = null

  // projectforge.jcr.cron.backup=0 30 0 * * *
  @Scheduled(cron = "\${projectforge.jcr.cron.backup}")
  fun execute() {
    log.info("JCR backup job started.")
    val time = System.currentTimeMillis()
    val backupFile = RepoBackupService.backupFilename
    val backupDirectory = repoBackupService.backupDirectory!!
    val zipFile = File(backupDirectory, backupFile)
    ZipOutputStream(FileOutputStream(zipFile)).use {
      repoBackupService.backupAsZipArchive(zipFile.name, it)
    }
    log.info("JCR backup job finished after ${(System.currentTimeMillis() - time) / 1000} seconds.")
    BackupFilesPurging.purgeDirectory(
      backupDirectory,
      filePrefix = RepoBackupService.backupFilenamePrefix,
      keepDailyBackups = keepDailyBackups ?: 8,
      keepWeeklyBackups = keepWeeklyBackups ?: 4,
    )
  }
}
