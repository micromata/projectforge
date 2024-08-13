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

package org.projectforge.framework.access

import org.apache.commons.lang3.builder.HashCodeBuilder
import org.apache.commons.lang3.builder.ToStringBuilder
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.ModificationStatus
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import java.io.Serializable
import jakarta.persistence.*

/**
 * Represents a single generic access entry for the four main SQL functionalities.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_GROUP_TASK_ACCESS_ENTRY", uniqueConstraints = [UniqueConstraint(columnNames = ["group_task_access_fk", "access_type"])], indexes = [jakarta.persistence.Index(name = "idx_fk_t_group_task_access_entry_group_task_access_fk", columnList = "group_task_access_fk")])
class AccessEntryDO : Comparable<AccessEntryDO>, Serializable, BaseDO<Int> {

    // private static final Logger log = Logger.getLogger(AccessEntryDO.class);

    /**
     */
    @get:Column(name = "access_type")
    @get:Enumerated(EnumType.STRING)
    var accessType: AccessType? = null

    /**
     */
    @get:Column(name = "access_select")
    var accessSelect = false

    @get:Column(name = "access_insert")
    var accessInsert = false

    @get:Column(name = "access_update")
    var accessUpdate = false

    @get:Column(name = "access_delete")
    var accessDelete = false

    @get:Id
    @get:GeneratedValue
    @get:Column(name = "pk")
    override var id: Int? = null

    /**
     * @return Always false.
     * @see org.projectforge.framework.persistence.api.BaseDO.isMinorChange
     */
    @get:Transient
    override var isMinorChange: Boolean
        get() = false
        set(value) { throw UnsupportedOperationException() }

    constructor()

    constructor(accessType: AccessType) {
        this.accessType = accessType
    }

    constructor(type: AccessType, accessSelect: Boolean, accessInsert: Boolean,
                accessUpdate: Boolean,
                accessDelete: Boolean) {
        this.accessType = type
        setAccess(accessSelect, accessInsert, accessUpdate, accessDelete)
    }

    fun hasPermission(opType: OperationType): Boolean {
        return when (opType) {
            OperationType.SELECT -> this.accessSelect
            OperationType.INSERT -> this.accessInsert
            OperationType.UPDATE -> this.accessUpdate
            else -> this.accessDelete
        }
    }

    fun setAccess(accessSelect: Boolean, accessInsert: Boolean, accessUpdate: Boolean,
                  accessDelete: Boolean) {
        this.accessSelect = accessSelect
        this.accessInsert = accessInsert
        this.accessUpdate = accessUpdate
        this.accessDelete = accessDelete
    }

    /*
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
    override fun compareTo(other: AccessEntryDO): Int {
        return this.accessType!!.compareTo(other.accessType!!)
    }

    override fun equals(other: Any?): Boolean {
        if (other is AccessEntryDO) {
            val o = other as AccessEntryDO?
            if (this.accessType != o!!.accessType)
                return false
            return this.id == o.id
        }
        return false
    }

    override fun hashCode(): Int {
        val hcb = HashCodeBuilder()
        if (accessType != null)
            hcb.append(accessType!!.ordinal)
        hcb.append(id)
        return hcb.toHashCode()
    }

    override fun toString(): String {
        val sb = ToStringBuilder(this)
        sb.append("id", id)
        sb.append("type", this.accessType)
        sb.append("select", this.accessSelect)
        sb.append("insert", this.accessInsert)
        sb.append("update", this.accessUpdate)
        sb.append("delete", this.accessDelete)
        return sb.toString()
    }

    /**
     * Copies the values accessSelect, accessInsert, accessUpdate and accessDelete from the given src object excluding the
     * values created and modified. Null values will be excluded.
     *
     * @param src
     */
    override fun copyValuesFrom(src: BaseDO<out Serializable>, vararg ignoreFields: String): ModificationStatus {
        return AbstractBaseDO.copyValues(src, this, *ignoreFields)
    }

    override fun getTransientAttribute(key: String): Any {
        throw UnsupportedOperationException()
    }

    override fun removeTransientAttribute(key: String): Any {
        throw UnsupportedOperationException()
    }

    override fun setTransientAttribute(key: String, value: Any?) {
        throw UnsupportedOperationException()
    }
}
