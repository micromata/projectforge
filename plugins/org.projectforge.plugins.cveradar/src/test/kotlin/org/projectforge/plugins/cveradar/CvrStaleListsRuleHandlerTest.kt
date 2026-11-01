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
import org.projectforge.business.notification.NotificationStatus
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.rest.AreaRequest
import org.projectforge.plugins.cveradar.rest.ComponentRequest
import org.projectforge.plugins.cveradar.rest.CvrInventoryRest
import org.projectforge.plugins.cveradar.rest.InstanceRequest
import org.projectforge.plugins.cveradar.rest.SystemRequest
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

/**
 * The notification about outdated lists of libraries: the responsible team and the recipients in copy, each one told
 * only of the instances visible to them; resolved as soon as the list is updated.
 */
class CvrStaleListsRuleHandlerTest : CveRadarTestBase() {
    @Autowired
    private lateinit var handler: CvrStaleListsRuleHandler

    @Autowired
    private lateinit var inventory: CvrInventoryRest

    @Autowired
    private lateinit var notificationRuleDao: NotificationRuleDao

    @Autowired
    private lateinit var notificationService: NotificationService

    private fun rule(): NotificationRuleDO {
        val rule = NotificationRuleDO().also {
            it.name = "Outdated lists"
            it.active = true
            it.ruleType = NotificationRuleType.CVERADAR_STALE_LISTS
            it.params = JsonUtils.toJson(NotificationParams(cvrMaxAgeDays = 30))
            it.recipients = JsonUtils.toJson(NotificationRecipients())
            it.delivery = JsonUtils.toJson(NotificationDelivery(listOf(NotificationDeliveryStep(NotificationChannel.IN_APP))))
            it.subject = "{{cvrStaleCount}} outdated"
            it.text = "<p>{{cvrStaleList}}</p><p>{{cvrMaxAgeDays}}</p>"
        }
        notificationRuleDao.insert(rule, checkAccess = false)
        return rule
    }

    private fun setDate(componentId: Long, date: LocalDate?) {
        persistenceService.runInTransaction { context ->
            context.em.createQuery("update CvrComponentDO c set c.librariesDate = :date where c.id = :id")
                .setParameter("date", date).setParameter("id", componentId).executeUpdate()
        }
    }

    private fun notifications(rule: NotificationRuleDO, user: PFUserDO): List<NotificationDO> =
        persistenceService.executeQuery(
            "from NotificationDO n where n.ruleId = :ruleId and n.recipientId = :userId order by n.id",
            NotificationDO::class.java,
            Pair("ruleId", rule.id), Pair("userId", user.id),
        )

    @Test
    fun `the responsible team is told of the outdated lists they see, and it is resolved by an update`() {
        val owner = user("owner-s1", CvrRole.READER)
        val noReader = user("noreader-s1", null)
        val outsider = user("outsider-s1", CvrRole.READER)
        listOf(owner, noReader).forEach { addToGroup(it, "cveradar-stale-g1") }
        logon(owner)
        val groupId = initTestDB.getGroup("cveradar-stale-g1")!!.id!!
        val area = inventory.createArea(AreaRequest(code = "SA1", name = "Area SA1", groupId = groupId))
        val system = inventory.createSystem(SystemRequest(areaId = area, code = "SA1-S", name = "Billing"))
        inventory.createInstance(InstanceRequest(systemId = system, code = "PROD"))
        fun component(code: String, name: String) =
            inventory.createComponent(ComponentRequest(systemId = system, code = code, name = name, technologies = emptyList()))
        val old = component("OLD", "Old list")
        val fresh = component("NEW", "Fresh list")
        val never = component("NONE", "Never listed")
        setDate(old, LocalDate.now().minusDays(45))
        setDate(fresh, LocalDate.now().minusDays(5))
        setDate(never, null)

        val rule = rule()
        notificationService.trigger(rule)
        val list = notifications(rule, owner)
        assertEquals(1, list.size)
        assertEquals("1 outdated", list.single().title)
        val body = list.single().body!!
        assertTrue(body.contains("Billing (PROD) / Old list (${LocalDate.now().minusDays(45)})"), body)
        assertFalse(body.contains("Fresh list") || body.contains("Never listed"), body)
        listOf(noReader, outsider).forEach { assertTrue(notifications(rule, it).isEmpty(), "${it.username} is not notified") }

        notificationService.trigger(rule)
        assertEquals(1, notifications(rule, owner).size, "once a week")

        assertTrue(handler.resolve(rule, list, LocalDate.now()).isEmpty(), "still outdated")
        setDate(old, LocalDate.now())
        assertEquals(NotificationStatus.RESOLVED, handler.resolve(rule, list, LocalDate.now())[list.single().id])
    }
}
