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

package org.projectforge.business.fibu

import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.history.DisplayHistoryEntryAttr
import org.projectforge.framework.persistence.history.HistoryFormatAdapter
import org.projectforge.framework.persistence.history.HistoryLoadContext
import org.projectforge.framework.persistence.history.PropertyOpType

class EmployeeValidSinceAttrHistoryAdapter : HistoryFormatAdapter() {
    override fun customizeDisplayHistoryEntry(item: Any, context: HistoryLoadContext) {
        context.requiredDisplayHistoryEntry.let { displayHistoryEntry ->
            var specifier: String? = null
            context.requiredHistoryEntry.let { historyEntry ->
                context.findLoadedEntity(historyEntry)?.let { entity ->
                    if (entity is EmployeeValidSinceAttrDO) { // Should be true
                        if (entity.type != null) {
                            specifier = translate(entity.type!!.i18nKey)
                        }
                        entity.validSince?.let { validSince ->
                            specifier += "[${entity.validSince}]"
                        }
                    }
                }
            }
            specifier?.let { prefix ->
                if (displayHistoryEntry.attributes.isEmpty()) {
                    displayHistoryEntry.attributes.add(DisplayHistoryEntryAttr().also {
                        it.operationType = PropertyOpType.convert(displayHistoryEntry.operationType)
                    })
                }
                displayHistoryEntry.attributes.forEach { attr ->
                    val displayPropname = attr.displayPropertyName
                    attr.displayPropertyName = if (displayPropname.isNullOrEmpty()) {
                        prefix
                    } else {
                        "$prefix.$displayPropname"
                    }
                }
            }
        }
    }
}
