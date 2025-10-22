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

package org.projectforge.business.availability.service

import org.projectforge.business.availability.AvailabilityTypeConfiguration
import org.projectforge.business.availability.model.AvailabilityDO
import org.projectforge.business.availability.repository.AvailabilityDao
import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.Serializable
import java.time.LocalDate

/**
 * Service for employee availability management.
 *
 * @author Kai Reinhard
 */
@Service
open class AvailabilityService {
    @Autowired
    private lateinit var availabilityDao: AvailabilityDao

    @Autowired
    private lateinit var availabilityTypeConfiguration: AvailabilityTypeConfiguration

    @Autowired
    private lateinit var conflictingAvailabilitiesCache: ConflictingAvailabilitiesCache

    @Autowired
    private lateinit var employeeCache: EmployeeCache

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    class AvailabilitiesByEmployee(val employee: EmployeeDO, val availabilities: List<AvailabilityDO>)

    class AvailabilityOverlaps(
        /**
         * Availabilities of all substitutes (replacement and otherReplacement) overlapping requested availability.
         */
        val otherAvailabilities: List<AvailabilityDO> = emptyList(),
        /**
         * If at least one day of the availability isn't covered by any substitute (all substitutes are absent on at least one
         * day).
         */
        val conflict: Boolean = false,
    )

    @JvmOverloads
    open fun getAvailabilityListForPeriod(
        employee: EmployeeDO,
        periodBegin: LocalDate,
        periodEnd: LocalDate,
    ): List<AvailabilityDO> {
        return getAvailabilityListForPeriod(employee.id, periodBegin, periodEnd)
    }

    @JvmOverloads
    open fun getAvailabilityListForPeriod(
        employeeId: Long?,
        periodBegin: LocalDate,
        periodEnd: LocalDate,
    ): List<AvailabilityDO> {
        return availabilityDao.getAvailabilityForPeriod(employeeId, periodBegin, periodEnd)
    }

    /**
     * Getting availability for given ids.
     *
     * @param idList
     * @return List of availabilities
     */
    open fun getAvailability(idList: List<Serializable>?): List<AvailabilityDO?>? {
        return availabilityDao.select(idList, checkAccess = false)
    }

    open fun getCurrentAndFutureAvailabilities(): List<AvailabilityDO> {
        return persistenceService.executeNamedQuery(
            AvailabilityDO.FIND_CURRENT_AND_FUTURE,
            AvailabilityDO::class.java,
            Pair("endDate", LocalDate.now()),
            entityGraphName = AvailabilityDO.ENTITY_GRAPH_WITH_OTHER_REPLACEMENTIDS
        )
    }

    /**
     * Getting all not deleted availabilities for given employee of the current year.
     *
     * @param employee
     * @param year
     * @return List of availabilities
     */
    open fun getActiveAvailabilityForYear(employee: EmployeeDO?, year: Int): List<AvailabilityDO> {
        return availabilityDao.getActiveAvailabilityForYear(employee, year)
    }

    /**
     * Checks for collisions etc.
     * @param availability The availability entry to check.
     * @param dbAvailability If modified, the previous entry (database entry).
     * @param throwException If true, an exception is thrown if validation failed. Default is false.
     * @return null if no validation error was detected, or i18n-key of error, if validation failed.
     */
    @JvmOverloads
    open fun validate(
        availability: AvailabilityDO,
        dbAvailability: AvailabilityDO? = null,
        throwException: Boolean = false
    ): AvailabilityValidator.Error? {
        var dbVal = dbAvailability
        if (dbAvailability == null && availability.id != null) {
            dbVal = availabilityDao.find(availability.id, checkAccess = false)
        }
        val validator = AvailabilityValidator()
        validator.validate(availability, dbVal, throwException)
        return null
    }

