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

import jakarta.annotation.PostConstruct
import org.projectforge.business.availability.model.AvailabilityDO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

/**
 * Cache for storing information about conflicting availabilities (replacement conflicts).
 * Analog to ConflictingVacationsCache.
 *
 * @author Kai Reinhard
 */
@Service
open class ConflictingAvailabilitiesCache {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    private lateinit var availabilityService: AvailabilityService

    private val conflictingAvailabilitiesSet = mutableSetOf<Long>()

    @PostConstruct
    private fun postConstruct() {
        // Lazy initialization to avoid circular dependencies
        availabilityService = applicationContext.getBean(AvailabilityService::class.java)
    }

    /**
     * Refresh cache by checking all current and future availabilities for conflicts.
     */
    @Synchronized
    open fun refresh() {
        log.info("Refreshing conflicting availabilities cache...")
        conflictingAvailabilitiesSet.clear()
        val availabilities = availabilityService.getCurrentAndFutureAvailabilities()
        availabilities.forEach { availability ->
            val overlaps = availabilityService.getAvailabilityOverlaps(availability)
            if (overlaps.conflict) {
                availability.id?.let {
                    conflictingAvailabilitiesSet.add(it)
                }
            }
        }
        log.info("Conflicting availabilities cache refreshed. Found ${conflictingAvailabilitiesSet.size} conflicts.")
    }

    /**
     * Update conflict status for a specific availability.
     */
    @Synchronized
    open fun updateAvailability(availability: AvailabilityDO, hasConflict: Boolean) {
        availability.id?.let { id ->
            if (hasConflict) {
                conflictingAvailabilitiesSet.add(id)
            } else {
                conflictingAvailabilitiesSet.remove(id)
            }
        }
    }

    /**
     * Check if an availability has a conflict.
     */
    @Synchronized
    open fun hasConflict(availabilityId: Long?): Boolean {
        return availabilityId?.let { conflictingAvailabilitiesSet.contains(it) } ?: false
    }

    /**
     * Clear the cache.
     */
    @Synchronized
    open fun clear() {
        conflictingAvailabilitiesSet.clear()
    }

    companion object {
        private val log = LoggerFactory.getLogger(ConflictingAvailabilitiesCache::class.java)
    }
}
