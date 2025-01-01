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

import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.UserRightService
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.springframework.context.ApplicationContext

// private val log = KotlinLogging.logger {}

class HistoryFormatUserRightAdapter(
    applicationContext: ApplicationContext,
) : HistoryFormatAdapter() {

    private val userRightService = applicationContext.getBean(UserRightService::class.java)

    override fun customizeDisplayHistoryEntry(item: Any, context: HistoryLoadContext) {
        val displayHistoryEntry = context.requiredDisplayHistoryEntry
        val historyEntry = context.requiredHistoryEntry
        context.findLoadedEntity(historyEntry)?.let { right ->
            if (right is UserRightDO) {
                val attr = displayHistoryEntry.attributes.firstOrNull()
                    ?: DisplayHistoryEntryAttr().also {
                        displayHistoryEntry.attributes.add(it)
                    }
                val propertyOpType = PropertyOpType.convert(historyEntry.entityOpType)
                val rightId = userRightService.getRightId(right.rightIdString)
                val valueI18nKey = right.value?.i18nKey
                attr.propertyName = right.rightIdString
                if (rightId != null) {
                    attr.displayPropertyName = translate(rightId.i18nKey)
                }
                attr.newValue = if (valueI18nKey != null) translate(valueI18nKey) else right.value?.toString()
                attr.operationType = propertyOpType
                attr.operation = HistoryFormatService.translate(propertyOpType)
            }
        }
    }

    override fun convertHistoryEntryAttr(item: Any, context: HistoryLoadContext): DisplayHistoryEntryAttr {
        val displayAttr = super.convertHistoryEntryAttr(item, context)
        return displayAttr
    }

    /*    override fun convertEntries(item: Any, entries: MutableList<DisplayHistoryEntry>, context: HistoryLoadContext) {
            if (item !is PFUserDO) {
                log.warn { "Can't handle history entries for entity of type ${item::class.java.name}" }
                return
            }
            item.rights?.forEach { right ->
                userRightDao.loadHistory(right).forEach { entry ->
                    context.setCurrent(entry)
                    val dto = convertHistoryEntry(item, context)
                    dto.attributes.firstOrNull { it.propertyName == "value" }?.let { attr ->
                        attr.propertyName = right.rightIdString.toString()
                        dto.attributes = mutableListOf(attr) // Drop all other entries, only the value rules.
                    }
                    entries.add(dto)
                    context.clearCurrents()
                }
            }
        }*/
}
