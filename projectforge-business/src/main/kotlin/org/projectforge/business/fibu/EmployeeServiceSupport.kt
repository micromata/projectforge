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

package org.projectforge.business.fibu

import mu.KotlinLogging
import org.projectforge.framework.persistence.api.BaseDOPersistenceService
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Standard implementation of the Employee service interface.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
@Service
internal class EmployeeServiceSupport {
    @Autowired
    private lateinit var baseDOPersistenceService: BaseDOPersistenceService

    @Autowired
    private lateinit var employeeCache: EmployeeCache

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    /**
     * @param id The id of the attribute to select.
     * @param expectedType If not null, the type of the attribute must match this type, otherwise, an exception will be thrown.
     * @param checkAccess If true, the logged-in user must have access to the attribute.
     * @return The attribute with the given id or null if it does not exist.
     */
    fun findValidSinceAttr(
        id: Long?,
        expectedType: EmployeeValidSinceAttrType? = null,
        checkAccess: Boolean = true
    ): EmployeeValidSinceAttrDO? {
        id ?: return null
        if (checkAccess) {
            employeeDao.checkLoggedInUserSelectAccess()
        }
        val result = persistenceService.find(EmployeeValidSinceAttrDO::class.java, id) ?: return null
        if (expectedType != null) {
            require(result.type == expectedType) { "Expected type $expectedType, but got ${result.type}." }
        }
        return result
    }

    /**
     * @param employee The employee to select the attribute for.
     * @param type The type of the attribute to select.
     * @param deleted If true, only deleted attributes will be returned, if false, only not deleted attributes will be returned. If null, deleted and not deleted attributes will be returned.
     */
    internal fun selectAllValidSinceAttrs(
        employee: EmployeeDO,
        type: EmployeeValidSinceAttrType? = null,
        deleted: Boolean? = false,
        checkAccess: Boolean = true,
    ): List<EmployeeValidSinceAttrDO> {
        requireNotNull(employee.id) { "Employee id must not be null." }
        if (checkAccess) {
            employeeDao.checkLoggedInUserSelectAccess(employee)
        }
        val list = if (type != null) {
            persistenceService.executeQuery(
                "from EmployeeValidSinceAttrDO a where a.employee.id = :employeeId and a.type = :type order by a.validSince desc",
                EmployeeValidSinceAttrDO::class.java,
                Pair("employeeId", employee.id),
                Pair("type", type)
            )
        } else {
            persistenceService.executeQuery(
                "from EmployeeValidSinceAttrDO a where a.employee.id = :employeeId order by a.validSince desc",
                EmployeeValidSinceAttrDO::class.java,
                Pair("employeeId", employee.id),
            )
        }
        if (deleted != null) {
            return list.filter { it.deleted == deleted }
        }
        return list
    }

    /**
     * @param employee The employee to select the attribute for.
     * @param checkAccess If true, the logged in user must have access to the employee.
     * @return The attribute that is currently valid.
     */
    fun getEmployeeStatus(
        employee: EmployeeDO,
        checkAccess: Boolean = true
    ): EmployeeStatus? {
        val list = selectAllValidSinceAttrs(
            employee,
            EmployeeValidSinceAttrType.STATUS,
            deleted = false,
            checkAccess = checkAccess
        )
        val validEntry = getActiveEntry(list)
        val status = validEntry?.value
        if (status != null) {
            try {
                return EmployeeStatus.safeValueOf(status)
            } catch (e: IllegalArgumentException) {
                log.error { "Oups, unknown status value in validSinceAttr: $validEntry" }
            }
        }
        return null
    }

    fun getAnnualLeaveDays(employee: EmployeeDO?, validAtDate: LocalDate?, checkAccess: Boolean = true): BigDecimal? {
        if (employee == null || validAtDate == null) { // Should only occur in CallAllPagesTest (Wicket).
            return null
        }
        return getActiveEntry(
            selectAnnualLeaveDayEntries(employee, deleted = false, checkAccess = checkAccess),
            validAtDate
        )?.value?.toBigDecimal()
    }

    private fun ensure(validAtDate: LocalDate?): LocalDate {
        return validAtDate ?: LocalDate.of(1970, Month.JANUARY, 1)
    }

