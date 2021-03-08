/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.IndexedEmbedded
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import java.time.LocalDate
import javax.persistence.*

/**
 * Repräsentiert einen Urlaub. Ein Urlaub ist einem ProjectForge-Mitarbeiter zugeordnet und enthält buchhalterische
 * Angaben.
 *
 * @author Florian Blumenstein
 */
@Entity
@Indexed
@Table(name = "t_employee_vacation",
        indexes = [javax.persistence.Index(name = "idx_fk_t_vacation_employee_id", columnList = "employee_id"),
            javax.persistence.Index(name = "idx_fk_t_vacation_manager_id", columnList = "manager_id"),
            javax.persistence.Index(name = "idx_fk_t_vacation_tenant_id", columnList = "tenant_id")])
@AUserRightId(value = "EMPLOYEE_VACATION", checkAccess = false)
open class VacationDO : DefaultBaseDO() {

    /**
     * The employee.
     */
    @PropertyInfo(i18nKey = "vacation.employee")
    @IndexedEmbedded(includePaths = ["user.firstname", "user.lastname"])
    @get:ManyToOne(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "employee_id", nullable = false)
    open var employee: EmployeeDO? = null

    val employeeId: Int?
        @Transient
        get() = employee?.id

    @PropertyInfo(i18nKey = "vacation.startdate")
    @get:Column(name = "start_date", nullable = false)
    open var startDate: LocalDate? = null

    @PropertyInfo(i18nKey = "vacation.enddate")
    @get:Column(name = "end_date", nullable = false)
    open var endDate: LocalDate? = null

    /**
     * Coverage (during leave).
     */
    @PropertyInfo(i18nKey = "vacation.replacement")
    @get:ManyToOne(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "replacement_id", nullable = false)
    open var replacement: EmployeeDO? = null

    /**
     * The manager.
     */
    @PropertyInfo(i18nKey = "vacation.manager")
    @get:ManyToOne(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "manager_id", nullable = false)
    open var manager: EmployeeDO? = null

    @PropertyInfo(i18nKey = "vacation.status")
    open var status: VacationStatus? = null
        @Enumerated(EnumType.STRING)
        @Column(name = "vacation_status", length = 30, nullable = false)
        get() = if (field == null) {
            VacationStatus.IN_PROGRESS
        } else field

    // Neede by Wicket in VacationListPage (could be removed after migration to ReactJS).
    @PropertyInfo(i18nKey = "vacation.vacationmode")
    private val vacationmode: VacationMode? = null

    @PropertyInfo(i18nKey = "vacation.special", tooltip = "vacation.special.tooltip")
    @get:Column(name = "is_special", nullable = false)
    open var special: Boolean? = null

    @PropertyInfo(i18nKey = "vacation.halfDayBegin", tooltip = "vacation.halfDayBegin.tooltip")
    @get:Column(name = "is_half_day_begin")
    open var halfDayBegin: Boolean? = null

    @PropertyInfo(i18nKey = "vacation.halfDayEnd", tooltip = "vacation.halfDayEnd.tooltip")
    @get:Column(name = "is_half_day_end")
    open var halfDayEnd: Boolean? = null

    @PropertyInfo(i18nKey = "comment")
    @Field
    @get:Column(length = 4000)
    open var comment: String? = null

    @Transient
    fun getVacationmode(): VacationMode {
        val currentUserId = ThreadLocalUserContext.getUserId()
        val employeeUserId = if (employee != null && employee!!.user != null) employee!!.user!!.pk else null
        val managerUserId = if (manager != null && manager!!.user != null) manager!!.user!!.pk else null
        if (currentUserId == employeeUserId) {
            return VacationMode.OWN
        }
        if (currentUserId == managerUserId) {
            return VacationMode.MANAGER
        }
        return if (isReplacement(currentUserId)) {
            VacationMode.REPLACEMENT
        } else VacationMode.OTHER
    }

    @Transient
    fun isReplacement(userId: Int?): Boolean {
        return userId != null && replacement?.userId == userId
    }
}
