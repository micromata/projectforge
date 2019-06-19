/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.core

import de.micromata.genome.db.jpa.history.api.DiffEntry
import de.micromata.genome.db.jpa.history.api.HistoryEntry
import de.micromata.genome.db.jpa.history.entities.EntityOpType
import de.micromata.genome.db.jpa.history.entities.PropertyOpType
import org.projectforge.business.multitenancy.TenantRegistryMap
import org.projectforge.common.BeanHelper
import org.projectforge.common.i18n.I18nEnum
import org.projectforge.common.props.PropUtils
import org.projectforge.framework.i18n.TimeAgo
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.stereotype.Component
import java.lang.reflect.Field
import java.util.*

/**
 * History entries will be transformed into human readable formats.
 */
@Component
class HistoryService {
    private val log = org.slf4j.LoggerFactory.getLogger(HistoryService::class.java)

    data class DisplayHistoryEntry(
            var modifiedAt: Date? = null,
            var timeAgo: String? = null,
            var modifiedByUserId: String? = null,
            var modifiedByUser: String? = null,
            var operationType: EntityOpType? = null,
            var operation: String? = null,
            var diffEntries: MutableList<DisplayHistoryDiffEntry> = mutableListOf())

    data class DisplayHistoryDiffEntry(
            var operationType: PropertyOpType? = null,
            var operation: String? = null,
            var property: String? = null,
            var oldValue: String? = null,
            var newValue: String? = null)

    /**
     * Creates a list of formatted history entries (get the user names etc.)
     */
    fun format(orig: Array<HistoryEntry<*>>): List<DisplayHistoryEntry> {
        val userGroupCache = TenantRegistryMap.getInstance().tenantRegistry.userGroupCache
        val entries = mutableListOf<DisplayHistoryEntry>()
        orig.forEach {
            var user: PFUserDO? = null
            try {
                user = userGroupCache?.getUser(it.modifiedBy.toInt())
            } catch (e: NumberFormatException) {
                // Ignore error.
            }
            val entry = DisplayHistoryEntry(
                    modifiedAt = it.modifiedAt,
                    timeAgo = TimeAgo.getMessage(it.modifiedAt),
                    modifiedByUserId = it.modifiedBy,
                    modifiedByUser = user?.getFullname(),
                    operationType = it.entityOpType,
                    operation = translate(it.entityOpType))
            var clazz: Class<*>? = null
            try {
                clazz = Class.forName(it.entityName)
            } catch (ex: ClassNotFoundException) {
                log.warn("Class '${it.entityName}' not found.")
            }
            it.diffEntries?.forEach { de ->
                val diffEntry = DisplayHistoryDiffEntry(
                        operationType = de.propertyOpType,
                        operation = translate(de.propertyOpType),
                        property = de.propertyName,
                        oldValue = de.oldValue,
                        newValue = de.newValue)
                if (clazz != null && !de.propertyName.isNullOrBlank()) {
                    val field = BeanHelper.getDeclaredField(clazz, de.propertyName)
                    if (field == null) {
                        log.warn("No such field '${it.entityName}.${de.propertyName}'.")
                    } else {
                        field.isAccessible = true
                        if (field.type.isEnum) {
                            diffEntry.oldValue = getI18nEnumTranslation(field, diffEntry.oldValue)
                            diffEntry.newValue = getI18nEnumTranslation(field, diffEntry.newValue)
                        }
                    }
                    diffEntry.property = translateProperty(de, clazz)
                }
                entry.diffEntries.add(diffEntry)
            }
            entries.add(entry)
        }
        return entries
    }

    fun getI18nEnumTranslation(field: Field, value: String?): String? {
        if (value == null) {
            return ""
        }
        val i18nEnum = I18nEnum.create(field.type, value) as I18nEnum
        return translate(i18nEnum.i18nKey)
    }

    /**
     * Tries to find a PropertyInfo annotation for the property field referred in the given diffEntry.
     * If found, the property name will be returned translated, if not, the property will be returned unmodified.
     */
    private fun translateProperty(diffEntry: DiffEntry, clazz: Class<*>): String? {
        // Try to get the PropertyInfo containing the i18n key of the property for translation.
        var propertyName = PropUtils.get(clazz, diffEntry.propertyName)?.i18nKey
        if (propertyName != null) {
            // translate the i18n key:
            propertyName = translate(propertyName)
        } else {
            propertyName = diffEntry.propertyName
        }
        return propertyName
    }

    private fun translate(opType: EntityOpType?): String {
        when (opType) {
            EntityOpType.Insert -> return translate("operation.inserted")
            EntityOpType.Update -> return translate("operation.updated")
            EntityOpType.Deleted -> return translate("operation.deleted")
            EntityOpType.MarkDeleted -> return translate("operation.markAsDeleted")
            EntityOpType.UmarkDeleted -> return translate("operation.undeleted")
            else -> return ""
        }
    }

    private fun translate(opType: PropertyOpType?): String {
        return when (opType) {
            PropertyOpType.Insert -> translate("operation.inserted")
            PropertyOpType.Update -> translate("operation.updated")
            PropertyOpType.Delete -> translate("operation.deleted")
            PropertyOpType.Undefined -> translate("operation.undefined")
            else -> ""
        }
    }
}
