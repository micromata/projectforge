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

import org.hibernate.Hibernate
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.CveRadarAccess
import org.projectforge.plugins.cveradar.CveRadarConfig
import org.projectforge.plugins.cveradar.CveRadarService
import org.projectforge.plugins.cveradar.CvrCaseStatusService
import org.projectforge.plugins.cveradar.CvrDeviceService
import org.projectforge.plugins.cveradar.CvrStatisticsService
import org.projectforge.plugins.cveradar.model.CvrCaseDO
import org.projectforge.plugins.cveradar.model.CvrClientAnswer
import org.projectforge.plugins.cveradar.model.CvrDeviceDO
import org.projectforge.plugins.cveradar.model.CvrTeamCheckDO
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Date

/**
 * The CVE-Radar besides editing cases (R0): closure overview and closing (CLS-02, CLS-05), team checks (CLS-03), the
 * devices for the security team and the own view of every employee (own devices, released cases to confirm, the team
 * checks of the own groups, CLS-04). Devices with their persons are answered to the security team and the person
 * only (VIS-15); every access is checked here on the server.
 */
@RestController
@RequestMapping("${Rest.URL}/cveradar")
class CveRadarRest {
    @Autowired
    private lateinit var access: CveRadarAccess

    @Autowired
    private lateinit var config: CveRadarConfig

    @Autowired
    private lateinit var deviceService: CvrDeviceService

    @Autowired
    private lateinit var service: CveRadarService

    @Autowired
    private lateinit var statisticsService: CvrStatisticsService

    @Autowired
    private lateinit var statusService: CvrCaseStatusService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    /** Called by Spring before every endpoint of this controller. */
    @ModelAttribute
    fun checkPluginActive() = access.checkPluginActive()

    @AccessChecked("CveRadarService.getCase: read right; devices with persons for the read-write right only (VIS-15)")
    @GetMapping("case/{id}/status")
    fun status(@PathVariable id: Long): ClosureView {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        return closureView(user, service.getCase(user, id))
    }

    @AccessChecked("CveRadarService.close: read-write right; with open items security officer only (CLS-05)")
    @PostMapping("case/{id}/close")
    fun close(@PathVariable id: Long, @RequestBody request: CloseRequest): ClosureView {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        return closureView(user, service.close(user, id, request.reason))
    }

    @AccessChecked("CveRadarService.createTeamCheck: read-write right, case and group from the database")
    @PostMapping("case/{id}/teamCheck")
    fun createTeamCheck(@PathVariable id: Long, @RequestBody request: TeamCheckRequest): ClosureView {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        service.createTeamCheck(user, id, request.groupId, request.subject)
        return closureView(user, service.getCase(user, id))
    }

    @AccessChecked("CveRadarService.answerTeamCheck: member of the stored check's group or read-write right")
    @PostMapping("teamCheck/{id}/answer")
    fun answerTeamCheck(@PathVariable id: Long, @RequestBody request: TeamCheckAnswerRequest): TeamCheckRow {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        return teamCheckRow(user, service.answerTeamCheck(user, id, request.answer, request.comment, request.date))
    }

    @AccessChecked("CveRadarService.deleteTeamCheck: read-write right")
    @PostMapping("teamCheck/{id}/delete")
    fun deleteTeamCheck(@PathVariable id: Long) {
        service.deleteTeamCheck(ThreadLocalUserContext.requiredLoggedInUser, id)
    }

    @AccessChecked("CveRadarAccess.checkSecurityTeam: devices with persons for the read-write right only (VIS-15)")
    @GetMapping("devices")
    fun devices(): List<DeviceRow> {
        access.checkSecurityTeam(ThreadLocalUserContext.requiredLoggedInUser)
        val devices = deviceService.selectDevices()
        return devices.map { deviceRow(it) }
    }

    @AccessChecked("CveRadarAccess.checkReader: read-only or read-write right; counts only")
    @GetMapping("statistics/reported")
    fun reportedStatistics(@RequestParam granularity: CvrStatisticsService.Granularity): ReportedStatistics {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        access.checkReader(user)
        val zone = ThreadLocalUserContext.timeZone.toZoneId()
        return ReportedStatistics(
            granularity,
            statisticsService.casesCreated(granularity, zone),
            statisticsService.vulnerabilitiesPublished(granularity, zone),
        )
    }

    @AccessChecked("own user only: own devices (by the user id of the session), released cases, own groups' checks")
    @GetMapping("my")
    fun my(): MyView {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val userId = user.id!!
        val activeSince = Date(System.currentTimeMillis() - config.clientActiveDays * 24L * 3600 * 1000)
        val devices = deviceService.selectDevicesOfUser(userId)
        val confirmations = deviceService.selectConfirmationsOfUser(userId).associateBy { it.caseId }
        val activeDevices = devices.filter { it.lastSeen?.before(activeSince) == false }
        val advisories = service.releasedCases().map { case ->
            // The own devices only, loaded once for all cases.
            val ownStates = statusService.deviceStates(case, activeDevices)
            val confirmation = confirmations[case.id]
            Advisory(
                id = case.id!!,
                code = case.code,
                title = case.title,
                nonTechMeasures = case.nonTechMeasures,
                myAnswer = confirmation?.answer,
                myComment = confirmation?.comment,
                fixedByEvidence = ownStates.isNotEmpty() &&
                        ownStates.all { it.state == CvrCaseStatusService.DeviceState.FIXED },
            )
        }
        return MyView(
            devices = devices.map { deviceRow(it) },
            advisories = advisories,
            teamChecks = service.myTeamChecks(user).map { teamCheckRow(user, it) },
            reader = access.isReader(user),
            securityTeam = access.isSecurityTeam(user),
            groups = RoleGroups(
                read = config.readGroup.trim(),
                write = config.writeGroup.trim(),
                securityOfficer = config.securityOfficerGroup.trim(),
                management = config.managementGroup.trim(),
            ),
        )
    }

