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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.admin.SystemStatisticsData
import org.projectforge.business.admin.SystemsStatisticsBuilderInterface
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * The key figures of the problem dashboard ([LogGroupSummary]) in the system statistics, for admins only (the
 * statistics are visible for all users).
 */
@Service
class LogAggregationStatisticsBuilder : SystemsStatisticsBuilderInterface {
    @Autowired
    private lateinit var logAggregationService: LogAggregationService

    @Autowired
    private lateinit var logGroupAdminService: LogGroupAdminService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    override fun addStatisticsEntries(stats: SystemStatisticsData) {
        if (!logAggregationService.enabled ||
            !userGroupCache.isUserMemberOfAdminGroup(ThreadLocalUserContext.loggedInUserId)
        ) {
            return
        }
        val summary = try {
            logGroupAdminService.list(LogGroupFilter(days = 1)).summary
        } catch (ex: Exception) {
            // The statistics must never fail because of the log aggregation.
            log.warn { "Can't get the key figures of the log aggregation: ${ex.message}" }
            return
        }
        stats.add("errors24h", GROUP, "'occurrences (24h)", summary.occurrences24h)
        stats.add("errorsNew24h", GROUP, "'new problems (24h)", summary.newProblems24h)
        stats.add("errorsRegressions", GROUP, "'regressions", summary.regressions)
        stats.add("errorsExternal24h", GROUP, "'external systems failing (24h)", summary.externalProblems24h)
        stats.add("errorsOpen", GROUP, "'open problems", summary.open)
    }

    companion object {
        private const val GROUP = "errors"
    }
}
