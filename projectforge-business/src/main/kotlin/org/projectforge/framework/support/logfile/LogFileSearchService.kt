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

package org.projectforge.framework.support.logfile

import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.support.LogGroupDO
import org.projectforge.framework.support.SupportLogEvents
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * The occurrences of a problem of the log aggregation in the log files, also of the days before the server's start
 * (the log viewer only has the last events in memory). No access checks here, see `AdminErrorsRest`.
 */
@Service
class LogFileSearchService {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    /** @return null, if there is no such problem (anymore). */
    fun search(problemId: Long): LogFileSearchResult? {
        val group = persistenceService.runReadOnly { it.find(LogGroupDO::class.java, problemId) } ?: return null
        val problem = LogFileProblem(
            level = group.level!!,
            location = group.location,
            exceptionClass = group.exceptionClass,
            sampleMessage = group.sampleMessage,
            byCode = group.code !in UNSPECIFIC_CODES,
            firstSeen = group.firstSeen!!.time,
            lastSeen = group.lastSeen!!.time,
        )
        return LogFileSearch(problem).search(LogFileLocator.locate())
    }

    private companion object {
        /** The codes of the problems grouped by their message rather than the code (see `ErrorDigestCollector.keyOf`). */
        val UNSPECIFIC_CODES = setOf(
            SupportLogEvents.LOGGED_ERROR.code,
            SupportLogEvents.REQUEST_ERROR.code,
            SupportLogEvents.EXTERNAL_UNREACHABLE.code,
        )
    }
}
