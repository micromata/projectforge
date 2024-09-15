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
 * Creates HistoryEntries from PFHistoryMasterDO and PFHistoryAttrDO.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object PFHistoryMasterUtils {
    /**
     * Concatenates also history attributes from older MGC versions.
     */
    fun createDiffEntries(attrs: List<PfHistoryAttrDO>): List<DiffEntry> {
        val diffEntries = mutableListOf<DiffEntry>()
        var currentDiffEntry: DiffEntry? = null
        attrs.sortedBy { it.propertyName }.forEach { attr ->
            currentDiffEntry.let { current ->
                if (current != null && current.propertyName == attr.plainPropertyName) {
                    mergeDiffEntry(current, attr)
                } else {
                    val newEntry = createDiffEntry(attr)
                    diffEntries.add(newEntry)
                    currentDiffEntry = newEntry
                }
            }
        }
        return diffEntries
    }

    private fun createDiffEntry(attr: PfHistoryAttrDO): DiffEntry {
        val diffEntry = DiffEntry()
        mergeDiffEntry(diffEntry, attr)
        return diffEntry
    }

    private fun mergeDiffEntry(diffEntry: DiffEntry, attr: PfHistoryAttrDO) {
        diffEntry.propertyName = attr.plainPropertyName
        if (attr.propertyName?.endsWith(":ov") == true) {
            diffEntry.oldProp = HistProp(value = attr.value)
        } else if (attr.propertyName?.endsWith(":nv") == true) {
            diffEntry.newProp = HistProp(value = attr.value)
        } else if (attr.propertyName?.endsWith(":op") == true) {
            diffEntry.propertyOpType = when (attr.value) {
                "Insert" -> PropertyOpType.Insert
                "Update" -> PropertyOpType.Update
                else -> PropertyOpType.Undefined
            }
        } else {
            diffEntry.propertyOpType = attr.optype
            diffEntry.oldProp = HistProp(value = attr.oldValue)
            diffEntry.newProp = HistProp(value = attr.value)
        }
    }

    internal fun getPropertyName(attr: PfHistoryAttrDO): String? {
        return attr.plainPropertyName
    }
}
