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

package org.projectforge.business.fibu

import com.fasterxml.jackson.annotation.JsonIdentityReference
import jakarta.persistence.*
import mu.KotlinLogging
import org.projectforge.Constants
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.candh.CandHHistoryEntryICustomizer
import org.projectforge.framework.persistence.candh.CandHIgnore
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import org.projectforge.framework.persistence.history.HistoryEntryDO
import org.projectforge.framework.persistence.history.WithHistory
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate

private val log = KotlinLogging.logger {}

/**
 * Represents timeable attributes of an employee (annual leave days and status).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
//@HibernateSearchInfo(fieldInfoProvider = HibernateSearchAttrSchemaFieldInfoProvider::class, param = "employee")
@Table(
    name = "t_fibu_employee_validity_period_attr",
    indexes = [Index(
        name = "idx_fk_t_fibu_employee_val_per_employee_id", columnList = "employee_id"
    )]
)
@WithHistory
open class EmployeeValidityPeriodAttrDO : Serializable, AbstractBaseDO<Long>(), CandHHistoryEntryICustomizer {
    @get:Id
    @get:GeneratedValue
    @get:Column(name = "pk")
    override var id: Long? = null

    @PropertyInfo(i18nKey = "fibu.employee")
    @JsonIdentityReference(alwaysAsId = true)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "employee_fk", nullable = false)
    open var employee: EmployeeDO? = null

    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "type", length = 30)
    open var type: EmployeeValidityPeriodAttrType? = null

    @get:Column(name = "valid_from", nullable = false)
    open var validFrom: LocalDate? = null

    @get:Column(name = "value", length = 255)
    open var value: String? = null

    @get:Column(name = "comment", length = Constants.LENGTH_TEXT)
    open var comment: String? = null


    @CandHIgnore
    @get:Transient
    var annualLeave: BigDecimal?
        get() {
            checkType(EmployeeValidityPeriodAttrType.ANNUAL_LEAVE)
            return value?.let { runCatching { BigDecimal(it) }.getOrNull() }
        }
        set(value) {
            checkType(EmployeeValidityPeriodAttrType.ANNUAL_LEAVE)
            this.value = value?.toString()
        }

    @get:Transient
    var status: EmployeeStatus?
        get() {
            checkType(EmployeeValidityPeriodAttrType.STATUS)
            return value?.let { EmployeeStatus.safeValueOf(it) }
        }
        set(value) {
            checkType(EmployeeValidityPeriodAttrType.STATUS)
            this.value = value?.toString()
        }

    override fun customize(historyEntry: HistoryEntryDO) {
        historyEntry.attributes?.filter { it.propertyName == "value" }?.forEach { attr ->
            if (type == EmployeeValidityPeriodAttrType.ANNUAL_LEAVE) {
                attr.setPropertyTypeClass(BigDecimal::class)
                attr.propertyName = buildPropertyName("annualLeave")
            } else if (type == EmployeeValidityPeriodAttrType.STATUS) {
                attr.setPropertyTypeClass(EmployeeStatus::class)
                attr.propertyName = buildPropertyName("status")
            }
        }
    }

    private fun buildPropertyName(propertyName: String): String {
        validFrom ?: return propertyName
        return "$propertyName:$validFrom"
    }

    private fun checkType(type: EmployeeValidityPeriodAttrType) {
        if (this.type != type) {
            throw IllegalArgumentException("Attribute is not $type: ${this.type}")
        }
    }

    override fun toString(): String {
        return JsonUtils.toJson(this)
    }
}
