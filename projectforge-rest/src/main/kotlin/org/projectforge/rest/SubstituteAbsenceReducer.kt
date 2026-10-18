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

import org.projectforge.business.PfCaches
import org.projectforge.business.availability.AbsenceVisibility
import org.projectforge.business.availability.AvailabilityDO
import org.projectforge.business.availability.AvailabilityDao
import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.business.vacation.service.VacationService
import org.projectforge.business.vacation.service.VacationStats
import org.projectforge.common.DateFormatType
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.rest.dto.Employee
import org.projectforge.rest.dto.User
import org.projectforge.rest.dto.Vacation
import org.projectforge.rest.dto.VacationChartDay
import org.projectforge.rest.dto.VacationReplacementAvailability
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The absences of the substitutes of an entry (a vacation or an availability entry) as its form shows them: their
 * vacations and availability entries overlapping the entry's period, the chart's days with the days none of them is on
 * duty, as far as the logged-in user may be told (see [reduce]).
 */
@Service
class SubstituteAbsenceReducer {
    @Autowired
    private lateinit var absenceVisibility: AbsenceVisibility

    @Autowired
    private lateinit var availabilityDao: AvailabilityDao

    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var employeeCache: EmployeeCache

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var vacationDao: VacationDao

    /** A substitute by employee (none for a freelancer) and user, as the absences are matched with. */
    data class Substitute(val employeeId: Long?, val userId: Long?)

    /**
     * What the form of an entry shows of its substitutes' absences.
     * @param conflict A day without any substitute on duty, as far as the user may be told (of the days drawn).
     */
    class Stats(
        var conflict: Boolean = false,
        var substituteVacations: List<Vacation>? = null,
        var substituteAvailabilities: List<VacationReplacementAvailability>? = null,
        var chartDays: List<VacationChartDay>? = null,
    )

    /**
     * The substitutes' [vacations] and [availabilities] overlapping the entry's period from [startDate] to [endDate],
     * with the chart's days and its [uncoveredDays] (no substitute on duty).
     *
     * HR staff see all of them (without the comments of the vacations: they are read past the DAO's privacy check,
     * VacationDao.afterLoad, and neither the chart nor the table shows them). Anybody else: the substitutes are posted,
     * so anybody could name anybody. The entries the user may read in full ([AbsenceVisibility]) are shown so, all
     * others reduced to their dates and status within the entry's period: the vacations of all of its days, the
     * availability entries only from today on, as the past of an availability entry of somebody else isn't told (it
     * might tell the duration of a sickness). So naming somebody tells no more than whether they are away while the
     * entry's own period runs. Before today, a day is drawn uncovered (and counts as a conflict) only if the entries
     * shown alone say so.
     */
    fun reduce(
        user: PFUserDO,
        startDate: LocalDate,
        endDate: LocalDate,
        substitutes: List<Substitute>,
        vacations: List<VacationDO>,
        availabilities: List<AvailabilityDO>,
        uncoveredDays: List<LocalDate>,
    ): Stats {
        val result = Stats()
        if (vacationDao.hasHrRights(user)) {
            result.substituteVacations = vacations.map { other -> Vacation(other).also { it.comment = null } }
            result.substituteAvailabilities = availabilities.mapNotNull { replacementAvailability(it, user, null, null) }
            result.chartDays = chartDays(startDate, endDate, uncoveredDays.toSet())
            result.conflict = uncoveredDays.isNotEmpty()
            return result
        }
        val today = LocalDate.now()
        result.substituteVacations = vacations.mapNotNull { other ->
            if (isReadableInFull(absenceVisibility.visibleFrom(user, other, today), other.startDate)) {
                Vacation(other).also { it.comment = null }
            } else {
                reducedVacation(other, startDate, endDate)
            }
        }
        val readableAvailabilities = availabilities.filter {
            isReadableInFull(absenceVisibility.visibleFrom(user, it, today), it.startDate)
        }
        result.substituteAvailabilities = availabilities.mapNotNull { availability ->
            if (readableAvailabilities.contains(availability)) {
                replacementAvailability(availability, user, null, null)
            } else {
                replacementAvailability(availability, user, maxOf(startDate, today), endDate)
            }
        }
        // All vacations are shown within the entry's period, the availability entries before today only if readable.
        val shownUncovered = uncoveredDays.filter { it >= today } +
                visibleUncoveredDays(
                    substitutes,
                    uncoveredDays.filter { it < today },
                    vacations,
                    readableAvailabilities,
                )
        result.chartDays = chartDays(startDate, endDate, shownUncovered.toSet())
        // Not the conflict of all days: for a past period it would tell whether the substitutes were away (sick).
        result.conflict = shownUncovered.isNotEmpty()
        return result
    }

