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


package org.projectforge.business.availability

import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.vacation.VacationCache
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * The vacations and availability entries of the given employees and users overlapping a period, by their kinds
 * ([AbsenceKind]): the one access to absences for whatever shows or exports them (calendar, team calendars and their
 * subscriptions, vacation export) and for the conflicts of the substitutes (VacationService.getSubstituteAbsences).
 *
 * The entries are the cached ones ([VacationCache], [AvailabilityCache]): read only, and their personal fields kept
 * raw. What a viewer may be told of them, the helpers here decide ([labelFor], [isCommentVisible],
 * [isInternalCommentVisible]).
 */
@Service
open class AbsenceService {
    @Autowired
    private lateinit var availabilityCache: AvailabilityCache

    @Autowired
    private lateinit var availabilityDao: AvailabilityDao

    @Autowired
    private lateinit var employeeCache: EmployeeCache

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var vacationCache: VacationCache

    @Autowired
    private lateinit var vacationDao: VacationDao

    /** Which of the entries of the people asked for are returned. */
    enum class Visibility {
        /**
         * Only those the logged-in user may see, each checked ([VacationDao.hasSelectAccess],
         * [AvailabilityDao.hasSelectAccess]: e.g. the entries of group mates up to some days after their end).
         */
        PER_ENTRY,

        /**
         * All of them: the caller has restricted the people and the period to what the logged-in user may see (e.g. the
         * vacation export) or doesn't show them as they are (the conflicts of substitutes).
         */
        PRE_AUTHORIZED,
    }

    /** Vacations (not rejected ones) and availability entries, each sorted by their start. */
    class Absences(
        val vacations: List<VacationDO> = emptyList(),
        val availabilities: List<AvailabilityDO> = emptyList(),
    )

    /**
     * The absences of the members of the given groups and of the given users overlapping the period, as far as [kinds]
     * ask for them: the vacations for [AbsenceKind.VACATION], the availability entries by their state
     * ([AvailabilityStatus.kind]).
     * @param groupIds Null items should only occur on (de)serialization issues.
     * @param userIds Null items should only occur on (de)serialization issues.
     */
    @JvmOverloads
    open fun getAbsences(
        from: LocalDate,
        until: LocalDate,
        kinds: Set<AbsenceKind>,
        groupIds: Collection<Long?>? = null,
        userIds: Collection<Long?>? = null,
        visibility: Visibility = Visibility.PER_ENTRY,
    ): Absences {
        if (groupIds.isNullOrEmpty() && userIds.isNullOrEmpty() || kinds.isEmpty() || until.isBefore(from)) {
            return Absences()
        }
        val vacations = if (!kinds.contains(AbsenceKind.VACATION)) {
            emptyList()
        } else if (visibility == Visibility.PER_ENTRY) {
            vacationCache.getVacationForPeriodAndUsers(from, until, groupIds?.toSet(), userIds?.toSet())
        } else {
            vacationCache.getVacationsOfUsers(userGroupCache.getMemberIds(groupIds, userIds), from, until)
        }
        val availabilityKinds = kinds - AbsenceKind.VACATION
        val availabilities = if (availabilityKinds.isEmpty()) {
            emptyList()
        } else if (visibility == Visibility.PER_ENTRY) {
            availabilityCache.getAvailabilityForPeriodAndUsers(from, until, groupIds?.toSet(), userIds?.toSet())
        } else {
            availabilityCache.getEntriesOfUsers(userGroupCache.getMemberIds(groupIds, userIds), from, until)
        }.filter { it.status?.kind in availabilityKinds }
        return Absences(vacations.sortedBy { it.startDate }, availabilities.sortedBy { it.startDate })
    }

    /** The user of the given vacation (of its employee). */
    open fun userOf(vacation: VacationDO): PFUserDO? = employeeCache.getUser(vacation.employee)

    /** The user of the given entry, from the cache. */
    open fun userOf(availability: AvailabilityDO): PFUserDO? =
        userGroupCache.getUser(availability.user?.id) ?: availability.user

    /**
     * What the entry is called for the given viewer: its type for the user and HR staff, only its status for everybody
     * else ([AvailabilityDao.labelFor]).
     * @param neutral Always its status only: for what may be read by anybody, e.g. a calendar subscription.
     */
    @JvmOverloads
    open fun labelFor(viewer: PFUserDO?, availability: AvailabilityDO, neutral: Boolean = false): String {
        return if (neutral) availability.neutralLabel else availabilityDao.labelFor(viewer, availability)
    }

    /** The comment of a vacation is personal: for its employee and who may change it ([VacationDao.isCommentVisibleFor]). */
    open fun isCommentVisible(viewer: PFUserDO?, vacation: VacationDO): Boolean {
        return viewer != null && !vacation.comment.isNullOrBlank() && vacationDao.isCommentVisibleFor(viewer, vacation)
    }

    /**
     * The public comment of an entry is for whoever may see the entry itself ([AvailabilityDao.hasUserSelectAccess]):
     * not for a viewer the entry is shown to as a vacation of the same people is (e.g. in the vacation export).
     */
    open fun isCommentVisible(viewer: PFUserDO?, availability: AvailabilityDO): Boolean {
        return viewer != null && !availability.comment.isNullOrBlank() &&
                availabilityDao.hasUserSelectAccess(viewer, availability, false)
    }

    /** The internal comment of an entry is for its user and HR staff only ([AvailabilityDao.isInternalCommentVisibleFor]). */
    open fun isInternalCommentVisible(viewer: PFUserDO?, availability: AvailabilityDO): Boolean {
        return viewer != null && !availability.internalComment.isNullOrBlank() &&
                availabilityDao.isInternalCommentVisibleFor(viewer, availability)
    }
}
