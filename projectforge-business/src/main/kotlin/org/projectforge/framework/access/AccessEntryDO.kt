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

package org.projectforge.framework.access

import java.io.Serializable

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.persistence.Transient
import javax.persistence.UniqueConstraint

import org.apache.commons.lang3.builder.HashCodeBuilder
import org.apache.commons.lang3.builder.ToStringBuilder
import org.hibernate.search.annotations.Indexed
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.ModificationStatus
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import org.projectforge.framework.persistence.user.entities.TenantDO

/**
 * Represents a single generic access entry for the four main SQL functionalities.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_GROUP_TASK_ACCESS_ENTRY", uniqueConstraints = [UniqueConstraint(columnNames = ["group_task_access_fk", "access_type"])], indexes = [javax.persistence.Index(name = "idx_fk_t_group_task_access_entry_group_task_access_fk", columnList = "group_task_access_fk"), javax.persistence.Index(name = "idx_fk_t_group_task_access_entry_tenant_id", columnList = "tenant_id")])
class AccessEntryDO : Comparable<AccessEntryDO>, Serializable, BaseDO<Int> {

    // private static final Logger log = Logger.getLogger(AccessEntryDO.class);

    private var tenant: TenantDO? = null

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

    private var id: Int? = null

    @Id
    @GeneratedValue
    @Column(name = "pk")
    override fun getId(): Int? {
        return id
    }

    override fun setId(id: Int?) {
        this.id = id
    }

    /**
     * @see org.projectforge.framework.persistence.api.BaseDO.getTenant
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    override fun getTenant(): TenantDO? {
        return this.tenant
    }

    /**
     * @see org.projectforge.framework.persistence.api.BaseDO.getTenantId
     */
    @Transient
    override fun getTenantId(): Int? {
        return if (tenant != null) tenant!!.id else null
    }

    /**
     * @see org.projectforge.framework.persistence.api.BaseDO.setTenant
     */
    override fun setTenant(tenant: TenantDO?): AccessEntryDO {
        this.tenant = tenant
        return this
    }

    /**
     * @return Always false.
     * @see org.projectforge.framework.persistence.api.BaseDO.isMinorChange
     */
    @Transient
    override fun isMinorChange(): Boolean {
        return false
    }

    /**
     * Throws UnsupportedOperationException.
     *
     * @see org.projectforge.framework.persistence.api.BaseDO.setMinorChange
     */
    override fun setMinorChange(value: Boolean) {
        throw UnsupportedOperationException()
    }

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
            return this.getId() == o.getId()
        }
        return false
    }

    override fun hashCode(): Int {
        val hcb = HashCodeBuilder()
        if (accessType != null)
            hcb.append(accessType!!.ordinal)
        hcb.append(getId())
        return hcb.toHashCode()
    }

    override fun toString(): String {
        val sb = ToStringBuilder(this)
        sb.append("id", getId())
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

    override fun setTransientAttribute(key: String, value: Any) {
        throw UnsupportedOperationException()
    }
}