    internal fun getActiveEntry(
        entries: List<EmployeeValidSinceAttrDO>,
        validAtDate: LocalDate? = null,
    ): EmployeeValidSinceAttrDO? {
        var found: EmployeeValidSinceAttrDO? = null
        // example
        // null (active before 2021-01-01), 2021-01-01, 2023-05-08 (active)
        val useDate = validAtDate ?: LocalDate.now()
        entries.filter { !it.deleted }.forEach { entry ->
            if (useDate >= ensure(entry.validSince)) {
                found.let { f ->
                    if (f == null) {
                        found = entry
                    } else if (ensure(f.validSince) < ensure(entry.validSince)) {
                        found = entry // entry is newer!
                    }
                }
            }
        }
        return found
    }

    /**
     * @param employeeId The employee (as id) to select the attribute for.
     * @param deleted If true, only deleted attributes will be returned, if false, only not deleted attributes will be returned. If null, deleted and not deleted attributes will be returned.
     * @param checkAccess If true, the logged-in user must have access to the employee.
     */
    fun selectAnnualLeaveDayEntries(
        employeeId: Long,
        deleted: Boolean? = false,
        checkAccess: Boolean = true
    ): List<EmployeeValidSinceAttrDO> {
        val employee = employeeDao.find(employeeId)!!
        return selectAnnualLeaveDayEntries(employee, deleted, checkAccess)
    }

    /**
     * @param employee The employee to select the attribute for.
     * @param deleted If true, only deleted attributes will be returned, if false, only not deleted attributes will be returned. If null, deleted and not deleted attributes will be returned.
     * @param checkAccess If true, the logged-in user must have access to the employee.
     */
    private fun selectAnnualLeaveDayEntries(
        employee: EmployeeDO,
        deleted: Boolean? = false,
        checkAccess: Boolean = true
    ): List<EmployeeValidSinceAttrDO> {
        return selectAllValidSinceAttrs(
            employee,
            EmployeeValidSinceAttrType.ANNUAL_LEAVE,
            deleted = deleted,
            checkAccess = checkAccess
        )
    }

    /**
     * @param attr The attribute to validate.
     * @return The existing attribute with the same type and date, if it exists and is deleted (for re-use).
     * @throws IllegalArgumentException If an attribute with the same type and date already exists, and the existing other isn't deleted.
     */
    @Throws(IllegalArgumentException::class)
    internal fun validate(attr: EmployeeValidSinceAttrDO): EmployeeValidSinceAttrDO? {
        val other = if (attr.id == null) {
            // New entry
            persistenceService.selectNamedSingleResult(
                EmployeeValidSinceAttrDO.FIND_BY_TYPE_AND_DATE,
                EmployeeValidSinceAttrDO::class.java,
                Pair("employeeId", attr.employee!!.id!!),
                Pair("type", attr.type),
                Pair("validSince", attr.validSince),
            )
        } else {
            // Attr already exists:
            persistenceService.selectNamedSingleResult(
                EmployeeValidSinceAttrDO.FIND_OTHER_BY_TYPE_AND_DATE,
                EmployeeValidSinceAttrDO::class.java,
                Pair("employeeId", attr.employee!!.id!!),
                Pair("type", attr.type),
                Pair("validSince", attr.validSince),
                Pair("id", attr.id),
            )
        }
        if (other == null) {
            return null
        }
        if (other.deleted && other.validSince == attr.validSince) { // validSince should be the same, but just in case.
            // This algorithm is needed to handle deleted entries. Otherwise, they aren't visible for the users and
            // the users can't add or modify entries with the validSince date of the deleted ones.
            return other
        }
        throw IllegalArgumentException("An entry with the same type and date already exists: $other")
    }

    fun insert(
        employeeId: Long,
        attrDO: EmployeeValidSinceAttrDO,
        checkAccess: Boolean = true,
    ): Long? {
        val employee = employeeDao.find(employeeId)!!
        return insert(employee, attrDO, checkAccess)
    }

