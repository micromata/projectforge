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

import jakarta.persistence.Tuple
import org.projectforge.business.notification.NotificationDO
import org.projectforge.business.notification.NotificationLinks
import org.projectforge.business.notification.NotificationRecipientResolver
import org.projectforge.business.notification.NotificationRuleDO
import org.projectforge.business.notification.NotificationRuleHandler
import org.projectforge.business.notification.NotificationRuleType
import org.projectforge.business.notification.NotificationStatus
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.model.CvrComponentDO
import org.projectforge.plugins.cveradar.model.CvrOrigin
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.IsoFields

/**
 * Outdated lists of libraries of the CVE radar: the findings of a component are only as good as its list of libraries
 * (INV-26). Where the last list was applied more than [org.projectforge.business.notification.NotificationParams.cvrMaxAgeDays]
 * days ago, the responsible team of the affected instances (the group of the system or of the nearest area above) is
 * told, the configured recipients of the rule in copy. A recipient is told only of the instances they may see (VIS-07),
 * at most once a week. A component without a list from a file is left out: nothing is outdated there.
 *
 * The day of the last list is [CvrComponentDO.librariesDate], for lists applied before it was recorded the newest
 * day a library of the component was added. A notification is resolved as soon as nothing outdated is left for its
 * recipient.
 */
@Service
class CvrStaleListsRuleHandler : NotificationRuleHandler {
    @Autowired
    private lateinit var access: CveRadarAccess

    @Autowired
    private lateinit var inventoryService: CvrInventoryService

    @Autowired
    private lateinit var notificationLinks: NotificationLinks

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var recipientResolver: NotificationRecipientResolver

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    override val ruleType = NotificationRuleType.CVERADAR_STALE_LISTS

    override val variables = listOf("cvrStaleCount", "cvrStaleList", "cvrMaxAgeDays", "cvrListLink")

    /** A component with an outdated list. */
    internal class Stale(val componentId: Long, val systemId: Long?, val instanceId: Long?, val name: String, val date: LocalDate)

    /** An outdated list on an instance (a component of a system is on every instance of it). */
    internal class Row(val stale: Stale, val instanceId: Long)

    override fun evaluate(rule: NotificationRuleDO, today: LocalDate): NotificationRuleHandler.Evaluation? {
        val rows = rows(stale(rule, today))
        if (rows.isEmpty()) {
            return null
        }
        val groups = inventoryService.responsibleGroups()
        val groupIds = rows.mapNotNull { groups[it.instanceId] }.toSet()
        val userIds = userGroupCache.getUserGroupIdMap().filterValues { ids -> ids.any { it in groupIds } }.keys +
                recipientResolver.resolve(rule.readRecipients(), null).mapNotNull { it.id }
        val users = recipientResolver.users(userIds).filter { access.isReader(it) }
        val readable = inventoryService.readableInstances(users)
        val affected = mutableMapOf<Long, Map<String, String>>()
        users.forEach { user ->
            val instances = readable[user.id].orEmpty()
            val own = rows.filter { it.instanceId in instances }
            if (own.isNotEmpty()) {
                affected[user.id!!] = variables(rule, own, instances)
            }
        }
        if (affected.isEmpty()) {
            return null
        }
        return NotificationRuleHandler.Evaluation(
            "stale:${today.get(IsoFields.WEEK_BASED_YEAR)}-W${today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}",
            affected, LINK_PATH, affectedAreRecipients = true,
        )
    }

    /** All outdated lists the user may see (e.g. a test to oneself). */
    override fun variables(rule: NotificationRuleDO, today: LocalDate, user: PFUserDO): Map<String, String> {
        val instances = inventoryService.readableInstances(user)
        return variables(rule, rows(stale(rule, today)).filter { it.instanceId in instances }, instances)
    }

    override fun exampleVariables(rule: NotificationRuleDO, today: LocalDate, user: PFUserDO): Map<String, String> {
        val days = rule.readParams().cvrMaxAgeDays
        return mapOf(
            "cvrStaleCount" to "2",
            "cvrStaleList" to "WEB Shop (PROD) / App (${today.minusDays(days + 12L)}); WEB Shop / Database (${today.minusDays(days + 40L)})",
            "cvrMaxAgeDays" to days.toString(),
            "cvrListLink" to notificationLinks.absolute(LINK_PATH),
        )
    }

