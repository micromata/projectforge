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

package org.projectforge.plugins.cveradar

import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.model.CvrCaseDO
import org.projectforge.plugins.cveradar.model.CvrCaseStatus
import org.projectforge.plugins.cveradar.model.CvrClientAnswer
import org.projectforge.plugins.cveradar.model.CvrConfirmationDO
import org.projectforge.plugins.cveradar.model.CvrTeamAnswer
import org.projectforge.plugins.cveradar.model.CvrTeamCheckDO
import org.projectforge.plugins.cveradar.rest.TeamCheckAnswerView
import org.projectforge.plugins.cveradar.rest.TeamCheckFile
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * The actions on cases besides editing them: closing (CLS-05), team checks (CLS-03) and the employees'
 * confirmations (CLS-04). Case, check and group are always taken from the database, the acting user from the session.
 */
@Service
class CveRadarService {
    @Autowired
    private lateinit var access: CveRadarAccess

    @Autowired
    private lateinit var caseDao: CvrCaseDao

    @Autowired
    private lateinit var deviceService: CvrDeviceService

    @Autowired
    private lateinit var statusService: CvrCaseStatusService

    @Autowired
    private lateinit var teamCheckDao: CvrTeamCheckDao

    @Autowired
    private lateinit var teamCheckAnswerService: CvrTeamCheckAnswerService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    /** The case for the security team or a reader, otherwise an [AccessException]. */
    fun getCase(user: PFUserDO, caseId: Long): CvrCaseDO {
        access.checkReader(user)
        return caseDao.find(caseId, checkAccess = false)?.takeIf { !it.deleted }
            ?: throw UserException(ERROR_NOT_FOUND)
    }

    /** The case without access check, e.g. as parent of a team check the user may see. */
    fun findCase(caseId: Long): CvrCaseDO? = caseDao.find(caseId, checkAccess = false)

    /**
     * Throws, if the case may not be closed by the user: a reason is needed, and a case with open items is closed by
     * a security officer only (CLS-05).
     */
    fun checkClosing(user: PFUserDO, case: CvrCaseDO, reason: String?) {
        if (reason.isNullOrBlank()) {
            throw UserException(ERROR_CLOSE_REASON)
        }
        if (!statusService.status(case).closable && !access.isSecurityOfficer(user)) {
            throw UserException(ERROR_CLOSE_OPEN_ITEMS)
        }
    }

    /** Closes the case with the reason (historised by the dao). */
    fun close(user: PFUserDO, caseId: Long, reason: String?): CvrCaseDO {
        access.checkSecurityTeam(user)
        val case = getCase(user, caseId)
        if (case.status?.isClosed == true) {
            // Would replace the documented reason (of the security officer, for example).
            throw UserException(ERROR_ALREADY_CLOSED)
        }
        checkClosing(user, case, reason)
        case.status = CvrCaseStatus.CLOSED
        case.closeReason = reason!!.trim()
        caseDao.update(case)
        return case
    }

    /** [perPerson]: every member answers (e.g. for the own client), otherwise one answer for the group. */
    fun createTeamCheck(
        user: PFUserDO,
        caseId: Long,
        groupId: Long?,
        subject: String?,
        perPerson: Boolean = false,
    ): CvrTeamCheckDO {
        access.checkSecurityTeam(user)
        val case = getCase(user, caseId)
        val group = groupId?.let { userGroupCache.getGroup(it) }?.takeIf { !it.deleted }
            ?: throw UserException(ERROR_GROUP)
        val text = subject?.trim()?.take(255)?.takeIf { it.isNotEmpty() } ?: throw UserException(ERROR_SUBJECT)
        val check = CvrTeamCheckDO().also {
            it.cvrCase = case
            it.group = GroupDO().also { g -> g.id = group.id }
            it.subject = text
            it.perPerson = perPerson
        }
        teamCheckDao.insert(check)
        return check
    }