    /** Visible from a day ([AbsenceVisibility.visibleFrom]) not after the entry's begin: nothing of it is hidden. */
    private fun isReadableInFull(visibleFrom: LocalDate?, entryStart: LocalDate?): Boolean =
        visibleFrom != null && entryStart != null && !entryStart.isBefore(visibleFrom)

    /**
     * A substitute's vacation the user may not read otherwise: only its days from [from] to [until] (the part of the
     * entry's period the user may be told about) with their status; no id, comment, manager, substitutes or special
     * flag. Null if nothing of it falls in there.
     */
    private fun reducedVacation(other: VacationDO, from: LocalDate, until: LocalDate): Vacation? {
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
     * otherwise, as [reducedVacation]); null if nothing of it falls in there.
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
            // Also of a freelancer, who has no employee.
            user = userGroupCache.getUser(availability.user?.id)?.let { User().apply { copyFromMinimal(it) } },
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
     * The [uncovered] days (all substitutes away) that the [visible] vacations and absent availability entries alone
     * tell: those on which every substitute has one of them.
     */
    private fun visibleUncoveredDays(
        substitutes: List<Substitute>,
        uncovered: List<LocalDate>,
        visible: List<VacationDO>,
        visibleAvailabilities: List<AvailabilityDO>,
    ): List<LocalDate> {
        val absent = visibleAvailabilities.filter { it.status?.absent == true }
        return uncovered.filter { date ->
            substitutes.all { substitute ->
                visible.any { it.employee?.id == substitute.employeeId && covers(it.startDate, it.endDate, date) } ||
                        absent.any { it.user?.id == substitute.userId && covers(it.startDate, it.endDate, date) }
            }
        }
    }

    private fun covers(start: LocalDate?, end: LocalDate?, date: LocalDate): Boolean =
        start != null && end != null && date in start..end

    companion object {
        private const val CHART_MARGIN_DAYS = 3L

        /** The longest period the form's chart and the list's bar draw (beyond is a mistyped year). */
        const val CHART_MAX_DAYS = 400L

        /** A period the chart and the days bar draw: not beyond [CHART_MAX_DAYS]. */
        fun inChartRange(startDate: LocalDate?, endDate: LocalDate?): Boolean {
            startDate ?: return false
            endDate ?: return false
            return endDate >= startDate && ChronoUnit.DAYS.between(startDate, endDate) <= CHART_MAX_DAYS
        }

        /**
         * The axis of the form's chart: the period with [CHART_MARGIN_DAYS] before and after, so the substitutes'
         * absences show where they begin and end. None for a period beyond [CHART_MAX_DAYS].
         */
        fun chartDays(startDate: LocalDate, endDate: LocalDate, uncovered: Set<LocalDate>): List<VacationChartDay>? {
            val first = startDate.minusDays(CHART_MARGIN_DAYS)
            val last = endDate.plusDays(CHART_MARGIN_DAYS)
            if (last < first || ChronoUnit.DAYS.between(first, last) > CHART_MAX_DAYS) {
                return null
            }
            return VacationChartDay.listOf(first, last, uncovered)
        }
    }
}
