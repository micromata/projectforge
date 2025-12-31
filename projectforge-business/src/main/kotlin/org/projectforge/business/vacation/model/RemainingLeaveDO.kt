/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.vacation.model

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import java.math.BigDecimal
import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency
import org.projectforge.framework.json.IdOnlySerializer

/**
 * Remaining leave entries for employees per year.
 *
 * @author Kai Reinhard
 */
@Entity
@Indexed
@Table(
  name = "t_employee_remaining_leave",
  uniqueConstraints = [UniqueConstraint(columnNames = ["employee_id", "year"])],
  indexes = [jakarta.persistence.Index(name = "idx_fk_t_vacation_remaining_employee_id", columnList = "employee_id")]
)
@NamedQueries(
  NamedQuery(
    name = RemainingLeaveDO.FIND_BY_EMPLOYEE_ID_AND_YEAR,
    query = "from RemainingLeaveDO where employee.id=:employeeId and year=:year"
  )
)
open class RemainingLeaveDO : DefaultBaseDO() {
  /**
   * The employee.
   */
  @PropertyInfo(i18nKey = "vacation.employee")
  @IndexedEmbedded(includePaths = ["user.firstname", "user.lastname"])
  @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
  @get:ManyToOne(fetch = FetchType.LAZY)
  @get:JoinColumn(name = "employee_id", nullable = false)
  @JsonSerialize(using = IdOnlySerializer::class)
  open var employee: EmployeeDO? = null

  @PropertyInfo(i18nKey = "calendar.year")
  @get:Column(name = "year", nullable = false)
  open var year: Int = 0

  @PropertyInfo(i18nKey = "vacation.previousyearleave")
  @get:Column(name = "remaining_from_previous_year", nullable = true)
  open var remainingFromPreviousYear: BigDecimal? = null

  @PropertyInfo(i18nKey = "comment")
  @get:Column(length = 4000)
  open var comment: String? = null

  companion object {
    internal const val FIND_BY_EMPLOYEE_ID_AND_YEAR = "RemainingLeaveDO_FindByEmployeeIdAndYear"
  }
}