    fun deleteTeamCheck(user: PFUserDO, checkId: Long) {
        access.checkSecurityTeam(user)
        val check = teamCheckDao.find(checkId, checkAccess = false) ?: throw UserException(ERROR_NOT_FOUND)
        teamCheckDao.markAsDeleted(check)
    }

    /**
     * The answer of a member of the asked group (or of the security team) for the stored check; only the answer
     * fields are taken from the request (SEC-04). If every member answers, the answer is the own one of the user
     * (members only), replacing the own earlier one; otherwise the answer for the group, replacing the earlier one.
     */
    fun answerTeamCheck(
        user: PFUserDO,
        checkId: Long,
        answer: CvrTeamAnswer?,
        comment: String?,
        date: LocalDate?,
    ): CvrTeamCheckDO {
        val check = teamCheckDao.find(checkId, checkAccess = false)?.takeIf { !it.deleted }
            ?: throw UserException(ERROR_NOT_FOUND)
        val member = access.isMemberOf(user, check.groupId)
        if (!member && (check.perPerson || !access.isSecurityTeam(user))) {
            throw AccessException(user, CveRadarAccess.NO_ACCESS)
        }
        val case = caseDao.find(check.caseId, checkAccess = false)
        if (case == null || case.deleted || case.status?.isClosed == true) {
            throw UserException(ERROR_CASE_CLOSED)
        }
        answer ?: throw UserException(ERROR_ANSWER)
        if ((answer == CvrTeamAnswer.NOT_AFFECTED || answer == CvrTeamAnswer.RISK_ACCEPTED) && comment.isNullOrBlank()) {
            throw UserException(ERROR_REASON)
        }
        val text = comment?.trim()?.take(4000)?.takeIf { it.isNotEmpty() }
        if (check.perPerson) {
            teamCheckAnswerService.answer(check, user, answer, text, date)
            if (case.clientAdvisory) {
                syncConfirmation(case.id!!, user.id!!, answer, text)
            }
            return check
        }
        check.answer = answer
        check.comment = text
        check.answerDate = date
        check.answeredBy = PFUserDO().also { it.id = user.id }
        teamCheckDao.update(check, checkAccess = false)
        return check
    }

    /**
     * In a case released to the clients a team check asking every member asks what the employee's own confirmation
     * (CLS-04) asks, so the member's answer is the confirmation too: one answer per person, not two contradicting
     * ones. Fixed confirms as done, not affected as not affected; in progress and risk accepted confirm nothing, so an
     * earlier confirmation is withdrawn.
     */
    private fun syncConfirmation(caseId: Long, userId: Long, answer: CvrTeamAnswer, comment: String?) {
        val clientAnswer = when (answer) {
            CvrTeamAnswer.FIXED -> CvrClientAnswer.DONE
            CvrTeamAnswer.NOT_AFFECTED -> CvrClientAnswer.NOT_AFFECTED
            CvrTeamAnswer.IN_PROGRESS, CvrTeamAnswer.RISK_ACCEPTED -> null
        }
        if (clientAnswer == null) {
            deviceService.withdrawConfirmation(caseId, userId)
        } else {
            deviceService.confirm(caseId, userId, clientAnswer, comment)
        }
    }