    fun insert(
        employee: EmployeeDO,
        attrDO: EmployeeValidSinceAttrDO,
        checkAccess: Boolean = true,
    ): Long? {
        if (employee.id != attrDO.employee?.id) {
            throw IllegalArgumentException("Employee id of attribute does not match employee id.")
        }
        if (checkAccess) {
            employeeDao.checkLoggedInUserInsertAccess(employee)
        }
        val other = validate(attrDO)
        if (other != null) {
            // This algorithm is needed to handle deleted entries. Otherwise, they aren't visible for the users and
            // the users can't add or modify entries with the validSince date of the deleted ones.
            // Please remember: There are no unique constraints in the database!
            other.copyFrom(attrDO)
            // Undeleting and updating existing entry instead of inserting new entry.
            undeleteValidSinceAttr(employee, attrDO = other, checkAccess = checkAccess)
            return other.id
        }
        val result = baseDOPersistenceService.insert(attrDO, checkAccess = checkAccess)
        employeeCache.setExpired()
        return result
    }

    internal fun insertValidSinceAttr(
        employee: EmployeeDO,
        validSince: LocalDate,
        value: String,
        type: EmployeeValidSinceAttrType,
        checkAccess: Boolean,
    ): EmployeeValidSinceAttrDO {
        val attr = EmployeeValidSinceAttrDO()
        attr.employee = employee
        attr.validSince = validSince
        attr.value = value
        attr.type = type
        attr.created = Date()
        attr.lastUpdate = attr.created
        val id = insert(employee, attr, checkAccess)
        attr.id = id // Should already be set by insert (but better safe than sorry).
        return attr
    }

    /**
     * @param employee The employee to update the attribute for. Needed for checkAccess.
     * @param attrDO: The attribute to update.
     * @param checkAccess: If true, the logged-in user must have update access to the employee.
     */
    fun updateValidSinceAttr(
        employee: EmployeeDO,
        attrDO: EmployeeValidSinceAttrDO,
        checkAccess: Boolean = true,
    ): EntityCopyStatus {
        if (employee.id != attrDO.employee?.id) {
            throw IllegalArgumentException("Employee id of attribute does not match employee id.")
        }
        if (checkAccess) {
            employeeDao.checkLoggedInUserUpdateAccess(employee, employee)
        }
        val other = validate(attrDO)
        try {
            if (other != null) {
                // This algorithm is needed to handle deleted entries. Otherwise, they aren't visible for the users and
                // the users can't add or modify entries with the validSince date of the deleted ones.
                // Please remember: There are no unique constraints in the database!
                findValidSinceAttr(attrDO.id, expectedType = attrDO.type, checkAccess = checkAccess).let { dbEntry ->
                    requireNotNull(dbEntry) { "Can't update EmployeeValidSinceAttr entry without existing id." }
                    // Mark current entry as deleted and modify existing deleted entry with desired validSince date.
                    markValidSinceAttrAsDeleted(employee, dbEntry, checkAccess)
                }
                other.copyFrom(attrDO)
                // Undeleting and updating existing entry instead of inserting new entry.
                return baseDOPersistenceService.undelete(obj = other, checkAccess = checkAccess)
            }
            return baseDOPersistenceService.update(attrDO, checkAccess = checkAccess)
        } finally {
            employeeCache.setExpired()
        }
    }

    fun markValidSinceAttrAsDeleted(
        employeeId: Long?,
        attrId: Long?,
        checkAccess: Boolean = true,
    ) {
        val employee = employeeDao.find(employeeId)!!
        val attrDO = findValidSinceAttr(attrId, checkAccess = checkAccess)!!
        markValidSinceAttrAsDeleted(employee, attrDO, checkAccess)
        employeeCache.setExpired()
    }

    fun markValidSinceAttrAsDeleted(
        employee: EmployeeDO,
        attrDO: EmployeeValidSinceAttrDO,
        checkAccess: Boolean = true,
    ) {
        require(attrDO.employee!!.id == employee.id!!) { "Employee id of attribute does not match employee id." }
        if (checkAccess) {
            employeeDao.checkLoggedInUserUpdateAccess(employee, employee)
        }
        validate(attrDO)
        baseDOPersistenceService.markAsDeleted(obj = attrDO, checkAccess = checkAccess)
        employeeCache.setExpired()
    }

    fun undeleteValidSinceAttr(
        employee: EmployeeDO,
        attrDO: EmployeeValidSinceAttrDO,
        checkAccess: Boolean = true,
    ) {
        if (checkAccess) {
            employeeDao.checkLoggedInUserUpdateAccess(employee, employee)
        }
        validate(attrDO)
        baseDOPersistenceService.undelete(obj = attrDO, checkAccess = checkAccess)
        employeeCache.setExpired()
    }
}
