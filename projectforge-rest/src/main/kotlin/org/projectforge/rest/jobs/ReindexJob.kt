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

import mu.KotlinLogging
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.jobs.AbstractJob
import org.projectforge.framework.persistence.api.ReindexSettings
import org.projectforge.framework.persistence.database.DatabaseDao
import org.projectforge.framework.persistence.database.IndexProgressMonitor
import org.projectforge.framework.persistence.user.entities.PFUserDO

private val log = KotlinLogging.logger {}

/**
 * Rebuilds the search index of one or more entities as a job, so the client doesn't have to wait for it: a full
 * re-index of the history takes minutes. The client polls JobsMonitorPageRest for the progress.
 *
 * @param classes The entities to re-index, in this order (see BaseDao.reindexClasses).
 * @param settings Empty for a full run, or with a fromDate for the newest entries only.
 * @param adminRequired True for the full run: it hits the whole system, so only admins may watch and cancel it.
 */
class ReindexJob(
    private val databaseDao: DatabaseDao,
    private val classes: List<Class<*>>,
    private val settings: ReindexSettings,
    private val adminRequired: Boolean,
    title: String,
) : AbstractJob(
    title,
    area = AREA,
    queueName = QUEUE_NAME,
    // Re-indexing is a system-wide operation, so a second run isn't queued but refused right away: the user gets
    // an error instead of a progress bar that doesn't move.
    queueStrategy = QueueStrategy.REFUSE_PER_QUEUE,
    // A full re-index of the history is a matter of minutes, the default of 120s would have the scheduler cancel it.
    timeoutSeconds = 2 * 60 * 60,
) {
    /** Entities already indexed by the previous classes — the progress has to grow over the whole run. */
    private var processedOffset = 0

    /** Total of the previous classes plus the total of the running one, as reported by Hibernate Search. */
    private var totalOffset = 0

    override suspend fun run() {
        for (clazz in classes) {
            if (!isActive) {
                log.info { "Re-index job was cancelled, skipping ${clazz.simpleName}: $logInfo" }
                return
            }
            val monitor = IndexProgressMonitor(clazz) { indexed, total ->
                // Hibernate Search only knows the total after counting, so it can't be summed up in advance.
                totalNumber = totalOffset + total.toInt()
                processedNumber = processedOffset + indexed.toInt()
            }
            databaseDao.reindexSuspending(clazz, settings, monitor)
            processedOffset = processedNumber.coerceAtLeast(0)
            totalOffset = totalNumber.coerceAtLeast(0)
        }
    }

    override fun writeAccess(user: PFUserDO?): Boolean {
        user ?: return false
        // Cancelling stops indexing for everybody, so the full run belongs to admins only. Not accessChecker of
        // AbstractJob: jobs are created with new and never autowired, so that field is an unset lateinit.
        val isAdmin = UserGroupCache.getInstance().isUserMemberOfAdminGroup(user.id)
        return if (adminRequired) isAdmin else isOwner || isAdmin
    }

    companion object {
        const val AREA = "Reindex"
        const val QUEUE_NAME = "reindex"
    }
}
