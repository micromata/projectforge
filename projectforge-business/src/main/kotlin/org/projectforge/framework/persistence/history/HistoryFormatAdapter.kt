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

/**
 * You may register history adapters for customizing conversion of history entries.
 */
open class HistoryFormatAdapter {
    /**
     * A customized adapter may manipulate all found history entries by modifying, deleting or adding entries.
     * Does nothing at default.
     * @param item Item the history entries are related to.
     * @param entries All found history entries for customization.
     */
    open fun convertEntries(
        item: Any,
        entries: MutableList<DisplayHistoryEntry>,
        context: HistoryLoadContext
    ) {
    }

    /**
     * A customized adapter may manipulate a single history entry.
     * This method gets the modifiedBy-user and creates a DisplayHistoryEntry including any attributes.
     * Attributes are not yet available.
     * @param item Item the history entry is related to.
     * @param context The load context, containing current history entry ([HistoryLoadContext.requiredHistoryEntry]).
     * @return The customized history entry.
     */
    open fun convertHistoryEntry(item: Any, context: HistoryLoadContext): DisplayHistoryEntry {
        return DisplayHistoryEntry.create(context.requiredHistoryEntry, context)
    }

    /**
     * A customized adapter may manipulate a single history entry after it has been converted.
     * Does nothing at default.
     * All attributes are available, if any.
     * @param item Item the history entry is related to.
     * @param context The load context, containing current history entry ([HistoryLoadContext.requiredDisplayHistoryEntry]).
     * @param displayHistoryEntry The converted history entry.
     */
    open fun customizeDisplayHistoryEntry(
        item: Any,
        context: HistoryLoadContext,
    ) {
    }

    /**
     * A customized adapter may manipulate a single history entry attribute.
     * This method gets the modifiedBy-user and creates a DisplayHistoryEntryAttr.
     * @param item Item the history entry attribute is related to.
     * @param context The load context, containing current history entry attribute ([HistoryLoadContext.currentHistoryEntryAttr]).
     * @return The customized history entry attribute.
     */
    open fun convertHistoryEntryAttr(item: Any, context: HistoryLoadContext): DisplayHistoryEntryAttr {
        val historyAttr = context.requiredHistoryEntryAttr
        val displayAttr = DisplayHistoryEntryAttr.create(historyAttr, context)
        val oldObjectValue = context.getObjectValue(displayAttr.oldValue, context)
        val newObjectValue = context.getObjectValue(displayAttr.newValue, context)
        val propertyClass = context.getClass(historyAttr.propertyTypeClass)
        if (oldObjectValue != null) {
            displayAttr.oldValue =
                formatObject(
                    valueObject = oldObjectValue,
                    typeClass = propertyClass,
                    propertyName = historyAttr.propertyName,
                )
        } else {
            displayAttr.oldValue =
                context.historyValueService.format(historyAttr.oldValue, propertyType = historyAttr.propertyTypeClass)
        }
        if (newObjectValue != null) {
            displayAttr.newValue = formatObject(newObjectValue, propertyClass, propertyName = historyAttr.propertyName)
        } else {
            displayAttr.newValue =
                context.historyValueService.format(historyAttr.value, propertyType = historyAttr.propertyTypeClass)
        }

        return displayAttr
    }

    /**
     * You may overwrite this method to provide a custom formatting for the object value.
     * Will format the object(s) by displayName, if available
     */
    protected open fun formatObject(valueObject: Any?, typeClass: Class<*>?, propertyName: String?): String {
        return HistoryValueService.instance.toDisplayNames(valueObject)
    }
}
