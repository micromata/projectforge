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

import org.projectforge.business.group.service.GroupService
import org.projectforge.business.user.service.UserService
import org.projectforge.common.props.PropUtils
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.history.HistoryOldFormatConverter.isOldAttr
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Some utility methods for formatting history entries.
 */
@Service
class HistoryFormatUtils {
    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var userService: UserService

    fun replaceGroupAndUserIdsValues(entry: DisplayHistoryEntry) {
        entry.attributes.forEach { attr ->
            replaceGroupAndUserIdsValues(attr)
        }
    }

    fun replaceGroupAndUserIdsValues(attr: DisplayHistoryEntryAttr) {
        val propertyName = attr.propertyName ?: return
        if (propertyName.endsWith("GroupIds")) {
            attr.oldValue?.takeIf { it.isNotBlank() && it != "null" }?.let { value ->
                attr.oldValue = groupService.getGroupNames(value)
                    .sorted()
                    .joinToString(", ")
            }
            attr.newValue?.takeIf { it.isNotBlank() && it != "null" }?.let { value ->
                attr.newValue = groupService.getGroupNames(value)
                    .sorted()
                    .joinToString(", ")
            }
        } else if (propertyName.endsWith("UserIds")) {
            attr.oldValue?.takeIf { it.isNotBlank() && it != "null" }?.let { value ->
                attr.oldValue = userService.getUserNames(value)
                    .sorted()
                    .joinToString(", ")
            }
            attr.newValue?.takeIf { it.isNotBlank() && it != "null" }?.let { value ->
                attr.newValue = userService.getUserNames(value)
                    .sorted()
                    .joinToString(", ")
            }
        }
    }


    companion object {
        fun getPlainPropertyName(attr: HistoryEntryAttr): String? {
            if (isOldAttr(attr)) {
                return HistoryOldFormatConverter.getPlainPropertyName(attr)
            } else {
                return attr.propertyName
            }
        }

        /**
         * Tries to find a PropertyInfo annotation for the property field referred in the given diffEntry.
         * If found, the property name will be returned translated, if not, the property will be returned unmodified.
         * // TODO Handle propertyName such as pos#1.kost1#3.menge
         */
        internal fun translatePropertyName(clazz: Class<*>, propertyName: String?): String? {
            // Try to get the PropertyInfo containing the i18n key of the property for translation.
            var usePropertyName = PropUtils.get(clazz, propertyName)?.i18nKey
            if (usePropertyName != null) {
                // translate the i18n key:
                usePropertyName = translate(usePropertyName)
            } else {
                usePropertyName = propertyName
            }
            return usePropertyName
        }

        /**
         * Calls [setPropertyNameForListEntries] for each given attribute.
         */
        @JvmOverloads
        @JvmStatic
        fun setPropertyNameForListEntries(
            historyEntries: Collection<HistoryEntryDO>,
            prefix: String,
            number: Number? = null
        ) {
            historyEntries.forEach { entry ->
                setPropertyNameForListEntries(entry, prefix = prefix, number = number)
            }
        }

        /**
         * Calls [setPropertyNameForListEntries] for each given attribute.
         */
        fun setPropertyNameForListEntries(
            historyEntries: Collection<HistoryEntryDO>,
            vararg prefixes: Pair<String, Number?>
        ) {
            historyEntries.forEach { entry ->
                setPropertyNameForListEntries(entry, prefixes = prefixes)
            }
        }

        /**
         * Calls [setPropertyNameForListEntries] for each given attribute.
         */
        fun setPropertyNameForListEntries(
            historyEntry: HistoryEntryDO,
            prefix: String,
            number: Number? = null
        ) {
            historyEntry.attributes?.forEach { attr ->
                attr.displayPropertyName = getPropertyNameForEmbedded(attr.propertyName, prefix, number)
            }
        }

        /**
         * Calls [setPropertyNameForListEntries] for each given attribute.
         */
        fun setPropertyNameForListEntries(
            historyEntry: HistoryEntryDO,
            vararg prefixes: Pair<String, Number?>
        ) {
            historyEntry.attributes?.forEach { attr ->
                attr.displayPropertyName = getPropertyNameForEmbedded(attr.propertyName, prefixes = prefixes)
            }
        }

        /**
         * Used to set the displayPropertyName for list entries (such as AuftragDO.positionen).
         * The displayPropertyName is used to display the history entries in the frontend.
         * The displayPropertyName is set to the prefix + '#' + number + ':' + propertyName.
         * If the propertyName is null or empty, the displayPropertyName is set to prefix + '#' + number.
         * Example: "pos#1:propertyName" or "pos#1".
         * @param propertyName The property to get the displayPropertyName for.
         * @param prefix The prefix to use.
         * @param number The number to use.
         */
        fun getPropertyNameForEmbedded(propertyName: String?, prefix: String, number: Number? = null): String {
            return getPropertyNameForEmbedded(propertyName, Pair(prefix, number))
        }

        /**
         * Used to set the displayPropertyName for list entries (such as AuftragDO.positionen).
         * The displayPropertyName is used to display the history entries in the frontend.
         * The displayPropertyName is set to the prefix + '#' + number + ':' + propertyName.
         * If the propertyName is null or empty, the displayPropertyName is set to prefix + '#' + number.
         * Example: "pos#1:propertyName" or "pos#1".
         * Example with multiple prefixes: "prefix1#number1.prefix2#number2:propertyName" or "prefix1#number1.prefix2#number2".
         * @param propertyName The property to get the displayPropertyName for.
         * @param prefixes The prefixes to use: (prefix1, number1), (prefix2, number2), ...
         */
        fun getPropertyNameForEmbedded(propertyName: String?, vararg prefixes: Pair<String, Number?>): String {
            val prefix = prefixes.joinToString(separator = ".") { getPrefix(it) }
            return if (propertyName.isNullOrBlank()) {
                prefix
            } else {
                "$prefix:$propertyName"
            }
        }

        private fun getPrefix(prefix: Pair<String, Number?>): String {
            return if (prefix.second != null) {
                "${prefix.first}#${prefix.second}"
            } else {
                prefix.first
            }
        }
    }
}