    override fun isRelevant(rule: NotificationRuleDO, today: LocalDate, user: PFUserDO): Boolean = access.isReader(user)

    /** Resolved, as soon as nothing outdated is left for the recipient; an older notification expires with a newer one. */
    override fun resolve(
        rule: NotificationRuleDO,
        notifications: List<NotificationDO>,
        today: LocalDate,
    ): Map<Long, NotificationStatus> {
        val rows = rows(stale(rule, today))
        val users = recipientResolver.users(notifications.mapNotNull { it.recipientId })
        val readable = inventoryService.readableInstances(users)
        val result = mutableMapOf<Long, NotificationStatus>()
        notifications.groupBy { it.recipientId }.forEach { (recipientId, list) ->
            val instances = readable[recipientId].orEmpty()
            val open = rows.any { it.instanceId in instances }
            val newest = list.maxBy { it.id ?: 0 }
            list.forEach {
                if (!open) result[it.id!!] = NotificationStatus.RESOLVED
                else if (it !== newest) result[it.id!!] = NotificationStatus.EXPIRED
            }
        }
        return result
    }

    private fun variables(
        rule: NotificationRuleDO,
        rows: List<Row>,
        labels: Map<Long, CvrInventoryService.InstanceLabel>,
    ): Map<String, String> {
        val byComponent = rows.groupBy { it.stale.componentId }.values.sortedBy { it.first().stale.date }
        val list = byComponent.take(MAX_LISTED).map { group ->
            val stale = group.first().stale
            val where = group.mapNotNull { labels[it.instanceId] }.distinctBy { it.system to it.instance }
                .joinToString(", ") { label -> label.instance?.let { "${label.system} ($it)" } ?: label.system }
            "$where / ${stale.name} (${stale.date})"
        }.toMutableList()
        if (byComponent.size > MAX_LISTED) {
            list += "…"
        }
        return mapOf(
            "cvrStaleCount" to byComponent.size.toString(),
            "cvrStaleList" to list.joinToString("; ").ifEmpty { "–" },
            "cvrMaxAgeDays" to rule.readParams().cvrMaxAgeDays.toString(),
            "cvrListLink" to notificationLinks.absolute(LINK_PATH),
        )
    }

    /** The components whose last list is older than the limit of the rule. */
    private fun stale(rule: NotificationRuleDO, today: LocalDate): List<Stale> {
        val limit = today.minusDays(rule.readParams().cvrMaxAgeDays.coerceAtLeast(1).toLong())
        // The newest library added from a list: for the components whose list was applied before the day was recorded.
        val added = persistenceService.executeQuery(
            "select t.component.id as id, max(t.originDate) as day from CvrTechnologyDO t" +
                    " where t.deleted = false and t.origin in :origins and t.component is not null group by t.component.id",
            Tuple::class.java,
            Pair("origins", CvrOrigin.LIBRARY_LISTS),
        ).mapNotNull { row ->
            row.get("day", LocalDate::class.java)?.let { row.get("id", Long::class.javaObjectType) to it }
        }.toMap()
        return persistenceService.executeQuery(
            "from CvrComponentDO c where c.deleted = false", CvrComponentDO::class.java,
        ).mapNotNull { component ->
            val date = component.librariesDate ?: added[component.id] ?: return@mapNotNull null
            if (date.isBefore(limit)) {
                Stale(component.id!!, component.systemId, component.instanceId, component.name ?: component.code ?: "?", date)
            } else null
        }
    }

    private fun rows(stale: List<Stale>): List<Row> {
        if (stale.isEmpty()) {
            return emptyList()
        }
        val instancesOfSystem = persistenceService.executeQuery(
            "select i.id as id, i.system.id as systemId from CvrInstanceDO i where i.deleted = false", Tuple::class.java,
        ).groupBy({ it.get("systemId", Long::class.javaObjectType) }, { it.get("id", Long::class.javaObjectType) })
        return stale.flatMap { s ->
            val instances = s.instanceId?.let { listOf(it) } ?: instancesOfSystem[s.systemId].orEmpty()
            instances.map { Row(s, it) }
        }
    }

    companion object {
        /** The outdated lists listed at most in a notification. */
        private const val MAX_LISTED = 10

        /** Where the lists are uploaded: the inventory. */
        const val LINK_PATH = "/cveradar/inventory"
    }
}
