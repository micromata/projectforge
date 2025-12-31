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

import org.projectforge.framework.persistence.api.HibernateUtils

/**
 * Converts the old history entries (until 2024) to the new format.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object HistoryOldFormatConverter {
    const val OP_SUFFIX: String = ":op"

    /**
     * The Constant OLDVAL_SUFFIX.
     */
    const val OLDVAL_SUFFIX: String = ":ov"

    /**
     * The Constant NEWVAL_SUFFIX.
     */
    const val NEWVAL_SUFFIX: String = ":nv"

    fun isOldAttr(attr: HistoryEntryAttr): Boolean {
        // Property names with these suffixes are old attributes.
        // Remember, there are some property names like 'timeableAttributes.timeofvisit.2023-09-12 00:00:00:000.arrive:nv'
        attr.propertyName.let { propertyName ->
            propertyName ?: return false
            return propertyName.endsWith(OP_SUFFIX)
                    || propertyName.endsWith(OLDVAL_SUFFIX)
                    || propertyName.endsWith(NEWVAL_SUFFIX)
        }
    }

    /**
     * timeableAttributes.timeofvisit.2023-09-11 00:00:00:000.arrive:nv -> arrive.2023-09-11
     * timeableAttributes.timeofvisit.2023-09-11 00:00:00:000.depart:nv -> depart.2023-09-11
     * timeableAttributes.employeeannualleave.2009-11-26 00:00:00:000.employeeannualleavedays:ov -> annualleave.2009-11-26
     */
    fun getPlainPropertyName(attr: HistoryEntryAttr): String? {
        attr.propertyName.let { propertyName ->
            propertyName ?: return null
            return if (propertyName.startsWith("timeableAttributes.")) {
                val result = propertyName.removePrefix("timeableAttributes.").substringBeforeLast(" ")
                if (result.startsWith("timeofvisit.")) {
                    if (propertyName.contains("arrive")) {
                        result.replace("timeofvisit", "arrive")
                    } else if (propertyName.contains("depart")) {
                        result.replace("timeofvisit", "depart")
                    } else {
                        result.substringBeforeLast(".")
                    }
                } else if (propertyName.contains("employeeannualleave")) {
                    result.replace("employeeannualleave", "annualleave")
                } else if (propertyName.contains("employeestatus")) {
                    result.replace("employeestatus", "status")
                } else {
                    result
                }
            } else {
                propertyName.substringBeforeLast(":")
            }
        }
    }

    fun getFixedPropertyClass(attr: HistoryEntryAttrDO): String {
        return getFixedClass(attr.propertyTypeClass)
    }

    fun getFixedEntityClass(entry: HistoryEntryDO): String {
        return getFixedClass(entry.entityName)
    }

    internal fun getFixedClass(className: String?): String {
        val unifiedClassName = HibernateUtils.getUnifiedClassname(className)
        return HistoryOldTypeClassMapping.getMappedClass(unifiedClassName)
    }

    /**
     * Transforms old attributes to new attributes by merging 3 attribute entries from older PF-Version (MGC)
     * as one attribute entry.
     * Concatenates history attributes from older MGC versions.
     * If no old attributes are found, nothing is done.
     */
    internal fun transformOldAttributes(historyEntry: HistoryEntryDO) {
        val oldAttrs = historyEntry.attributes ?: return
        if (oldAttrs.any { isOldAttr(it) }) {
            // Old attributes found, nothing to do.
            val transformedAttrs = mutableSetOf<HistoryEntryAttrDO>()
            var currentEntry: HistoryEntryAttrDO? = null
            oldAttrs.sortedBy { it.propertyName }.forEach { attr ->
                currentEntry.let { current ->
                    if (current != null && current.propertyName == HistoryFormatUtils.getPlainPropertyName(attr)) {
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
            historyEntry.attributes = transformedAttrs
        }
        historyEntry.attributes?.forEach { attr ->
            attr.propertyTypeClass = getFixedPropertyClass(attr)
        }
    }

    private fun cloneAndTransformAttr(attr: HistoryEntryAttrDO): HistoryEntryAttrDO {
        val clone = HistoryEntryAttrDO()
        mergeDiffEntry(clone, attr)
        return clone
    }

    private fun mergeDiffEntry(newAttr: HistoryEntryAttrDO, oldAttr: HistoryEntryAttrDO) {
        newAttr.propertyName = newAttr.propertyName ?: HistoryFormatUtils.getPlainPropertyName(oldAttr)
        newAttr.parent = newAttr.parent ?: oldAttr.parent
        newAttr.id = newAttr.id ?: oldAttr.id
        if (oldAttr.propertyName?.endsWith(OLDVAL_SUFFIX) == true) {
            if (oldAttr.propertyName?.contains("startTime") != true) {
                // timeableAttributes.employeestatus.2019-03-01 00:00:00:000.startTime:op is of type java.util.Date. Ignore this.
                newAttr.propertyTypeClass = newAttr.propertyTypeClass ?: oldAttr.propertyTypeClass
                newAttr.oldValue = oldAttr.value
            }
        } else if (oldAttr.propertyName?.endsWith(NEWVAL_SUFFIX) == true) {
            if (oldAttr.propertyName?.contains("startTime") != true) {
                newAttr.propertyTypeClass = newAttr.propertyTypeClass ?: oldAttr.propertyTypeClass
                newAttr.value = oldAttr.value
            }
        } else if (oldAttr.propertyName?.endsWith(OP_SUFFIX) == true) {
            newAttr.opType = when (oldAttr.value) {
                "Insert" -> PropertyOpType.Insert
                "Update" -> PropertyOpType.Update
                "Delete" -> PropertyOpType.Delete
                else -> PropertyOpType.Undefined
            }
        } else {
            newAttr.value = newAttr.value ?: oldAttr.value
            newAttr.oldValue = newAttr.oldValue ?: oldAttr.oldValue
            newAttr.propertyTypeClass = newAttr.propertyTypeClass ?: oldAttr.propertyTypeClass
            newAttr.opType = newAttr.opType ?: oldAttr.opType
        }
    }

    internal fun getPropertyName(attr: HistoryEntryAttrDO): String? {
        return getPlainPropertyName(attr)
    }
}
