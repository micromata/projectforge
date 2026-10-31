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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.business.notification.NotificationChannel
import org.projectforge.business.notification.NotificationDO
import org.projectforge.business.notification.NotificationDelivery
import org.projectforge.business.notification.NotificationDeliveryStep
import org.projectforge.business.notification.NotificationParams
import org.projectforge.business.notification.NotificationRecipients
import org.projectforge.business.notification.NotificationRuleDO
import org.projectforge.business.notification.NotificationRuleDao
import org.projectforge.business.notification.NotificationRuleType
import org.projectforge.business.notification.NotificationService
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.model.CvrImportStateDO
import org.projectforge.plugins.cveradar.model.CvrTechnologyKind
import org.projectforge.plugins.cveradar.rest.AreaRequest
import org.projectforge.plugins.cveradar.rest.ComponentRequest
import org.projectforge.plugins.cveradar.rest.CvrInventoryRest
import org.projectforge.plugins.cveradar.rest.InstanceRequest
import org.projectforge.plugins.cveradar.rest.SystemRequest
import org.projectforge.plugins.cveradar.rest.TechnologyRequest
import org.projectforge.plugins.cveradar.sync.NvdCveParser
import org.projectforge.plugins.cveradar.sync.NvdCveParserTest
import org.projectforge.plugins.cveradar.sync.NvdCveParserTest.Companion.cve
import org.projectforge.plugins.cveradar.sync.NvdImportService
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

/**
 * The notification of new findings (NOT-01, NOT-02): the responsible team and the recipients in copy, each one told
 * only of the systems visible to them (VIS-07), from the severity of the rule on, every finding once.
 */
class CvrFindingsRuleHandlerTest : CveRadarTestBase() {
    @Autowired
    private lateinit var importService: NvdImportService

    @Autowired
    private lateinit var inventory: CvrInventoryRest

    @Autowired
    private lateinit var notificationRuleDao: NotificationRuleDao

    @Autowired
    private lateinit var notificationService: NotificationService

    private var clock = 0L

    @BeforeEach
    fun resetImportState() {
        clock = 0L
        persistenceService.runInTransaction { context ->
            context.em.find(CvrImportStateDO::class.java, NvdImportService.SOURCE)?.let { context.em.remove(it) }
        }
    }

    /** A CVE of the product before 9.0.90 with the given score and severity. */
    private fun import(cveId: String, product: String, score: String, severity: String) {
        logon(TEST_ADMIN_USER)
        val configurations = """{"nodes":[{"operator":"OR","negate":false,"cpeMatch":[
            {"vulnerable":true,"criteria":"cpe:2.3:a:apache:$product:*:*:*:*:*:*:*:*","versionEndExcluding":"9.0.90"}]}]}"""
        val metrics = """"cvssMetricV31":[{"type":"Primary","cvssData":{"vectorString":"CVSS:3.1/x","baseScore":$score,"baseSeverity":"$severity"}}]"""
        importService.import(
            { _, _, _ -> NvdCveParser.parsePage(NvdCveParserTest.page(cve(cveId, metrics = metrics, configurations = configurations))) },
            Instant.parse("2026-10-28T10:00:00Z").plusSeconds(clock++),
        )
    }

    private fun rule(ccUserIds: List<Long>): NotificationRuleDO {
        val rule = NotificationRuleDO().also {
            it.name = "New findings"
            it.active = true
            it.ruleType = NotificationRuleType.CVERADAR_FINDINGS
            it.params = JsonUtils.toJson(NotificationParams(cvrMinSeverity = "HIGH"))
            it.recipients = JsonUtils.toJson(NotificationRecipients(userIds = ccUserIds))
            it.delivery = JsonUtils.toJson(NotificationDelivery(listOf(NotificationDeliveryStep(NotificationChannel.IN_APP))))
            it.subject = "{{cvrVulnerabilityCount}} new: {{cvrMaxSeverity}}"
            it.text = "<p>{{cvrVulnerabilityList}}</p><p>{{cvrSystemList}}</p>"
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
    fun `the responsible team and the recipients in copy are notified of the systems they see, every finding once`() {
        val p = "tomcatn1"
        val owner = user("owner-n1", CvrRole.READER)
        val member = user("member-n1", CvrRole.READER)
        val noReader = user("noreader-n1", null)
        val outsider = user("outsider-n1", CvrRole.READER)
        val team = user("team-n1", CvrRole.SECURITY_TEAM) // In copy, reads the internal systems.
        val blind = user("blind-n1", CvrRole.READER) // In copy, sees no system.
        listOf(owner, member, noReader).forEach { addToGroup(it, "cveradar-notify-g1") }
        logon(owner)
        val groupId = initTestDB.getGroup("cveradar-notify-g1")!!.id!!
        val area = inventory.createArea(AreaRequest(code = "NA1", name = "Area NA1", groupId = groupId))
        val system = inventory.createSystem(SystemRequest(areaId = area, code = "NA1-S", name = "Shop"))
        inventory.createInstance(InstanceRequest(systemId = system, code = "PROD"))
        fun tomcat(withCpe: Boolean = true) = TechnologyRequest(
            kind = CvrTechnologyKind.PRODUCT, vendor = "apache", product = "Tomcatn1", version = "9.0.80",
            cpe = if (withCpe) "cpe:2.3:a:apache:$p:9.0.80:*:*:*:*:*:*:*" else null,
        )
        inventory.createComponent(ComponentRequest(systemId = system, code = "APP", name = "App", technologies = listOf(tomcat())))
        // Only possible (by name): not notified by default.
        inventory.createComponent(ComponentRequest(systemId = system, code = "NAME", name = "By name", technologies = listOf(tomcat(false))))
        import("CVE-2026-95001", p, "9.8", "CRITICAL")
        import("CVE-2026-95002", p, "5.0", "MEDIUM") // Below the severity of the rule.

        val rule = rule(listOf(team.id!!, blind.id!!))
        notificationService.trigger(rule)
        listOf(owner, member, team).forEach { user ->
            val list = notifications(rule, user)
            assertEquals(1, list.size, "${user.username} is notified once")
            if (user !== team) {
                // The security team sees the internal systems of the other tests too (the database is shared).
                assertEquals("1 new: CRITICAL", list.single().title)
            }
            val body = list.single().body!!
            assertTrue(body.contains("CVE-2026-95001 (CRITICAL 9.8)"), body)
            assertTrue(body.contains("NA1-S Shop (PROD)"), body)
            assertFalse(body.contains("CVE-2026-95002"), "below the severity")
        }
        listOf(noReader, outsider, blind).forEach { assertTrue(notifications(rule, it).isEmpty(), "${it.username} is not notified") }

        notificationService.trigger(rule)
        assertEquals(1, notifications(rule, owner).size, "nothing new, nothing notified")

        import("CVE-2026-95003", p, "8.1", "HIGH")
        notificationService.trigger(rule)
        val list = notifications(rule, owner)
        assertEquals(2, list.size)
        assertTrue(list.last().body!!.contains("CVE-2026-95003 (HIGH 8.1)"))
        assertFalse(list.last().body!!.contains("CVE-2026-95001"), "notified before")
        assertEquals(2, notifications(rule, team).size)
    }
}
