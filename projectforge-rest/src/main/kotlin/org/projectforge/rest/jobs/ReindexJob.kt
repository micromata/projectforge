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
import org.projectforge.framework.utils.NumberFormatter
import java.util.Collections

private val log = KotlinLogging.logger {}

/**
 * Rebuilds the search index of one or more entities as a job, so the client doesn't have to wait for it: a full
 * re-index of a large table takes minutes. The client polls JobsMonitorPageRest for the progress.
 *
 * @param classes The entities to re-index, in this order (see BaseDao.reindexClasses).
 * @param settings Empty for a full run, or with a fromDate for the newest entries only.
 * @param adminRequired True for the full run: it purges the index before rebuilding it, so only admins may start,
 * watch and cancel it.
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

    /**
     * Indexed and total entities per class, in the order they are processed. The counters of the job itself are the
     * sum over all classes, which says little for a run covering an entity and its change history: "169/169" for
     * three books is puzzling until it is split into 3 books and 166 history entries.
     *
     * Written from the indexing threads of Hibernate Search and read by the client polling for the progress, so
     * synchronized (a LinkedHashMap wouldn't survive that).
     */
    private val statistics = Collections.synchronizedMap(LinkedHashMap<String, Progress>())

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
                statistics[clazz.simpleName] = Progress(indexed, total)
            }
            databaseDao.reindexSuspending(clazz, settings, monitor)
            processedOffset = processedNumber.coerceAtLeast(0)
            totalOffset = totalNumber.coerceAtLeast(0)
        }
    }

    /**
     * The counts per class, e. g. "BookDO: 3/3, HistoryEntryDO: 166/166" — the sum in the progress title is what the
     * bar needs, but it doesn't say how much of it is the entity and how much its change history.
     */
    override val progressDetails: String?
        get() = synchronized(statistics) {
            statistics.entries
                .joinToString { (className, progress) -> "$className: $progress" }
                .ifEmpty { null }
        }

    private class Progress(private val indexed: Long, private val total: Long) {
        override fun toString(): String {
            return "${NumberFormatter.format(indexed)}/${NumberFormatter.format(total)}"
        }
    }

    override fun writeAccess(user: PFUserDO?): Boolean {
        user ?: return false
        // Cancelling a full run leaves the index of the entity half rebuilt, so it belongs to admins only. Not
        // accessChecker of AbstractJob: jobs are created with new and never autowired, so that field is an unset
        // lateinit.
        val isAdmin = UserGroupCache.getInstance().isUserMemberOfAdminGroup(user.id)
        return if (adminRequired) isAdmin else isOwner || isAdmin
    }

    companion object {
        const val AREA = "Reindex"
        const val QUEUE_NAME = "reindex"
    }
}
