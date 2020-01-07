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

import org.apache.commons.lang3.StringUtils
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.configuration.DomainService
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.mail.Mail
import org.projectforge.mail.SendMail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

/**
 * Standard implementation of the vacation service interface.
 *
 * @author Florian Blumenstein
 */
@Service
open class VacationSendMailService {
    @Autowired
    private lateinit var sendMailService: SendMail
    @Autowired
    private lateinit var configService: ConfigurationService
    @Autowired
    private lateinit var domainService: DomainService

    /**
     * Sends an information mail to the vacation data users involved
     *
     * @param vacationData data
     * @param isNew        flag for new vacation request
     * @param isDeleted
     */
    open fun sendMailToVacationInvolved(vacationData: VacationDO?, isNew: Boolean, isDeleted: Boolean) {
        val urlOfVacationEditPage = domainService.domain + vacationEditPagePath + "?id=" + vacationData!!.id
        val employeeFullName = vacationData.employee!!.user!!.getFullname()
        val managerFirstName = vacationData.manager!!.user!!.firstname
        val periodI18nKey = if (vacationData.halfDayBegin!!) "vacation.mail.period.halfday" else "vacation.mail.period.fromto"
        val vacationStartDate = dateFormatter.getFormattedDate(vacationData.startDate)
        val vacationEndDate = dateFormatter.getFormattedDate(vacationData.endDate)
        val periodText = I18nHelper.getLocalizedMessage(periodI18nKey, vacationStartDate, vacationEndDate)
        val i18nSubject: String
        val i18nPMContent: String
        if (isNew && !isDeleted) {
            i18nSubject = I18nHelper.getLocalizedMessage("vacation.mail.subject", employeeFullName)
            i18nPMContent = I18nHelper
                    .getLocalizedMessage("vacation.mail.pm.application", managerFirstName, employeeFullName, periodText, urlOfVacationEditPage)
        } else if (!isNew && !isDeleted) {
            i18nSubject = I18nHelper.getLocalizedMessage("vacation.mail.subject.edit", employeeFullName)
            i18nPMContent = I18nHelper
                    .getLocalizedMessage("vacation.mail.pm.application.edit", managerFirstName, employeeFullName, periodText, urlOfVacationEditPage)
        } else { // isDeleted
            i18nSubject = I18nHelper.getLocalizedMessage("vacation.mail.subject.deleted", employeeFullName)
            i18nPMContent = I18nHelper
                    .getLocalizedMessage("vacation.mail.application.deleted", managerFirstName, employeeFullName, periodText, urlOfVacationEditPage)
        }
        // Send mail to manager and employee
        sendMail(i18nSubject, i18nPMContent,
                vacationData.manager!!.user!!,
                vacationData.employee!!.user!!
        )
        // Send mail to substitutions and employee
        for (substitution in vacationData.substitutions!!) {
            val substitutionUser = substitution.user
            val substitutionFirstName = substitutionUser!!.firstname
            val i18nSubContent: String
            i18nSubContent = if (isNew && !isDeleted) {
                I18nHelper
                        .getLocalizedMessage("vacation.mail.sub.application", substitutionFirstName, employeeFullName, periodText, urlOfVacationEditPage)
            } else if (!isNew && !isDeleted) {
                I18nHelper
                        .getLocalizedMessage("vacation.mail.sub.application.edit", substitutionFirstName, employeeFullName, periodText, urlOfVacationEditPage)
            } else { // isDeleted
                I18nHelper
                        .getLocalizedMessage("vacation.mail.application.deleted", substitutionFirstName, employeeFullName, periodText, urlOfVacationEditPage)
            }
            sendMail(i18nSubject, i18nSubContent,
                    substitutionUser,
                    vacationData.employee!!.user!!
            )
        }
    }

    /**
     * Sends an information to employee and HR, that vacation request is approved.
     *
     * @param vacationData data
     * @param approved     is application approved
     */
    open fun sendMailToEmployeeAndHR(vacationData: VacationDO, approved: Boolean) {
        val employeeUser = vacationData.employee?.user
        val urlOfVacationEditPage = domainService.domain + vacationEditPagePath + "?id=" + vacationData.id
        val employeeFullName = vacationData.employee?.user?.getFullname()
        val managerFullName = vacationData.manager?.user?.getFullname()
        val substitutionFullNames = vacationData.substitutions!!.map { it.user?.getFullname() }.joinToString(", ")
        val periodI18nKey = if (vacationData.halfDayBegin!!) "vacation.mail.period.halfday" else "vacation.mail.period.fromto"
        val vacationStartDate = dateFormatter.getFormattedDate(vacationData.startDate)
        val vacationEndDate = dateFormatter.getFormattedDate(vacationData.endDate)
        val periodText = I18nHelper.getLocalizedMessage(periodI18nKey, vacationStartDate, vacationEndDate)
        if (approved && configService.hrEmailadress != null) { //Send mail to HR (employee in copy)
            val subject = I18nHelper.getLocalizedMessage("vacation.mail.subject", employeeFullName) ?: ""
            val content = I18nHelper.getLocalizedMessage("vacation.mail.hr.approved", employeeFullName, periodText, substitutionFullNames, managerFullName, urlOfVacationEditPage)
            sendMail(subject, content, configService.hrEmailadress, "HR-MANAGEMENT", vacationData.manager?.user, employeeUser)
        }
        // Send mail to substitutions and employee
        val subject = I18nHelper.getLocalizedMessage("vacation.mail.subject.edit", employeeFullName)
        val i18nKey = if (approved) "vacation.mail.employee.approved" else "vacation.mail.employee.declined"
        val content = I18nHelper.getLocalizedMessage(i18nKey, employeeFullName, periodText, substitutionFullNames, urlOfVacationEditPage)
        val recipients = vacationData.substitutions!!.map { it.user!! }.toMutableList()
        if (employeeUser != null)
            recipients.add(employeeUser)
        sendMail(subject, content, *recipients.toTypedArray())
    }

    private fun sendMail(subject: String, content: String, vararg recipients: PFUserDO): Boolean {
        return sendMail(subject, content, null, null, *recipients)
    }

    private fun sendMail(subject: String, content: String, recipientMailAddress: String?, recipientRealName: String?,
                         vararg additionalRecipients: PFUserDO?): Boolean {
        val mail = Mail()
        mail.contentType = Mail.CONTENTTYPE_HTML
        mail.subject = subject
        mail.content = content
        if (StringUtils.isNotBlank(recipientMailAddress) && StringUtils.isNotBlank(recipientRealName)) {
            mail.setTo(recipientMailAddress, recipientRealName)
        }
        Arrays.stream(additionalRecipients).forEach { user: PFUserDO? -> mail.setTo(user) }
        return sendMailService.send(mail, null, null)
    }

    companion object {
        private const val vacationEditPagePath = "/wa/wicket/bookmarkable/org.projectforge.web.vacation.VacationEditPage"
        private val dateFormatter = DateTimeFormatter.instance()
    }
}
