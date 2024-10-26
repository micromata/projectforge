/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Caches employees with actual status and annual leave days for faster access.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
open class EmployeeCache : AbstractCache() {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @PostConstruct
    private fun postConstruct() {
        instance = this
    }

    /**
     * The key is the employee id (database pk). Mustn't be synchronized, because it is only read.
     */
    private var employeeMap: Map<Long, EmployeeDO> = emptyMap()

    fun getEmployee(id: Long?): EmployeeDO? {
        id ?: return null
        checkRefresh()
        return employeeMap[id]
    }

    fun getEmployeeByUserId(userId: Long?): EmployeeDO? {
        userId ?: return null
        checkRefresh()
        return employeeMap.values.firstOrNull { it.user?.id == userId }
    }

    fun getEmployeeIdByUserId(userId: Long?): Long? {
        userId ?: return null
        checkRefresh()
        return employeeMap.values.firstOrNull { it.user?.id == userId }?.id
    }

    fun getUser(employee: EmployeeDO?): PFUserDO? {
        val user = employee?.user ?: return null
        checkRefresh()
        return userGroupCache.getUser(user.id)
    }

    fun setStatusAndAnnualLeave(employee: EmployeeDO?) {
        employee ?: return
        checkRefresh()
        val cached = employeeMap[employee.id] ?: return
        employee.status = cached.status
        employee.annualLeave = cached.annualLeave
    }

    fun setStatusAndAnnualLeave(employees: Collection<EmployeeDO>) {
        employees.forEach { setStatusAndAnnualLeave(it) }
    }

    /**
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    public override fun refresh() {
        try {
            PfPersistenceService.startCallsStatsRecording()
            log.info("Initializing EmployeeCache...")
            val saved = persistenceService.saveStatsState()
            // This method must not be synchronized because it works with a new copy of maps.
            val map = mutableMapOf<Long, EmployeeDO>()
            persistenceService.executeQuery(
                "from EmployeeDO t where deleted=false",
                EmployeeDO::class.java,
            ).forEach { employee ->
                map[employee.id!!] = employee
            }
            getLatestValidSinceEntries(EmployeeValidSinceAttrType.STATUS)
                .forEach { validSinceEntry ->
                    log.debug { "EmployeeCache.refresh: Processing $validSinceEntry" }
                    validSinceEntry.employee?.id?.let { employeeId ->
                        map[employeeId]?.let { employee ->
                            employee.status = validSinceEntry.status
                            log.debug { "EmployeeCache.refresh: Set status=${validSinceEntry.status} for employeeId=$employeeId, userId=${employee.userId}" }
                        }
                    }
                }
            getLatestValidSinceEntries(EmployeeValidSinceAttrType.ANNUAL_LEAVE)
                .forEach { validSinceEntry ->
                    log.debug { "EmployeeCache.refresh: Processing $validSinceEntry" }
                    validSinceEntry.employee?.id?.let { employeeId ->
                        map[employeeId]?.let { employee ->
                            employee.annualLeave = validSinceEntry.annualLeave
                            log.debug { "EmployeeCache.refresh: Set annualLeave=${validSinceEntry.annualLeave} for employeeId=$employeeId, userId=${employee.userId}" }
                        }
                    }
                }
            this.employeeMap = map
            log.info(
                "EmployeeCache.refresh done. stats=${persistenceService.formatStats(saved)}, callsStats=${
                    PfPersistenceService.showCallsStatsRecording()
                }"
            )
        } finally {
            PfPersistenceService.stopCallsStatsRecording()
        }
    }

    private fun getLatestValidSinceEntries(type: EmployeeValidSinceAttrType): Collection<EmployeeValidSinceAttrDO> {
        return persistenceService.executeQuery(
            queryAllValidSinceValues,
            EmployeeValidSinceAttrDO::class.java,
            Pair("type", type),
        ).groupBy { it.employee?.id } // Group by employee id
            .mapValues { entry -> entry.value.first() } // Get the first record (latest) in each group
            .values // Extract the results as a collection of latest records
    }

    companion object {
        @JvmStatic
        lateinit var instance: EmployeeCache
            private set

        // select * from t_fibu_employee_valid_since_attr t where type='STATUS' AND t.deleted=false order by t.employee_fk, t.valid_since desc;
        const val queryAllValidSinceValues = "SELECT t FROM EmployeeValidSinceAttrDO t WHERE t.type=:type AND t.deleted=false ORDER BY t.employee.id, t.validSince DESC"

        // Following query didn't work properly with PostgreSQL (some entries were missing):
        // Gets the validSince attribute for all employees and takes the last (max) entry.
        //  SELECT e FROM t_fibu_employee_valid_since_attr e WHERE e.valid_since = (SELECT MAX(innerE.valid_since) FROM t_fibu_employee_valid_since_attr innerE WHERE innerE.employee_fk = e.employee_fk) AND e.type='STATUS' ORDER BY e.employee_fk, e.valid_since DESC;
        /*const val queryAllValidSinceMaxValues =
            """
            SELECT e FROM EmployeeValidSinceAttrDO e
            WHERE e.validSince = (
                SELECT MAX(innerE.validSince)
                FROM EmployeeValidSinceAttrDO innerE
                WHERE innerE.employee.id = e.employee.id
            )
            AND e.type = :type AND e.deleted = false
            ORDER BY e.employee.id, e.validSince DESC
            """*/
    }
}
