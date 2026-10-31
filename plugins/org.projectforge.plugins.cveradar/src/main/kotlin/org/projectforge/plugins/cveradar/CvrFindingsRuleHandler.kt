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
import org.projectforge.business.notification.NotificationDao
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
import org.projectforge.plugins.cveradar.model.CvrFindingCertainty
import org.projectforge.plugins.cveradar.model.CvrFindingDO
import org.projectforge.plugins.cveradar.model.CvrFindingStatus
import org.projectforge.plugins.cveradar.model.CvrTechnologyDO
import org.projectforge.plugins.cveradar.model.CvrVulnerabilityDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/**
 * New findings of the CVE radar (NOT-01, NOT-02): who is responsible for an affected system hears of it, the
 * configured recipients of the rule in copy. A finding counts from the CVSS severity of the rule on (a known exploited
 * vulnerability always), only sure findings unless the rule includes the possible ones.
 *
 * The responsible team of an instance is the group of its system or else the nearest group of the areas above. Each
 * recipient is told only of the instances they may see (VIS-07): a recipient seeing none of the new findings gets
 * nothing. A recipient is told of every finding once: the period key of a notification is the highest finding id it
 * names, a later run names only the findings above the highest id notified to the recipient. Findings older than
 * [WINDOW_DAYS] are left out, so a new rule or a new team member doesn't get the whole backlog.
 *
 * Run right after a matching created findings ([CvrFindingNotifier]) and by its schedule as a fallback.
 */
@Service
class CvrFindingsRuleHandler : NotificationRuleHandler {
    @Autowired
    private lateinit var access: CveRadarAccess

    @Autowired
    private lateinit var inventoryService: CvrInventoryService

    @Autowired
    private lateinit var notificationDao: NotificationDao

    @Autowired
    private lateinit var notificationLinks: NotificationLinks

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var recipientResolver: NotificationRecipientResolver

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    override val ruleType = NotificationRuleType.CVERADAR_FINDINGS

    override val variables = listOf(
        "cvrFindingCount",
        "cvrVulnerabilityCount",
        "cvrMaxSeverity",
        "cvrMaxCvss",
        "cvrVulnerabilityList",
        "cvrSystemList",
        "cvrMinSeverity",
        "cvrLink",
    )

    /** What the variables show of a finding (a real one or an example). */
    internal class Row(
        val id: Long,
        val instanceId: Long,
        val cveId: String,
        val vulnerabilityId: Long?,
        val severity: String?,
        val cvss: BigDecimal?,
        val kev: Boolean,
        val possible: Boolean,
        val system: String,
        val instance: String?,
        val technology: String?,
    )

    override fun evaluate(rule: NotificationRuleDO, today: LocalDate): NotificationRuleHandler.Evaluation? {
        val findings = newFindings(rule, today)
        if (findings.isEmpty()) {
            return null
        }
        val notified = rule.id?.let { highestNotified(it) } ?: emptyMap()
        // The responsible teams of the instances, then the configured recipients in copy.
        val groups = inventoryService.responsibleGroups()
        val groupIds = findings.mapNotNull { groups[it.first.instanceId] }.toSet()
        val userIds = userGroupCache.getUserGroupIdMap().filterValues { ids -> ids.any { it in groupIds } }.keys +
                recipientResolver.resolve(rule.readRecipients(), null).mapNotNull { it.id }
        val users = recipientResolver.users(userIds).filter { access.isReader(it) }
        val readable = inventoryService.readableInstances(users)
        val rows = rows(findings, readable)
        val affected = mutableMapOf<Long, Map<String, String>>()
        val periodKeys = mutableMapOf<Long, String>()
        users.forEach { user ->
            val instances = readable[user.id].orEmpty()
            val since = notified[user.id] ?: 0L
            val own = rows.filter { it.instanceId in instances && it.id > since }
            if (own.isNotEmpty()) {
                affected[user.id!!] = variables(rule, own)
                periodKeys[user.id!!] = periodKey(own.maxOf { it.id })
            }
        }
        if (affected.isEmpty()) {
            return null
        }
        return NotificationRuleHandler.Evaluation(
            today.toString(), affected, LIST_PATH, userPeriodKeys = periodKeys, affectedAreRecipients = true,
        )
    }

