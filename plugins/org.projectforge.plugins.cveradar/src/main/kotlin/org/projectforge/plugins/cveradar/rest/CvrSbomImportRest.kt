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

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.plugins.cveradar.CveRadarAccess
import org.projectforge.plugins.cveradar.CvrSbomImportService
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.pub.next.RestError
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
import org.springframework.web.bind.annotation.RestController

/**
 * The bulk import of SBOMs into an area (INV-35, [CvrSbomImportService]). The zip comes as the plain body of the
 * request (`application/zip`), not as multipart: it is streamed into a file with the limit of the area, independent
 * of the multipart limits of the installation. Reading and storing run as jobs, followed via the job monitor.
 */
@RestController
@RequestMapping("${Rest.URL}/cveradar/inventory/area/{areaId}/sbomImport")
class CvrSbomImportRest {
    @Autowired
    private lateinit var access: CveRadarAccess

    @Autowired
    private lateinit var service: CvrSbomImportService

    /** Called by Spring before every endpoint of this controller. */
    @ModelAttribute
    fun checkPluginActive() = access.checkPluginActive()

    private val user get() = ThreadLocalUserContext.requiredLoggedInUser

    @AccessChecked("CvrSbomImportService.startAnalysis: write access to the stored area, size limit of the area")
    @PostMapping
    fun upload(@PathVariable areaId: Long, request: HttpServletRequest): JobStarted =
        service.startAnalysis(user, areaId, request.contentLengthLong, request.inputStream)

    /** The plan of an analysis; the area of the path is ignored, the plan names its own. */
    @AccessChecked("CvrSbomImportService.plan: the analysis job of the logged-in user only")
    @GetMapping("{importId}")
    fun plan(@PathVariable areaId: Long, @PathVariable importId: Int): SbomImportPlanView = service.plan(user, importId)

    @AccessChecked("CvrSbomImportService.startApply: the analysis job of the logged-in user, write access to its area")
    @PostMapping("{importId}/apply")
    fun apply(
        @PathVariable areaId: Long,
        @PathVariable importId: Int,
        @RequestBody request: SbomImportApplyRequest,
    ): JobStarted = service.startApply(user, importId, request)

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
