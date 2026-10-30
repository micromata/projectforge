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
import org.projectforge.plugins.cveradar.CveRadarService
import org.projectforge.plugins.cveradar.CvrFindingService
import org.projectforge.plugins.cveradar.CvrCaseFileService
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
 * The file of a CVE or an incident (`/rs/cveradar/file`): NVD data, triage, ratings of the security team members,
 * measures. Readers read and open the file of a CVE (to start the discussion); the security team changes it. The case,
 * its assignee and its vulnerability are always taken from the database. Changes are POST only.
 */
@RestController
@RequestMapping("${Rest.URL}/cveradar/file")
class CvrCaseFileRest {
    @Autowired
    private lateinit var access: CveRadarAccess

    @Autowired
    private lateinit var fileService: CvrCaseFileService

    @Autowired
    private lateinit var findingService: CvrFindingService

    @Autowired
    private lateinit var service: CveRadarService

    /** Called by Spring before every endpoint of this controller. */
    @ModelAttribute
    fun checkPluginActive() = access.checkPluginActive()

    @AccessChecked("CvrCaseFileService.viewVulnerability: readers (group of CveRadarConfig.readGroup or higher)")
    @GetMapping("vulnerability/{id}")
    fun vulnerability(@PathVariable id: Long): CaseFile {
        return fileService.viewVulnerability(ThreadLocalUserContext.requiredLoggedInUser, id)
    }

    @AccessChecked("CvrFindingService.findings: readers; findings of instances the user may not see are counted only (VIS-04)")
    @GetMapping("vulnerability/{id}/findings")
    fun findings(@PathVariable id: Long): FindingList {
        return findingService.findings(ThreadLocalUserContext.requiredLoggedInUser, id)
    }

    @AccessChecked("CvrFindingService.discard: security team, in instances it may see")
    @PostMapping("finding/{id}/discard")
    fun discardFinding(@PathVariable id: Long, @RequestBody request: DiscardRequest): FindingList {
        return findingService.discard(ThreadLocalUserContext.requiredLoggedInUser, id, request.reason)
    }

    @AccessChecked("CvrFindingService.setStatus: security team, in instances it may see")
    @PostMapping("finding/status")
    fun setFindingStatus(@RequestBody request: FindingStatusRequest): FindingList {
        return findingService.setStatus(ThreadLocalUserContext.requiredLoggedInUser, request)
    }

    @AccessChecked("CvrFindingService.approveRisk: security officer, management from P2 on, in instances they see")
    @PostMapping("finding/{id}/approveRisk")
    fun approveRisk(@PathVariable id: Long): FindingList {
        return findingService.approveRisk(ThreadLocalUserContext.requiredLoggedInUser, id)
    }

    @AccessChecked("CvrFindingService.rejectRisk: security officer, management, in instances they see")
    @PostMapping("finding/{id}/rejectRisk")
    fun rejectRisk(@PathVariable id: Long): FindingList {
        return findingService.rejectRisk(ThreadLocalUserContext.requiredLoggedInUser, id)
    }

    @AccessChecked("CvrFindingService.restore: security team, in instances it may see")
    @PostMapping("finding/{id}/restore")
    fun restoreFinding(@PathVariable id: Long): FindingList {
        return findingService.restore(ThreadLocalUserContext.requiredLoggedInUser, id)
    }

    @AccessChecked("CveRadarService.getCase (reader) in CvrCaseFileService.viewCase")
    @GetMapping("case/{id}")
    fun case(@PathVariable id: Long): CaseFile {
        return fileService.viewCase(ThreadLocalUserContext.requiredLoggedInUser, id)
    }

    @AccessChecked("CvrCaseFileService.openCase: readers; idempotent, the vulnerability is taken from the database")
    @PostMapping("vulnerability/{id}/open")
    fun open(@PathVariable id: Long): CaseFile {
        return fileService.openCase(ThreadLocalUserContext.requiredLoggedInUser, id)
    }

    @AccessChecked("CvrCaseFileService.triage: security team; binding priority by the assignee or a security officer")
    @PostMapping("case/{id}/triage")
    fun triage(@PathVariable id: Long, @RequestBody request: TriageRequest): CaseFile {
        return fileService.triage(ThreadLocalUserContext.requiredLoggedInUser, id, request)
    }

    @AccessChecked("CvrCaseFileService.rate: security team; the rating is always the one of the logged-in user")
    @PostMapping("case/{id}/rating")
    fun rate(@PathVariable id: Long, @RequestBody request: RatingRequest): CaseFile {
        return fileService.rate(ThreadLocalUserContext.requiredLoggedInUser, id, request)
    }

    @AccessChecked("CvrCaseFileService.deleteRating: security team; only the rating of the logged-in user")
    @PostMapping("case/{id}/rating/delete")
    fun deleteRating(@PathVariable id: Long): CaseFile {
        return fileService.deleteRating(ThreadLocalUserContext.requiredLoggedInUser, id)
    }

    @AccessChecked("CvrCaseFileService.saveMeasures: security team")
    @PostMapping("case/{id}/measures")
    fun measures(@PathVariable id: Long, @RequestBody request: MeasuresRequest): CaseFile {
        return fileService.saveMeasures(ThreadLocalUserContext.requiredLoggedInUser, id, request)
    }

    @AccessChecked("CvrCaseFileService.mass: security team, per vulnerability the rules of the single action")
    @PostMapping("mass")
    fun mass(@RequestBody request: MassRequest): List<MassResult> {
        return fileService.mass(ThreadLocalUserContext.requiredLoggedInUser, request)
    }

    @AccessChecked("CveRadarService.teamCheckFile: members of the stored check's group, security team")
    @GetMapping("teamCheck/{id}")
    fun teamCheck(@PathVariable id: Long): TeamCheckFile {
        return service.teamCheckFile(ThreadLocalUserContext.requiredLoggedInUser, id)
    }

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
