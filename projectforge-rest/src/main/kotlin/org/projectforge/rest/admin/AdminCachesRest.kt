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

import jakarta.validation.Valid
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.cache.CacheAdminService
import org.projectforge.framework.cache.CacheAdminService.TerminateStatus
import org.projectforge.framework.cache.CacheEntry
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.aggrid.AGGridSupport
import org.projectforge.rest.core.aggrid.GridState
import org.projectforge.rest.dto.datatable.DataTableStateRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * The tab "Caches" of the system dashboard (`next/systemDashboard?tab=caches`): all caches with their state, last
 * refresh and errors, and the manual refresh of one or all of them. Admin group only.
 */
@RestController
@RequestMapping("${Rest.URL}/adminCaches")
class AdminCachesRest {
    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var agGridSupport: AGGridSupport

    @Autowired
    private lateinit var cacheAdminService: CacheAdminService

    @AccessChecked("Admin group only")
    @GetMapping("list")
    fun list(): List<CacheEntry> {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        return cacheAdminService.list()
    }

    /** Starts the refresh of the cache in the background and returns the list. 404 for an unknown cache. */
    @AccessChecked("Admin group only")
    @PostMapping("refresh")
    fun refresh(@RequestParam("id") id: String): ResponseEntity<List<CacheEntry>> {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        if (!cacheAdminService.refresh(id)) {
            return ResponseEntity.notFound().build()
        }
        return ResponseEntity.ok(cacheAdminService.list())
    }

    /**
     * Terminates the running refresh of the cache (interrupts and abandons it, see [AbstractCache.terminateRefresh])
     * and returns the list. 404 for an unknown cache, 409 if no refresh is running (any more).
     */
    @AccessChecked("Admin group only")
    @PostMapping("terminate")
    fun terminate(@RequestParam("id") id: String): ResponseEntity<List<CacheEntry>> {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        val user = ThreadLocalUserContext.loggedInUser?.username
        return when (cacheAdminService.terminate(id, user)) {
            TerminateStatus.TERMINATED -> ResponseEntity.ok(cacheAdminService.list())
            TerminateStatus.NOT_FOUND -> ResponseEntity.notFound().build()
            TerminateStatus.NOT_RUNNING -> ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }

    /** Starts the refresh of all caches in the background and returns the list. 409 if it is still running. */
    @AccessChecked("Admin group only")
    @PostMapping("refreshAll")
    fun refreshAll(): ResponseEntity<List<CacheEntry>> {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        if (!cacheAdminService.refreshAll()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
        return ResponseEntity.ok(cacheAdminService.list())
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
        private const val GRID_CATEGORY = "adminCaches"
    }
}
