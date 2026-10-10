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

package org.projectforge.framework.persistence.database

import jakarta.annotation.PostConstruct
import org.projectforge.common.BackupFilesPurging
import org.projectforge.common.scheduling.SchedulerJobArea
import org.projectforge.common.scheduling.SchedulerJobDefinition
import org.projectforge.common.scheduling.SchedulerJobRunner
import org.projectforge.common.scheduling.SchedulerSchedule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.File

/**
 * Purges data base backup files by using [BackupFilesPurging] if backup dir is configured in projectforge.properties.
 * Several backup series (e. g. the main dump and the dump of the files schema pf_files) may be given as
 * comma-separated prefixes (projectforge.cron.purgeBackupFilesPrefix); each series is purged on its own.
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

    @Autowired
    private lateinit var schedulerJobRunner: SchedulerJobRunner

    @PostConstruct
    private fun postConstruct() {
        schedulerJobRunner.register(SCHEDULER_JOB, ::execute) {
            // If wanted, that all daily backups will be removed after 30 days but the monthly backups will be kept,
            // configure projectforge.cron.purgeBackupDir in projectforge.properties.
            if (dbBackupDir.isNullOrBlank()) "projectforge.cron.purgeBackupDir isn't configured." else null
        }
    }

    // projectforge.cron.dbBackupCleanup=0 40 0 * * *
    @Scheduled(cron = CRON)
    fun execute() {
        schedulerJobRunner.runAsync(SCHEDULER_JOB) { run ->
            val backupDir = File(dbBackupDir!!)
            if (!backupDir.isDirectory) {
                // Logged by the scheduler as failed job.
                run.fail("Configured backup dir '$dbBackupDir' isn't a directory. Can't clean up old backups from this directory.")
                return@runAsync
            }
            parsePrefixes(dbBackupFilesPrefix).forEach { prefix ->
                BackupFilesPurging.purgeDirectory(backupDir,
                    filePrefix = prefix,
                    keepDailyBackups = dbBackupKeepDailyBackups ?: 8,
                    keepWeeklyBackups = dbBackupKeepWeeklyBackups ?: 4)
            }
        }
    }

    companion object {
        private const val CRON = "\${projectforge.cron.purgeBackup}"

        val SCHEDULER_JOB = SchedulerJobDefinition(
            "backup.purgeDatabaseBackups", SchedulerJobArea.BACKUP, DatabaseBackupPurgeJob::class.java, "execute",
            SchedulerSchedule.Cron(CRON),
        )

        /**
         * @return The comma-separated prefixes, or a list containing null (all files containing a date), if not given.
         */
        internal fun parsePrefixes(prefixes: String?): List<String?> {
            val list = prefixes?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.distinct()
            return if (list.isNullOrEmpty()) listOf(null) else list
        }
    }
}
