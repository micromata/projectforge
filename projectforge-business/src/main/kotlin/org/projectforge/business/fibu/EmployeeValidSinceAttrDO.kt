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

package org.projectforge.business.fibu

import com.fasterxml.jackson.annotation.JsonIdentityReference
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.*
import org.projectforge.Constants
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.candh.CandHHistoryEntryICustomizer
import org.projectforge.framework.persistence.candh.CandHIgnore
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import org.projectforge.framework.persistence.history.HistoryEntryDO
import org.projectforge.framework.persistence.history.WithHistory
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Represents timeable attributes of an employee (annual leave days and status).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
//@HibernateSearchInfo(fieldInfoProvider = HibernateSearchAttrSchemaFieldInfoProvider::class, param = "employee")
@Table(
    name = "t_fibu_employee_valid_since_attr",
    uniqueConstraints = [UniqueConstraint(columnNames = ["employee_fk", "type", "valid_since"])],
    indexes = [Index(
        name = "idx_fk_t_fibu_employee_val_per_employee_id", columnList = "employee_fk"
    )]
)
@NamedQueries(
    NamedQuery(
        name = EmployeeValidSinceAttrDO.FIND_BY_TYPE_AND_DATE,
        query = "from EmployeeValidSinceAttrDO where employee.id=:employeeId and type=:type and validSince=:validSince",
    ),
    NamedQuery(
        name = EmployeeValidSinceAttrDO.FIND_OTHER_BY_TYPE_AND_DATE,
        query = "from EmployeeValidSinceAttrDO where employee.id=:employeeId and type=:type and validSince=:validSince and id!=:id",
    ),
)
@WithHistory
open class EmployeeValidSinceAttrDO : Serializable, AbstractBaseDO<Long>(), CandHHistoryEntryICustomizer {
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Column(name = "pk")
    override var id: Long? = null

    @PropertyInfo(i18nKey = "fibu.employee")
    @JsonIdentityReference(alwaysAsId = true)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "employee_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var employee: EmployeeDO? = null

    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "type", length = 30)
    open var type: EmployeeValidSinceAttrType? = null

    @PropertyInfo(i18nKey = "attr.validSince")
    @get:Column(name = "valid_since", nullable = false)
    open var validSince: LocalDate? = null

    @PropertyInfo(i18nKey = "value")
    @get:Column(name = "value", length = 255)
    open var value: String? = null

    @PropertyInfo(i18nKey = "comment")
    @get:Column(name = "comment", length = Constants.LENGTH_TEXT)
    open var comment: String? = null

    @CandHIgnore
    @get:JsonIgnore
    @get:Transient
    var annualLeave: BigDecimal?
        get() {
            checkType(EmployeeValidSinceAttrType.ANNUAL_LEAVE)
            return value?.let { runCatching { BigDecimal(it) }.getOrNull() }
        }
        set(value) {
            checkType(EmployeeValidSinceAttrType.ANNUAL_LEAVE)
            this.value = value?.toString()
        }

    @CandHIgnore
    @get:JsonIgnore
    @get:Transient
    var weeklyWorkingHours: BigDecimal?
        get() {
            checkType(EmployeeValidSinceAttrType.WEEKLY_HOURS)
            return value?.let { runCatching { BigDecimal(it) }.getOrNull() }
        }
        set(value) {
            checkType(EmployeeValidSinceAttrType.WEEKLY_HOURS)
            this.value = value?.toString()
        }

    @CandHIgnore
    @get:JsonIgnore
    @get:Transient
    var status: EmployeeStatus?
        get() {
            checkType(EmployeeValidSinceAttrType.STATUS)
            return value?.let { EmployeeStatus.safeValueOf(it) }
        }
        set(value) {
            checkType(EmployeeValidSinceAttrType.STATUS)
            this.value = value?.toString()
        }

    /**
     * Copies validSince, value and comment from other.
     * Please note: type, id and employee are not copied.
     * @param other the other EmployeeValidSinceAttrDO to copy from.
     */
    fun copyFrom(other: EmployeeValidSinceAttrDO) {
        this.validSince = other.validSince
        this.value = other.value
        this.comment = other.comment
    }

    override fun customize(historyEntry: HistoryEntryDO) {
        historyEntry.attributes?.filter { it.propertyName == "value" }?.forEach { attr ->
            if (type == EmployeeValidSinceAttrType.ANNUAL_LEAVE) {
                attr.setPropertyTypeClass(BigDecimal::class)
                attr.propertyName = buildPropertyName("annualLeave")
            } else if (type == EmployeeValidSinceAttrType.WEEKLY_HOURS) {
                attr.setPropertyTypeClass(BigDecimal::class)
                attr.propertyName = buildPropertyName("weeklyWorkingHours")
            } else if (type == EmployeeValidSinceAttrType.STATUS) {
                attr.setPropertyTypeClass(EmployeeStatus::class)
                attr.propertyName = buildPropertyName("status")
            }
        }
    }

    private fun buildPropertyName(propertyName: String): String {
        validSince ?: return propertyName
        return "$propertyName:$validSince"
    }

    private fun checkType(type: EmployeeValidSinceAttrType) {
        if (this.type != type) {
            throw IllegalArgumentException("Attribute is not $type: ${this.type}")
        }
    }

    override fun toString(): String {
        return JsonUtils.toJson(this)
    }

    companion object {
        internal const val FIND_BY_TYPE_AND_DATE = "EmployeeValidSinceAttr_FindByTypeAndDate"
        internal const val FIND_OTHER_BY_TYPE_AND_DATE = "EmployeeValidSinceAttr_FindOtherByTypeAndDate"
    }
}
