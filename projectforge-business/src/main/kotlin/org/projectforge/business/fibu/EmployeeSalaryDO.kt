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

package org.projectforge.business.fibu

import org.hibernate.search.annotations.Analyze
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.IndexedEmbedded
import org.projectforge.common.StringHelper
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.utils.Constants
import java.math.BigDecimal
import javax.persistence.*

/**
 * Das monatliche Gehalt eines festangestellten Mitarbeiters.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_FIBU_EMPLOYEE_SALARY",
        uniqueConstraints = [UniqueConstraint(columnNames = ["employee_id", "year", "month"])],
        indexes = [Index(name = "idx_fk_t_fibu_employee_salary_employee_id", columnList = "employee_id"),
            Index(name = "idx_fk_t_fibu_employee_salary_tenant_id", columnList = "tenant_id")])
@NamedQueries(
        NamedQuery(name = EmployeeSalaryDO.SELECT_MIN_MAX_YEAR, query = "select min(year), max(year) from EmployeeSalaryDO"))
open class EmployeeSalaryDO : DefaultBaseDO() {

    /**
     * @return Zugehöriger Mitarbeiter.
     */
    // TODO: Support this type on the edit page
    @PropertyInfo(i18nKey = "fibu.employee")
    @IndexedEmbedded(depth = 2)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "employee_id", nullable = false)
    open var employee: EmployeeDO? = null

    /**
     * @return Abrechnungsjahr.
     */
    @PropertyInfo(i18nKey = "calendar.year")
    @Field(analyze = Analyze.NO)
    @get:Column
    open var year: Int? = null

    /**
     * @return Abrechnungsmonat.
     */
    @PropertyInfo(i18nKey = "calendar.month")
    @Field(analyze = Analyze.NO)
    @get:Column
    open var month: Int? = null

    /**
     * Die Bruttoauszahlung an den Arbeitnehmer (inklusive AG-Anteil Sozialversicherungen).
     */
    @PropertyInfo(i18nKey = "fibu.employee.salary.bruttoMitAgAnteil")
    @get:Column(name = "brutto_mit_ag_anteil", scale = 2, precision = 12)
    open var bruttoMitAgAnteil: BigDecimal? = null

    @PropertyInfo(i18nKey = "comment")
    @Field
    @get:Column(length = Constants.COMMENT_LENGTH)
    open var comment: String? = null

    @PropertyInfo(i18nKey = "fibu.employee.salary.type")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20)
    open var type: EmployeeSalaryType? = null

    val employeeId: Int?
        @Transient
        get() = if (this.employee == null) null else employee!!.id

    val formattedMonth: String
        @Transient
        get() = StringHelper.format2DigitNumber(month!! + 1)

    val formattedYearAndMonth: String
        @Transient
        get() = year.toString() + "-" + StringHelper.format2DigitNumber(month!! + 1)

    companion object {
        internal const val SELECT_MIN_MAX_YEAR = "EmployeeSalaryDO_SelectMinMaxYear"
    }
}
