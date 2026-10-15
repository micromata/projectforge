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
import org.projectforge.business.availability.AvailabilityDO
import org.projectforge.business.availability.AvailabilityDao
import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.business.vacation.model.*
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.business.vacation.service.ConflictingVacationsCache
import org.projectforge.business.vacation.service.VacationService
import org.projectforge.business.vacation.service.VacationStats
import org.projectforge.common.DateFormatType
import org.projectforge.framework.calendar.Holidays
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.LocalDatePeriod
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.ResultSet
import org.projectforge.rest.dto.Employee
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.Vacation
import org.projectforge.rest.dto.VacationChartDay
import org.projectforge.rest.dto.VacationReplacementAvailability
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
import java.time.LocalDate
import java.time.Year
import java.time.temporal.ChronoUnit

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
 * The vacation account ([VacationAccountRest]) links into this page with `?returnTo=/vacationAccount`.
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

    @Autowired
    private lateinit var availabilityDao: AvailabilityDao

    @Autowired
    private lateinit var employeeCache: EmployeeCache

    /**
     * What the form shows beside its fields, as it follows the form ([stats]). The same names as in [Vacation],
     * whose fields the client updates with it.
     */
    class VacationFormStats(
        var workingDays: BigDecimal? = null,
        var workingDaysFormatted: String? = null,
        var vacationDaysLeftInYear: BigDecimal? = null,
        var vacationDaysLeftInYearString: String? = null,
        var vacationDaysLeftAfterThis: BigDecimal? = null,
        var vacationDaysLeftAfterThisString: String? = null,
        var conflictingVacations: List<Vacation>? = null,
        var replacementAvailabilities: List<VacationReplacementAvailability>? = null,
        var conflict: Boolean? = null,
        var allowedStatus: List<VacationStatus>? = null,
        var chartDays: List<VacationChartDay>? = null,
    )

    /**
     * Columns a database `ORDER BY` can't sort as shown, see [computedSortProperties]: the mode (own, replacement,
     * manager, other) is computed from the logged-in user, the status is shown translated (sorted as displayed,
     * not by the enum's name) and the working days are computed from the period.
     */
    override val computedSortProperties: Map<String, (VacationDO) -> Comparable<*>?>
        get() = mapOf(
            "vacationModeString" to { vacation -> translate(vacation.getVacationmode().i18nKey) },
            "status" to { vacation -> vacation.status?.let { translate(it.i18nKey) } },
            "workingDaysFormatted" to { vacation -> VacationService.getVacationDays(vacation) },
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
            applyStats(vacation, computeStats(vacation, updateCache = true, withDaysLeft = mayReadDaysLeft(obj)))
        } else {
            applyListDays(vacation, obj)
        }
        return vacation
    }

    /**
     * The list's rows with the days without any substitute on duty of the entries the conflict cache flags (few, as
     * it holds current and future ones only): checked against the vacations of all their substitutes at once
     * ([VacationService.getUncoveredDays]), nothing is written to the cache.
     */
    override fun postProcessResultSet(
        resultSet: ResultSet<VacationDO>,
        request: HttpServletRequest,
        magicFilter: MagicFilter,
    ): ResultSet<*> {
        val result = super.postProcessResultSet(resultSet, request, magicFilter)
        val conflicting = resultSet.resultSet.filter { conflictingVacationsCache.hasConflict(it.id) && inBarRange(it) }
        if (conflicting.isNotEmpty()) {
            val uncovered = vacationService.getUncoveredDays(conflicting)
            result.resultSet.forEach { row ->
                (row as? Vacation)?.let { dto -> dto.uncoveredDays = uncovered[dto.id] }
            }
        }
        return result
    }

    /**
     * The days the list's bar draws the vacation with: its weekends and holidays. Its days without any substitute
     * on duty are added for all rows at once ([postProcessResultSet]).
     */
    private fun applyListDays(vacation: Vacation, obj: VacationDO) {
        vacation.nonWorkingDays = barNonWorkingDays(obj)
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
        applyStats(result, computeStats(result, updateCache = false, withDaysLeft = true))
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
        // Still offered (stored favorites carry it), but the period is pinned instead: it pages by month or year.
        elements.add(UIFilterElement("year", label = translate("calendar.year")))
        elements.add(UIFilterBooleanElement("special", label = translate("vacation.special")))
        elements.add(
            UIFilterElement(
                "period",
                label = translate("timePeriod"),
                filterType = UIFilterElement.FilterType.DATE,
                defaultFilter = true,
            )
        )
        elements.add(
            UIFilterObjectElement(
                "employee",
                label = translate("vacation.employee"),
                autoCompletion = AutoCompletion.getAutoCompletion4Employees(),
            ).also {
                // Pinned: the filter most lists of leave are looked at by, always on the filter row.
                it.defaultFilter = true
            }
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
        if (!vacationService.hasLoggedInUserUnrestrictedVacationView()) {
            filters.add(createVisibilityFilter())
        }
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
            NumberHelper.parseInteger(entry.plainValue)?.let { filters.add(VacationYearFilter(it)) }
        }
        // The vacations overlapping the period, by their dates in the query (not a search of the period's text):
        // ending on or after its begin and beginning on or before its end. Either bound may be open.
        source.entries.find { it.field == "period" }?.let { entry ->
            entry.synthetic = true
            PFDayUtils.parseDate(entry.value.fromValue)?.let { target.add(QueryFilter.ge("endDate", it)) }
            PFDayUtils.parseDate(entry.value.toValue)?.let { target.add(QueryFilter.le("startDate", it)) }
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

    /** With all vacations of the logged-in user's employee (with their substitutes), in one query. */
    private fun createVisibilityFilter(): VacationVisibilityFilter {
        val employeeId = ThreadLocalUserContext.userContext?.employeeId
            ?: return VacationVisibilityFilter(null, emptyList())
        val queryFilter = baseDao.createQueryFilter().also { it.add(QueryFilter.eq("employee.id", employeeId)) }
        return VacationVisibilityFilter(employeeId, baseDao.select(queryFilter, checkAccess = false))
    }

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
     * left without any of them, and the statuses the user may set. The successor of the React watch fields; the
     * conflict cache isn't written (the carry of the current year may be, see [VacationService.getVacationStats]).
     *
     * The employee is the entry's as far as the user may see it: HR staff the posted one, anybody else the stored
     * entry's employee (read-checked) or the own one for a new entry. Their days left only for HR staff, the
     * employee and the manager of the stored entry ([mayReadDaysLeft]): a substitute or a colleague of the same
     * group may read the entry, but not the remaining leave of its employee.
     */
    @AccessChecked("DAO: select access; employee: HR the posted one, else the stored entry's (find) or the own one; days left: HR, employee or manager")
    @PostMapping("stats")
    fun stats(@RequestBody postData: PostData<Vacation>): VacationFormStats {
        baseDao.hasLoggedInUserSelectAccess(throwException = true)
        val dto = postData.data
        var withDaysLeft = true
        if (!hasHrRights()) {
            val stored = dto.id?.let { baseDao.find(it) }
            val employeeDO = stored?.employee ?: employeeService.findByUserId(ThreadLocalUserContext.loggedInUserId)
            dto.employee = createEmployee(employeeDO)
            withDaysLeft = stored == null || mayReadDaysLeft(stored)
        }
        return computeStats(dto, updateCache = false, withDaysLeft = withDaysLeft)
    }

    /** HR staff, the entry's employee and its manager: who may see the remaining leave of the entry's employee. */
    private fun mayReadDaysLeft(vacation: VacationDO): Boolean {
        if (hasHrRights()) {
            return true
        }
        val ownEmployeeId = employeeService.findByUserId(ThreadLocalUserContext.loggedInUserId)?.id ?: return false
        return vacation.employee?.id == ownEmployeeId || vacation.manager?.id == ownEmployeeId
    }

    private fun computeStats(dto: Vacation, updateCache: Boolean, withDaysLeft: Boolean): VacationFormStats {
        val result = VacationFormStats()
        val vacationDO = VacationDO()
        dto.copyTo(vacationDO)
        dto.employee?.id?.takeIf { withDaysLeft }?.let { employeeId ->
            val employeeDO = EmployeeDO()
            employeeDO.id = employeeId
            val year = dto.startDate?.year ?: Year.now().value
            val vacationStats = vacationService.getVacationStats(employeeDO, year)
            result.vacationDaysLeftInYear = vacationStats.vacationDaysLeftInYear
            result.vacationDaysLeftInYearString = VacationStats.format(vacationStats.vacationDaysLeftInYear)
            daysLeftAfter(employeeDO, year, vacationDO)?.let {
                result.vacationDaysLeftAfterThis = it
                result.vacationDaysLeftAfterThisString = VacationStats.format(it)
            }
        }
        val startDate = dto.startDate
        val endDate = dto.endDate
        if (startDate != null && endDate != null) {
            result.workingDays = VacationService.getVacationDays(startDate, endDate, dto.halfDayBegin, dto.halfDayEnd)
            result.workingDaysFormatted = VacationStats.format(result.workingDays)
        }
        val user = ThreadLocalUserContext.loggedInUser!!
        // A period the chart draws only: the overlaps of a mistyped (or crafted) decade aren't searched for.
        if (startDate != null && endDate != null && inBarRange(vacationDO)) {
            val vacationOverlaps = vacationService.getVacationOverlaps(vacationDO, updateCache = updateCache)
            val uncoveredDays = vacationOverlaps.uncoveredDays
            // The substitutes' vacations are conflicts whoever they are: the entry needs them on duty.
            result.conflict = vacationOverlaps.conflict
            if (baseDao.hasHrRights(user)) {
                // Without their comments: the substitutes' entries are read past the DAO's privacy check
                // (VacationDao.afterLoad), and neither the chart nor the table shows them.
                result.conflictingVacations =
                    vacationOverlaps.otherVacations.map { other -> Vacation(other).also { it.comment = null } }
                result.replacementAvailabilities = vacationOverlaps.availabilities.mapNotNull {
                    replacementAvailability(it, user, null, null)
                }
                result.chartDays = chartDays(startDate, endDate, uncoveredDays.toSet())
            } else {
                // The substitutes are posted, so anybody could name anybody: the vacations the user may read anyway
                // (VacationDao.hasUserSelectAccess) are shown in full, all others reduced to their dates and status
                // (reducedConflict), and only from visibleFrom on, as the list shows a colleague's vacations. So
                // naming somebody tells no more than whether they are away while the entry's own period runs.
                val visibleFrom = LocalDate.now().minusDays(VISIBLE_PAST_DAYS)
                val readable = vacationOverlaps.otherVacations.filter { baseDao.hasUserSelectAccess(user, it, false) }
                result.conflictingVacations = vacationOverlaps.otherVacations.mapNotNull { other ->
                    if (readable.contains(other)) {
                        Vacation(other).also { it.comment = null }
                    } else {
                        reducedConflict(other, maxOf(startDate, visibleFrom), endDate)
                    }
                }
                // The same for the substitutes' availability entries (AvailabilityDao.hasUserSelectAccess).
                val readableAvailabilities =
                    vacationOverlaps.availabilities.filter { availabilityDao.hasUserSelectAccess(user, it, false) }
                result.replacementAvailabilities = vacationOverlaps.availabilities.mapNotNull { availability ->
                    if (readableAvailabilities.contains(availability)) {
                        replacementAvailability(availability, user, null, null)
                    } else {
                        replacementAvailability(availability, user, maxOf(startDate, visibleFrom), endDate)
                    }
                }
                // Before visibleFrom, a day is drawn uncovered only if the readable entries alone say so.
                val shownUncovered = uncoveredDays.filter { it >= visibleFrom } +
                        visibleUncoveredDays(
                            vacationDO,
                            uncoveredDays.filter { it < visibleFrom },
                            readable,
                            readableAvailabilities,
                        )
                result.chartDays = chartDays(startDate, endDate, shownUncovered.toSet())
            }
        }
        result.allowedStatus = baseDao.getAllowedStatus(user, vacationDO)
        return result
    }

    /**
     * A substitute's vacation the user may not read otherwise, as a conflict of the edited entry: only its days from
     * [from] to [until] (the part of the entry's period the user may be told about) with their status; no id,
     * comment, manager, substitutes or special flag. Null if nothing of it falls in there.
     */
    private fun reducedConflict(other: VacationDO, from: LocalDate, until: LocalDate): Vacation? {
        val otherStart = other.startDate ?: return null
        val otherEnd = other.endDate ?: return null
        val start = maxOf(otherStart, from)
        val end = minOf(otherEnd, until)
        if (end < start) {
            return null
        }
        return Vacation(
            employee = caches.getEmployeeIfNotInitialized(other.employee)?.let { Employee().apply { copyFromMinimal(it) } },
            startDate = start,
            startDateFormatted = PFDayUtils.format(start, DateFormatType.DATE),
            endDate = end,
            endDateFormatted = PFDayUtils.format(end, DateFormatType.DATE),
            // A half day cut off by the period isn't one of the days shown.
            halfDayBegin = other.halfDayBegin == true && start == otherStart,
            halfDayEnd = other.halfDayEnd == true && end == otherEnd,
            status = other.status,
            statusString = other.status?.let { translate(it.i18nKey) },
        ).also {
            it.workingDays = VacationService.getVacationDays(start, end, it.halfDayBegin, it.halfDayEnd)
            it.workingDaysFormatted = VacationStats.format(it.workingDays)
        }
    }

    /**
     * A substitute's availability entry as the form shows it, called as the user may see it
     * ([AvailabilityDao.labelFor]). Cut to the days from [from] to [until] if given (an entry the user may not read
     * otherwise, as [reducedConflict]); null if nothing of it falls in there.
     */
    private fun replacementAvailability(
        availability: AvailabilityDO,
        user: PFUserDO,
        from: LocalDate?,
        until: LocalDate?,
    ): VacationReplacementAvailability? {
        val entryStart = availability.startDate ?: return null
        val entryEnd = availability.endDate ?: return null
        val start = from?.let { maxOf(entryStart, it) } ?: entryStart
        val end = until?.let { minOf(entryEnd, it) } ?: entryEnd
        if (end < start) {
            return null
        }
        return VacationReplacementAvailability(
            // With its user resolved (EmployeeCache.getEmployee), whose name the minimal copy takes.
            employee = employeeCache.getEmployee(employeeCache.getEmployeeIdByUserId(availability.user?.id))
                ?.let { Employee().apply { copyFromMinimal(it) } },
            startDate = start,
            startDateFormatted = PFDayUtils.format(start, DateFormatType.DATE),
            endDate = end,
            endDateFormatted = PFDayUtils.format(end, DateFormatType.DATE),
            status = availability.status,
            statusString = availability.status?.let { translate(it.i18nKey) },
            label = availabilityDao.labelFor(user, availability),
            absent = availability.status?.absent == true,
        )
    }

    /**
     * The [uncovered] days (all substitutes away) that the [visible] vacations and absent availability entries
     * alone tell: those on which every substitute has one of them. As [VacationService.getVacationOverlaps] counts,
     * restricted to what is shown.
     */
    private fun visibleUncoveredDays(
        vacation: VacationDO,
        uncovered: List<LocalDate>,
        visible: List<VacationDO>,
        visibleAvailabilities: List<AvailabilityDO>,
    ): List<LocalDate> {
        val replacements = vacationService.collectAllReplacements(vacation)
        val absent = visibleAvailabilities.filter { it.status?.absent == true }
        return uncovered.filter { date ->
            replacements.all { replacement ->
                visible.any { it.employee?.id == replacement.id && it.isInBetween(date) } ||
                        absent.any { availability ->
                            employeeCache.getEmployeeIdByUserId(availability.user?.id) == replacement.id &&
                                    availability.startDate?.let { it <= date } == true &&
                                    availability.endDate?.let { it >= date } == true
                        }
            }
        }
    }

    /**
     * The days left in [year] with the edited entry counted as it is in the form instead of as stored (a new one
     * isn't stored at all), as [VacationService.getVacationStats] counts them.
     * Null without a period: then there's nothing to count.
     */
    private fun daysLeftAfter(employeeDO: EmployeeDO, year: Int, vacationDO: VacationDO): BigDecimal? {
        if (vacationDO.startDate == null || vacationDO.endDate == null) {
            return null
        }
        // Not special unless said so: the sum takes it only for the flag it adds up (special or not), never null.
        vacationDO.special = vacationDO.special ?: false
        val yearPeriod = LocalDatePeriod.wholeYear(year)
        val entries = vacationService.getVacationsListForPeriod(employeeDO, yearPeriod.begin, yearPeriod.end, true)
            .filter { it.id != vacationDO.id } + vacationDO
        return vacationService.getVacationStats(employeeDO, year, vacationEntries = entries).vacationDaysLeftInYear
    }

    private fun applyStats(dto: Vacation, stats: VacationFormStats) {
        stats.workingDays?.let {
            dto.workingDays = it
            dto.workingDaysFormatted = stats.workingDaysFormatted
        }
        dto.vacationDaysLeftInYear = stats.vacationDaysLeftInYear
        dto.vacationDaysLeftInYearString = stats.vacationDaysLeftInYearString
        dto.vacationDaysLeftAfterThis = stats.vacationDaysLeftAfterThis
        dto.vacationDaysLeftAfterThisString = stats.vacationDaysLeftAfterThisString
        dto.conflictingVacations = stats.conflictingVacations
        dto.replacementAvailabilities = stats.replacementAvailabilities
        dto.conflict = stats.conflict
        dto.allowedStatus = stats.allowedStatus
        dto.chartDays = stats.chartDays
    }

    /**
     * The axis of the form's chart: the period with [CHART_MARGIN_DAYS] before and after, so the vacations of the
     * substitutes show where they begin and end. None for a period beyond [CHART_MAX_DAYS] (a mistyped year).
     */
    private fun chartDays(startDate: LocalDate, endDate: LocalDate, uncovered: Set<LocalDate>): List<VacationChartDay>? {
        val first = startDate.minusDays(CHART_MARGIN_DAYS)
        val last = endDate.plusDays(CHART_MARGIN_DAYS)
        if (last < first || ChronoUnit.DAYS.between(first, last) > CHART_MAX_DAYS) {
            return null
        }
        return VacationChartDay.listOf(first, last, uncovered)
    }

    /** The list starts with the vacations overlapping the current calendar year (the pinned period). */
    override fun newMagicFilter(): MagicFilter {
        val filter = super.newMagicFilter()
        val today = PFDay.now()
        filter.entries.add(MagicFilterEntry("period").also {
            it.value.fromValue = today.beginOfYear.isoString
            it.value.toValue = today.endOfYear.isoString
            it.value.periodKind = "year"
        })
        return filter
    }

    private fun hasHrRights(): Boolean = baseDao.hasHrRights(ThreadLocalUserContext.loggedInUser)

    private fun getUserPref(): VacationDO {
        return userPrefService.ensureEntry("vacation", "newEntry", VacationDO())
    }

    companion object {
        private const val CHART_MARGIN_DAYS = 3L
        private const val CHART_MAX_DAYS = 400L

        /** The days back a colleague's vacation is visible (VacationDao.hasUserSelectAccess). */
        private const val VISIBLE_PAST_DAYS = 31L

        /** A period the days bar draws (list and vacation account): not beyond [CHART_MAX_DAYS] (a mistyped year). */
        internal fun inBarRange(obj: VacationDO): Boolean {
            val startDate = obj.startDate ?: return false
            val endDate = obj.endDate ?: return false
            return endDate >= startDate && ChronoUnit.DAYS.between(startDate, endDate) <= CHART_MAX_DAYS
        }

        /** The weekends and holidays of the vacation for the days bar; null for a period it doesn't draw. */
        internal fun barNonWorkingDays(obj: VacationDO): List<LocalDate>? {
            if (!inBarRange(obj)) {
                return null
            }
            val holidays = Holidays.instance
            return generateSequence(obj.startDate!!) { it.plusDays(1) }.takeWhile { it <= obj.endDate!! }
                .filter { !holidays.isWorkingDay(it) }.toList()
        }
    }
}
