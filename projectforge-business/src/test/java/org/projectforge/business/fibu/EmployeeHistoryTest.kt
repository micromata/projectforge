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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.employee.EmployeeTest.Companion.createEmployee
import org.projectforge.framework.persistence.history.EntityOpType
import org.projectforge.framework.persistence.history.PropertyOpType
import org.projectforge.test.AbstractTestBase
import org.projectforge.test.HistoryTester
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class EmployeeHistoryTest : AbstractTestBase() {
    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Test
    fun testHistory() {
        logon(TEST_FULL_ACCESS_USER)
        val employee = createEmployee(employeeService, employeeDao, this, "EmployeeHistoryTest")
        val hist = createHistoryTester()
        employeeService.insertStatus(employee, LocalDate.of(2024, 1, 1), EmployeeStatus.FEST_ANGESTELLTER)
        hist.loadRecentHistoryEntries(1)
        var historyEntries = employeeDao.selectHistoryEntries(employee, false)
        Assertions.assertEquals(
            4,
            historyEntries.size
        ) // Employee inserted, 2xannualleave inserted by createEmployee(), 1 status inserted
        Assertions.assertEquals(
            3,
            historyEntries.count { it.entityName == EmployeeValidityPeriodAttrDO::class.qualifiedName })
        val statusList = employeeService.selectAllValidityPeriodAttrs(employee, EmployeeValidityPeriodAttrType.STATUS)
        Assertions.assertEquals(1, statusList.size)
        val validAttr = statusList[0]
        validAttr.status = EmployeeStatus.FREELANCER
        validAttr.validFrom = LocalDate.of(2024, 5, 1)
        employeeService.updateValidityPeriodAttr(employee, validAttr)
        historyEntries = employeeDao.selectHistoryEntries(employee, false)
        Assertions.assertEquals(5, historyEntries.size)
        historyEntries[0].let { entry ->
            HistoryTester.assertHistoryAttr(
                entry,
                "status:2024-05-01",
                value = EmployeeStatus.FREELANCER.name,
                oldValue = EmployeeStatus.FEST_ANGESTELLTER.name,
                opType = PropertyOpType.Update,
                propertyTypeClass = EmployeeStatus::class,
            )
            HistoryTester.assertHistoryAttr(
                entry,
                "validFrom",
                value = "2024-05-01",
                oldValue = "2024-01-01",
                opType = PropertyOpType.Update,
                propertyTypeClass = LocalDate::class,
            )
        }
        val attr: EmployeeValidityPeriodAttrDO
        employeeService.selectAllValidityPeriodAttrs(employee, EmployeeValidityPeriodAttrType.STATUS).let { list ->
            attr = list[0]
            attr.status = EmployeeStatus.STUDENTISCHE_HILFSKRAFT
            Assertions.assertEquals(1, list.size)
            employeeService.markValidityPeriodAttrAsDeleted(employee, attr, false)
        }
        historyEntries = employeeDao.selectHistoryEntries(employee, false)
        Assertions.assertEquals(6, historyEntries.size)
        historyEntries[0].let { entry ->
            HistoryTester.assertHistoryEntry(
                entry,
                EmployeeValidityPeriodAttrDO::class,
                entityId = attr.id,
                EntityOpType.MarkAsDeleted,
                numberOfAttributes = 2,
            )
            HistoryTester.assertHistoryAttr(
                entry, propertyName = "status:2024-05-01", value = EmployeeStatus.STUDENTISCHE_HILFSKRAFT.name,
                oldValue = EmployeeStatus.FREELANCER.name, propertyTypeClass = EmployeeStatus::class,
            )
            HistoryTester.assertHistoryAttr(
                entry, propertyName = "deleted", value = "true",
                oldValue = "false", propertyTypeClass = Boolean::class,
            )
        }
        attr.deleted = false
        employeeService.markValidityPeriodAttrAsDeleted(employee, attr, false)
        historyEntries = employeeDao.selectHistoryEntries(employee, false)
        Assertions.assertEquals(
            6,
            historyEntries.size
        ) // No more history entries, object was already marked as deleted.
        attr.status = EmployeeStatus.AZUBI
        employeeService.markValidityPeriodAttrAsDeleted(employee, attr, false)
        historyEntries = employeeDao.selectHistoryEntries(employee, false)
        Assertions.assertEquals(7, historyEntries.size) // attr.status changed.
        historyEntries[0].let { entry ->
            HistoryTester.assertHistoryEntry(
                entry,
                EmployeeValidityPeriodAttrDO::class,
                entityId = attr.id,
                EntityOpType.MarkAsDeleted,
                numberOfAttributes = 1,
            )
            HistoryTester.assertHistoryAttr(
                entry, propertyName = "status:2024-05-01", value = EmployeeStatus.AZUBI.name,
                oldValue = EmployeeStatus.STUDENTISCHE_HILFSKRAFT.name, propertyTypeClass = EmployeeStatus::class,
            )
        }

        //Assertions.assertEquals(4, historyEntries.size) // Employee inserted, 2xannualleave inserted by createEmployee(), 1 status inserted
        employeeService.selectAllValidityPeriodAttrs(employee, EmployeeValidityPeriodAttrType.STATUS).let { list ->
            Assertions.assertEquals(1, list.size)
            employeeService.undeleteValidityPeriodAttr(employee, attr, false)
        }
        historyEntries = employeeDao.selectHistoryEntries(employee, false)
        Assertions.assertEquals(8, historyEntries.size)
        historyEntries[0].let { entry ->
            HistoryTester.assertHistoryEntry(
                entry,
                EmployeeValidityPeriodAttrDO::class,
                entityId = attr.id,
                EntityOpType.Undelete,
                numberOfAttributes = 1,
            )
            HistoryTester.assertHistoryAttr(
                entry, propertyName = "deleted", value = "false",
                oldValue = "true", propertyTypeClass = Boolean::class,
            )
        }
    }
}
