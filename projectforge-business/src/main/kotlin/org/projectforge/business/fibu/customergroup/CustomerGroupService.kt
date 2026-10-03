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

package org.projectforge.business.fibu.customergroup

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.kost.KundeCache
import org.projectforge.business.fibu.kost.ProjektCache
import org.projectforge.business.task.TaskTree
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationDao
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.configuration.entities.ConfigurationDO
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.LocalDate

private val log = KotlinLogging.logger {}

/**
 * The customer groups and business units of [ConfigurationParam.CUSTOMER_GROUPS].
 *
 * Reading follows the [Configuration] cache: the index is rebuilt whenever its raw value changes, so a save on
 * this or (after the cache's expiry) another node is picked up without a listener of its own.
 *
 * Saving is reserved to finance and controlling, checked by the caller (`CustomerGroupPageRest`): the parameter
 * is read-only on the generic configuration page for everyone ([ConfigurationDao.hasAccess]), so [save] writes
 * without the dao's access check.
 */
@Service
class CustomerGroupService {
    @Autowired
    private lateinit var configurationDao: ConfigurationDao

    @Autowired
    private lateinit var kundeCache: KundeCache

    @Autowired
    private lateinit var projektCache: ProjektCache

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    /** What [cachedIndex] was built from; any of it changed (by identity) means a rebuild. */
    private var cachedSources: List<Any?> = emptyList()

    private var cachedMillis = 0L

    private var cachedIndex = CustomerGroupIndex.EMPTY

    private val random = SecureRandom()

    @PostConstruct
    private fun init() {
        instanceOrNull = this
    }

    /**
     * The configuration applied to the current customers and projects. Rebuilt when the configuration, the
     * customers or the projects change (each cache replaces its map on a refresh), and after [MAX_AGE_MILLIS]
     * at the latest: a task moved within the tree changes no map, but may move projects to another business
     * unit.
     */
    val index: CustomerGroupIndex
        get() {
            val raw = Configuration.instance.getStringValue(ConfigurationParam.CUSTOMER_GROUPS)
            val customers = kundeCache.all
            val projects = projektCache.all
            val sources = listOf(raw, customers, projects)
            synchronized(this) {
                val now = System.currentTimeMillis()
                val changed = sources.size != cachedSources.size ||
                        sources.zip(cachedSources).any { (a, b) -> if (a is String?) a != b else a !== b }
                if (changed || now - cachedMillis > MAX_AGE_MILLIS) {
                    cachedIndex = CustomerGroupIndex(CustomerGroupConfig.parse(raw), directory(customers, projects))
                    cachedSources = sources
                    cachedMillis = now
                }
                return cachedIndex
            }
        }

    /** The name of the group of a customer entity (by number) or, without one, of a free-text customer. */
    fun groupNameOf(kundeId: Long?, kundeText: String? = null): String? = index.groupOf(kundeId, kundeText)?.name

    fun groupNameOf(projekt: ProjektDO?): String? = groupNameOf(projekt?.kunde?.nummer)

    /**
     * The name of the business unit of a customer (see [groupNameOf]), else of the one whose task the project
     * lies below. For scripts, bound as `customerGroupService`: `customerGroupService.businessUnitNameOf(projekt) ?: "Sonstige"`.
     */
    fun businessUnitNameOf(kundeId: Long?, kundeText: String? = null, projektId: Long? = null): String? =
        index.businessUnitOf(kundeId, kundeText, projektId)?.name

    fun businessUnitNameOf(projekt: ProjektDO?): String? = businessUnitNameOf(projekt?.kunde?.nummer, null, projekt?.id)

