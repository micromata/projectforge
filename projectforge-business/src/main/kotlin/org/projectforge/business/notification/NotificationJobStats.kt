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


package org.projectforge.business.notification

import org.projectforge.framework.integration.SubsystemSync
import org.projectforge.framework.integration.SyncStats

/**
 * The runs of a job of the notification system since the start, for its tile on the problem dashboard
 * ([NotificationSubsystemStatusProvider]). Not a [SyncStats]: that one logs every run (the delivery job runs every
 * 15 minutes), the errors are logged by [NotificationService] with the [NotificationLogEvents].
 */
class NotificationJobStats(val type: String) {
    private var runs = 0
    private var lastRun: Long? = null
    private var lastStatus: SyncStats.Status? = null
    private var lastDurationMs: Long? = null
    private var lastError: String? = null
    private var lastErrorDate: Long? = null

    /** The start of the last run, epoch millis. */
    val lastRunStart: Long?
        @Synchronized get() = lastRun

    /**
     * @param start The start of the run, epoch millis.
     * @param errors The number of failed deliveries or rules, the last [error] of them.
     */
    @Synchronized
    fun record(start: Long, errors: Int = 0, error: String? = null) {
        record(start, if (errors > 0) SyncStats.Status.ERRORS else SyncStats.Status.SUCCESS)
        if (errors > 0) {
            lastError = "$errors error(s)${error?.let { ", last: $it" } ?: ""}"
            lastErrorDate = System.currentTimeMillis()
        }
    }

    /** The run failed as a whole. */
    @Synchronized
    fun recordAbort(start: Long, ex: Exception) {
        record(start, SyncStats.Status.ABORTED)
        lastError = ex.message ?: ex.javaClass.simpleName
        lastErrorDate = System.currentTimeMillis()
    }

    /** Null, if not yet run. */
    @Synchronized
    fun toSync(): SubsystemSync? {
        if (runs == 0) {
            return null
        }
        return SubsystemSync(
            type = type,
            runs = runs,
            lastRun = lastRun,
            lastStatus = lastStatus,
            lastDurationMs = lastDurationMs,
            lastError = lastError,
            lastErrorDate = lastErrorDate,
        )
    }

    private fun record(start: Long, status: SyncStats.Status) {
        runs++
        lastRun = start
        lastStatus = status
        lastDurationMs = System.currentTimeMillis() - start
    }
}
