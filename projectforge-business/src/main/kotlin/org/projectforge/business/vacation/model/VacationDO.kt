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

package org.projectforge.business.vacation.model

import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.IndexedEmbedded
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.util.*
import javax.persistence.*

/**
 * Repräsentiert einen Urlaub. Ein Urlaub ist einem ProjectForge-Mitarbeiter zugeordnet und enthält buchhalterische
 * Angaben.
 *
 * @author Florian Blumenstein
 */
@Entity
@Indexed
@Table(name = "t_employee_vacation", indexes = [javax.persistence.Index(name = "idx_fk_t_vacation_employee_id", columnList = "employee_id"), javax.persistence.Index(name = "idx_fk_t_vacation_manager_id", columnList = "manager_id"), javax.persistence.Index(name = "idx_fk_t_vacation_tenant_id", columnList = "tenant_id")])
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

    @PropertyInfo(i18nKey = "vacation.startdate")
    @get:Temporal(TemporalType.DATE)
    @get:Column(name = "start_date", nullable = false)
    open var startDate: Date? = null

    @PropertyInfo(i18nKey = "vacation.enddate")
    @get:Temporal(TemporalType.DATE)
    @get:Column(name = "end_date", nullable = false)
    open var endDate: Date? = null

    /**
     * The substitutions.
     */
    @PropertyInfo(i18nKey = "vacation.substitution")
    @get:ManyToMany
    @get:JoinTable(name = "t_employee_vacation_substitution", joinColumns = [JoinColumn(name = "vacation_id", referencedColumnName = "PK")], inverseJoinColumns = [JoinColumn(name = "substitution_id", referencedColumnName = "PK")], indexes = [Index(name = "idx_fk_t_employee_vacation_substitution_vacation_id", columnList = "vacation_id"), Index(name = "idx_fk_t_employee_vacation_substitution_substitution_id", columnList = "substitution_id")])
    open var substitutions: Set<EmployeeDO>? = HashSet()

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

    //TODO FB: Wird leider nur über dem Feld ausgewertet und nicht an der Methode.
    //Feld wird eigentlich nicht benötigt
    @PropertyInfo(i18nKey = "vacation.vacationmode")
    private val vacationmode: VacationMode? = null

    @PropertyInfo(i18nKey = "vacation.isSpecial")
    @get:Column(name = "is_special", nullable = false)
    open var special: Boolean? = null

    @PropertyInfo(i18nKey = "vacation.isHalfDay")
    @get:Column(name = "is_half_day")
    open var halfDay: Boolean? = null

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
        return if (isSubstitution(currentUserId)) {
            VacationMode.SUBSTITUTION
        } else VacationMode.OTHER
    }

    @Transient
    fun isSubstitution(userId: Int?): Boolean {
        return userId != null && substitutions != null && substitutions!!.stream()
                .map<PFUserDO> { it.user }
                .map<Int> { it.pk }
                .anyMatch { pk -> pk == userId }

    }

}
