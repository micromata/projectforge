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

package org.projectforge.rest

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.business.vacation.model.*
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.business.vacation.service.ConflictingVacationsCache
import org.projectforge.business.vacation.service.VacationService
import org.projectforge.business.vacation.service.VacationStats
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.dto.Employee
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.Vacation
import org.projectforge.ui.AutoCompletion
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.ValidationError
import org.projectforge.ui.filter.UIFilterBooleanElement
import org.projectforge.ui.filter.UIFilterElement
import org.projectforge.ui.filter.UIFilterListElement
import org.projectforge.ui.filter.UIFilterObjectElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.Year

/**
 * The layout-free REST endpoint of the vacation entries (menu "Vacation"), serving the hand-built list and edit
 * page of projectforge-next (see components/features/vacation). Replaces the React UILayout `VacationPagesRest`,
 * whose behaviour it takes over:
 *
 * - a new entry is of the logged-in user's employee (HR staff may preset another one by `?employee=<id>`) with
 *   the manager and replacement of the last saved one,
 * - the form shows the working days, the days left in the year and the vacations of the replacements overlapping
 *   this one; these follow the form through [stats] (the React watch fields),
 * - the statuses offered are the ones the user may set ([VacationDao.getAllowedStatus]),
 * - the employee is changeable by HR staff only.
 *
 * The vacation account (still React, `VacationAccountPageRest`) links into this page; the way back to it isn't
 * offered until the account is migrated as well.
 */
