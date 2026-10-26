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
import org.projectforge.business.availability.AbsenceService
import org.projectforge.business.availability.AvailabilityConfig
import org.projectforge.business.availability.AvailabilityDO
import org.projectforge.business.availability.AvailabilityDao
import org.projectforge.business.availability.AvailabilityLocation
import org.projectforge.business.availability.AvailabilityConflictsFilter
import org.projectforge.business.availability.AvailabilityStatus
import org.projectforge.business.availability.ConflictingAvailabilitiesCache
import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.vacation.service.VacationService
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.ResultSet
import org.projectforge.rest.dto.Availability
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.User
import org.projectforge.rest.dto.Vacation
import org.projectforge.rest.dto.VacationChartDay
import org.projectforge.rest.dto.VacationReplacementAvailability
import org.projectforge.ui.AutoCompletion
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
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
import java.time.LocalDate

/**
 * The layout-free REST endpoint of the availability entries (menu "Availability"), serving the hand-built list and
 * edit page of projectforge-next (see components/features/availability). Replaces the React UILayout
 * `AvailabilityPagesRest`:
 *
 * - an entry is of a user, not of an employee: freelancers without an employee record enter their availability, too,
 * - a new entry is of the logged-in user (HR staff may preset another one by `?user=<id>`),
 * - the user is changeable by HR staff only ([Availability.userEditable]),
 * - the types offered are the configured ones ([AvailabilityConfig], [Availability.availableTypes]),
 * - the internal comment is offered to the user and HR staff only ([Availability.internalCommentVisible]); for
 *   anybody else the dao doesn't send it nor the type ([AvailabilityDao.afterLoad]),
 * - optional substitutes: an entry is in conflict if all of them are away on a day of it, shown in the form with
 *   their absences ([stats]), in the list and by the menu's badge ([ConflictingAvailabilitiesCache]).
 */
