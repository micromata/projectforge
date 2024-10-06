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

import org.projectforge.framework.persistence.api.HibernateUtils

/**
 * Creates HistoryEntries from PFHistoryMasterDO and PFHistoryAttrDO.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object PFHistoryMasterUtils {
    const val OP_SUFFIX: String = ":op"

    /**
     * The Constant OLDVAL_SUFFIX.
     */
    const val OLDVAL_SUFFIX: String = ":ov"

    /**
     * The Constant NEWVAL_SUFFIX.
     */
    const val NEWVAL_SUFFIX: String = ":nv"

    fun isOldAttr(attr: PfHistoryAttrDO): Boolean {
        // Property names with these suffixes are old attributes.
        // Remember, there are some property names like 'timeableAttributes.timeofvisit.2023-09-12 00:00:00:000.arrive:nv'
        attr.propertyName.let { propertyName ->
            propertyName ?: return false
            return propertyName.endsWith(OP_SUFFIX)
                    || propertyName.endsWith(OLDVAL_SUFFIX)
                    || propertyName.endsWith(NEWVAL_SUFFIX)
        }
    }

    fun getPlainPropertyName(attr: PfHistoryAttrDO): String? {
        if (isOldAttr(attr)) {
            return attr.propertyName?.substringBeforeLast(":")
        } else {
            return attr.propertyName
        }
    }

    fun getFixedPropertyClass(attr: PfHistoryAttrDO): String {
        return HibernateUtils.getUnifiedClassname(attr.propertyTypeClass)
    }

    /**
     * Transforms old attributes to new attributes by merging 3 attribute entries from older PF-Version (MGC)
     * as one attribute entry.
     * Concatenates history attributes from older MGC versions.
     * If no old attributes are found, nothing is done.
     */
    fun transformOldAttributes(masterDO: PfHistoryMasterDO) {
        val oldAttrs = masterDO.attributes ?: return
        if (oldAttrs.any { isOldAttr(it) }) {
            // Old attributes found, nothing to do.
            val transformedAttrs = mutableSetOf<PfHistoryAttrDO>()
            var currentEntry: PfHistoryAttrDO? = null
            oldAttrs.sortedBy { it.propertyName }.forEach { attr ->
                currentEntry.let { current ->
                    if (current != null && current.propertyName == attr.plainPropertyName) {
                        mergeDiffEntry(current, attr)
                    } else if (isOldAttr(attr)) {
                        val newEntry = cloneAndTransformAttr(attr)
                        transformedAttrs.add(newEntry)
                        currentEntry = newEntry
                    } else {
                        // Nothing to do, because the attribute is already in current format.
                        transformedAttrs.add(attr)
                    }
                }
            }
            masterDO.attributes = transformedAttrs
        }
        masterDO.attributes?.forEach { attr ->
            attr.propertyTypeClass = getFixedPropertyClass(attr)
        }
    }

    private fun cloneAndTransformAttr(attr: PfHistoryAttrDO): PfHistoryAttrDO {
        val clone = PfHistoryAttrDO()
        mergeDiffEntry(clone, attr)
        return clone
    }

    private fun mergeDiffEntry(newAttr: PfHistoryAttrDO, oldAttr: PfHistoryAttrDO) {
        newAttr.propertyName = newAttr.propertyName ?: oldAttr.plainPropertyName
        newAttr.master = newAttr.master ?: oldAttr.master
        newAttr.id = newAttr.id ?: oldAttr.id
        if (oldAttr.propertyName?.endsWith(OLDVAL_SUFFIX) == true) {
            newAttr.oldValue = oldAttr.value
            newAttr.propertyTypeClass = newAttr.propertyTypeClass ?: oldAttr.propertyTypeClass
        } else if (oldAttr.propertyName?.endsWith(NEWVAL_SUFFIX) == true) {
            newAttr.value = oldAttr.value
            newAttr.propertyTypeClass = newAttr.propertyTypeClass ?: oldAttr.propertyTypeClass
        } else if (oldAttr.propertyName?.endsWith(OP_SUFFIX) == true) {
            newAttr.opType = when (oldAttr.value) {
                "Insert" -> PropertyOpType.Insert
                "Update" -> PropertyOpType.Update
                else -> PropertyOpType.Undefined
            }
        } else {
            newAttr.value = newAttr.value ?: oldAttr.value
            newAttr.oldValue = newAttr.oldValue ?: oldAttr.oldValue
            newAttr.propertyTypeClass = newAttr.propertyTypeClass ?: oldAttr.propertyTypeClass
            newAttr.opType = newAttr.opType ?: oldAttr.opType
        }
    }

    internal fun getPropertyName(attr: PfHistoryAttrDO): String? {
        return attr.plainPropertyName
    }
}