    /**
     * The file of a team check (CLS-03): visible to the members of the asked group and the security team only, as the
     * discussion there may name projects and systems. If every member answers, a member sees the own answer, the
     * security team all answers and the members still open.
     */
    fun teamCheckFile(user: PFUserDO, checkId: Long): TeamCheckFile {
        val check = teamCheckDao.find(checkId, checkAccess = false)?.takeIf { !it.deleted }
            ?: throw UserException(ERROR_NOT_FOUND)
        val member = access.isMemberOf(user, check.groupId)
        val securityTeam = access.isSecurityTeam(user)
        if (!securityTeam && !member) {
            throw AccessException(user, CveRadarAccess.NO_ACCESS)
        }
        val case = caseDao.find(check.caseId, checkAccess = false)
        val state = teamCheckAnswerService.state(check)
        val own = state.answerOf(user.id)
        val open = case?.status?.isClosed != true
        return TeamCheckFile(
            id = check.id!!,
            caseId = case?.id,
            caseCode = case?.code,
            caseTitle = case?.title,
            caseStatus = case?.status,
            groupName = userGroupCache.getGroup(check.groupId)?.name,
            subject = check.subject,
            answer = if (check.perPerson) own?.answer else check.answer,
            comment = if (check.perPerson) own?.comment else check.comment,
            answerDate = if (check.perPerson) own?.answerDate else check.answerDate,
            answeredBy = if (check.perPerson) {
                own?.let { user.displayName }
            } else {
                userGroupCache.getUser(check.answeredBy?.id)?.displayName
            },
            canAnswer = open && (member || (!check.perPerson && securityTeam)),
            perPerson = check.perPerson,
            members = state.memberIds.size,
            answered = state.answered,
            openMembers = state.openMemberIds.size,
            answers = if (check.perPerson && securityTeam) {
                state.answers.sortedByDescending { it.lastUpdate }.map {
                    TeamCheckAnswerView(
                        user = userGroupCache.getUser(it.userId)?.displayName,
                        answer = it.answer,
                        comment = it.comment,
                        answerDate = it.answerDate,
                        lastUpdate = it.lastUpdate,
                    )
                }
            } else null,
            missing = if (check.perPerson && securityTeam) {
                state.openMemberIds.filter { state.answerOf(it) == null }
                    .mapNotNull { userGroupCache.getUser(it)?.displayName }.sorted()
            } else null,
        )
    }

    /** The open team checks of the user's groups (not of closed cases). */
    fun myTeamChecks(user: PFUserDO): List<CvrTeamCheckDO> {
        val groupIds = userGroupCache.getUserGroups(user) ?: return emptyList()
        return teamCheckDao.selectByGroups(groupIds, openCasesOnly = true)
    }

    /** The cases released to the clients and not closed: those every employee sees (SEC-03). */
    fun releasedCases(): List<CvrCaseDO> {
        return caseDao.select(QueryFilter(), checkAccess = false)
            .filter { !it.deleted && it.clientAdvisory && it.status?.isClosed != true }
            .sortedByDescending { it.created }
    }

    /** The confirmation of the logged-in user for a released case. */
    fun confirm(user: PFUserDO, caseId: Long, answer: CvrClientAnswer?, comment: String?): CvrConfirmationDO {
        val case = caseDao.find(caseId, checkAccess = false)
        if (case == null || case.deleted || !case.clientAdvisory || case.status?.isClosed == true) {
            throw UserException(ERROR_NOT_FOUND)
        }
        answer ?: throw UserException(ERROR_ANSWER)
        if (answer == CvrClientAnswer.QUESTION && comment.isNullOrBlank()) {
            throw UserException(ERROR_QUESTION)
        }
        return deviceService.confirm(caseId, user.id!!, answer, comment)
    }

    companion object {
        const val ERROR_NOT_FOUND = "plugins.cveradar.error.notFound"
        const val ERROR_CLOSE_REASON = "plugins.cveradar.closure.error.reason"
        const val ERROR_ALREADY_CLOSED = "plugins.cveradar.closure.error.alreadyClosed"
        const val ERROR_CLOSE_OPEN_ITEMS = "plugins.cveradar.closure.error.openItems"
        const val ERROR_GROUP = "plugins.cveradar.teamCheck.error.group"
        const val ERROR_SUBJECT = "plugins.cveradar.teamCheck.error.subject"
        const val ERROR_ANSWER = "plugins.cveradar.error.answer"
        const val ERROR_REASON = "plugins.cveradar.teamCheck.error.reason"
        const val ERROR_CASE_CLOSED = "plugins.cveradar.teamCheck.error.caseClosed"
        const val ERROR_QUESTION = "plugins.cveradar.my.error.question"
    }
}
