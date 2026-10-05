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
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.jcr.RepoBackupService
import java.io.File

private val log = KotlinLogging.logger {}

/**
 * Writes a backup ZIP of the whole JCR (without the DataTransfer files) into the backup directory, the same as
 * the nightly JCRBackupJob does. It is the source for importing the entity files into the new file store in the
 * release without Oak, so the admin can create a fresh one right before the update.
 */
class JcrBackupZipJob(
    private val repoBackupService: RepoBackupService,
    title: String,
) : AbstractJob(
    title,
    area = AREA,
    queueName = QUEUE_NAME,
    queueStrategy = QueueStrategy.REFUSE_PER_QUEUE,
    // Several GB of files take a while, the default of 120s would have the scheduler cancel it.
    timeoutSeconds = 6 * 60 * 60,
) {
    private var zipFile: File? = null

    override suspend fun run() {
        log.info { "Creating JCR backup ZIP: $logInfo" }
        zipFile = repoBackupService.createBackupFile()
    }

    /** The written file, e. g. "/home/pf/backup/projectforge-jcr-backup-2026-10-05.zip (1.2 GB)". */
    override val progressDetails: String?
        get() = zipFile?.let { "${it.absolutePath} (${NumberHelper.formatBytes(it.length())})" }

    override fun writeAccess(user: PFUserDO?): Boolean {
        user ?: return false
        // Not accessChecker of AbstractJob: jobs are created with new and never autowired.
        return UserGroupCache.getInstance().isUserMemberOfAdminGroup(user.id)
    }

    companion object {
        const val AREA = "JcrBackup"
        const val QUEUE_NAME = "jcr"
    }
}
