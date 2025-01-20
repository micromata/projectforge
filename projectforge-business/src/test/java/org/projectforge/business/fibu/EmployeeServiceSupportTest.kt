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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.framework.persistence.history.EntityOpType
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.test.HistoryTester
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month

class EmployeeServiceSupportTest : AbstractTestBase() {
    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var employeeServiceSupport: EmployeeServiceSupport

    @Test
    fun testValidSinceAttrs() {
        val employee = EmployeeDO()
        employee.user = getUser(TEST_USER)
        Assertions.assertNotNull(employee)
        employeeDao.insert(employee, false)
        val attr1 = employeeServiceSupport.insertValidSinceAttr(
            employee,
            LocalDate.of(2024, Month.JANUARY, 1),
            28.toString(),
            EmployeeValidSinceAttrType.ANNUAL_LEAVE,
            false
        )
        employeeServiceSupport.insertValidSinceAttr(
            employee,
            LocalDate.of(2024, Month.JANUARY, 1),
            EmployeeStatus.FREELANCER.name,
            EmployeeValidSinceAttrType.STATUS,
            false
        ) // Different type with same date allowed.
        val attr1Id = attr1.id
        suppressErrorLogs {
            try {
                employeeServiceSupport.insertValidSinceAttr(
                    employee,
                    LocalDate.of(2024, Month.JANUARY, 1),
                    32.toString(),
                    EmployeeValidSinceAttrType.ANNUAL_LEAVE,
                    false
                )
                Assertions.fail("Should not be possible to insert two annual leave entries for the same date.")
            } catch (ex: IllegalArgumentException) {
                // Expected exception.
                Assertions.assertTrue(ex.message!!.startsWith("An entry with the same type and date already exists"))
            }
        }
        var attr2 = employeeServiceSupport.insertValidSinceAttr(
            employee,
            LocalDate.of(2024, Month.JUNE, 1),
            30.toString(),
            EmployeeValidSinceAttrType.ANNUAL_LEAVE,
            false
        )

        // Current state: attr1: 2024-01-01 (28 days), attr2: 2024-06-01 (30 days)

        val attr2Id = attr2.id
        attr2.validSince = LocalDate.of(2024, Month.FEBRUARY, 1)
        employeeServiceSupport.updateValidSinceAttr(employee, attr2, false)
        suppressErrorLogs {
            try {
                attr2.validSince = LocalDate.of(2024, Month.JANUARY, 1)
                employeeServiceSupport.updateValidSinceAttr(employee, attr2, false)
                Assertions.fail("Should not be possible to update annual leave entry to the same date as another one.")
            } catch (ex: IllegalArgumentException) {
                // Expected exception.
                Assertions.assertTrue(ex.message!!.startsWith("An entry with the same type and date already exists"))
            }
        }
        employeeServiceSupport.markValidSinceAttrAsDeleted(employee, attr1, false)

        // Current state: attr1: 2024-01-01 (28 days, deleted), attr2: 2024-02-01 (30 days)

        val histTester = createHistoryTester()
        employeeServiceSupport.insertValidSinceAttr(
            employee,
            LocalDate.of(2024, Month.JANUARY, 1),
            "29",
            EmployeeValidSinceAttrType.ANNUAL_LEAVE,
            false
        )
        histTester.loadRecentHistoryEntries(1, 2)
        histTester.recentEntries!![0].let { entry ->
            // Expected: Undeleted und updated entry instead of inserting.
            Assertions.assertFalse((entry.entity as EmployeeValidSinceAttrDO).deleted)
            HistoryTester.assertHistoryEntry(
                entry,
                EmployeeValidSinceAttrDO::class,
                attr1Id,
                EntityOpType.Undelete,
                numberOfAttributes = 2
            )
            // Property "value" was customized to "annualLeave:2024-01-01":
            HistoryTester.assertHistoryAttr(
                entry,
                "value",
                value = "29",
                oldValue = "28",
                propertyTypeClass = BigDecimal::class
            )
            HistoryTester.assertHistoryAttr(
                entry,
                "deleted",
                value = "false",
                oldValue = "true",
                propertyTypeClass = Boolean::class
            )
        }

        // Current state: attr1: 2024-01-01 (29 days), attr2: 2024-06-01 (30 days)
        employeeServiceSupport.markValidSinceAttrAsDeleted(employee, attr1, false)

        // Current state: attr1: 2024-01-01 (deleted, 29 days), attr2: 2024-06-01 (30 days)

        attr2 = employeeServiceSupport.findValidSinceAttr(
            attr2Id,
            EmployeeValidSinceAttrType.ANNUAL_LEAVE,
            checkAccess = false
        )!!
        attr2.validSince = LocalDate.of(2024, Month.JANUARY, 1)
        histTester.reset()
        employeeServiceSupport.updateValidSinceAttr(employee, attr2, false)
        // Expected: current attr2 should be deleted (with date unchanged) and other deleted attr with same date should be undeleted.

        // Current state: attr1: 2024-01-01 (deleted, 30 days), attr2: 2024-06-01 (deleted, 30 days)

        histTester.loadRecentHistoryEntries(2, 3)
        histTester.recentEntries!!.find { it.entry.entityId == attr1Id }!!.let { entry ->
            // Expected: The current attr2 should be deleted and the other deleted entry attr1 (with desired validUntil date)
            // will be undeleted und updated entry instead of updating the existing one (resulting in a constraint violation).
            Assertions.assertFalse((entry.entity as EmployeeValidSinceAttrDO).deleted)
            HistoryTester.assertHistoryEntry(
                entry,
                EmployeeValidSinceAttrDO::class,
                attr1Id,
                EntityOpType.Undelete,
                numberOfAttributes = 2
            )
            // Property "value" was customized to "annualLeave:2024-01-01":
            HistoryTester.assertHistoryAttr(
                entry,
                "value",
                value = "30",
                oldValue = "28",
                propertyTypeClass = BigDecimal::class
            )
            HistoryTester.assertHistoryAttr(
                entry,
                "deleted",
                value = "false",
                oldValue = "true",
                propertyTypeClass = Boolean::class,
                msg = "Entry is now undeleted",
            )
        }
        histTester.recentEntries!!.find { it.entry.entityId == attr2Id }!!.let { entry ->
            // Expected: The current attr2 should be deleted and the other deleted entry attr1 (with desired validUntil date)
            // will be undeleted und updated entry instead of updating the existing one (resulting in a constraint violation).
            Assertions.assertTrue((entry.entity as EmployeeValidSinceAttrDO).deleted)
            HistoryTester.assertHistoryEntry(
                entry,
                EmployeeValidSinceAttrDO::class,
                attr2Id,
                EntityOpType.MarkAsDeleted,
                numberOfAttributes = 1
            )
            HistoryTester.assertHistoryAttr(
                entry,
                "deleted",
                value = "true",
                oldValue = "false",
                propertyTypeClass = Boolean::class,
                msg = "Entry is now deleted",
            )
        }
        attr2.validSince = LocalDate.of(2024, Month.JANUARY, 1)
        suppressErrorLogs {
            try {
                // This works not again, because the entry with date 2024-01-01 isn't deleted and can't be overwritten.
                employeeServiceSupport.updateValidSinceAttr(employee, attr2, false)
            } catch (ex: IllegalArgumentException) {
                // Expected exception.
                Assertions.assertTrue(ex.message!!.startsWith("An entry with the same type and date already exists"))
            }
        }
    }
}