    @AccessChecked("own user only: the confirmation of the session user for a released case")
    @PostMapping("case/{id}/confirm")
    fun confirm(@PathVariable id: Long, @RequestBody request: ConfirmRequest): MyView {
        service.confirm(ThreadLocalUserContext.requiredLoggedInUser, id, request.answer, request.comment)
        return my()
    }

    private fun closureView(user: PFUserDO, case: CvrCaseDO): ClosureView {
        val status = statusService.status(case)
        val securityTeam = access.isSecurityTeam(user)
        val confirmations = status.confirmations
        return ClosureView(
            case = caseRef(case),
            closable = status.closable,
            openItems = status.openItems.map { OpenItem(it.key, it.count) },
            clientsRelevant = status.clientsRelevant,
            devices = DeviceCounts(
                fixed = status.count(CvrCaseStatusService.DeviceState.FIXED),
                open = status.count(CvrCaseStatusService.DeviceState.OPEN),
                confirmed = status.count(CvrCaseStatusService.DeviceState.CONFIRMED),
                noData = status.count(CvrCaseStatusService.DeviceState.NO_DATA),
                inactive = status.inactiveDevices,
            ),
            deviceRows = if (!securityTeam) null else status.devices.map {
                deviceRow(it.device, it.state, it.checkPassed, it.confirmation?.answer)
            },
            teamChecks = status.teamChecks.map { teamCheckRow(user, it) },
            confirmations = ConfirmationCounts(
                done = confirmations.count { it.answer == CvrClientAnswer.DONE },
                notAffected = confirmations.count { it.answer == CvrClientAnswer.NOT_AFFECTED },
                question = confirmations.count { it.answer == CvrClientAnswer.QUESTION },
            ),
            questions = if (!securityTeam) null else confirmations.filter { it.answer == CvrClientAnswer.QUESTION }
                .map { QuestionRow(userGroupCache.getUser(it.userId)?.displayName, it.comment, it.lastUpdate) },
            canEdit = securityTeam,
            canCloseWithOpenItems = access.isSecurityOfficer(user),
        )
    }

    private fun caseRef(case: CvrCaseDO) = CaseRef(
        id = case.id!!,
        code = case.code,
        title = case.title,
        kind = case.kind,
        status = case.status,
        priority = case.priority,
        clientAdvisory = case.clientAdvisory,
        minClientAgentVersion = case.minClientAgentVersion,
        clientAgentCheckId = case.clientAgentCheckId,
        closeReason = case.closeReason,
    )

    private fun deviceRow(
        device: CvrDeviceDO,
        state: CvrCaseStatusService.DeviceState? = null,
        checkPassed: Boolean? = null,
        confirmation: CvrClientAnswer? = null,
    ) = DeviceRow(
        id = device.id!!,
        username = device.username,
        userDisplayName = userGroupCache.getUser(device.userId)?.displayName,
        hostname = device.hostname,
        serialNumber = device.serialNumber,
        agentVersion = device.agentVersion,
        agentVersionAt = device.agentVersionAt,
        lastSeen = device.lastSeen,
        state = state,
        checkPassed = checkPassed,
        confirmation = confirmation,
    )

    /**
     * Who answered and the comment are shown to the security team and to the members of the asked group only; a reader
     * sees the answer without persons (VIS-10).
     */
    private fun teamCheckRow(user: PFUserDO, check: CvrTeamCheckDO): TeamCheckRow {
        // Fetched by the queries of CvrTeamCheckDao, otherwise loaded here.
        val case = check.cvrCase?.takeIf { Hibernate.isInitialized(it) } ?: check.caseId?.let { service.findCase(it) }
        val securityTeamOrMember = access.isSecurityTeam(user) || access.isMemberOf(user, check.groupId)
        return TeamCheckRow(
            id = check.id!!,
            caseId = check.caseId,
            caseCode = case?.code,
            caseTitle = case?.title,
            groupId = check.groupId,
            groupName = userGroupCache.getGroup(check.groupId)?.name,
            subject = check.subject,
            answer = check.answer,
            comment = if (securityTeamOrMember) check.comment else null,
            answerDate = check.answerDate,
            answeredBy = if (securityTeamOrMember) userGroupCache.getUser(check.answeredBy?.id)?.displayName else null,
            lastUpdate = check.lastUpdate,
            canAnswer = case?.status?.isClosed != true && securityTeamOrMember,
        )
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
