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

package org.projectforge.framework.persistence.history

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * History entries will be transformed from [HistoryEntry] and [HistoryEntryAttr] into human-readable
 * formats [DisplayHistoryEntry] and [DisplayHistoryEntryAttr].
 */
@Component
class HistoryFormatService {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var historyValueService: HistoryValueService

    /**
     * Adapter for specific entities. Key is the entityName of HistoryEntryDO built via [HistoryEntryDO.asEntityName] or,
     * the full qualified class name.
     */
    private val historyServiceAdapters =
        mutableMapOf<String, HistoryFormatAdapter>()

    private lateinit var stdHistoryFormatAdapter: HistoryFormatAdapter

    @PostConstruct
    private fun postConstruct() {
        stdHistoryFormatAdapter = HistoryFormatAdapter()
        register(UserRightDO::class.java, HistoryFormatUserRightAdapter(applicationContext))
    }

    fun <O : ExtendedBaseDO<Long>> register(clazz: Class<out O>, historyServiceAdapter: HistoryFormatAdapter) {
        if (historyServiceAdapters[clazz.name] != null) {
            log.warn { "Can't register HistoryServiceAdapter ${historyServiceAdapter::class.java.name} twice. Ignoring." }
            return
        }
        this.historyServiceAdapters[clazz.name] = historyServiceAdapter
    }

    fun <O : ExtendedBaseDO<Long>> selectAsDisplayEntries(
        baseDao: BaseDao<O>,
        item: O,
        loadContext: HistoryLoadContext = HistoryLoadContext(baseDao),
        checkAccess: Boolean = true,
    ): List<DisplayHistoryEntry> {
        val loadContext = baseDao.loadHistory(item, checkAccess = checkAccess, loadContext = loadContext)
        val entries = mutableListOf<DisplayHistoryEntry>()
        loadContext.originUnsortedEntries.forEach { historyEntry ->
            entries.add(convert(item, historyEntry, loadContext))
        }
        val adapter = historyServiceAdapters[item::class.java.name]
        adapter?.convertEntries(item, entries, loadContext)
        return entries.sortedByDescending { it.modifiedAt }
    }

    fun <O : BaseDO<*>> convert(
        item: O,
        historyEntry: HistoryEntryDO,
        context: HistoryLoadContext,
    ): DisplayHistoryEntry {
        context.setCurrent(historyEntry)
        val adapter = historyServiceAdapters[historyEntry.entityName]
        context.setCurrent(historyEntry)
        val displayHistoryEntry = adapter?.convertHistoryEntry(item, context)
            ?: stdHistoryFormatAdapter.convertHistoryEntry(item, context)
        context.setCurrent(displayHistoryEntry)
        context.baseDao?.customizeHistoryEntry(context)
        if (historyEntry.entityOpType == EntityOpType.Insert && historyEntry.attributes.isNullOrEmpty()) {
            // Special case: Insert without attributes.
            context.baseDao?.getHistoryPropertyPrefix(context)?.let { propertyPrefix ->
                // Add the operation as attribute, if a propertyPrefix is given.
                displayHistoryEntry.attributes.add(DisplayHistoryEntryAttr().also { attr ->
                    attr.operationType = PropertyOpType.Insert
                    attr.displayPropertyName = propertyPrefix
                    attr.newValue = attr.operation
                })
            }
        }
        historyEntry.attributes?.forEach { attr ->
            context.setCurrent(attr)
            val displayAttr = adapter?.convertHistoryEntryAttr(item, context)
                ?: stdHistoryFormatAdapter.convertHistoryEntryAttr(item, context)
            displayHistoryEntry.attributes.add(displayAttr)
            context.setCurrent(displayAttr)
            context.baseDao?.getHistoryPropertyPrefix(context)?.let {
                displayAttr.displayPropertyName = "$it:${displayAttr.displayPropertyName}"
            }
            context.baseDao?.customizeHistoryEntryAttr(context)
            context.clearCurrentDisplayHistoryEntryAttr()
        }
        adapter?.customizeDisplayHistoryEntry(item, context)
            ?: stdHistoryFormatAdapter.customizeDisplayHistoryEntry(item, context)
        context.baseDao?.customizeDisplayHistoryEntry(context)
        context.clearCurrents()
        return displayHistoryEntry
    }

    companion object {
        fun translate(opType: EntityOpType?): String {
            return when (opType) {
                EntityOpType.Insert -> translate("operation.inserted")
                EntityOpType.Update -> translate("operation.updated")
                EntityOpType.Delete -> translate("operation.deleted")
                EntityOpType.MarkAsDeleted -> translate("operation.markAsDeleted")
                EntityOpType.Undelete -> translate("operation.undeleted")
                else -> ""
            }
        }

        fun translate(opType: PropertyOpType?): String {
            return when (opType) {
                PropertyOpType.Insert -> translate("operation.inserted")
                PropertyOpType.Update -> translate("operation.updated")
                PropertyOpType.Delete -> translate("operation.deleted")
                PropertyOpType.Undefined -> translate("operation.undefined")
                else -> ""
            }
        }
    }
}