    /**
     * Check, if user is able to use availability services.
     */
    open fun hasAccessToAvailabilityService(
        user: PFUserDO?,
        throwException: Boolean,
    ): Boolean {
        if (user?.id == null)
            return false
        val employee = employeeCache.getEmployeeByUserId(user.id)
        return when {
            employee == null -> {
                if (throwException) {
                    throw AccessException("access.exception.noEmployeeToUser")
                }
                false
            }

            else -> true
        }
    }

    /**
     * Checks, if logged in User has HR availability access.
     */
    open fun hasLoggedInUserHRAvailabilityAccess(): Boolean {
        return availabilityDao.hasLoggedInUserHRAvailabilityAccess()
    }

    /**
     * Method for detecting availability overlaps between employees and their substitutes (replacement).
     */
    open fun getAvailabilityOfEmployees(
        employees: Collection<EmployeeDO>,
        periodBegin: LocalDate,
        periodEnd: LocalDate,
    ): List<AvailabilitiesByEmployee> {
        val result = mutableListOf<AvailabilitiesByEmployee>()
        employees.forEach { employee ->
            val availabilities = getAvailabilityListForPeriod(employee.id, periodBegin, periodEnd)
            result.add(AvailabilitiesByEmployee(employee, availabilities))
        }
        return result
    }

    open fun getAvailabilityOverlaps(availability: AvailabilityDO): AvailabilityOverlaps {
        val periodBegin = availability.startDate ?: return AvailabilityOverlaps()
        val periodEnd = availability.endDate ?: return AvailabilityOverlaps()
        val employees = collectAllReplacements(availability)
        if (employees.isEmpty()) {
            return AvailabilityOverlaps()
        }
        val availabilityOverlaps = mutableListOf<AvailabilityDO>()
        getAvailabilityOfEmployees(employees, periodBegin, periodEnd).forEach { employeeAvailabilities ->
            employeeAvailabilities.availabilities.forEach { otherAvailability ->
                if (availability.hasOverlap(otherAvailability)) {
                    availabilityOverlaps.add(otherAvailability)
                }
            }
        }
        val conflict = checkConflict(availability, availabilityOverlaps)
        conflictingAvailabilitiesCache.updateAvailability(availability, conflict)
        return AvailabilityOverlaps(availabilityOverlaps.sortedBy { it.startDate }, conflict)
    }

    /**
     * Will fetch employees.
     */
    fun collectAllReplacements(availability: AvailabilityDO): Collection<EmployeeDO> {
        return availability.allReplacements
    }

    internal fun checkConflict(availability: AvailabilityDO, availabilitiesOfReplacements: List<AvailabilityDO>): Boolean {
        if (availabilitiesOfReplacements.isEmpty()) {
            return false
        }
        val allReplacements = collectAllReplacements(availability)
        if (allReplacements.isEmpty()) {
            return false
        }
        allReplacements.forEach { employeeDO ->
            if (availabilitiesOfReplacements.none { it.employee?.id == employeeDO.id }) {
                return false // one replacement employee found without any availability in the availability period -> no conflict.
            }
        }

        val startDate = availability.startDate ?: return false
        val endDate = availability.endDate ?: return false
        if (startDate > endDate) {
            return false
        }
        var date = startDate
        var paranoiaCounter = 10000
        while (date <= endDate) {
            if (--paranoiaCounter <= 0) {
                break
            }
            var substituteAvailable = false
            allReplacements.forEach replacements@{ replacement ->
                availabilitiesOfReplacements.filter { it.employee?.id == replacement.id }.forEach { other ->
                    if (!other.isInBetween(date)) {
                        substituteAvailable = true
                        return@replacements
                    }
                }
            }
            if (!substituteAvailable) {
                return true
            }
            date = date.plusDays(1)
        }
        return false
    }

    /**
     * Get all configured availability types.
     */
    open fun getAvailabilityTypes(): List<AvailabilityTypeConfiguration.AvailabilityTypeConfig> {
        return availabilityTypeConfiguration.types
    }
}
