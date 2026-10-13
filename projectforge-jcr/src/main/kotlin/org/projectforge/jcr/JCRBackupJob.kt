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

package org.projectforge.jcr

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

@Component
class JCRBackupJob {
    @Autowired
    private lateinit var repoBackupService: RepoBackupService

    @Value("\${projectforge.jcr.cron.purgeBackupKeepDailyBackups}")
    private val keepDailyBackups: Long? = null

    @Value("\${projectforge.jcr.cron.purgeBackupKeepWeeklyBackups}")
    private val keepWeeklyBackups: Long? = null

    @Autowired
    private lateinit var schedulerJobRunner: SchedulerJobRunner

    @PostConstruct
    private fun postConstruct() {
        schedulerJobRunner.register(SCHEDULER_JOB, ::execute) {
            if (repoBackupService.backupDirectory == null) "No backup directory of the repository." else null
        }
    }

    // projectforge.jcr.cron.backup=0 30 0 * * *
    @Scheduled(cron = CRON)
    fun execute() {
        // Duration and exceptions are logged by the scheduler.
        schedulerJobRunner.runAsync(SCHEDULER_JOB) {
            val backupDirectory = repoBackupService.backupDirectory!!
            repoBackupService.createBackupFile()
            BackupFilesPurging.purgeDirectory(
                backupDirectory,
                filePrefix = RepoBackupService.backupFilenamePrefix,
                keepDailyBackups = keepDailyBackups ?: 8,
                keepWeeklyBackups = keepWeeklyBackups ?: 4,
            )
        }
    }

    companion object {
        private const val CRON = "\${projectforge.jcr.cron.backup}"

        val SCHEDULER_JOB = SchedulerJobDefinition(
            "jcr.backup", SchedulerJobArea.BACKUP, JCRBackupJob::class.java, "execute", SchedulerSchedule.Cron(CRON),
        )
    }
}
