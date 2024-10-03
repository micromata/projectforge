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
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * For storing the hibernate history entries in flat format.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de), Roger Kommer, Florian Blumenstein
 */
open class DisplayHistoryEntry(entry: HistoryEntry) : Serializable {
    /**
     * For information / testing purposes: the id of the PfHistoryMasterDO (t_pf_history)
     */
    var masterId: Long? = null

    var user: PFUserDO? = null

    /**
     * @return the entryType
     */
    val entryType: EntityOpType
    /**
     * @return the propertyName
     */
    /**
     * Use-full for prepending id of children (e. g. entries in a collection displayed in the history table of the parent
     * object). Example: AuftragDO -> AuftragsPositionDO.
     *
     * @param propertyName
     */
    var propertyName: String? = null

    /**
     * @return the propertyType
     */
    var propertyType: String? = null
        private set
    /**
     * @return the oldValue
     */
    /**
     * @param oldValue the oldValue to set
     * @return this for chaining.
     */
    var oldValue: String? = null
    /**
     * @return the newValue
     */
    /**
     * @param newValue the newValue to set
     * @return this for chaining.
     */
    var newValue: String? = null
    val timestamp: java.util.Date

    private fun getUser(userId: String?): PFUserDO? {
        return HistoryValueService.instance.getUser(userId)
    }

    constructor(
        entry: HistoryEntry, diffEntry: DiffEntry,
    ) : this(entry) {
        val historyValueService = HistoryValueService.instance
        diffEntry.newProp?.let {
            propertyType = HistoryValueService.getUnifiedTypeName(it.type)
        }
        diffEntry.oldProp?.let {
            propertyType = HistoryValueService.getUnifiedTypeName(it.type)
        }
        val oldObjectValue = getObjectValue(diffEntry.oldProp)
        val newObjectValue = getObjectValue(diffEntry.newProp)
        val clazz = historyValueService.getClass(propertyType)
        if (oldObjectValue != null) {
            oldValue = formatObject(oldObjectValue, clazz, propertyType)
        } else {
            oldValue = historyValueService.format(diffEntry.oldValue, propertyType)
        }
        if (newObjectValue != null) {
            newValue = formatObject(newObjectValue, clazz, propertyType)
        } else {
            newValue = historyValueService.format(diffEntry.newValue, propertyType)
        }

        if (clazz != null) {
            propertyName = translateProperty(diffEntry, clazz)
        } else {
            propertyName = diffEntry.propertyName
        }
    }

    /**
     * You may overwrite this method to provide a custom formatting for the object value.
     * @return null if nothing done and nothing to proceed.
     */
    protected open fun getObjectValue(prop: HistProp?): Any? {
        val value = prop?.value ?: return null
        val valueType = HistoryValueService.instance.getValueType(prop?.type)
        if (valueType != HistoryValueService.ValueType.ENTITY) {
            return null
        }
        val entityClass = HistoryValueService.instance.getClass(prop?.type) ?: return null
        if (entityClass == PFUserDO::class.java) {
            if (!value.isNullOrBlank() && !value.contains(",")) {
                // Single user expected.
                val user = getUser(prop.value ?: "###")
                if (user != null) {
                    return user
                }
            }
        }
        if (entityClass == EmployeeDO::class.java || entityClass == AddressbookDO::class.java) {
            val sb = StringBuilder()
            getDBObjects(prop, entityClass).forEach { dbObject ->
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
        return HistoryValueService.instance.getDBObjects(prop, entityClass)
    }

    /**
     * Loads the DB objects for the given property. The objects are given as coma separated id list.
     */
    protected fun getDBObjects(prop: HistProp, entityClass: Class<*>): List<Any> {
        return HistoryValueService.instance.getDBObjects(prop, entityClass)
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

    /**
     * Tries to find a PropertyInfo annotation for the property field referred in the given diffEntry.
     * If found, the property name will be returned translated, if not, the property will be returned unmodified.
     */
    private fun translateProperty(diffEntry: DiffEntry, clazz: Class<*>): String? {
        return Companion.translateProperty(clazz, diffEntry.propertyName)
    }

    companion object {
        private const val serialVersionUID = 3900345445639438747L

        private val basicDateTypes = arrayOf(
            String::class.java.name,
            Date::class.java.name,
            java.sql.Date::class.java,
            Timestamp::class.java.name,
            BigDecimal::class.java.name,
            Long::class.java.name,
            Int::class.java.name,
        )

        /**
         * Tries to find a PropertyInfo annotation for the property field referred in the given diffEntry.
         * If found, the property name will be returned translated, if not, the property will be returned unmodified.
         */
        internal fun translateProperty(clazz: Class<*>, propertyName: String?): String? {
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
