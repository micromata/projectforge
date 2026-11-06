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

import org.projectforge.business.notification.NotificationDO
import org.projectforge.business.notification.NotificationLinks
import org.projectforge.business.notification.NotificationRuleDO
import org.projectforge.business.notification.NotificationRuleHandler
import org.projectforge.business.notification.NotificationRecipientMode
import org.projectforge.business.notification.NotificationRuleParam
import org.projectforge.business.notification.NotificationStatus
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.model.CvrCaseDO
import org.projectforge.plugins.cveradar.model.CvrTeamCheckDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * Reminder of the team checks of a case asking every member ([CvrTeamCheckDO.perPerson]): the members who haven't
 * answered yet, as the file of a team check lists them for the security team ("not answered yet": active members
 * without any answer, an answer in progress counts as answered). Only they are notified, the configured recipients of
 * the rule aren't used (the rule editors, admins and finance members, see the open members in the preview and the
 * log). Checks answered for the group are left out, they have no open persons.
 *
 * A recipient is reminded at most once a day (the schedule of the rule decides how often). A notification is resolved
 * as soon as its recipient answered all checks of the case, it expires when the case is closed (or the rule refers to
 * another case now) and with a newer reminder.
 */
@Service
class CvrTeamCheckUnansweredRuleHandler : NotificationRuleHandler {
    @Autowired
    private lateinit var answerService: CvrTeamCheckAnswerService

    @Autowired
    private lateinit var caseDao: CvrCaseDao

    @Autowired
    private lateinit var notificationLinks: NotificationLinks

    @Autowired
    private lateinit var teamCheckDao: CvrTeamCheckDao

    @Autowired
    private lateinit var access: CveRadarAccess

    override val ruleType = TYPE

    override val isAvailable: Boolean
        get() = access.isPluginActive()

    override val order = 90

    override val params = listOf(CASE)

    override val recipientMode = NotificationRecipientMode.AFFECTED_ONLY

    override val recipientsHintKey = "notification.params.cvrTeamCheckRecipientsInfo"

    override val templateSubjectKey = "notification.template.teamCheckUnansweredSubject"

    override val templateTextKey = "notification.template.teamCheckUnansweredText"

    override val variables = listOf(
        "cvrCaseCode", "cvrCaseTitle", "cvrTeamCheckCount", "cvrTeamCheckList", "cvrTeamCheckLink",
    )

    override fun evaluate(rule: NotificationRuleDO, today: LocalDate): NotificationRuleHandler.Evaluation? {
        val case = openCase(rule) ?: return null
        val missing = missing(case)
        if (missing.isEmpty()) {
            return null
        }
        return NotificationRuleHandler.Evaluation(
            periodKey(case.id!!, today),
            missing.mapValues { (_, checks) -> variables(case, checks) },
            MY_PATH,
            affectedAreRecipients = true,
        )
    }

    /** The checks of the case the user hasn't answered (none, if the user isn't asked, e.g. a test to oneself). */
    override fun variables(rule: NotificationRuleDO, today: LocalDate, user: PFUserDO): Map<String, String> {
        val case = case(rule) ?: return emptyMap()
        return variables(case, missing(case)[user.id].orEmpty())
    }

    override fun exampleVariables(rule: NotificationRuleDO, today: LocalDate, user: PFUserDO): Map<String, String> {
        val case = case(rule) ?: CvrCaseDO().also {
            it.code = "CVE-2026-12345"
            it.title = "Apache Tomcat: remote code execution"
        }
        val checks = listOf("Own client", "Build server").map { subject ->
            CvrTeamCheckDO().also { it.id = 1; it.subject = subject }
        }
        return variables(case, checks)
    }

    override fun paramDisplayNames(rule: NotificationRuleDO): Map<String, String> {
        val case = case(rule) ?: return emptyMap()
        return mapOf(CASE.key to CvrCaseDao.label(case))
    }

