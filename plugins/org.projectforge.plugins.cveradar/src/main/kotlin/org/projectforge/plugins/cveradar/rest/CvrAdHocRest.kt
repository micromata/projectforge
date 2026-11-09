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

package org.projectforge.plugins.cveradar.rest

import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.plugins.cveradar.CveRadarAccess
import org.projectforge.plugins.cveradar.CvrAdHocCheckService
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.pub.next.RestError
import org.projectforge.rest.config.Rest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** The ad-hoc check of uploaded files against all vulnerabilities (see [CvrAdHocCheckService]); nothing is stored. */
@RestController
@RequestMapping("${Rest.URL}/cveradar/adhoc")
class CvrAdHocRest {
    @Autowired
    private lateinit var access: CveRadarAccess

    @Autowired
    private lateinit var service: CvrAdHocCheckService

    /** Called by Spring before every endpoint of this controller. */
    @ModelAttribute
    fun checkPluginActive() = access.checkPluginActive()

    private val user get() = ThreadLocalUserContext.requiredLoggedInUser

    @AccessChecked("CvrAdHocCheckService.start: reader role; the files are read and the job belongs to the caller")
    @PostMapping("check")
    fun start(@RequestBody request: AdHocRequest): AdHocStatus = service.start(user, request)

    /** A GET: reading the progress changes nothing. */
    @AccessChecked("CvrAdHocCheckService.status: reader role, the job of the caller only")
    @GetMapping("{jobId}")
    fun status(@PathVariable jobId: Int): AdHocStatus = service.status(user, jobId)

    @AccessChecked("CvrAdHocCheckService.cancel: reader role, the job of the caller only")
    @PostMapping("{jobId}/cancel")
    fun cancel(@PathVariable jobId: Int) = service.cancel(user, jobId)

    /** A GET: the report changes nothing. */
    @AccessChecked("CvrAdHocCheckService.report: reader role, the finished job of the caller only")
    @GetMapping("{jobId}/report")
    fun report(
        @PathVariable jobId: Int,
        @RequestParam(defaultValue = "true") anonymize: Boolean,
        @RequestParam(required = false) exclude: List<String>? = null,
    ): FindingReport = service.report(user, jobId, anonymize, exclude.orEmpty().toSet())

    @ExceptionHandler(AccessException::class)
    fun handleAccessException(ex: AccessException): ResponseEntity<RestError> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(RestError(status = HttpStatus.FORBIDDEN.value(), message = translateMsg(ex)))
    }

    @ExceptionHandler(UserException::class)
    fun handleUserException(ex: UserException): ResponseEntity<CveRadarError> {
        return ResponseEntity.badRequest().body(CveRadarError(translateMsg(ex)))
    }
}
