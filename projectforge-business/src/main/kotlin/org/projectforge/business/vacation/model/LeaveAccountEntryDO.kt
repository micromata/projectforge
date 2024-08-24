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

package org.projectforge.business.vacation.model

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import java.math.BigDecimal
import java.time.LocalDate
import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency

/**
 * You may add manual correction entries to the leave account for an employee, e. g. for special leave days or for adding
 * or substracting leave days from previous employer.
 *
 * Examples:
 * * Due to special agreements some vacation days of the overlap period has to be preserved over the the end of
 * vacation year, or
 * * The employee hºas vacation days left from the previous employer.
 *
 * @author Kai Reinhard
 */
@Entity
@Indexed
@Table(name = "t_employee_leave_account_entry",
        indexes = [jakarta.persistence.Index(name = "idx_fk_t_leave_account_employee_id", columnList = "employee_id")])
@NamedQueries(NamedQuery(name = LeaveAccountEntryDO.FIND_BY_EMPLOYEE_ID_AND_DATEPERIOD,
        query = "from LeaveAccountEntryDO where employee.id=:employeeId and date>=:fromDate and date<=:toDate and deleted=false order by date desc"))
open class LeaveAccountEntryDO : DefaultBaseDO() {
    /**
     * The employee.
     */
    @PropertyInfo(i18nKey = "fibu.employee", required = true)
    @IndexedEmbedded(includePaths = ["user.firstname", "user.lastname"])
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "employee_id", nullable = false)
    open var employee: EmployeeDO? = null

    @PropertyInfo(i18nKey = "date", required = true)
    @get:Column(name = "date", nullable = false)
    open var date: LocalDate? = null

    @PropertyInfo(i18nKey = "vacation.leaveAccountEntry.amount", required = true)
    @get:Column(nullable = true)
    open var amount: BigDecimal? = null

    @PropertyInfo(i18nKey = "description")
    @get:Column(nullable = true)
    open var description: String? = null

    companion object {
        internal const val FIND_BY_EMPLOYEE_ID_AND_DATEPERIOD = "LeaveAccountEntryDO_FindByEmployeeIdAndDatePeriod"
    }
}
