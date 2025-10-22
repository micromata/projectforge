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
import org.projectforge.framework.access.AccessException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * Validator for availability entries.
 *
 * @author Kai Reinhard
 */
@Service
open class AvailabilityValidator {
    @Autowired
    private lateinit var availabilityTypeConfiguration: AvailabilityTypeConfiguration

    enum class Error(val messageKey: String) {
        INVALID_DATE_RANGE("availability.error.invalidDateRange"),
        MISSING_EMPLOYEE("availability.error.missingEmployee"),
        MISSING_TYPE("availability.error.missingType"),
        INVALID_TYPE("availability.error.invalidType"),
        MISSING_REPLACEMENT("availability.error.missingReplacement"),
        INVALID_PERCENTAGE("availability.error.invalidPercentage"),
    }

    /**
     * Validates an availability entry.
     * @param availability The availability entry to validate
     * @param dbAvailability The database entry if this is an update
     * @param throwException If true, throws AccessException on validation error
     * @return null if validation passed, otherwise the error
     */
    open fun validate(
        availability: AvailabilityDO,
        dbAvailability: AvailabilityDO? = null,
        throwException: Boolean = false
    ): Error? {
        // Check employee
        if (availability.employee == null) {
            return handleError(Error.MISSING_EMPLOYEE, throwException)
        }

        // Check availability type
        if (availability.availabilityType.isNullOrBlank()) {
            return handleError(Error.MISSING_TYPE, throwException)
        }

        val typeConfig = availabilityTypeConfiguration.getTypeByKey(availability.availabilityType!!)
        if (typeConfig == null) {
            return handleError(Error.INVALID_TYPE, throwException)
        }

        // Check dates
        val startDate = availability.startDate
        val endDate = availability.endDate
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return handleError(Error.INVALID_DATE_RANGE, throwException)
        }

        // Check replacement (required if type suggests unavailability)
        if (typeConfig.reachabilityStatus == "UNAVAILABLE" && availability.replacement == null) {
            return handleError(Error.MISSING_REPLACEMENT, throwException)
        }

        // Check percentage
        availability.percentage?.let { percentage ->
            if (percentage < 0 || percentage > 100) {
                return handleError(Error.INVALID_PERCENTAGE, throwException)
            }
        }

        return null
    }

    private fun handleError(error: Error, throwException: Boolean): Error? {
        if (throwException) {
            throw AccessException(error.messageKey)
        }
        return error
    }
}
