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


package org.projectforge.rest.admin

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.support.logfile.LogFileContext
import org.projectforge.framework.support.logfile.LogFileLocator
import org.projectforge.framework.support.logfile.LogFileQuery
import org.projectforge.framework.support.logfile.LogFileSearch
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.ZoneId

private val log = KotlinLogging.logger {}

/**
 * The admin log viewer (`next/adminLogViewer`): the last 10,000 log events of the whole system, admin group only.
 * Under a REST path of its own, so the 2FA registration of ADMIN gates it without gating the users' log viewer
 * (see [LogViewerRest]).
 *
 * Its mode "log files" searches the log files (`ProjectForge.log` and the rolled ones of the last days) by text,
 * period, level and logger, also for the days before the server's start, and shows the context of a hit.
 */
@RestController
@RequestMapping("${Rest.URL}/adminLogViewer")
open class AdminLogViewerRest : LogViewerRest() {
    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var userPrefService: UserPrefService

    init {
        adminLogViewer = true
    }

    /** The admin's last search of the log files (user pref), not run yet. */
    @AccessChecked("Admin group only")
    @GetMapping("logFile/initial")
    fun logFileInitial(): LogFileQueryFilter {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        return getLogFilePref()
    }

    @AccessChecked("Admin group only")
    @PostMapping("logFile/query")
    fun queryLogFile(@RequestBody filter: LogFileQueryFilter): LogFileSearchData {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        getLogFilePref().let { pref ->
            pref.search = filter.search
            pref.threshold = filter.threshold
            pref.period = filter.period
            pref.from = filter.from
            pref.until = filter.until
            pref.includeLoggers = filter.includeLoggers
            pref.excludeLoggers = filter.excludeLoggers
            pref.before = filter.before
            pref.after = filter.after
        }
        val now = System.currentTimeMillis()
        val until = filter.until ?: now
        val from = filter.from ?: LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val query = LogFileQuery(
            search = filter.search,
            threshold = filter.threshold,
            from = from,
            until = until,
            includeLoggers = filter.includeLoggers,
            excludeLoggers = filter.excludeLoggers,
        )
        val result = LogFileSearch(query, maxHits = LogFileQuery.MAX_HITS).search(LogFileLocator.locate())
        log.info {
            "${ThreadLocalUserContext.loggedInUser?.username} searched the log files: " +
                    "${result.records.size} hit(s) in ${result.searchedFiles}."
        }
        return LogFileSearchData(result)
    }

    @AccessChecked("Admin group only")
    @PostMapping("logFile/context")
    fun logFileContext(@RequestBody request: LogFileContextRequest): LogFileContextData {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        val result = LogFileContext(
            line = request.line,
            timestampMillis = request.timestampMillis,
            user = request.user,
            logger = request.logger,
            before = request.before,
            after = request.after,
            scope = request.scope,
            excludeLoggers = request.excludeLoggers,
        ).read(LogFileLocator.locate(), request.file)
        return LogFileContextData(result)
    }

    private fun getLogFilePref(): LogFileQueryFilter {
        return userPrefService.ensureEntry("logging", "logFileQueryFilter", LogFileQueryFilter())
    }
}