    /** The case must exist and be open (a closed one has nothing to answer any more). */
    override fun paramErrors(rule: NotificationRuleDO): Map<String, String> {
        CASE.of(rule) ?: return emptyMap() // Required, checked by the param.
        val case = case(rule) ?: return mapOf(CASE.key to ERROR_CASE_NOT_FOUND)
        return if (case.status?.isClosed == true) mapOf(CASE.key to ERROR_CASE_CLOSED) else emptyMap()
    }

    /**
     * Resolved, as soon as the recipient answered all checks of the case; expired with the case closed (or another case
     * chosen in the rule) and with a newer reminder of the recipient.
     */
    override fun resolve(
        rule: NotificationRuleDO,
        notifications: List<NotificationDO>,
        today: LocalDate,
    ): Map<Long, NotificationStatus> {
        val case = openCase(rule)
        val missing = case?.let { missing(it) }.orEmpty()
        val result = mutableMapOf<Long, NotificationStatus>()
        notifications.groupBy { it.recipientId }.forEach { (recipientId, list) ->
            val newest = list.maxBy { it.id ?: 0 }
            list.forEach {
                result[it.id!!] = when {
                    case == null || caseId(it.periodKey) != case.id -> NotificationStatus.EXPIRED
                    recipientId !in missing -> NotificationStatus.RESOLVED
                    it !== newest -> NotificationStatus.EXPIRED
                    else -> return@forEach
                }
            }
        }
        return result
    }

    private fun variables(case: CvrCaseDO, checks: List<CvrTeamCheckDO>): Map<String, String> {
        val link = checks.singleOrNull()?.id?.let { "$TEAM_CHECK_PATH/$it" } ?: MY_PATH
        return mapOf(
            "cvrCaseCode" to (case.code ?: "–"),
            "cvrCaseTitle" to (case.title ?: "–"),
            "cvrTeamCheckCount" to checks.size.toString(),
            "cvrTeamCheckList" to checks.mapNotNull { it.subject }.sorted().joinToString("; ").ifEmpty { "–" },
            "cvrTeamCheckLink" to notificationLinks.absolute(link),
        )
    }

    /** The members not answered yet by id, with the checks (per person) of the case they haven't answered. */
    private fun missing(case: CvrCaseDO): Map<Long, List<CvrTeamCheckDO>> {
        val checks = teamCheckDao.selectByCase(case.id!!).filter { it.perPerson }
        val result = mutableMapOf<Long, MutableList<CvrTeamCheckDO>>()
        answerService.states(checks).forEach { state ->
            state.openMemberIds.filter { state.answerOf(it) == null }.forEach { userId ->
                result.getOrPut(userId) { mutableListOf() } += state.check
            }
        }
        return result
    }

    private fun case(rule: NotificationRuleDO): CvrCaseDO? {
        val id = CASE.of(rule) ?: return null
        return caseDao.find(id, checkAccess = false)?.takeIf { !it.deleted }
    }

    /** The case of the rule, null if it's closed (nothing to answer any more). */
    private fun openCase(rule: NotificationRuleDO): CvrCaseDO? = case(rule)?.takeIf { it.status?.isClosed != true }

    companion object {
        const val TYPE = "CVERADAR_TEAM_CHECK_UNANSWERED"

        /** The case whose team checks are reminded of. */
        val CASE = NotificationRuleParam.EntityParam(
            "cvrCaseId", CveRadarPlugin.CASE_CATEGORY, i18nKey = "notification.params.cvrCase",
            hintKey = "notification.params.cvrCaseInfo",
        )

        /** The own team checks of the user. */
        const val MY_PATH = "/cveradar/my"

        /** The file of a team check. */
        const val TEAM_CHECK_PATH = "/cveradar/teamcheck"

        const val ERROR_CASE_NOT_FOUND = "notification.rule.error.cvrCaseNotFound"

        const val ERROR_CASE_CLOSED = "notification.rule.error.cvrCaseClosed"

        private const val PERIOD_PREFIX = "teamcheck:"

        internal fun periodKey(caseId: Long, today: LocalDate) = "$PERIOD_PREFIX$caseId:$today"

        internal fun caseId(periodKey: String?): Long? =
            periodKey?.removePrefix(PERIOD_PREFIX)?.substringBefore(':')?.toLongOrNull()
    }
}
