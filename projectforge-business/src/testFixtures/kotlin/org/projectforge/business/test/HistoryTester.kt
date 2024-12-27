/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.test

import org.junit.jupiter.api.Assertions
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.history.EntityOpType
import org.projectforge.framework.persistence.history.HistoryEntry
import org.projectforge.framework.persistence.history.HistoryEntryAttr
import org.projectforge.framework.persistence.history.HistoryEntryAttrDO
import org.projectforge.framework.persistence.history.HistoryEntryDO
import org.projectforge.framework.persistence.history.HistoryService
import org.projectforge.framework.persistence.history.PropertyOpType
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import kotlin.reflect.KClass

class HistoryTester(
    private val persistenceService: PfPersistenceService,
    private val historyService: HistoryService,
) {
    private var totalNumberHistoryEntries: Int = 0
    private var totalNumberOfHistoryAttrs: Int = 0

    /**
     * The recent history entries since last check. This is set by [loadRecentHistoryEntries].
     * You may also set it manually or by [loadAndTailHistoryEntries] or [loadHistory] if you want to check the entries later.
     */
    var recentEntries: List<HistoryEntryHolder>? = null

    init {
        reset()
    }

    /**
     * Resets the statistics (by reading current number of history entries).
     */
    fun reset() {
        val count = count()
        totalNumberHistoryEntries = count.first
        totalNumberOfHistoryAttrs = count.second
    }

    /**
     * Loads the history entries for the given id.
     * Please note: Embedded objects are only loaded, if they're part of any history entry attribute of the given object.
     * Please use [org.projectforge.framework.persistence.api.BaseDao.loadHistory] for getting all embedded history entries.
     * @param baseDO The baseDO to load the history entries for.
     * @param expectedNumberOfNewHistoryEntries The expected number of new history entries.
     * @param expectedNumberOfNewHistoryAttrEntries The expected number of new history attributes.
     * @param msg The message for the assertion error.
     */
    fun <O : ExtendedBaseDO<Long>> loadHistory(
        baseDO: O,
        expectedNumberOfNewHistoryEntries: Int? = null,
        expectedNumberOfNewHistoryAttrEntries: Int = 0,
        msg: String = "",
    ): List<HistoryEntryHolder>? {
        val loadContext = historyService.loadHistory(baseDO)
        wrapHistoryEntries(loadContext.sortedEntries)
        if (expectedNumberOfNewHistoryEntries != null) {
            assertSizes(expectedNumberOfNewHistoryEntries, expectedNumberOfNewHistoryAttrEntries, msg)
        }
        return recentEntries
    }

    fun selectHistory(entityId: Long): List<HistoryEntryDO> {
        return persistenceService.runReadOnly { context ->
            val entries = context.createQuery(
                "from HistoryEntryDO where entityId=:entityId", HistoryEntryDO::class.java,
                Pair("entityId", entityId),
            ).resultList
            entries.forEach { entry ->
                context.createQuery(
                    "from HistoryEntryAttrDO where parent.id=:parentId",
                    HistoryEntryAttrDO::class.java,
                    Pair("parentId", entry.id),
                ).resultList.forEach { attr ->
                    entry.attributes = entry.attributes ?: mutableSetOf()
                    entry.attributes!!.add(attr)
                }
            }
            entries
        }
    }

    /**
     * Load the recent history entries for debugging. The recent entries are the last entries in the database, independent of the
     * entity. The entities are loaded as well if available.
     */
    fun loadAndTailHistoryEntries(maxResults: Int): List<HistoryEntryHolder>? {
        persistenceService.runReadOnly { context ->
            context.executeQuery(
                "from HistoryEntryDO order by id desc",
                HistoryEntryDO::class.java,
                maxResults = maxResults,
            ).let { entries ->
                wrapHistoryEntries(entries)
            }
        }
        return recentEntries
    }

    /**
     * Get the entry at the given index.
     * @param idx The index of the entry.
     * @param expectedNumberOfAttributes The expected number of attributes. If null, the number of attributes is not checked.
     * @param msg The message for the assertion error.
     * @return The history entry.
     */
    fun getEntry(
        idx: Int,
        expectedNumberOfAttributes: Int? = null,
        msg: String = "entries[$idx]"
    ): HistoryEntryHolder {
        val entry = recentEntries!![idx]
        if (expectedNumberOfAttributes != null) {
            Assertions.assertEquals(expectedNumberOfAttributes, entry.attributes?.size, msg)
        }
        return entry
    }

    /**
     * Get the entry by the primary key. Only usable for testing old history entries, imported as csv?
     * @param id The primary key of the database entry.
     * @param expectedNumberOfAttributes The expected number of attributes. If null, the number of attributes is not checked.
     * @param msg The message for the assertion error.
     * @return The history entry.
     */
    fun getEntriesByPk(
        id: Long,
        expectedNumberOfHistoryEntries: Int,
        expectedNumberOfAttributes: Int = 0,
        msg: String = "pk=$id"
    ): List<HistoryEntryHolder> {
        val wrappers = recentEntries!!.filter { it.entry.id == id }
        Assertions.assertNotNull(wrappers, "$msg: History entry with id pk=$id not found.")
        assertSizes(wrappers, expectedNumberOfHistoryEntries, expectedNumberOfAttributes, msg)
        return wrappers
    }

    fun getEntriesByEntityId(
        entityId: Long,
        expectedNumberOfNewHistoryEntries: Int,
        expectedNumberOfNewHistoryAttrEntries: Int,
        msg: String = "entityId=$entityId",
    ): List<HistoryEntryHolder>? {
        val result = recentEntries?.filter { it.entry.entityId == entityId }
        assertSizes(result, expectedNumberOfNewHistoryEntries, expectedNumberOfNewHistoryAttrEntries, msg)
        return result
    }

    private fun wrapHistoryEntries(historyEntries: List<HistoryEntryDO>) {
        val result = mutableListOf<HistoryEntryHolder>()
        persistenceService.runReadOnly { context ->
            historyEntries.forEach { entry ->
                val entity = entry.entityId?.let {
                    try {
                        val entityClass = Class.forName(entry.entityName)
                        context.find(entityClass, it)
                    } catch (_: Exception) {
                        // Not found, OK.
                        null
                    }
                }
                result.add(HistoryEntryHolder(entry, entity))
            }

        }
        recentEntries = result
    }

    /**
     * Asserts the number of new history entries and attributes.
     * This method loads the recent history entries and asserts the number of new entries.
     * The expected number of new entries is calculated by the difference between the current number of history entries and the
     * number of history entries at the last call of this method.
     * If the expected number of new entries is not equal to the actual number of new entries, an assertion error is thrown.
     * The total number of history entries is updated.
     * @param expectedNumberOfNewHistoryEntries The expected number of new history entries.
     * @param expectedNumberOfNewHistoryAttrEntries The expected number of new history attributes.
     * @return This for chaining.
     */
    fun loadRecentHistoryEntries(
        expectedNumberOfNewHistoryEntries: Int,
        expectedNumberOfNewHistoryAttrEntries: Int = 0,
        msg: String = "",
    ): HistoryTester {
        val count = count()
        val numberOfNewHistoryEntries = count.first - totalNumberHistoryEntries
        loadAndTailHistoryEntries(numberOfNewHistoryEntries)
        // ####### For debugging, it's recommended to set the breakpoint at the following line: ##############
        assertSizes(expectedNumberOfNewHistoryEntries, expectedNumberOfNewHistoryAttrEntries, msg = msg)
        totalNumberHistoryEntries = count.first
        totalNumberOfHistoryAttrs = count.second
        return this
    }

    fun assertSizes(
        expectedNumberOfNewHistoryEntries: Int,
        expectedNumberOfNewHistoryAttrEntries: Int = 0,
        msg: String = "",
    ): HistoryTester {
        assertSizes(recentEntries, expectedNumberOfNewHistoryEntries, expectedNumberOfNewHistoryAttrEntries, msg)
        return this
    }

    fun assertSizes(
        wrappers: List<HistoryEntryHolder>?,
        expectedNumberOfHistoryEntries: Int,
        expectedNumberOfHistoryAttrEntries: Int = 0,
        msg: String = "",
    ): HistoryTester {
        var countAttrs = 0
        wrappers?.forEach { wrapper ->
            countAttrs += wrapper.entry.attributes?.size ?: 0
        }
        Assertions.assertEquals(
            expectedNumberOfHistoryEntries,
            wrappers?.size,
            "$msg: Number of history entries = ${wrappers?.size}, attrs = $countAttrs. If it fails, check the recent entries by calling recentEntries."
        )
        Assertions.assertEquals(
            expectedNumberOfHistoryAttrEntries,
            countAttrs,
            "$msg: Number of history attrs. If it fails, check the recent entries by calling recentEntries."
        )
        return this
    }

    private fun count(): Pair<Int, Int> {
        val countHistoryEntries = persistenceService.selectSingleResult(
            "select count(*) from HistoryEntryDO",
            Long::class.java,
        )
        val countAttrEntries = persistenceService.selectSingleResult(
            "select count(*) from HistoryEntryAttrDO",
            Long::class.java,
        )
        return Pair(countHistoryEntries!!.toInt(), countAttrEntries!!.toInt())
    }

    companion object {
        fun filterHistoryEntries(
            entries: List<HistoryEntry>,
            numberOfExpectedEntries: Int,
            entityClass: KClass<*>? = null,
            id: Long? = null,
            opType: EntityOpType? = null,
            modUser: PFUserDO? = null,
        ): List<HistoryEntry> {
            val found = entries.filter {
                (entityClass == null || it.entityName == entityClass.java.name) &&
                        (id == null || it.entityId == id) &&
                        (opType == null || it.entityOpType == opType) &&
                        (modUser == null || it.modifiedBy == modUser.id?.toString())
            }
            Assertions.assertEquals(
                numberOfExpectedEntries,
                found.size,
                "Number of found entries: ${found.size}, expected: $numberOfExpectedEntries"
            )
            return found
        }

        /**
         * Asserts the history entry.
         * @return The attributes of the history entry (might be null).
         */
        fun assertHistoryEntry(
            holder: HistoryEntryHolder,
            entityClass: KClass<*>,
            entityId: Long?,
            opType: EntityOpType,
            modUser: PFUserDO? = null,
            numberOfAttributes: Int = 0,
        ): Set<HistoryEntryAttr>? {
            return assertHistoryEntry(
                entry = holder.entry,
                entityClass = entityClass,
                entityId = entityId,
                opType = opType,
                modUser = modUser,
                numberOfAttributes = numberOfAttributes,
            )
        }

        /**
         * Asserts the history entry.
         * @return The attributes of the history entry (might be null).
         */
        fun assertHistoryEntry(
            entry: HistoryEntry,
            entityClass: KClass<*>,
            entityId: Long?,
            opType: EntityOpType,
            modUser: PFUserDO? = null,
            numberOfAttributes: Int = 0,
        ): Set<HistoryEntryAttr>? {
            Assertions.assertEquals(entityClass.java.name, entry.entityName)
            if (entityId != null) {
                Assertions.assertEquals(entityId, entry.entityId)
            }
            Assertions.assertEquals(opType, entry.entityOpType)
            if (modUser != null) {
                Assertions.assertEquals(modUser.id?.toString(), entry.modifiedBy)
            }
            Assertions.assertTrue(
                System.currentTimeMillis() - entry.modifiedAt!!.time < 10000,
                "Time difference is too big",
            )
            entry as HistoryEntryDO
            Assertions.assertEquals(numberOfAttributes, entry.attributes?.size ?: 0)
            return entry.attributes
        }

        fun assertHistoryAttr(
            wrapper: HistoryEntryHolder,
            propertyName: String,
            value: String?,
            oldValue: String?,
            opType: PropertyOpType = PropertyOpType.Update,
            propertyTypeClass: KClass<*> = java.lang.String::class,
            msg: String = "",
        ) {
            assertHistoryAttr(
                wrapper.entry,
                propertyName = propertyName,
                value = value,
                oldValue = oldValue,
                opType = opType,
                propertyTypeClass = propertyTypeClass,
                msg = msg
            )
        }

        fun assertHistoryAttr(
            entry: HistoryEntry,
            propertyName: String,
            value: String?,
            oldValue: String?,
            opType: PropertyOpType = PropertyOpType.Update,
            propertyTypeClass: KClass<*>? = java.lang.String::class,
            msg: String = "",
        ) {
            Assertions.assertNotNull(entry.attributes, "$msg: No attributes found in entry")
            entry.attributes!!.find { it.propertyName == propertyName }.let { attr ->
                Assertions.assertNotNull(attr, "Property ${entry.entityName}.$propertyName not found")
                Assertions.assertEquals(
                    propertyName,
                    attr!!.propertyName,
                    "$msg: propertyName for ${entry.entityName}.$propertyName"
                )
                Assertions.assertEquals(value, attr.value, "$msg: value for ${entry.entityName}.$propertyName")
                Assertions.assertEquals(oldValue, attr.oldValue, "$msg: oldValue for ${entry.entityName}.$propertyName")
                Assertions.assertEquals(opType, attr.opType, "$msg: opType for ${entry.entityName}.$propertyName")
                if (propertyTypeClass != null) {
                    Assertions.assertEquals(
                        propertyTypeClass.java.name,
                        attr.propertyTypeClass,
                        "$msg: propertyClass for ${entry.entityName}.$propertyName",
                    )
                } else {
                    Assertions.assertEquals(
                        "",
                        attr.propertyTypeClass,
                        "$msg: propertyClass for ${entry.entityName}.$propertyName"
                    )
                }
            }
        }
    }
}