    /** All new findings the user may see, regardless of what was notified (e.g. a test to oneself). */
    override fun variables(rule: NotificationRuleDO, today: LocalDate, user: PFUserDO): Map<String, String> {
        val instances = inventoryService.readableInstances(user)
        return variables(rule, rows(newFindings(rule, today), mapOf(user.id!! to instances)).filter { it.instanceId in instances })
    }

    override fun exampleVariables(rule: NotificationRuleDO, today: LocalDate, user: PFUserDO): Map<String, String> {
        val rows = listOf(
            Row(2, 1, "CVE-2026-12345", null, "CRITICAL", BigDecimal("9.8"), true, false, "WEB Shop", "PROD", "Apache Tomcat 9.0.80"),
            Row(1, 2, "CVE-2026-12345", null, "CRITICAL", BigDecimal("9.8"), true, false, "WEB Shop", "TEST", "Apache Tomcat 9.0.80"),
            Row(3, 1, "CVE-2026-23456", null, "HIGH", BigDecimal("7.5"), false, true, "WEB Shop", "PROD", "lodash 4.17.20"),
        )
        return variables(rule, rows)
    }

    override fun isRelevant(rule: NotificationRuleDO, today: LocalDate, user: PFUserDO): Boolean = access.isReader(user)

    /** An older pending notification of a recipient expires, as soon as a newer one exists. */
    override fun resolve(
        rule: NotificationRuleDO,
        notifications: List<NotificationDO>,
        today: LocalDate,
    ): Map<Long, NotificationStatus> {
        val result = mutableMapOf<Long, NotificationStatus>()
        notifications.groupBy { it.recipientId }.forEach { (_, list) ->
            val newest = list.maxWith(compareBy<NotificationDO> { highestId(it.periodKey) ?: 0L }.thenBy { it.id ?: 0 })
            list.filter { it !== newest }.forEach { result[it.id!!] = NotificationStatus.EXPIRED }
        }
        return result
    }

    private fun variables(rule: NotificationRuleDO, rows: List<Row>): Map<String, String> {
        val byVulnerability = rows.groupBy { it.cveId }.values
            .sortedWith(compareByDescending<List<Row>> { it.first().kev }.thenByDescending { it.first().cvss ?: BigDecimal.ZERO })
        val list = byVulnerability.take(MAX_VULNERABILITIES).map { group ->
            val first = group.first()
            val rating = listOfNotNull(first.severity, first.cvss?.toPlainString(), "KEV".takeIf { first.kev }).joinToString(" ")
            val where = group.groupBy { it.system }.entries.joinToString(", ") { (system, list) ->
                val instances = list.mapNotNull { it.instance }.distinct().sorted()
                if (instances.isEmpty()) system else "$system (${instances.joinToString(", ")})"
            }
            val technologies = group.mapNotNull { it.technology }.distinct().joinToString(", ")
            val possible = if (group.all { it.possible }) " ?" else ""
            listOf("${first.cveId}${if (rating.isNotEmpty()) " ($rating)" else ""}$possible", where, technologies)
                .filter { it.isNotEmpty() }.joinToString(" – ")
        }.toMutableList()
        if (byVulnerability.size > MAX_VULNERABILITIES) {
            list += "…"
        }
        val systems = rows.groupBy { it.system }.entries.sortedBy { it.key }.map { (system, list) ->
            "$system (${list.map { it.cveId }.distinct().size})"
        }
        val top = rows.maxWithOrNull(compareBy { SEVERITIES.indexOf(it.severity) })
        val single = byVulnerability.singleOrNull()?.first()?.vulnerabilityId
        return mapOf(
            "cvrFindingCount" to rows.size.toString(),
            "cvrVulnerabilityCount" to byVulnerability.size.toString(),
            "cvrMaxSeverity" to (top?.severity ?: "–"),
            "cvrMaxCvss" to (rows.mapNotNull { it.cvss }.maxOrNull()?.toPlainString() ?: "–"),
            "cvrVulnerabilityList" to list.joinToString("; ").ifEmpty { "–" },
            "cvrSystemList" to systems.joinToString("; ").ifEmpty { "–" },
            "cvrMinSeverity" to rule.readParams().cvrMinSeverity,
            // One vulnerability: its file; several: the list (of the affected ones by default).
            "cvrLink" to notificationLinks.absolute(single?.let { "$FILE_PATH/$it" } ?: LIST_PATH),
        )
    }

