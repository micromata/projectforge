/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.database

import mu.KotlinLogging
import org.projectforge.common.BackupFilesPurging
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.File

private val log = KotlinLogging.logger {}

/**
 * Purges data base backup files by using [BackupFilesPurging] if backup dir is configured in projectforge.properties.
 */
@Component
class DatabaseBackupPurgeJob {
    @Value("\${projectforge.cron.purgeBackupDir}")
    val dbBackupDir: String? = null

    @Value("\${projectforge.cron.purgeBackupFilesPrefix}")
    private val dbBackupFilesPrefix: String? = null

    @Value("\${projectforge.cron.purgeBackupKeepDailyBackups}")
    private val dbBackupKeepDailyBackups: Long? = null

    @Value("\${projectforge.cron.purgeBackupKeepWeeklyBackups}")
    private val dbBackupKeepWeeklyBackups: Long? = null

    // projectforge.cron.dbBackupCleanup=0 40 0 * * *
    @Scheduled(cron = "\${projectforge.cron.purgeBackup}")
    fun execute() {
        if (dbBackupDir.isNullOrBlank()) {
            log.info { "No backup dir will be cleaned up, because the backup dir isn't configured. If you want the feature, that all daily backups will be removed after 30 days but the montly backups will be kept, please configure projectforge.cron.dbBackupCleanup in projectforge.properties." }
            return
        }
        val backupDir = File(dbBackupDir)
        if (!backupDir.isDirectory) {
            log.error { "Configured backup dir '$dbBackupDir' isn't a directory. Can't clean up old backups from this directory." }
            return
        }
        log.info { "Starting job for cleaning daily backup files older than 30 days, but monthly backups will be kept." }
        BackupFilesPurging.purgeDirectory(backupDir,
            filePrefix = dbBackupFilesPrefix,
            keepDailyBackups = dbBackupKeepDailyBackups ?: 8,
            keepWeeklyBackups = dbBackupKeepWeeklyBackups ?: 4)
    }
}