@RestController
@RequestMapping("${Rest.URL}/availability")
class AvailabilityEntityRest :
    AbstractDTOEntityRest<AvailabilityDO, Availability, AvailabilityDao>(
        AvailabilityDao::class.java,
        "availability.title"
    ) {

    @Autowired
    private lateinit var absenceService: AbsenceService

    @Autowired
    private lateinit var availabilityConfig: AvailabilityConfig

    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var conflictingAvailabilitiesCache: ConflictingAvailabilitiesCache

    @Autowired
    private lateinit var employeeCache: EmployeeCache

    @Autowired
    private lateinit var substituteAbsenceReducer: SubstituteAbsenceReducer

    @Autowired
    private lateinit var vacationService: VacationService

    @Autowired
    private lateinit var vacationRest: VacationEntityRest

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    /**
     * What the form shows of the substitutes' absences, as it follows the form ([stats]). The same names as in
     * [Availability], whose fields the client updates with it.
     */
    class AvailabilityFormStats(
        var conflict: Boolean? = null,
        var substituteVacations: List<Vacation>? = null,
        var substituteAvailabilities: List<VacationReplacementAvailability>? = null,
        var chartDays: List<VacationChartDay>? = null,
    )

    /**
     * Status and location are shown translated: sorted as displayed, not by the enum's name. The working days are
     * computed from the period. The type and the begin as shown, too: the order of the database would reveal the types
     * hidden from the viewer ([AvailabilityDao.afterLoad]) and the real begin of an entry clipped for the viewer
     * ([AbsenceService.shownTo]).
     */
    override val computedSortProperties: Map<String, (AvailabilityDO) -> Comparable<*>?>
        get() = mapOf(
            "type" to { obj -> obj.type },
            "startDate" to { obj -> shown(obj).startDate },
            // Computed from the period shown, as the list shows them.
            "workingDaysFormatted" to { obj ->
                shown(obj).let { entry ->
                    val start = entry.startDate
                    val end = entry.endDate
                    if (start != null && end != null) VacationService.getVacationDays(start, end) else null
                }
            },
            "status" to { obj -> obj.status?.let { translate(it.i18nKey) } },
            "location" to { obj -> obj.location?.let { translate(it.i18nKey) } },
        )

    /** The entry as the logged-in user may see it ([AbsenceService.shownTo]), the list checked the access. */
    private fun shown(obj: AvailabilityDO): AvailabilityDO =
        absenceService.shownTo(ThreadLocalUserContext.loggedInUser, obj) ?: obj

    override fun transformForDB(dto: Availability): AvailabilityDO {
        val availabilityDO = AvailabilityDO()
        dto.copyTo(availabilityDO)
        return availabilityDO
    }

    /**
     * The entry of somebody else as far as the logged-in user may see it ([AbsenceService.shownTo]): e.g. of a group
     * mate from today on, its real begin (and its timestamps, which would tell it) not sent ([Availability.startHidden]).
     */
    override fun transformFromDB(obj: AvailabilityDO, editMode: Boolean): Availability {
        val availability = Availability()
        caches.initialize(obj)
        // The dao checked the access: shown at least from some day on.
        val shown = absenceService.shownTo(ThreadLocalUserContext.loggedInUser, obj) ?: obj
        availability.copyFrom(shown)
        if (shown !== obj) {
            availability.startHidden = true
        }
        if (conflictingAvailabilitiesCache.hasConflict(obj.id)) {
            availability.conflict = true
        }
        if (editMode) {
            applyFormInfo(availability, obj)
            applyStats(availability, computeStats(availability))
        } else {
            availability.nonWorkingDays = SubstituteAbsenceReducer.barNonWorkingDays(shown.startDate, shown.endDate)
        }
        return availability
    }

    /**
     * The list's rows with the days without any substitute on duty of the entries the conflict cache flags, for the
     * days bar: checked against the absences of all their substitutes at once
     * ([VacationService.getUncoveredDaysOfAvailabilities]). For anybody but HR staff from today on only, as the
     * conflict flag counts them: a past day would tell that the substitutes were away, e.g. sick (see
     * [SubstituteAbsenceReducer]).
     */
    override fun postProcessResultSet(
        resultSet: ResultSet<AvailabilityDO>,
        request: HttpServletRequest,
        magicFilter: MagicFilter,
    ): ResultSet<*> {
        val result = super.postProcessResultSet(resultSet, request, magicFilter)
        val conflicting = resultSet.resultSet.filter {
            conflictingAvailabilitiesCache.hasConflict(it.id) &&
                    SubstituteAbsenceReducer.inChartRange(it.startDate, it.endDate)
        }
        if (conflicting.isNotEmpty()) {
            val uncovered = vacationService.getUncoveredDaysOfAvailabilities(conflicting)
            val today = if (hasHrRights()) null else LocalDate.now()
            result.resultSet.forEach { row ->
                (row as? Availability)?.let { dto ->
                    val days = uncovered[dto.id] ?: return@let
                    dto.uncoveredDays = if (today == null) days else days.filter { it >= today }
                }
            }
        }
        return result
    }

    override fun newBaseDTO(request: HttpServletRequest?): Availability {
        val result = Availability()
        val userDO = NumberHelper.parseLong(request?.getParameter("user"))
            ?.takeIf { hasHrRights() } // The HR vacation right is what entitles to choose any user.
            ?.let { userGroupCache.getUser(it) }
            ?: ThreadLocalUserContext.loggedInUser
        userDO?.let {
            val user = User()
            user.copyFromMinimal(it)
            result.user = user
        }
        result.status = AvailabilityStatus.NOT_AVAILABLE
        val obj = AvailabilityDO()
        result.copyTo(obj)
        applyFormInfo(result, obj)
        return result
    }

    /**
     * Prepares a new vacation from the posted availability entry ("Als Urlaub bearbeiten"), see
     * [VacationEntityRest.cloneFromAvailability]. The entry itself stays as it is.
     */
    @AccessChecked("DAO: select access; the prepared vacation is saved (and access-checked) only by the vacation's save")
    @PostMapping("switch2Vacation")
    fun switch2Vacation(request: HttpServletRequest, @RequestBody postData: PostData<Availability>): ResponseAction {
        baseDao.hasLoggedInUserSelectAccess(throwException = true)
        return vacationRest.cloneFromAvailability(request, postData.data)
    }

    /**
     * A new availability entry prepared from a vacation (the switch from its form, see
     * [VacationEntityRest.switch2Availability]): its employee's user, period, comment and replacements as
     * substitutes. Half days and the special flag have no counterpart and are dropped; status and location are
     * the presets of any new entry ([newBaseDTO]). Nothing is saved; the prepared entry travels under
     * `variables.data`, where the client (`convertEntity` in projectforge-next) reads it.
     */
    fun cloneFromVacation(request: HttpServletRequest, vacation: Vacation): ResponseAction {
        val availability = newBaseDTO(request)
        if (hasHrRights()) {
            // Anybody else gets the own user anyway (newBaseDTO), as for any new entry.
            employeeCache.getUserByEmployee(vacation.employee?.id)?.let { availability.user = toUser(it) }
        }
        availability.startDate = vacation.startDate
        availability.endDate = vacation.endDate
        availability.comment = vacation.comment
        val substitutes = (listOfNotNull(vacation.replacement) + vacation.otherReplacements.orEmpty())
            .mapNotNull { employeeCache.getUserByEmployee(it.id) }
            .filter { it.id != availability.user?.id }
            .distinctBy { it.id }
            .map { toUser(it) }
        availability.substitutes = substitutes.ifEmpty { null }
        val obj = AvailabilityDO()
        availability.copyTo(obj)
        applyFormInfo(availability, obj)
        return ResponseAction(targetType = TargetType.UPDATE).addVariable("data", availability)
    }

    private fun toUser(userDO: PFUserDO): User = User().also { it.copyFromMinimal(userDO) }

    /** What the form needs beside the entry's values. */
    private fun applyFormInfo(availability: Availability, obj: AvailabilityDO) {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        availability.userEditable = hasHrRights()
        availability.internalCommentVisible = obj.user == null || baseDao.isInternalCommentVisibleFor(user, obj)
        // The stored type is always offered, also one removed from the configuration meanwhile.
        availability.availableTypes = (availabilityConfig.getTypes() + listOfNotNull(obj.type?.takeIf { it.isNotBlank() }))
            .distinct()
    }

    /**
     * What the form shows of the substitutes' absences for the state the user is editing: their vacations and
     * availability entries overlapping the period (as far as the user may be told, see [SubstituteAbsenceReducer]) and
     * whether a day is left without any of them on duty. Nothing is written.
     *
     * The user is the entry's as far as the logged-in user may see it: HR staff the posted one, anybody else the stored
     * entry's user (read-checked) or the own one for a new entry; it is no substitute of its own entry.
     */
    @AccessChecked("DAO: select access; user: HR the posted one, else the stored entry's (find) or the own one")
    @PostMapping("stats")
    fun stats(@RequestBody postData: PostData<Availability>): AvailabilityFormStats {
        baseDao.hasLoggedInUserSelectAccess(throwException = true)
        val dto = postData.data
        if (!hasHrRights()) {
            val userDO = dto.id?.let { baseDao.find(it) }?.user ?: ThreadLocalUserContext.loggedInUser
            dto.user = caches.getUserIfNotInitialized(userDO)?.let { User().apply { copyFromMinimal(it) } }
        }
        return computeStats(dto)
    }

    private fun computeStats(dto: Availability): AvailabilityFormStats {
        val result = AvailabilityFormStats()
        val startDate = dto.startDate
        val endDate = dto.endDate
        val substituteIds = dto.substitutes.orEmpty().mapNotNull { it.id }.filter { it != dto.user?.id }.distinct()
        // A period the chart draws only: the absences of a mistyped (or crafted) decade aren't searched for.
        if (startDate == null || endDate == null || substituteIds.isEmpty() ||
            !SubstituteAbsenceReducer.inChartRange(startDate, endDate)
        ) {
            return result
        }
        val entry = AvailabilityDO()
        dto.copyTo(entry)
        val absences = vacationService.getSubstituteAbsences(substituteIds, startDate, endDate)
        val uncoveredDays = vacationService.uncoveredDaysOfAvailability(entry, absences, substituteIds)
        val shown = substituteAbsenceReducer.reduce(
            ThreadLocalUserContext.requiredLoggedInUser,
            startDate,
            endDate,
            substituteIds.map { SubstituteAbsenceReducer.Substitute(employeeCache.getEmployeeIdByUserId(it), it) },
            absences.vacations.sortedBy { it.startDate },
            absences.availabilities.filter { it.id != dto.id }.sortedBy { it.startDate },
            uncoveredDays,
        )
        // The substitutes are users: the chart's rows are keyed by them, so a vacation tells its employee's user.
        shown.substituteVacations?.forEach { vacation ->
            vacation.employee?.let { employee ->
                employee.user = employeeCache.getUserByEmployee(employee.id)?.let { User(it) }
            }
        }
        result.conflict = shown.conflict
        result.substituteVacations = shown.substituteVacations
        result.substituteAvailabilities = shown.substituteAvailabilities
        result.chartDays = shown.chartDays
        return result
    }

    private fun applyStats(dto: Availability, stats: AvailabilityFormStats) {
        dto.conflict = stats.conflict ?: dto.conflict
        dto.substituteVacations = stats.substituteVacations
        dto.substituteAvailabilities = stats.substituteAvailabilities
        dto.chartDays = stats.chartDays
    }

    /**
     * The user is mandatory (`required` of `@PropertyInfo` doesn't reach a reference, see
     * [org.projectforge.ui.ElementsRegistry]); the errors of [AvailabilityDao.validate] are shown at their fields, the
     * dao checks them on the write, too. Not for the entry of somebody else unless HR staff: the write is refused then
     * anyway, and the collision check mustn't tell anybody when that person is away.
     */
    override fun validate(validationErrors: MutableList<ValidationError>, dto: Availability) {
        super.validate(validationErrors, dto)
        if (dto.user?.id == null) {
            validationErrors.add(
                ValidationError(
                    translateMsg("validation.error.fieldRequired", translate("availability.user")),
                    fieldId = "user",
                )
            )
            return
        }
        if (!hasHrRights() && dto.user?.id != ThreadLocalUserContext.loggedInUserId) {
            return // Refused by the access check of the write.
        }
        val availability = AvailabilityDO()
        dto.copyTo(availability)
        baseDao.validate(availability)?.let { error ->
            val fieldId = when {
                error == AvailabilityDao.ERROR_STATUS_NOT_SET -> "status"
                error == AvailabilityDao.ERROR_SUBSTITUTE_SELF -> "substitutes"
                error == AvailabilityDao.ERROR_DATE_NOT_SET && dto.startDate == null -> "startDate"
                else -> "endDate"
            }
            validationErrors.add(ValidationError(translate(error), fieldId = fieldId))
        }
    }

    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        elements.add(
            UIFilterListElement("status", label = translate("availability.status"), defaultFilter = true)
                .buildValues(AvailabilityStatus::class.java)
        )
        elements.add(
            UIFilterListElement("location", label = translate("availability.location"), defaultFilter = true)
                .buildValues(AvailabilityLocation::class.java)
        )
        elements.add(
            UIFilterBooleanElement("conflicts", label = translate("availability.conflicts"), defaultFilter = true)
        )
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
                "user",
                label = translate("availability.user"),
                autoCompletion = AutoCompletion.getAutoCompletion4Users(),
            ).also {
                it.defaultFilter = true
            }
        )
    }

    override fun preProcessMagicFilter(
        target: QueryFilter,
        source: MagicFilter
    ): List<CustomResultFilter<AvailabilityDO>> {
        val filters = mutableListOf<CustomResultFilter<AvailabilityDO>>()
        source.entries.find { it.field == "conflicts" }?.let { entry ->
            entry.synthetic = true
            if (entry.value.value == "true") {
                filters.add(AvailabilityConflictsFilter())
            }
        }
        if (!hasHrRights()) {
            // The type and the internal comment are hidden from others (AvailabilityDao.afterLoad), but a filter or an
            // order by them in the query would still tell their values by the rows returned.
            source.entries.filter { it.field in PERSONAL_FIELDS }.forEach { it.synthetic = true }
            source.sortProperties.removeIf { it.property in PERSONAL_FIELDS }
        }
        // The entries overlapping the period, by their dates in the query: ending on or after its begin and beginning
        // on or before its end. Either bound may be open.
        source.entries.find { it.field == "period" }?.let { entry ->
            entry.synthetic = true
            PFDayUtils.parseDate(entry.value.fromValue)?.let { target.add(QueryFilter.ge("endDate", it)) }
            PFDayUtils.parseDate(entry.value.toValue)?.let { until ->
                target.add(QueryFilter.le("startDate", until))
                // By the part shown, not the real begin the query reads (AbsenceService.shownTo).
                val viewer = ThreadLocalUserContext.loggedInUser
                filters.add(
                    ShownAbsenceFilter({ absenceService.shownTo(viewer, it) }, { it.startDate }, { it.endDate }, until)
                )
            }
        }
        // The pickers send the user as `value.id`, which the generic processor doesn't read.
        source.entries.find { it.field == "user" }?.let { entry ->
            entry.synthetic = true
            (entry.value.id ?: entry.value.value?.toLongOrNull())?.let {
                target.add(QueryFilter.eq("user.id", it))
            }
        }
        return filters
    }

    /** The list starts with the entries overlapping the current calendar year (the pinned period). */
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

    companion object {
        /** Only for the user and HR staff (see [AvailabilityDao.isInternalCommentVisibleFor]). */
        private val PERSONAL_FIELDS = setOf("type", "internalComment")
    }
}
