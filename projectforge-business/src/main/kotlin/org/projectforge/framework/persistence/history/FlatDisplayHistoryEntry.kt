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

import org.apache.commons.lang3.builder.ReflectionToStringBuilder
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.io.Serializable

/**
 * For displaying the hibernate history entries in flat format.
 *
 * The history entries [HistoryEntryDO] will first be converted to [DisplayHistoryEntry] and then to [FlatDisplayHistoryEntry].
 *
 * Used by Wicket pages as well as by AuftragDao in e-mail-notifications.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de), Roger Kommer, Florian Blumenstein
 */
open class FlatDisplayHistoryEntry : Serializable {
    /**
     * For information / testing purposes: the id of the HistoryEntryDO (t_pf_history)
     */
    var historyEntryId: Long? = null

    /**
     * For information / testing purposes and for avoiding multiple entries in the result list:
     * the id of the HistoryEntryAttrDO (t_pf_history_attr)
     */
    var attributeId: Long? = null

    var user: PFUserDO? = null

    var userComment: String? = null

    /**
     * @return the entryType
     */
    var opType: EntityOpType? = null

    /**
     * @return the propertyName
     */
    var propertyName: String? = null

    /**
     * @return the propertyName to display (e.g. translated)
     */
    var displayPropertyName: String? = null

    /**
     * @return the oldValue
     */
    var oldValue: String? = null

    /**
     * @return the newValue
     */
    var newValue: String? = null

    /**
     * Timestamp of the history entry.
     */
    var timestamp: java.util.Date? = null

    /*
    fun initialize(context: Context) {
        val oldObjectValue = getObjectValue(context.setProp(oldValue))
        val newObjectValue = getObjectValue(context.setProp(newValue))
        val propertyClass = context.historyValueService.getClass(propertyType)
        if (oldObjectValue != null) {
            oldValue = formatObject(
                valueObject = oldObjectValue,
                typeClass = propertyClass,
                propertyName = context.propertyName,
            )
        } else {
            oldValue = context.historyValueService.format(oldValue, propertyType = propertyType)
        }
        if (newObjectValue != null) {
            newValue = formatObject(newObjectValue, propertyClass, propertyName = propertyName)
        } else {
            newValue = context.historyValueService.format(newValue, propertyType = propertyType)
        }
    }*/

    /**
     * You may overwrite this method to provide a custom formatting for the object value.
     * @return null if nothing done and nothing to proceed.
     */
    /*
    protected open fun getObjectValue(context: Context): Any? {
        val value = context.value ?: return null
        val valueType = HistoryValueService.instance.getValueType(propertyType)
        if (valueType != HistoryValueService.ValueType.ENTITY) {
            return null
        }
        val propertyClass = context.propertyClass ?: return null
        if (propertyClass == PFUserDO::class.java) {
            if (!value.isBlank() && !value.contains(",")) {
                // Single user expected.
                return getUser(value) ?: "###"
            }
        }
        if (propertyClass == EmployeeDO::class.java || propertyClass == AddressbookDO::class.java) {
            val sb = StringBuilder()
            getDBObjects(context).forEach { dbObject ->
                if (dbObject is EmployeeDO) {
                    sb.append("${dbObject.user?.getFullname() ?: "???"};")
                }
                if (dbObject is AddressbookDO) {
                    sb.append("${dbObject.title};")
                }
            }
            sb.deleteCharAt(sb.length - 1)
            return sb.toString()
        }
        return HistoryValueService.instance.getDBObjects(context)
    }*/

    /**
     * Loads the DB objects for the given property. The objects are given as coma separated id list.
     */
    /*
    protected fun getDBObjects(context: Context): List<Any> {
        return HistoryValueService.instance.getDBObjects(context)
    }
*/
    /**
     * You may overwrite this method to provide a custom formatting for the object value.
     *//*
    protected open fun formatObject(valueObject: Any?, typeClass: Class<*>?, propertyName: String?): String {
        return HistoryValueService.instance.toShortNames(valueObject)
    }
*/
    /**
     * Returns string containing all fields (except the password, via ReflectionToStringBuilder).
     *
     * @return
     */
    override fun toString(): String {
        return ReflectionToStringBuilder(this).toString()
    }

    companion object {
        private const val serialVersionUID = 3900345445639438747L

        fun create(entry: DisplayHistoryEntry, attr: DisplayHistoryEntryAttr? = null): FlatDisplayHistoryEntry {
            return FlatDisplayHistoryEntry().also {
                it.historyEntryId = entry.id
                it.opType = entry.operationType
                it.timestamp = entry.modifiedAt
                it.userComment = entry.userComment
                it.attributeId = attr?.id
                it.user = UserGroupCache.getInstance().getUser(entry.modifiedByUserId)
                it.propertyName = attr?.propertyName
                it.displayPropertyName = attr?.displayPropertyName ?: attr?.propertyName
                it.oldValue = attr?.oldValue
                it.newValue = attr?.newValue
            }
        }

        /*private val basicDateTypes = arrayOf(
            String::class.java.name,
            Date::class.java.name,
            java.sql.Date::class.java,
            Timestamp::class.java.name,
            BigDecimal::class.java.name,
            Long::class.java.name,
            Int::class.java.name,
        )*/
    }
}