    /** The open findings of the last [WINDOW_DAYS] the rule notifies of, with their vulnerability. */
    private fun newFindings(rule: NotificationRuleDO, today: LocalDate): List<Pair<CvrFindingDO, CvrVulnerabilityDO>> {
        val params = rule.readParams()
        val minLevel = SEVERITIES.indexOf(params.cvrMinSeverity).coerceAtLeast(1)
        val since = Date.from(today.minusDays(WINDOW_DAYS).atStartOfDay(ZoneId.systemDefault()).toInstant())
        val findings = persistenceService.executeQuery(
            "select f from CvrFindingDO f where f.deleted = false and f.status = :status and f.created >= :since",
            CvrFindingDO::class.java,
            Pair("status", CvrFindingStatus.NEW),
            Pair("since", since),
        ).filter { params.cvrIncludePossible || it.certainty == CvrFindingCertainty.AFFECTED }
        val vulnerabilities = byId(CvrVulnerabilityDO::class.java, findings.mapNotNull { it.vulnerabilityId })
        return findings.mapNotNull { finding ->
            val vulnerability = vulnerabilities[finding.vulnerabilityId]?.takeIf { !it.deleted } ?: return@mapNotNull null
            val level = SEVERITIES.indexOf(vulnerability.severity)
            val wanted = vulnerability.kevAdded != null ||
                    (if (level < 0 || vulnerability.cvssScore == null) params.cvrIncludeUnscored else level >= minLevel)
            if (wanted) finding to vulnerability else null
        }
    }

    /** The rows of the findings in an instance readable by one of the users (labels taken from there). */
    private fun rows(
        findings: List<Pair<CvrFindingDO, CvrVulnerabilityDO>>,
        readable: Map<Long, Map<Long, CvrInventoryService.InstanceLabel>>,
    ): List<Row> {
        val labels = readable.values.fold(mutableMapOf<Long, CvrInventoryService.InstanceLabel>()) { all, map -> all.also { it.putAll(map) } }
        val visible = findings.filter { it.first.instanceId in labels }
        val components = byId(CvrComponentDO::class.java, visible.mapNotNull { it.first.componentId })
        val technologies = byId(CvrTechnologyDO::class.java, visible.mapNotNull { it.first.technologyId })
        return visible.map { (finding, vulnerability) ->
            val label = labels[finding.instanceId]!!
            val technology = technologies[finding.technologyId]?.let { listOfNotNull(it.product, it.version).joinToString(" ") }
                ?: components[finding.componentId]?.name
            Row(
                finding.id!!, finding.instanceId!!, vulnerability.cveId ?: "?", vulnerability.id, vulnerability.severity,
                vulnerability.cvssScore, vulnerability.kevAdded != null, finding.certainty != CvrFindingCertainty.AFFECTED,
                label.system, label.instance, technology,
            )
        }
    }

    /** The highest finding id notified to each recipient by the rule. */
    private fun highestNotified(ruleId: Long): Map<Long, Long> =
        notificationDao.findPeriodKeysByRule(ruleId).mapValues { (_, keys) -> keys.mapNotNull { highestId(it) }.maxOrNull() ?: 0L }

    private fun <T : org.projectforge.framework.persistence.api.BaseDO<Long>> byId(clazz: Class<T>, ids: Collection<Long>): Map<Long, T> {
        if (ids.isEmpty()) return emptyMap()
        return persistenceService.executeQueryBatched(
            "from ${clazz.simpleName} e where e.id in :ids", clazz, "ids", ids.toSet(),
        ).associateBy { it.id!! }
    }

    companion object {
        /** The NVD severities, the lowest first; their index is the level. */
        private val SEVERITIES = listOf("NONE", "LOW", "MEDIUM", "HIGH", "CRITICAL")

        /** Findings older than this are not notified any more (a new rule or recipient doesn't get the backlog). */
        const val WINDOW_DAYS = 7L

        /** The vulnerabilities listed at most in a notification. */
        private const val MAX_VULNERABILITIES = 10

        /** The list of the vulnerabilities (the affected ones by default) and the file of one. */
        const val LIST_PATH = "/cveradar"
        const val FILE_PATH = "/cveradar/vulnerability"

        private const val PERIOD_PREFIX = "finding:"

        internal fun periodKey(highestFindingId: Long) = "$PERIOD_PREFIX$highestFindingId"

        internal fun highestId(periodKey: String?): Long? = periodKey?.removePrefix(PERIOD_PREFIX)?.toLongOrNull()
    }
}
