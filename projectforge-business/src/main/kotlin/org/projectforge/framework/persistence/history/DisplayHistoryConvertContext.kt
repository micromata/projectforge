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

package org.projectforge.framework.persistence.history

import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * Context for converting history entries into human-readable formats.
 */
class DisplayHistoryConvertContext<O : ExtendedBaseDO<Long>>(val baseDao: BaseDao<O>, val item: O, val historyValueService: HistoryValueService) {
    internal val userGroupCache = UserGroupCache.getInstance()

    var currentHistoryEntry: HistoryEntryDO? = null

    val requiredHistoryEntry: HistoryEntryDO
        get() = currentHistoryEntry ?: throw IllegalStateException("No current history entry set in DisplayHistoryConvertContext.")

    var currentHistoryEntryAttr: HistoryEntryAttrDO? = null

    val requiredHistoryEntryAttr: HistoryEntryAttrDO
        get() = currentHistoryEntryAttr ?: throw IllegalStateException("No current history entry attr set in DisplayHistoryConvertContext.")

    var currentDisplayHistoryEntry: DisplayHistoryEntry? = null

    val requiredDisplayHistoryEntry: DisplayHistoryEntry
        get() = currentDisplayHistoryEntry ?: throw IllegalStateException("No current display history entry set in DisplayHistoryConvertContext.")

    var currentDisplayHistoryEntryAttr: DisplayHistoryEntryAttr? = null

    val requiredDisplayHistoryEntryAttr: DisplayHistoryEntryAttr
        get() = currentDisplayHistoryEntryAttr ?: throw IllegalStateException("No current display history entry attr set in DisplayHistoryConvertContext.")

    fun getObjectValue(value: String?, context: DisplayHistoryConvertContext<*>): Any? {
        return context.historyValueService.getObjectValue(value, context)
    }

    fun getClass(clazz: String?): Class<*>? {
        return historyValueService.getClass(clazz)
    }

    internal fun getUser(id: String?): PFUserDO? {
        return userGroupCache.getUserById(id)
    }
}
