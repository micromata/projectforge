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

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.business.vacation.service.VacationService
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.api.BaseDOModifiedListener
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.time.LocalDate

private val log = KotlinLogging.logger {}

/**
 * The availability entries in conflict: on at least one day of a current or future entry all its substitutes are
 * away, on vacation or not available (see [VacationService.uncoveredDaysOfAvailability]), as for the substitutes of a
 * vacation (ConflictingVacationsCache). Any change of a vacation or an availability entry expires the cache.
 */
@Service
class ConflictingAvailabilitiesCache : AbstractCache() {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var availabilityCache: AvailabilityCache

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    private lateinit var vacationService: VacationService

    /** Guards [conflictingIds] and [conflictsByUser], both replaced by [refresh]. */
    private val lock = Any()

    private var conflictingIds = setOf<Long>()

    /** Key is the user id of the entries, value their ids. */
    private var conflictsByUser = mapOf<Long, Set<Long>>()

    @PostConstruct
    private fun postConstruct() {
        // By the context: VacationService and the DAOs depend on further caches, injecting them would make a cycle.
        vacationService = applicationContext.getBean(VacationService::class.java)
        applicationContext.getBean(AvailabilityDao::class.java).register(object : BaseDOModifiedListener<AvailabilityDO> {
            override fun afterInsertOrModify(obj: AvailabilityDO, operationType: OperationType) = setExpired()
        })
        applicationContext.getBean(VacationDao::class.java).register(object : BaseDOModifiedListener<VacationDO> {
            override fun afterInsertOrModify(obj: VacationDO, operationType: OperationType) = setExpired()
        })
    }

    fun hasConflict(id: Long?): Boolean {
        id ?: return false
        checkRefresh()
        synchronized(lock) {
            return conflictingIds.contains(id)
        }
    }

    /** The number of the user's own entries in conflict: the badge of the menu entry. */
    fun numberOfConflicts(userId: Long?): Int {
        userId ?: return 0
        checkRefresh()
        synchronized(lock) {
            return conflictsByUser[userId]?.size ?: 0
        }
    }

    override fun refresh() {
        log.info { "Refreshing cache of conflicting availability entries..." }
        persistenceService.runIsolatedReadOnly(recordCallStats = true) { context ->
            // A running entry also counts its days already past, as the edit form does.
            val entries = availabilityCache.getEntriesWithSubstitutes(LocalDate.now())
            val newConflictingIds = mutableSetOf<Long>()
            val newConflictsByUser = mutableMapOf<Long, MutableSet<Long>>()
            val periodBegin = entries.mapNotNull { it.startDate }.minOrNull()
            val periodEnd = entries.mapNotNull { it.endDate }.maxOrNull()
            if (periodBegin != null && periodEnd != null) {
                val substituteIds = entries.flatMap { it.substitutes.orEmpty() }.mapNotNull { it.id }.toSet()
                val absences = vacationService.getSubstituteAbsences(substituteIds, periodBegin, periodEnd)
                entries.forEach { entry ->
                    val id = entry.id ?: return@forEach
                    if (vacationService.uncoveredDaysOfAvailability(entry, absences).isNotEmpty()) {
                        newConflictingIds.add(id)
                        entry.user?.id?.let { newConflictsByUser.getOrPut(it) { mutableSetOf() }.add(id) }
                    }
                }
            }
            synchronized(lock) {
                conflictingIds = newConflictingIds
                conflictsByUser = newConflictsByUser
            }
            log.info {
                "Refreshing cache of conflicting availability entries done. Found ${newConflictingIds.size} conflicts. ${
                    context.formatStats()
                }"
            }
        }
    }
}
