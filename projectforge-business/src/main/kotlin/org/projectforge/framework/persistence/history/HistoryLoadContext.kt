/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.history

import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.IdObject
import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * Context used while loading history entries.
 * Also for converting history entries into human-readable formats.
 */
class HistoryLoadContext(
    /**
     * For customizing history entries.
     */
    val baseDao: BaseDao<*>? = null,
) {

    class HistoryEntryMap {
        var historyEntry: HistoryEntryDO? = null
        var displayHistoryEntry: DisplayHistoryEntry? = null
        var map = mutableMapOf<String, Any?>()
    }

    class HistoryEntryAttrMap {
        var historyEntryAttr: HistoryEntryAttrDO? = null
        var displayHistoryEntryAttr: DisplayHistoryEntryAttr? = null
        var map = mutableMapOf<String, Any?>()
    }

    private val historyEntries = mutableListOf<HistoryEntryDO>()
    val loadedEntities = mutableListOf<IdObject<Long>>()

    fun merge(entries: List<HistoryEntryDO>) {
        for (entry in entries) {
            if (historyEntries.none { it.id == entry.id }) {
                historyEntries.add(entry)
            }
        }
    }

    val originUnsortedEntries: List<HistoryEntryDO>
        get() = historyEntries

    val sortedEntries: List<HistoryEntryDO>
        get() = historyEntries.sortedByDescending { it.id }

    var historyEntryAttrMap = mutableMapOf<Long, HistoryEntryAttrMap>()

    internal val userGroupCache = UserGroupCache.getInstance()

    val historyValueService = HistoryValueService.instance

    var currentHistoryEntry: HistoryEntryDO? = null
        private set

    val requiredHistoryEntry: HistoryEntryDO
        get() = currentHistoryEntry
            ?: throw IllegalStateException("No current history entry set in DisplayHistoryConvertContext.")

    var currentHistoryEntryAttr: HistoryEntryAttrDO? = null
        private set

    val requiredHistoryEntryAttr: HistoryEntryAttrDO
        get() = currentHistoryEntryAttr
            ?: throw IllegalStateException("No current history entry attr set in DisplayHistoryConvertContext.")

    var currentDisplayHistoryEntry: DisplayHistoryEntry? = null
        private set

    val requiredDisplayHistoryEntry: DisplayHistoryEntry
        get() = currentDisplayHistoryEntry
            ?: throw IllegalStateException("No current display history entry set in DisplayHistoryConvertContext.")

    var currentDisplayHistoryEntryAttr: DisplayHistoryEntryAttr? = null
        private set

    val requiredDisplayHistoryEntryAttr: DisplayHistoryEntryAttr
        get() = currentDisplayHistoryEntryAttr
            ?: throw IllegalStateException("No current display history entry attr set in DisplayHistoryConvertContext.")

    fun findLoadedEntity(historyEntry: HistoryEntryDO): Any? {
        val entityName = historyEntry.entityName
        val entityId = historyEntry.entityId
        return loadedEntities.find { HistoryEntryDO.asEntityName(it) == entityName && it.id == entityId }
    }

    fun addLoadedEntity(entity: IdObject<Long>) {
        loadedEntities.add(entity)
    }

    fun setCurrent(entry: HistoryEntryDO, attr: HistoryEntryAttrDO? = null) {
        currentHistoryEntry = entry
        currentHistoryEntryAttr = attr
    }

    fun setCurrent(attr: HistoryEntryAttrDO? = null) {
        requireNotNull(currentHistoryEntry)
        currentHistoryEntryAttr = attr
    }

    fun setCurrent(entry: DisplayHistoryEntry, attr: DisplayHistoryEntryAttr? = null) {
        currentDisplayHistoryEntry = entry
        currentDisplayHistoryEntryAttr = attr
    }

    fun setCurrent(attr: DisplayHistoryEntryAttr? = null) {
        requireNotNull(currentDisplayHistoryEntry)
        currentDisplayHistoryEntryAttr = attr
    }

    fun clearCurrentDisplayHistoryEntry() {
        currentDisplayHistoryEntry = null
        currentDisplayHistoryEntryAttr = null
    }

    fun clearCurrentDisplayHistoryEntryAttr() {
        currentDisplayHistoryEntryAttr = null
    }

    fun clearCurrents() {
        currentHistoryEntry = null
        currentHistoryEntryAttr = null
        clearCurrentDisplayHistoryEntry()
    }

    fun getObjectValue(value: String?, context: HistoryLoadContext): Any? {
        return context.historyValueService.getObjectValue(value, context)
    }

    fun getClass(clazz: String?): Class<*>? {
        return historyValueService.getClass(clazz)
    }

    internal fun getUser(id: String?): PFUserDO? {
        return userGroupCache.getUserById(id)
    }

    fun getDisplayPropertyName(attr: HistoryEntryAttrDO): String? {
        return historyEntryAttrMap[attr.id]?.map?.get("displayPropertyName") as? String
    }

    private fun ensureDisplayHistoryEntryAttr(attrId: Long?): HistoryEntryAttrMap {
        attrId ?: throw IllegalArgumentException("attrId must not be null.")
        return historyEntryAttrMap.getOrPut(attrId) { HistoryEntryAttrMap() }
    }
}
