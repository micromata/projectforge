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
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.scheduling.SchedulerJobAdminService
import org.projectforge.framework.scheduling.SchedulerJobDetail
import org.projectforge.framework.scheduling.SchedulerJobList
import org.projectforge.framework.scheduling.SchedulerJobRegistry.RunNowStatus
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

/**
 * The tab "Scheduler" of the system dashboard (`next/systemDashboard?tab=scheduler`): the scheduled jobs with their
 * runs, durations and errors, and "run now". Admin group only.
 */
@RestController
@RequestMapping("${Rest.URL}/adminScheduler")
class AdminSchedulerRest {
    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var schedulerJobAdminService: SchedulerJobAdminService

    @AccessChecked("Admin group only")
    @GetMapping("list")
    fun list(): SchedulerJobList {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        return schedulerJobAdminService.list()
    }

    @AccessChecked("Admin group only")
    @GetMapping("detail")
    fun detail(@RequestParam("id") id: String): ResponseEntity<SchedulerJobDetail> {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        return schedulerJobAdminService.detail(id)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    /**
     * Starts the job at once. 404 for an unknown job, 409 (with the reason as body) if the job is inactive or
     * still running.
     */
    @AccessChecked("Admin group only")
    @PostMapping("runNow")
    fun runNow(@RequestParam("id") id: String): ResponseEntity<String> {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        val user = ThreadLocalUserContext.loggedInUser?.username
        val result = schedulerJobAdminService.runNow(id, user)
        return when (result.status) {
            RunNowStatus.STARTED -> ResponseEntity.ok("STARTED")
            RunNowStatus.NOT_FOUND -> ResponseEntity.notFound().build()
            RunNowStatus.INACTIVE, RunNowStatus.RUNNING -> {
                log.info { "$user couldn't start scheduler job $id: ${result.status} ${result.reason ?: ""}" }
                ResponseEntity.status(HttpStatus.CONFLICT).body(result.status.name)
            }
        }
    }
}
