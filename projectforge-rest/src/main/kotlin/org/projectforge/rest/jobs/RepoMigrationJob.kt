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

package org.projectforge.rest.jobs

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.jobs.AbstractJob
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.jcr.RepoMigrationService

private val log = KotlinLogging.logger {}

/**
 * Moves the files of all paths registered as file system paths (DataTransfer) out of Oak into the file system.
 * Files already moved are skipped, so the job may run more than once. Shares the queue with [JcrBackupZipJob]:
 * both read the whole repository.
 */
class RepoMigrationJob(
    private val repoMigrationService: RepoMigrationService,
    title: String,
) : AbstractJob(
    title,
    area = AREA,
    queueName = JcrBackupZipJob.QUEUE_NAME,
    queueStrategy = QueueStrategy.REFUSE_PER_QUEUE,
    // Several 100 GB of DataTransfer files take hours.
    timeoutSeconds = 12 * 60 * 60,
) {
    private var result: RepoMigrationService.Result? = null

    override suspend fun run() {
        log.info { "Moving files out of the JCR into the file system: $logInfo" }
        result = repoMigrationService.migrateFileSystemPaths { processed, total ->
            totalNumber = total
            processedNumber = processed
        }
    }

    override val progressDetails: String?
        get() = result?.let { result ->
            if (result.errors.isEmpty()) {
                result.toString()
            } else {
                "$result: ${result.errors.joinToString("; ")}"
            }
        }

    override fun writeAccess(user: PFUserDO?): Boolean {
        user ?: return false
        return UserGroupCache.getInstance().isUserMemberOfAdminGroup(user.id)
    }

    companion object {
        const val AREA = "JcrMigration"
    }
}
