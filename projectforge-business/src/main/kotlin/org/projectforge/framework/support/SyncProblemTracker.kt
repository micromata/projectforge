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

package org.projectforge.framework.support

import org.projectforge.framework.integration.SyncStats
import org.projectforge.framework.integration.SyncStatsRegistry
import java.util.Date

/**
 * The sync runs (LDAP, IdP, gateway, Sipgate, iCal subscriptions, ...) that went wrong since the last digest:
 * the failure counters of [SyncStats] are compared with those seen last time, so a lasting outage is reported
 * as long as runs keep failing, and not again once it is over.
 */
class SyncProblemTracker(private val source: () -> List<SyncStats> = SyncStatsRegistry::all) {
    class Problem(
        val type: String,
        val runsWithErrors: Int,
        val abortedRuns: Int,
        val timeouts: Int,
        val lastStatus: SyncStats.Status?,
        val lastError: String?,
        val lastErrorDate: Date?,
    )

    private data class Counters(val runsWithErrors: Int, val abortedRuns: Int, val timeouts: Int)

    private val seen = mutableMapOf<String, Counters>()

    /** The problems since the last call. */
    @Synchronized
    fun collect(): List<Problem> {
        return source().mapNotNull { stats ->
            val now = Counters(stats.runsWithErrors, stats.abortedRuns, stats.timeouts)
            val before = seen.put(stats.type, now) ?: Counters(0, 0, 0)
            val problem = Problem(
                type = stats.type,
                runsWithErrors = (now.runsWithErrors - before.runsWithErrors).coerceAtLeast(0),
                abortedRuns = (now.abortedRuns - before.abortedRuns).coerceAtLeast(0),
                timeouts = (now.timeouts - before.timeouts).coerceAtLeast(0),
                lastStatus = stats.lastStatus,
                lastError = stats.lastError,
                lastErrorDate = stats.lastErrorDate,
            )
            problem.takeIf { it.runsWithErrors + it.abortedRuns + it.timeouts > 0 }
        }
    }
}