@RestController
@RequestMapping("${Rest.URL}/vacation")
class VacationEntityRest :
    AbstractDTOEntityRest<VacationDO, Vacation, VacationDao>(VacationDao::class.java, "vacation.title") {

    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var conflictingVacationsCache: ConflictingVacationsCache

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var vacationService: VacationService

    /**
     * What the form shows beside its fields, as it follows the form ([stats]). The same names as in [Vacation],
     * whose fields the client updates with it.
     */
    class VacationFormStats(
        var workingDays: BigDecimal? = null,
        var workingDaysFormatted: String? = null,
        var vacationDaysLeftInYear: BigDecimal? = null,
        var vacationDaysLeftInYearString: String? = null,
        var conflictingVacations: List<Vacation>? = null,
        var conflict: Boolean? = null,
        var allowedStatus: List<VacationStatus>? = null,
    )

    /**
     * The mode column (own, replacement, manager, other) is computed from the logged-in user, there's no database
     * column to sort on. See [computedSortProperties].
     */
    override val computedSortProperties: Map<String, (VacationDO) -> Comparable<*>?>
        get() = mapOf(
            "vacationModeString" to { vacation -> translate(vacation.getVacationmode().i18nKey) }
        )

    override fun transformForDB(dto: Vacation): VacationDO {
        val vacationDO = VacationDO()
        dto.copyTo(vacationDO)
        return vacationDO
    }

    /**
     * In edit mode with everything the form shows beside the fields, as stored. Only then the conflict cache is
     * updated, as the React edit page did on opening an entry.
     */
    override fun transformFromDB(obj: VacationDO, editMode: Boolean): Vacation {
        val vacation = Vacation()
        caches.initialize(obj)
        vacation.copyFrom(obj)
        if (conflictingVacationsCache.hasConflict(obj.id)) {
            vacation.conflict = true
        }
        Employee.restoreDisplayNames(vacation.otherReplacements)
        if (editMode) {
            vacation.employeeEditable = hasHrRights()
            applyStats(vacation, computeStats(vacation, updateCache = true))
        }
        return vacation
    }

    override fun newBaseDTO(request: HttpServletRequest?): Vacation {
        val vacation = getUserPref()
        val result = Vacation()
        var employeeDO: EmployeeDO? = null
        if (hasHrRights()) {
            val employeeId = NumberHelper.parseLong(request?.getParameter("employee"))
            employeeDO = if (employeeId != null) {
                // Unchecked: the HR vacation right is what entitles to choose any employee, read access to the
                // employee data (HR_EMPLOYEE) isn't needed for that.
                employeeDao.find(employeeId, checkAccess = false)
            } else {
                vacation.employee
            }
        }
        if (employeeDO == null) {
            // For non HR staff members, choose always the logged in employee:
            employeeDO = employeeService.findByUserId(ThreadLocalUserContext.loggedInUserId)
        }
        result.employee = createEmployee(employeeDO)
        vacation.manager?.let { result.manager = createEmployee(it) }
        vacation.replacement?.let { result.replacement = createEmployee(it) }
        result.status = VacationStatus.IN_PROGRESS
        result.employeeEditable = hasHrRights()
        applyStats(result, computeStats(result, updateCache = false))
        return result
    }

    private fun createEmployee(employeeDO: EmployeeDO?): Employee? {
        val id = employeeDO?.id ?: return null
        val employee = Employee()
        employeeDao.find(id, checkAccess = false)?.let { employee.copyFromMinimal(it) }
        return employee
    }

    /** Saves the edited entry's employee, manager and replacement as the preset of the next new one. */
    override fun onAfterSave(request: HttpServletRequest, obj: VacationDO, postData: PostData<Vacation>): ResponseAction {
        val vacation = getUserPref()
        vacation.employee = obj.employee
        vacation.manager = obj.manager
        vacation.replacement = obj.replacement
        return super.onAfterSave(request, obj, postData)
    }

    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        elements.add(
            UIFilterListElement("status", label = translate("vacation.status"), defaultFilter = true)
                .buildValues(VacationStatus::class.java)
        )
        elements.add(
            UIFilterListElement("assignment", label = translate("vacation.vacationmode"), defaultFilter = true)
                .buildValues(VacationMode.OWN, VacationMode.REPLACEMENT, VacationMode.MANAGER, VacationMode.OTHER)
        )
        elements.add(UIFilterBooleanElement("conflicts", label = translate("vacation.conflicts"), defaultFilter = true))
        elements.add(UIFilterElement("year", label = translate("calendar.year"), defaultFilter = true))
        elements.add(UIFilterBooleanElement("special", label = translate("vacation.special")))
        elements.add(
            UIFilterElement(
                "period",
                label = translate("timePeriod"),
                filterType = UIFilterElement.FilterType.DATE
            )
        )
        elements.add(
            UIFilterObjectElement(
                "employee",
                label = translate("vacation.employee"),
                autoCompletion = AutoCompletion.getAutoCompletion4Employees(),
            )
        )
        elements.add(
            UIFilterObjectElement(
                "replacement",
                label = translate("vacation.replacement"),
                autoCompletion = AutoCompletion.getAutoCompletion4Employees()
            )
        )
        elements.add(
            UIFilterObjectElement(
                "manager",
                label = translate("vacation.manager"),
                autoCompletion = AutoCompletion.getAutoCompletion4Employees()
            )
        )
    }

    override fun preProcessMagicFilter(target: QueryFilter, source: MagicFilter): List<CustomResultFilter<VacationDO>> {
        val filters = mutableListOf<CustomResultFilter<VacationDO>>()
        source.entries.find { it.field == "assignment" }?.let { entry ->
            entry.synthetic = true
            val values = entry.value.values
            if (!values.isNullOrEmpty()) {
                filters.add(VacationModeFilter(values.map { VacationMode.valueOf(it) }))
            }
        }
        source.entries.find { it.field == "conflicts" }?.let { entry ->
            entry.synthetic = true
            if (entry.value.value == "true") {
                filters.add(VacationConflictsFilter())
            }
        }
        source.entries.find { it.field == "year" }?.let { entry ->
            entry.synthetic = true
            NumberHelper.parseInteger(entry.value.value)?.let { filters.add(VacationYearFilter(it)) }
        }
        source.entries.find { it.field == "period" }?.let { entry ->
            entry.synthetic = true
            val fromDate = PFDayUtils.parseDate(entry.value.fromValue)
            val toDate = PFDayUtils.parseDate(entry.value.toValue)
            if (fromDate != null) {
                filters.add(VacationPeriodFilter(fromDate, toDate))
            }
        }
        // The pickers send the employee as `value.id`, which the generic processor doesn't read.
        source.entries.find { it.field == "employee" }?.let { entry ->
            entry.synthetic = true
            employeeIdOf(entry.value.id, entry.value.value)?.let { target.add(QueryFilter.eq("employee.id", it)) }
        }
        source.entries.find { it.field == "replacement" }?.let { entry ->
            entry.synthetic = true
            employeeIdOf(entry.value.id, entry.value.value)?.let { filters.add(VacationReplacementFilter(it)) }
        }
        source.entries.find { it.field == "manager" }?.let { entry ->
            entry.synthetic = true
            employeeIdOf(entry.value.id, entry.value.value)?.let { target.add(QueryFilter.eq("manager.id", it)) }
        }
        return filters
    }

    private fun employeeIdOf(id: Long?, value: String?): Long? = id ?: value?.toLongOrNull()

    /**
     * Manager and replacement are mandatory, as on the React form. Checked here, because `required` of
     * `@PropertyInfo` doesn't reach a reference ([org.projectforge.ui.ElementsRegistry] applies it to columns
     * only). The rules of the vacation itself (dates, days left, collisions) are [VacationService.validate]'s,
     * run by the dao on the write.
     */
    override fun validate(validationErrors: MutableList<ValidationError>, dto: Vacation) {
        super.validate(validationErrors, dto)
        listOf("manager" to dto.manager, "replacement" to dto.replacement).forEach { (field, value) ->
            if (value?.id == null) {
                validationErrors.add(
                    ValidationError(
                        translateMsg("validation.error.fieldRequired", translate("vacation.$field")),
                        fieldId = field,
                    )
                )
            }
        }
    }

    /**
     * What the form shows beside its fields for the state the user is editing: the working days of the period,
     * the days left in the year of its start, the overlapping vacations of the replacements and whether a day is
     * left without any of them, and the statuses the user may set. The successor of the React watch fields;
     * nothing is written, the conflict cache neither.
     *
     * The days left are those of the employee of the entry as far as the user may see it: HR staff the posted
     * one, anybody else the stored entry's employee (read-checked) or the own one for a new entry. A posted
     * employee of somebody else would tell their remaining leave otherwise.
     */
    @AccessChecked("DAO: select access; employee of the days left: HR the posted one, else the stored entry's (find) or the own one")
    @PostMapping("stats")
    fun stats(@RequestBody postData: PostData<Vacation>): VacationFormStats {
        baseDao.hasLoggedInUserSelectAccess(throwException = true)
        val dto = postData.data
        if (!hasHrRights()) {
            val employeeDO = dto.id?.let { baseDao.find(it)?.employee }
                ?: employeeService.findByUserId(ThreadLocalUserContext.loggedInUserId)
            dto.employee = createEmployee(employeeDO)
        }
        return computeStats(dto, updateCache = false)
    }

    private fun computeStats(dto: Vacation, updateCache: Boolean): VacationFormStats {
        val result = VacationFormStats()
        dto.employee?.id?.let { employeeId ->
            val employeeDO = EmployeeDO()
            employeeDO.id = employeeId
            val vacationStats = vacationService.getVacationStats(employeeDO, dto.startDate?.year ?: Year.now().value)
            result.vacationDaysLeftInYear = vacationStats.vacationDaysLeftInYear
            result.vacationDaysLeftInYearString = VacationStats.format(vacationStats.vacationDaysLeftInYear)
        }
        val startDate = dto.startDate
        val endDate = dto.endDate
        if (startDate != null && endDate != null) {
            result.workingDays = VacationService.getVacationDays(startDate, endDate, dto.halfDayBegin, dto.halfDayEnd)
            result.workingDaysFormatted = VacationStats.format(result.workingDays)
        }
        val vacationDO = VacationDO()
        dto.copyTo(vacationDO)
        val vacationOverlaps = vacationService.getVacationOverlaps(vacationDO, updateCache = updateCache)
        result.conflictingVacations = vacationOverlaps.otherVacations.map { Vacation(it) }
        result.conflict = vacationOverlaps.conflict
        result.allowedStatus = baseDao.getAllowedStatus(ThreadLocalUserContext.loggedInUser!!, vacationDO)
        return result
    }

    private fun applyStats(dto: Vacation, stats: VacationFormStats) {
        stats.workingDays?.let {
            dto.workingDays = it
            dto.workingDaysFormatted = stats.workingDaysFormatted
        }
        dto.vacationDaysLeftInYear = stats.vacationDaysLeftInYear
        dto.vacationDaysLeftInYearString = stats.vacationDaysLeftInYearString
        dto.conflictingVacations = stats.conflictingVacations
        dto.conflict = stats.conflict
        dto.allowedStatus = stats.allowedStatus
    }

    private fun hasHrRights(): Boolean = baseDao.hasHrRights(ThreadLocalUserContext.loggedInUser)

    private fun getUserPref(): VacationDO {
        return userPrefService.ensureEntry("vacation", "newEntry", VacationDO())
    }
}
