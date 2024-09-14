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

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import jakarta.persistence.*
import mu.KotlinLogging
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.projectforge.Constants
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import java.io.Serializable
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
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
open class EmployeeValidityPeriodAttrDO : Serializable, AbstractBaseDO<Long>() {
    @get:Id
    @get:GeneratedValue
    @get:Column(name = "pk")
    override var id: Long? = null

    @PropertyInfo(i18nKey = "fibu.employee")
    @get:ManyToOne(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "employee_fk", nullable = false)
    open var employee: EmployeeDO? = null

    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "attribute", length = 30)
    open var attribute: EmployeeValidityPeriodAttrType? = null

    @get:Column(name = "valid_from", nullable = false)
    open var validFrom: LocalDate? = null

    @get:Column(name = "value", length = 255)
    open var value: String? = null

    @get:Column(name = "comment", length = Constants.LENGTH_TEXT)
    open var comment: String? = null

    override fun toString(): String {
        return JsonUtils.toJson(this)
    }
}
