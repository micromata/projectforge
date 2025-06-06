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

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*
import org.projectforge.Constants
import org.projectforge.common.StringHelper
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.time.PFDayUtils
import java.math.BigDecimal

/**
 * Das monatliche Gehalt eines festangestellten Mitarbeiters.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(
    name = "T_FIBU_EMPLOYEE_SALARY",
    uniqueConstraints = [UniqueConstraint(columnNames = ["employee_id", "year", "month"])],
    indexes = [Index(name = "idx_fk_t_fibu_employee_salary_employee_id", columnList = "employee_id")]
)
@NamedQueries(
    NamedQuery(
        name = EmployeeSalaryDO.SELECT_MIN_MAX_YEAR,
        query = "select min(year), max(year) from EmployeeSalaryDO",
    ),
    NamedQuery(
        name = EmployeeSalaryDO.SELECT_SALARIES_BY_MONTH,
        query = "from EmployeeSalaryDO where year = :year and month = :month",
    )
)
open class EmployeeSalaryDO : DefaultBaseDO() {

    /**
     * @return Zugehöriger Mitarbeiter.
     */
    // TODO: Support this type on the edit page
    @PropertyInfo(i18nKey = "fibu.employee")
    @IndexedEmbedded(includeDepth = 2)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:JoinColumn(name = "employee_id", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    open var employee: EmployeeDO? = null

    /**
     * @return Abrechnungsjahr.
     */
    @PropertyInfo(i18nKey = "calendar.year")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Column
    open var year: Int? = null

    /**
     * 1-based: 1 - January, 12 - December
     * @return Abrechnungsmonat.
     */
    @PropertyInfo(i18nKey = "calendar.month")
    @GenericField // was: @FullTextField(analyze = Analyze.NO)
    @get:Column
    open var month: Int? = null
        set(value) {
            field = PFDayUtils.validateMonthValue(value)
        }

    /**
     * Die Bruttoauszahlung an den Arbeitnehmer (inklusive AG-Anteil Sozialversicherungen).
     */
    @PropertyInfo(i18nKey = "fibu.employee.salary.bruttoMitAgAnteil")
    @get:Column(name = "brutto_mit_ag_anteil", scale = 2, precision = 12)
    open var bruttoMitAgAnteil: BigDecimal? = null

    @PropertyInfo(i18nKey = "comment")
    @FullTextField
    @get:Column(length = Constants.COMMENT_LENGTH)
    open var comment: String? = null

    @PropertyInfo(i18nKey = "fibu.employee.salary.type")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20)
    open var type: EmployeeSalaryType? = null

    val employeeId: Long?
        @Transient
        get() = if (this.employee == null) null else employee!!.id

    val formattedMonth: String
        @Transient
        get() = StringHelper.format2DigitNumber(month!!)

    val formattedYearAndMonth: String
        @Transient
        get() = year.toString() + "-" + StringHelper.format2DigitNumber(month!!)

    companion object {
        internal const val SELECT_MIN_MAX_YEAR = "EmployeeSalaryDO_SelectMinMaxYear"
        internal const val SELECT_SALARIES_BY_MONTH = "EmployeeSalaryDO_SelectByMonth"
    }
}
