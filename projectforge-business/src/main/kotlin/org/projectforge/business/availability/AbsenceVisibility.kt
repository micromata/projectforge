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

import org.hibernate.Hibernate
import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDay
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * Who sees which part of the absences of others (vacations and availability entries): the one rule set for the
 * lists, forms, calendars, team calendars, their subscriptions and the vacation export.
 *
 * The past of others is hidden: the begin and end of an absence would tell the duration of e.g. a sickness. So an
 * entry of somebody else is shown from a day on only ([visibleFrom], "clipped"), its real begin isn't told:
 *
 * | Viewer | Vacation | Availability entry |
 * |---|---|---|
 * | own entry | full | full |
 * | HR staff (vacation right, read-only suffices) | full | full |
 * | finance, controlling or HR group | full | as anybody else |
 * | involved: manager and substitutes of the vacation, substitutes of the entry | full | full |
 * | a member of one of the user's groups | from 1 January of the current year | from today |
 * | anybody else | nothing | nothing |
 *
 * What may be told of an entry shown (type, comments) is decided elsewhere ([AvailabilityDao.isInternalCommentVisibleFor],
 * VacationDao.isCommentVisibleFor). Calculations (conflicts, uncovered days) use all entries, unclipped.
 */
@Service
open class AbsenceVisibility {
    @Autowired
    private lateinit var accessChecker: AccessChecker

    // Lazy: the cache reads the entries by the dao, whose access check asks this rule set.
    @Autowired
    @Lazy
    private lateinit var availabilityCache: AvailabilityCache

    @Autowired
    private lateinit var employeeCache: EmployeeCache

    /**
     * The first day of the vacation the viewer may see: [FULL] for all of it, a later day for the part from then
     * on, null for nothing (or no viewer).
     */
    @JvmOverloads
    open fun visibleFrom(viewer: PFUserDO?, vacation: VacationDO, today: LocalDate = LocalDate.now()): LocalDate? {
        viewer ?: return null
        if (hasUnrestrictedVacationView(viewer)) {
            return FULL
        }
        val viewerEmployeeId = employeeCache.getEmployeeIdByUserId(viewer.id)
        if (viewerEmployeeId != null && (vacation.employee?.id == viewerEmployeeId ||
                    vacation.manager?.id == viewerEmployeeId || vacation.replacement?.id == viewerEmployeeId ||
                    vacation.otherReplacements?.any { it.id == viewerEmployeeId } == true)
        ) {
            return FULL
        }
        val employeeUser = employeeCache.getUserByEmployee(vacation.employee?.id) ?: return null
        if (employeeUser.id == viewer.id) {
            return FULL
        }
        if (!accessChecker.areUsersInSameGroup(viewer, employeeUser)) {
            return null
        }
        return clipped(vacation.endDate, PFDay.from(today).beginOfYear.date)
    }

    /**
     * The first day of the availability entry the viewer may see: [FULL] for all of it, today for the part from
     * today on, null for nothing (or no viewer).
     */
    @JvmOverloads
    open fun visibleFrom(viewer: PFUserDO?, availability: AvailabilityDO, today: LocalDate = LocalDate.now()): LocalDate? {
        viewer ?: return null
        val userId = availability.user?.id ?: return null
        if (userId == viewer.id || hasHrView(viewer) || substituteIdsOf(availability).contains(viewer.id)) {
            return FULL
        }
        val user = availability.user
        if (!accessChecker.areUsersInSameGroup(viewer, user)) {
            return null
        }
        return clipped(availability.endDate, today)
    }

    /** Whether the viewer may see anything of the vacation ([visibleFrom]). */
    open fun isVisible(viewer: PFUserDO?, vacation: VacationDO): Boolean = visibleFrom(viewer, vacation) != null

    /** Whether the viewer may see anything of the entry ([visibleFrom]). */
    open fun isVisible(viewer: PFUserDO?, availability: AvailabilityDO): Boolean = visibleFrom(viewer, availability) != null

    /**
     * HR staff (the HR vacation right, read-only suffices) and the members of the finance, controlling or HR group:
     * they see all vacations, also of the past.
     */
    open fun hasUnrestrictedVacationView(viewer: PFUserDO): Boolean {
        return hasHrView(viewer) || accessChecker.isUserMemberOfGroup(
            viewer, false, ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP,
            ProjectForgeGroup.HR_GROUP,
        )
    }

    /** HR staff (the HR vacation right, read-only suffices): they see all availability entries, also of the past. */
    open fun hasHrView(viewer: PFUserDO): Boolean {
        return accessChecker.hasRight(
            viewer, UserRightId.HR_VACATION, false, UserRightValue.READONLY, UserRightValue.READWRITE,
        )
    }

    /**
     * The substitutes of the entry: of the cached one (hydrated), an entry read from the database has them lazy. Those
     * of a new one, if given.
     */
    private fun substituteIdsOf(availability: AvailabilityDO): Set<Long?> {
        val substitutes = availability.id?.let { availabilityCache.getEntry(it)?.substitutes }
            ?: availability.substitutes?.takeIf { Hibernate.isInitialized(it) }
        return substitutes.orEmpty().map { it.id }.toSet()
    }

    /** [from], or null if the entry ends before (or has no end). */
    private fun clipped(endDate: LocalDate?, from: LocalDate): LocalDate? {
        return if (endDate == null || endDate.isBefore(from)) null else from
    }

    companion object {
        /** [visibleFrom]: all of the entry. */
        @JvmField
        val FULL: LocalDate = LocalDate.MIN
    }
}
