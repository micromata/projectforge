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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.rest.notification.NotificationRule
import org.projectforge.rest.notification.NotificationRuleEntityRest
import org.projectforge.ui.ValidationError
import org.projectforge.business.notification.NotificationChannel
import org.projectforge.business.notification.NotificationDO
import org.projectforge.business.notification.NotificationDelivery
import org.projectforge.business.notification.NotificationDeliveryStep
import org.projectforge.business.notification.ManualRuleHandler
import org.projectforge.business.notification.NotificationParams
import org.projectforge.business.notification.NotificationRecipientMode
import org.projectforge.business.notification.NotificationRuleParam
import org.projectforge.business.notification.NotificationRecipients
import org.projectforge.business.notification.NotificationRuleDO
import org.projectforge.business.notification.NotificationRuleDao
import org.projectforge.business.notification.NotificationService
import org.projectforge.business.notification.NotificationStatus
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.model.CvrCaseDO
import org.projectforge.plugins.cveradar.model.CvrCaseKind
import org.projectforge.plugins.cveradar.model.CvrCaseStatus
import org.projectforge.plugins.cveradar.model.CvrTeamAnswer
import org.projectforge.plugins.cveradar.rest.CveRadarRest
import org.projectforge.plugins.cveradar.rest.TeamCheckAnswerRequest
import org.projectforge.plugins.cveradar.rest.TeamCheckRequest
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.Date

/**
 * The reminder of the team checks of a case: only the members who haven't answered a check asking every member yet,
 * resolved by their answer, expired with the case closed.
 */
class CvrTeamCheckUnansweredRuleHandlerTest : CveRadarTestBase() {
    @Autowired
    private lateinit var caseDao: CvrCaseDao

    @Autowired
    private lateinit var handler: CvrTeamCheckUnansweredRuleHandler

    @Autowired
    private lateinit var notificationRuleDao: NotificationRuleDao

    @Autowired
    private lateinit var notificationService: NotificationService

    @Autowired
    private lateinit var rest: CveRadarRest

    @Autowired
    private lateinit var ruleRest: NotificationRuleEntityRest

    private fun rule(caseId: Long?, recipients: NotificationRecipients = NotificationRecipients()): NotificationRuleDO {
        val rule = NotificationRuleDO().also {
            it.name = "Unanswered team checks"
            it.active = true
            it.ruleType = CvrTeamCheckUnansweredRuleHandler.TYPE
            it.params = JsonUtils.toJson(NotificationParams.of("cvrCaseId" to caseId))
            it.recipients = JsonUtils.toJson(recipients)
            it.delivery = JsonUtils.toJson(NotificationDelivery(listOf(NotificationDeliveryStep(NotificationChannel.IN_APP))))
            it.subject = "{{cvrCaseCode}} missing"
            it.text = "<p>{{cvrTeamCheckCount}}: {{cvrTeamCheckList}}</p>"
        }
        notificationRuleDao.insert(rule, checkAccess = false)
        return rule
    }

    private fun notifications(rule: NotificationRuleDO, user: PFUserDO): List<NotificationDO> =
        persistenceService.executeQuery(
            "from NotificationDO n where n.ruleId = :ruleId and n.recipientId = :userId order by n.id",
            NotificationDO::class.java,
            Pair("ruleId", rule.id), Pair("userId", user.id),
        )

