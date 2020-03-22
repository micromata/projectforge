/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.vacation.service

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.employee.EmployeeTest
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.api.EmployeeService
import org.projectforge.business.user.UserDao
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationMode
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.mail.Mail
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class VacationSendMailServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var vacationSendMailService: VacationSendMailService

    @Test
    fun mailTest() {
        val vacationer = createEmployee("vacationer")
        val manager = createEmployee("manager")
        val replacement = createEmployee("replacement")
        val vacation = createVacation(vacationer, manager, replacement, VacationStatus.APPROVED)
        vacation.id = 47114711
        vacation.comment = "This is a really important comment ;-)"
        logon(vacationer.user)
        val mail = vacationSendMailService.prepareMail(vacation, OperationType.INSERT, VacationMode.MANAGER, manager.user!!)
        Assertions.assertNotNull(mail)
        Assertions.assertEquals(1, mail!!.to.size)
        Assertions.assertEquals(manager.user!!.email, mail.to[0].address)
        Assertions.assertEquals(1, mail.cc.size)
        Assertions.assertEquals(vacationer.user!!.email, mail.cc[0].address)

        val i18nArgs = arrayOf(vacationer.user!!.getFullname(), translate("vacation.mail.modType.${OperationType.INSERT.name.toLowerCase()}"))
        Assertions.assertEquals(translateMsg("vacation.mail.action.short", *i18nArgs), mail.subject)
        assertContent(mail, "${vacation.id}")
        assertContent(mail, translateMsg("vacation.mail.action", *i18nArgs))
        assertContent(mail, translate("vacation.mail.reason.${VacationMode.MANAGER.name.toLowerCase()}"))

        Assertions.assertTrue(mail.content.contains(vacationer.user!!.getFullname()))
        Assertions.assertTrue(mail.content.contains(manager.user!!.getFullname()))
        Assertions.assertTrue(mail.content.contains(replacement.user!!.getFullname()))
        Assertions.assertTrue(mail.content.contains(vacation.comment!!))
        println(mail.content)
    }

    private fun assertContent(mail: Mail, expectedContent: String) {
        Assertions.assertTrue(mail.content.contains(expectedContent), "Exprected content '$expectedContent' not found in mail content: ${mail.content}")

    }

    private fun createVacation(vacationer: EmployeeDO, manager: EmployeeDO, replacement: EmployeeDO, status: VacationStatus): VacationDO {
        val nextPeriod = getNextPeriod()
        val startDate = nextPeriod.first
        val endDate = nextPeriod.second
        return VacationDaoTest.createVacation(vacationer, manager, replacement, startDate, endDate, status)
    }

    private fun createEmployee(name: String): EmployeeDO {
        return EmployeeTest.createEmployee(employeeService, employeeDao, this, name, email = "$name@acme.com")
    }

    private fun getNextPeriod(): Pair<LocalDate, LocalDate> {
        val result = Pair(currentDate, currentDate.plusDays(10))
        currentDate = currentDate.plusDays(11)
        return result
    }

    companion object {
        private var currentDate = LocalDate.now().plusDays(1)
    }
}
