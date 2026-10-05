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
import jakarta.validation.Valid
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.support.LogGroupAdminService
import org.projectforge.framework.support.LogGroupDetail
import org.projectforge.framework.support.LogGroupFilter
import org.projectforge.framework.support.LogGroupList
import org.projectforge.framework.support.LogGroupUpdate
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.aggrid.AGGridSupport
import org.projectforge.rest.core.aggrid.GridState
import org.projectforge.rest.dto.datatable.DataTableStateRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger {}

/**
 * The error dashboard of projectforge-next (`next/adminErrors`): the problems of the log aggregation with their
 * trends, and their status (acknowledge, ignore, mute, resolve). Admin group only, 2FA-gated as ADMIN
 * (`ProjectForge2FAInitialization`).
 */
@RestController
@RequestMapping("${Rest.URL}/adminErrors")
class AdminErrorsRest {
    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var agGridSupport: AGGridSupport

    @Autowired
    private lateinit var logGroupAdminService: LogGroupAdminService

    @AccessChecked("Admin group only")
    @PostMapping("list")
    fun list(@RequestBody filter: LogGroupFilter): LogGroupList {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        return logGroupAdminService.list(filter)
    }

    @AccessChecked("Admin group only")
    @GetMapping("detail")
    fun detail(@RequestParam("id") id: Long): ResponseEntity<LogGroupDetail> {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        // Gone, if deleted by the cleanup meanwhile.
        return logGroupAdminService.detail(id)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    /**
     * All problems of the filter with their samples as JSON file, for an analysis by an AI (see
     * [org.projectforge.framework.support.LogAnalysisExport]).
     */
    @AccessChecked("Admin group only")
    @PostMapping("export")
    fun export(@RequestBody filter: LogGroupFilter): ResponseEntity<String> {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        val export = logGroupAdminService.export(filter)
        log.info {
            "${ThreadLocalUserContext.loggedInUser?.username} downloaded ${export.problems.size} problem(s) of the log aggregation."
        }
        val filename = "projectforge-errors-${LocalDateTime.now().format(FILENAME_FORMAT)}.json"
        return RestUtils.downloadFile(filename, export.toJson())
    }

    /** @return The number of changed problems. */
    @AccessChecked("Admin group only")
    @PostMapping("update")
    fun update(@RequestBody update: LogGroupUpdate): ResponseEntity<Int> {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        val count = try {
            logGroupAdminService.update(update)
        } catch (ex: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }
        log.info {
            "${ThreadLocalUserContext.loggedInUser?.username} changed $count problem(s) of the log aggregation: " +
                    "${update.action} ${update.muteDays?.let { "$it days" } ?: update.notify ?: ""}, ids=${update.ids}"
        }
        return ResponseEntity.ok(count)
    }

    /** Stores the table's column state (order, width, visibility, pinning, sorting) in the user's prefs. */
    @AccessChecked("Admin group only")
    @PostMapping(RestPaths.SET_COLUMN_STATES)
    fun updateColumnStates(@Valid @RequestBody request: DataTableStateRequest): String {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        agGridSupport.storeGridState(GRID_CATEGORY, request)
        return "OK"
    }

    /** The user's stored column state of the table, or an empty one if there is none yet. */
    @AccessChecked("Admin group only")
    @GetMapping(RestPaths.COLUMN_STATES)
    fun getColumnStates(): GridState {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        return agGridSupport.getGridState(GRID_CATEGORY) ?: GridState()
    }

    companion object {
        private const val GRID_CATEGORY = "adminErrors"

        private val FILENAME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")
    }
}