    @Test
    fun `the members not answered yet are reminded, resolved by their answer and expired with the closed case`() {
        val st = user("st-u1", CvrRole.SECURITY_TEAM)
        val answered = user("answered-u1", null)
        val inProgress = user("progress-u1", null)
        val open = user("open-u1", null)
        val away = user("away-u1", null, Date(System.currentTimeMillis() - 100L * 24 * 3600 * 1000))
        val groupMember = user("group-u1", null)
        listOf(answered, inProgress, open, away).forEach { addToGroup(it, "cveradar-unanswered-g1") }
        addToGroup(groupMember, "cveradar-unanswered-g2")
        logon(st)
        val case = CvrCaseDO().also {
            it.title = "Reminder"
            it.kind = CvrCaseKind.INCIDENT
            it.status = CvrCaseStatus.NEW
        }
        caseDao.insert(case)
        val perPerson = rest.createTeamCheck(
            case.id!!, TeamCheckRequest(getGroupId("cveradar-unanswered-g1"), "Own client", perPerson = true),
        ).teamChecks.single().id
        rest.createTeamCheck(case.id!!, TeamCheckRequest(getGroupId("cveradar-unanswered-g2"), "Project Q"))
        logon(answered)
        rest.answerTeamCheck(perPerson, TeamCheckAnswerRequest(CvrTeamAnswer.FIXED, "version 2.1"))
        logon(inProgress)
        rest.answerTeamCheck(perPerson, TeamCheckAnswerRequest(CvrTeamAnswer.IN_PROGRESS, "next week"))

        // Configured recipients aren't used: only the members not answered yet.
        val rule = rule(case.id, NotificationRecipients(userIds = listOf(st.id!!)))
        notificationService.trigger(rule)
        val list = notifications(rule, open)
        assertEquals(1, list.size)
        assertEquals("${case.code} missing", list.single().title)
        assertTrue(list.single().body!!.contains("1: Own client"), list.single().body)
        listOf(st, answered, inProgress, away, groupMember).forEach {
            assertTrue(notifications(rule, it).isEmpty(), "${it.username} is not notified")
        }
        notificationService.trigger(rule)
        assertEquals(1, notifications(rule, open).size, "once a day")

        assertTrue(handler.resolve(rule, list, LocalDate.now()).isEmpty(), "still not answered")
        logon(open)
        rest.answerTeamCheck(perPerson, TeamCheckAnswerRequest(CvrTeamAnswer.NOT_AFFECTED, "no such client"))
        assertEquals(NotificationStatus.RESOLVED, handler.resolve(rule, list, LocalDate.now())[list.single().id])
        assertNull(handler.evaluate(rule, LocalDate.now()), "nobody missing any more")

        persistenceService.runInTransaction { context ->
            context.em.createQuery("update CvrCaseDO c set c.status = :status where c.id = :id")
                .setParameter("status", CvrCaseStatus.CLOSED).setParameter("id", case.id).executeUpdate()
        }
        assertEquals(NotificationStatus.EXPIRED, handler.resolve(rule, list, LocalDate.now())[list.single().id])
    }

    @Test
    fun `a rule without a case notifies nobody`() {
        assertNull(handler.evaluate(rule(null), LocalDate.now()))
    }

    @Test
    fun `a rule editor sees the members not answered yet, the case must be open`() {
        val st = user("st-u2", CvrRole.SECURITY_TEAM)
        val open = user("open-u2", null)
        addToGroup(open, "cveradar-unanswered-g3")
        logon(st)
        val case = CvrCaseDO().also {
            it.title = "Editor"
            it.kind = CvrCaseKind.INCIDENT
            it.status = CvrCaseStatus.NEW
        }
        caseDao.insert(case)
        rest.createTeamCheck(case.id!!, TeamCheckRequest(getGroupId("cveradar-unanswered-g3"), "Laptop", perPerson = true))
        val rule = rule(case.id)
        val dto = NotificationRule().also { it.copyFrom(rule) }

        // A finance member, not in the security team: the rule editors may see the persons.
        logon(TEST_FINANCE_USER)
        assertEquals(listOf(open.getFullname()), ruleRest.preview(dto).recipients)
        assertEquals(CvrCaseDao.label(case), (ruleRest.transformFromDB(rule).params["cvrCaseId"] as Map<*, *>)["displayName"])
        assertTrue(errors(dto).isEmpty())
        dto.params = emptyMap()
        assertEquals(listOf("params.cvrCaseId"), errors(dto), "case required")
        dto.params = mapOf("cvrCaseId" to mapOf("id" to -1))
        assertEquals(listOf("params.cvrCaseId"), errors(dto), "case not found")
        dto.params = mapOf("cvrCaseId" to case.id)
        persistenceService.runInTransaction { context ->
            context.em.createQuery("update CvrCaseDO c set c.status = :status where c.id = :id")
                .setParameter("status", CvrCaseStatus.CLOSED).setParameter("id", case.id).executeUpdate()
        }
        assertEquals(listOf("params.cvrCaseId"), errors(dto), "case closed")
    }

    @Test
    fun `the rule types are described for the rule editors only`() {
        logon(TEST_USER)
        assertThrows(AccessException::class.java) { ruleRest.getRuleTypes() }
        logon(TEST_FINANCE_USER)
        val type = ruleRest.getRuleTypes().single { it.key == CvrTeamCheckUnansweredRuleHandler.TYPE }
        assertEquals(NotificationRecipientMode.AFFECTED_ONLY, type.recipientMode)
        val case = type.params.single()
        assertEquals("cvrCaseId", case.key)
        assertEquals(NotificationRuleParam.Type.ENTITY, case.type)
        assertEquals(CveRadarPlugin.CASE_CATEGORY, case.entity)
        assertTrue(type.templateSubject!!.contains("{{cvrCaseCode}}"), type.templateSubject)
        val types = ruleRest.getRuleTypes().map { it.key }
        assertTrue(types.indexOf(CvrFindingsRuleHandler.TYPE) < types.indexOf(ManualRuleHandler.TYPE), "$types")
    }

    private fun errors(dto: NotificationRule): List<String?> =
        mutableListOf<ValidationError>().also { ruleRest.validate(it, dto) }.map { it.fieldId }
}
