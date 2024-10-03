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

import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.builder.ReflectionToStringBuilder
import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.common.props.PropUtils
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.io.Serializable

private val log = KotlinLogging.logger {}

/**
 * For storing the hibernate history entries in flat format.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de), Roger Kommer, Florian Blumenstein
 */
open class DisplayHistoryEntry(entry: HistoryEntry) : Serializable {
    class Context(val entityName: String?, entry: DisplayHistoryEntry) {
        val propertyName: String? = entry.propertyName
        val propertyType: String? = entry.propertyType
        val historyValueService: HistoryValueService = HistoryValueService.instance

        val entityClass: Class<*>? = historyValueService.getClass(entityName)
        val propertyClass = historyValueService.getClass(propertyType)
        var value: String? = null
            private set

        internal fun setProp(value: String?): Context {
            this.value = value
            return this
        }

        init {
            if (entityClass != null) {
                entry.displayPropertyName = translatePropertyName(entityClass, propertyName)
            }
        }
    }

    /**
     * For information / testing purposes: the id of the PfHistoryMasterDO (t_pf_history)
     */
    var masterId: Long? = null

    /**
     * For information / testing purposes and for avoiding multiple entries in the result list:
     * the id of the PfHistoryAttrDO (t_pf_history_attr)
     */
    var attributeId: Long? = null

    var user: PFUserDO? = null

    /**
     * @return the entryType
     */
    val entryType: EntityOpType

    /**
     * @return the propertyName
     */
    var propertyName: String? = null

    /**
     * The property name to display (i18n). If not set, the propertyName will be used.
     */
    var displayPropertyName: String? = null

    /**
     * @return the propertyType
     */
    var propertyType: String? = null
        private set

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
    val timestamp: java.util.Date

    private fun getUser(userId: String?): PFUserDO? {
        return HistoryValueService.instance.getUser(userId)
    }

    init {
        timestamp = entry.modifiedAt!!
        val str = entry.modifiedBy
        if (StringUtils.isNotEmpty(str) && "anon" != str) { // Anonymous user, see PfEmgrFactory.java
            user = getUser(entry.modifiedBy)
        }
        // entry.getClassName();
        // entry.getComment();
        entryType = entry.entityOpType!!
        masterId = entry.id
        // entry.getEntityId();
    }

    constructor(
        entry: HistoryEntry, diffEntry: DiffEntry,
    ) : this(entry) {
        attributeId = diffEntry.attributeId
        propertyName = diffEntry.propertyName
        diffEntry.oldProp?.let {
            propertyType = propertyType ?: HistoryValueService.getUnifiedTypeName(it.type)
            oldValue = oldValue ?: it.value
        }
        diffEntry.newProp?.let {
            propertyType = propertyType ?: HistoryValueService.getUnifiedTypeName(it.type)
            newValue = newValue ?: it.value
        }
    }

    fun initialize(context: Context) {
        val oldObjectValue = getObjectValue(context.setProp(oldValue))
        val newObjectValue = getObjectValue(context.setProp(newValue))
        val propertyClass = context.historyValueService.getClass(propertyType)
        if (oldObjectValue != null) {
            oldValue = formatObject(oldObjectValue, propertyClass, propertyType)
        } else {
            oldValue = context.historyValueService.format(oldValue, propertyType)
        }
        if (newObjectValue != null) {
            newValue = formatObject(newObjectValue, propertyClass, propertyType)
        } else {
            newValue = context.historyValueService.format(newValue, propertyType)
        }
    }

    /**
     * You may overwrite this method to provide a custom formatting for the object value.
     * @return null if nothing done and nothing to proceed.
     */
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
    }

    /**
     * Loads the DB objects for the given property. The objects are given as coma separated id list.
     */
    protected fun getDBObjects(context: Context): List<Any> {
        return HistoryValueService.instance.getDBObjects(context)
    }

    /**
     * You may overwrite this method to provide a custom formatting for the object value.
     */
    protected open fun formatObject(valueObject: Any?, entityClass: Class<*>?, propertyName: String?): String {
        return HistoryValueService.instance.toShortNames(valueObject)
    }

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

        /*private val basicDateTypes = arrayOf(
            String::class.java.name,
            Date::class.java.name,
            java.sql.Date::class.java,
            Timestamp::class.java.name,
            BigDecimal::class.java.name,
            Long::class.java.name,
            Int::class.java.name,
        )*/

        /**
         * Tries to find a PropertyInfo annotation for the property field referred in the given diffEntry.
         * If found, the property name will be returned translated, if not, the property will be returned unmodified.
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
    }
}