    /**
     * The customer entities and free-text customers the given names and patterns match, sorted by name: the
     * editor shows them, so a pattern catching more than it should is noticed before saving.
     */
    fun matches(texts: List<String>): CustomerMatches {
        val patterns = texts.mapNotNull { TextPattern.of(it) }.distinct()
        if (patterns.isEmpty()) {
            return CustomerMatches()
        }
        val customers = kundeCache.all.values
            .filter { kunde -> patterns.any { it.matches(kunde.name?.trim()) } }
            .map { it.displayName }
        val freeTexts = cachedFreeTexts().filter { text -> patterns.any { it.matches(text) } }.map { it.trim() }
        return CustomerMatches(
            customers.sortedWith(String.CASE_INSENSITIVE_ORDER),
            freeTexts.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER),
        )
    }

    class CustomerMatches(val customers: List<String> = emptyList(), val freeTexts: List<String> = emptyList())

    /**
     * What the given (unsaved) configuration leaves without a business unit: the groups, and the customer entities
     * and free-text customers belonging to no group. A customer or free text of a group without a business unit is
     * represented by its group. Only customers and free texts of orders and invoices of the last [RECENT_YEARS]
     * years are considered, the others no longer matter; a group none of whose members occurs there is left out
     * as well.
     *
     * Each entry carries the year it was last used in (a group: the latest of its members), and they are sorted by
     * this year (most recent first), then by name.
     *
     * Orders and invoices of a project belonging to a business unit by its task are not considered: a customer
     * all of whose projects lie below business-unit tasks is not listed, one with other rows still is.
     */
    fun unassigned(config: CustomerGroupConfig): Unassigned {
        normalize(config)
        val index = CustomerGroupIndex(config, directory(kundeCache.all, projektCache.all))
        val groupsInBusinessUnits = config.businessUnits.flatMap { it.groups }.toSet()
        val recent = cachedRecentCustomers().without(index.businessUnitProjects)
        val entries = mutableListOf<UnassignedEntry>()
        val groupYears = mutableMapOf<String, Int>()
        val add = { kundeId: Long?, text: String?, year: Int, name: String?, kind: UnassignedKind ->
            val group = index.groupOf(kundeId, text)
            if (group != null) {
                if (group.key !in groupsInBusinessUnits) {
                    group.key?.let { key -> groupYears.merge(key, year, ::maxOf) }
                }
            } else if (index.businessUnitOf(kundeId, text) == null && name != null) {
                entries.add(UnassignedEntry(name, kind, year))
            }
        }
        recent.kundeYears.forEach { (kundeId, year) ->
            add(kundeId, null, year, kundeCache.getKunde(kundeId)?.displayName, UnassignedKind.CUSTOMER)
        }
        recent.freeTextYears.forEach { (text, year) -> add(null, text, year, text, UnassignedKind.FREE_TEXT) }
        config.groups.forEach { group ->
            val year = groupYears[group.key] ?: return@forEach
            group.name?.let { entries.add(UnassignedEntry(it, UnassignedKind.GROUP, year)) }
        }
        return Unassigned(
            entries.sortedWith(
                compareByDescending<UnassignedEntry> { it.year }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            )
        )
    }

    /**
     * What each business unit of the given (unsaved) configuration stands for in the orders and invoices of the
     * last [RECENT_YEARS] years, by business-unit key: the groups, and the customer entities and free-text
     * customers belonging to no group, sorted by name. A customer of a group is represented by its group.
     *
     * An entry reaching its business unit only through the tasks of the projects ([BusinessUnitMember.viaTask])
     * is set apart: the customer itself belongs to none, so its other orders and invoices may count to another
     * one or to "Sonstige" (see [unassigned]).
     */
    fun businessUnitMembers(config: CustomerGroupConfig): Map<String, List<BusinessUnitMember>> {
        normalize(config)
        val index = CustomerGroupIndex(config, directory(kundeCache.all, projektCache.all))
        val members = mutableMapOf<String, MutableMap<String, BusinessUnitMember>>()
        val add = { kundeId: Long?, text: String?, projektId: Long?, name: String? ->
            val bu = index.businessUnitOf(kundeId, text, projektId)
            if (bu?.key != null && name != null) {
                val viaTask = index.businessUnitOf(kundeId, text) == null
                val group = index.groupOf(kundeId, text)
                val member = if (group?.name != null) {
                    BusinessUnitMember(group.name!!, UnassignedKind.GROUP, viaTask)
                } else {
                    BusinessUnitMember(name, if (kundeId != null) UnassignedKind.CUSTOMER else UnassignedKind.FREE_TEXT, viaTask)
                }
                members.getOrPut(bu.key!!) { mutableMapOf() }
                    .putIfAbsent("${member.kind}:${member.name.lowercase()}", member)
            }
        }
        val recent = cachedRecentCustomers()
        recent.kundeYears.keys.forEach { (kundeId, projektId) ->
            add(kundeId, null, projektId, kundeCache.getKunde(kundeId)?.displayName)
        }
        recent.freeTextYears.keys.forEach { (text, projektId) -> add(null, text, projektId, text) }
        return members.mapValues { (_, byName) ->
            byName.values.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        }
    }

    class BusinessUnitMember(val name: String, val kind: UnassignedKind, val viaTask: Boolean)

    enum class UnassignedKind { GROUP, CUSTOMER, FREE_TEXT }

    class UnassignedEntry(val name: String, val kind: UnassignedKind, val year: Int)

    class Unassigned(val entries: List<UnassignedEntry> = emptyList())

    /** The stored row's last update (epoch millis) for the optimistic lock of [save], null if none yet. */
    val lastUpdate: Long?
        get() = configurationDao.getEntry(ConfigurationParam.CUSTOMER_GROUPS)?.lastUpdate?.time

    /** Against the current customers and tasks, and the free-text customers of all orders and invoices. */
    fun validate(config: CustomerGroupConfig, freeTexts: Collection<String> = loadFreeTexts()): List<CustomerGroupError> =
        CustomerGroupValidator(directory(kundeCache.all, projektCache.all, freeTexts)).validate(config)

    /**
     * Validates an unsaved configuration as [save] would, but without the optimistic lock: the editor asks
     * after every change, so the free texts are taken from a short-lived cache instead of being queried each
     * time ([save] queries them afresh).
     */
    fun check(config: CustomerGroupConfig): List<CustomerGroupError> {
        normalize(config)
        return validate(config, cachedFreeTexts())
    }

    private fun directory(
        customers: Map<Long, KundeDO>,
        projects: Map<Long, ProjektDO>,
        freeTexts: Collection<String> = emptyList(),
    ) = CustomerDirectory(
        customers = customers.mapValues { it.value.name },
        freeTexts = freeTexts,
        projects = projects.mapValues { ProjectRef(it.value.kunde?.nummer, it.value.task?.id) },
        taskPath = { taskId -> taskPath(taskId) },
        taskTitle = { taskTree.getTaskById(it)?.title },
    )

    /** Root first, the task itself last ([TaskTree.getPathToRoot] leaves the root out). */
    private fun taskPath(taskId: Long): List<Long>? {
        taskTree.getTaskNodeById(taskId) ?: return null
        val rootId = taskTree.rootTaskNode.id
        return (listOf(rootId) + taskTree.getPathToRoot(taskId).map { it.id }).filterNotNull().distinct()
    }

    @Volatile
    private var freeTextsCache: Pair<Long, List<String>>? = null

    private fun cachedFreeTexts(): List<String> {
        val now = System.currentTimeMillis()
        freeTextsCache?.let { (loaded, texts) -> if (now - loaded < FREE_TEXTS_MAX_AGE_MILLIS) return texts }
        return loadFreeTexts().also { freeTextsCache = now to it }
    }

    @Volatile
    private var recentCustomersCache: Pair<Long, RecentCustomers>? = null

    private fun cachedRecentCustomers(): RecentCustomers {
        val now = System.currentTimeMillis()
        recentCustomersCache?.let { (loaded, recent) -> if (now - loaded < FREE_TEXTS_MAX_AGE_MILLIS) return recent }
        return loadRecentCustomers().also { recentCustomersCache = now to it }
    }

    /**
     * The year each customer entity and free-text customer was last used in, per project (null: rows without
     * one), so the rows of the projects belonging to a business unit by task can be left out ([without]).
     */
    private data class RecentCustomers(
        val kundeYears: Map<Pair<Long, Long?>, Int>,
        val freeTextYears: Map<Pair<String, Long?>, Int>,
    ) {
        /** Per customer and per free text (trimmed, the latest spelling), without the rows of [projektIds]. */
        fun without(projektIds: Set<Long>): Recent {
            val kunden = mutableMapOf<Long, Int>()
            kundeYears.forEach { (key, year) ->
                if (key.second !in projektIds) kunden.merge(key.first, year, ::maxOf)
            }
            val texts = mutableMapOf<String, Pair<String, Int>>() // lowercase -> spelling, year
            freeTextYears.forEach { (key, year) ->
                if (key.second in projektIds) return@forEach
                val text = key.first
                texts.merge(text.lowercase(), text to year) { a, b -> if (b.second > a.second) b else a }
            }
            return Recent(kunden, texts.values.associate { it })
        }
    }

    private class Recent(val kundeYears: Map<Long, Int>, val freeTextYears: Map<String, Int>)

    /**
     * The customer entities and free-text customers of the orders (by offer date, else entry date) and invoices
     * of the last [RECENT_YEARS] years, with the year of their latest one per project.
     */
    private fun loadRecentCustomers(): RecentCustomers {
        val since = "since" to LocalDate.now().minusYears(RECENT_YEARS)
        val dates = listOf("AuftragDO" to "coalesce(t.angebotsDatum, t.erfassungsDatum)", "RechnungDO" to "t.datum")
        val kundeYears = mutableMapOf<Pair<Long, Long?>, Int>()
        val freeTextYears = mutableMapOf<Pair<String, Long?>, Int>()
        dates.forEach { (entity, date) ->
            persistenceService.executeQuery(
                "select t.kunde.id, p.id, max($date) from $entity t left join t.projekt p where t.kunde is not null and t.deleted = false and $date >= :since group by t.kunde.id, p.id",
                Array<Any?>::class.java,
                since,
            ).forEach { row ->
                val kundeId = (row[0] as? Number)?.toLong() ?: return@forEach
                val year = (row[2] as? LocalDate)?.year ?: return@forEach
                kundeYears.merge(kundeId to (row[1] as? Number)?.toLong(), year, ::maxOf)
            }
            persistenceService.executeQuery(
                "select t.kundeText, p.id, max($date) from $entity t left join t.projekt p where t.kunde is null and t.kundeText is not null and t.deleted = false and $date >= :since group by t.kundeText, p.id",
                Array<Any?>::class.java,
                since,
            ).forEach { row ->
                val text = (row[0] as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: return@forEach
                val year = (row[2] as? LocalDate)?.year ?: return@forEach
                freeTextYears.merge(text to (row[1] as? Number)?.toLong(), year, ::maxOf)
            }
        }
        return RecentCustomers(kundeYears, freeTextYears)
    }

    /** The distinct free-text customers of the orders and invoices without a customer entity. */
    private fun loadFreeTexts(): List<String> =
        listOf("AuftragDO", "RechnungDO").flatMap { entity ->
            persistenceService.executeQuery(
                "select distinct t.kundeText from $entity t where t.kunde is null and t.kundeText is not null and t.deleted = false",
                String::class.java,
            )
        }.filter { it.isNotBlank() }.distinct()

    /**
     * Normalizes (trimmed names and texts, no duplicate members, keys for new groups and business units),
     * validates and stores the given configuration.
     *
     * @param expectedLastUpdate The [lastUpdate] the editor was loaded with: someone else's save meanwhile is
     * refused instead of being overwritten.
     * @return The errors; if any, nothing was stored.
     */
    fun save(config: CustomerGroupConfig, expectedLastUpdate: Long?): List<CustomerGroupError> {
        normalize(config)
        validate(config).let { if (it.isNotEmpty()) return it }
        val json = config.toJson()
        if (json.length > ConfigurationDO.PARAM_LENGTH) {
            return listOf(CustomerGroupError("", CustomerGroupValidator.ERROR_TOO_LARGE, listOf(ConfigurationDO.PARAM_LENGTH)))
        }
        synchronized(this) {
            val entry = configurationDao.getEntry(ConfigurationParam.CUSTOMER_GROUPS) ?: run {
                configurationDao.checkAndUpdateDatabaseEntries()
                configurationDao.getEntry(ConfigurationParam.CUSTOMER_GROUPS)
            } ?: throw IllegalStateException("Configuration parameter ${ConfigurationParam.CUSTOMER_GROUPS.key} missing.")
            if (entry.lastUpdate?.time != expectedLastUpdate && !entry.stringValue.isNullOrBlank()) {
                return listOf(CustomerGroupError("", CustomerGroupValidator.ERROR_MODIFIED_MEANWHILE))
            }
            entry.stringValue = json
            configurationDao.update(entry, checkAccess = false)
            log.info { "Customer groups saved: ${config.groups.size} groups, ${config.businessUnits.size} business units." }
        }
        Configuration.instance.forceReload()
        return emptyList()
    }

    private fun normalize(config: CustomerGroupConfig) {
        config.version = CustomerGroupConfig.VERSION
        val usedKeys = (config.groups + config.businessUnits).mapNotNull { it.key }.toMutableSet()
        (config.groups + config.businessUnits).forEach { set ->
            set.name = set.name?.trim()
            set.customers = set.customers.distinct().toMutableList()
            set.texts = set.texts.map { it.trim() }.filter { it.isNotEmpty() }
                .distinctBy { TextPattern.of(it) ?: it }.toMutableList()
            if (set.key.isNullOrBlank()) {
                set.key = newKey(usedKeys)
            }
        }
        config.businessUnits.forEach { it.groups = it.groups.distinct().toMutableList() }
    }

    private fun newKey(usedKeys: MutableSet<String>): String {
        while (true) {
            val key = (1..KEY_LENGTH).map { KEY_CHARS[random.nextInt(KEY_CHARS.length)] }.joinToString("")
            if (usedKeys.add(key)) return key
        }
    }

    companion object {
        private const val KEY_LENGTH = 6
        private const val KEY_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789"

        /** A task moved in the tree is noticed after this time at the latest (see [index]). */
        private const val MAX_AGE_MILLIS = 10 * 60 * 1000L

        /** For [check], [matches] and [unassigned], asked after every change in the editor. */
        private const val FREE_TEXTS_MAX_AGE_MILLIS = 60 * 1000L

        /** Customers and free texts without an order or invoice since are left out of [unassigned]. */
        const val RECENT_YEARS = 5L

        /** Null outside a Spring context (unit tests), where the lists then offer no groups. */
        @JvmStatic
        var instanceOrNull: CustomerGroupService? = null
            private set

        /** For scripts. */
        @JvmStatic
        val instance: CustomerGroupService
            get() = instanceOrNull ?: throw IllegalStateException("CustomerGroupService not initialized.")
    }
}
