/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.configuration.DomainService
import org.projectforge.business.employee.EmployeeTest
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.api.EmployeeService
import org.projectforge.business.user.UserDao
import org.projectforge.business.utils.HtmlHelper
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationMode
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.mail.Mail
import org.projectforge.mail.SendMail
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class VacationSendMailServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var domainService: DomainService

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var sendMail: SendMail

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

        // Check all combinations (4*4*3=48):
        arrayOf(OperationType.INSERT, OperationType.UPDATE, OperationType.DELETE, OperationType.UNDELETE)
                .forEach { operationType ->
                    arrayOf(VacationMode.MANAGER, VacationMode.REPLACEMENT, VacationMode.OWN, VacationMode.OTHER, VacationMode.HR)
                            .forEach { mode ->
                                arrayOf(vacationer, manager, replacement).forEach { employee ->
                                    assertMail(vacation, operationType, mode, employee.user!!)
                                }
                            }
                }
        // println(assertMail(vacation, OperationType.UPDATE, VacationMode.MANAGER, manager.user!!).content)
    }

    private fun assertMail(vacation: VacationDO, operationType: OperationType, vacationMode: VacationMode, receiver: PFUserDO): Mail {
        val mail = vacationSendMailService.prepareMail(vacation, operationType, vacationMode, receiver)
        Assertions.assertNotNull(mail)
        mail!!
        val vacationer = vacation.employee!!.user!!
        val manager = vacation.manager!!.user!!
        val replacement = vacation.replacement!!.user!!
        Assertions.assertNotNull(mail)
        Assertions.assertEquals(1, mail.to.size)
        Assertions.assertEquals(receiver.email, mail.to[0].address)
        if (receiver.id != ThreadLocalUserContext.getUserId()) {
            Assertions.assertEquals(1, mail.cc.size)
            Assertions.assertEquals(vacationer.email, mail.cc[0].address)
        } else {
            Assertions.assertEquals(0, mail.cc.size)
        }

        val vacationInfo = VacationSendMailService.VacationInfo(sendMail, employeeDao, vacation)
        val i18nArgs = arrayOf(vacationer.getFullname(),
                vacationInfo.periodText,
                i18n("vacation.mail.modType.${operationType.name.toLowerCase()}"))
        Assertions.assertEquals(i18n("vacation.mail.action.short", *i18nArgs), mail.subject)
        assertContent(mail, "${vacation.id}")
        assertContent(mail, vacationer.getFullname())
        assertContent(mail, manager.getFullname())
        assertContent(mail, replacement.getFullname())
        assertContent(mail, vacation.comment)
        assertContent(mail, translate("vacation"))

        Assertions.assertFalse(mail.content.contains("???"), "Unexpected content '???' in mail content: ${mail.content}")
        arrayOf(0, 1, 2, 3).forEach {
            Assertions.assertFalse(mail.content.contains("{$it}"), "At least one message param was not replaced in i18n message ('{$it}'): ${mail.content}")
        }
        return mail
    }

    private fun assertContent(mail: Mail, expectedContent: String?) {
        expectedContent!!
        Assertions.assertTrue(mail.content.contains(expectedContent), "Expected content '$expectedContent' not found in mail content: ${mail.content}")

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

    private fun i18n(key: String): String {
        return HtmlHelper.formatText(translate(key), true)
    }

    private fun i18n(key: String, vararg params: Any): String {
        return HtmlHelper.formatText(translateMsg(key, *params), true)
    }

    companion object {
        private var currentDate = LocalDate.now().plusDays(1)
    }
}
