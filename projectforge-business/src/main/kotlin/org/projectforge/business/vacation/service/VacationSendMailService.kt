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

import mu.KotlinLogging
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.configuration.DomainService
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationMode
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.mail.Mail
import org.projectforge.mail.SendMail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Standard implementation of the vacation service interface.
 *
 * @author Florian Blumenstein
 */
@Service
open class VacationSendMailService {
    @Autowired
    private lateinit var configService: ConfigurationService

    @Autowired
    private lateinit var domainService: DomainService

    @Autowired
    private lateinit var vacationService: VacationService

    @Autowired
    private lateinit var sendMailService: SendMail

    /**
     * Analyzes the changes of the given vacation. If necessary, e-mails will be send to the involved
     * employees (replacement and management).
     * @param obj The object to save.
     * @param dbObj The already existing object in the data base (if updated). For new objects dbObj is null.
     */
    @JvmOverloads
    open fun checkAndSendMail(obj: VacationDO, operationType: OperationType, dbObj: VacationDO? = null) {
        log.warn("*** sending notification e-mails for vacations is under construction.")
/*        if (operationType == OperationType.DELETE) {
            sendMailToEmployeeAndHR(obj)
            return
        }
        if (dbObj == null) {
            sendMailToVacationInvolved(obj, true, false)
            return
        }
        val currentStatus = obj.status
        val formerStatus = dbObj.status
        if (currentStatus == VacationStatus.IN_PROGRESS) {
            sendMailToVacationInvolved(obj, false, false)
            return
        }
        if (formerStatus != currentStatus) {
            sendMailToEmployeeAndHR(obj)
            return
        }*/
    }

    /**
     * Especially for testing.
     *
     * Analyzes the changes of the given vacation. If necessary, e-mails will be send to the involved
     * employees (replacement and management).
     * @param obj The object to save.
     * @param dbObj The already existing object in the data base (if updated). For new objects dbObj is null.
     */
    open fun prepareMail(obj: VacationDO, operationType: OperationType, receiverType: VacationMode, recipient: PFUserDO, dbObj: VacationDO? = null): Mail? {
        val vacationer = obj.employee?.user
        if (vacationer == null) {
            log.warn { "No vacationer set in vacaion entry. Will not send e-mail: ${ToStringUtil.toJsonString(obj)}" }
            return null
        }
        val vacationInfo = VacationInfo(domainService, vacationService, obj)
        val i18nArgs = arrayOf(vacationInfo.employeeFullname,
                vacationInfo.periodText,
                translate("vacation.mail.modType.${operationType.name.toLowerCase()}"))
        val subject = translateMsg("vacation.mail.action.short", *i18nArgs)
        val action: String = translateMsg("vacation.mail.action", *i18nArgs)
        val reason: String = translateMsg("vacation.mail.reason.${receiverType.name.toLowerCase()}", vacationInfo.employeeFullname)
        val mailInfo = MailInfo(subject, reason, action)
        val mail = Mail()
        mail.subject = subject
        mail.contentType = Mail.CONTENTTYPE_HTML
        mail.setTo(recipient)

        val data = mutableMapOf<String, Any>("vacationInfo" to vacationInfo, "vacation" to obj, "mailInfo" to mailInfo)
        mail.content = sendMailService.renderGroovyTemplate(mail, "mail/vacationMail.html", data, recipient)
        if (vacationer.id != recipient.id) {
            mail.addCC(vacationer.email)
        }
        return mail
    }

        /* OLD code
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
        val substitutionUser = vacationData.replacement?.user
        if (substitutionUser != null) {
            val substitutionFirstName = substitutionUser.firstname
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
        }*/

    /**
     * Sends an information to employee and HR, that vacation request is approved.
     *
     * @param vacationData data
     */
    /* OLD code
    private fun sendMailToEmployeeAndHR(vacationData: VacationDO) {
        val employeeUser = vacationData.employee?.user
        val urlOfVacationEditPage = domainService.domain + vacationEditPagePath + "?id=" + vacationData.id
        val employeeFullName = vacationData.employee?.user?.getFullname()
        val managerFullName = vacationData.manager?.user?.getFullname()
        val replacementFullname = vacationData.replacement?.user?.getFullname()
        val periodI18nKey = if (vacationData.halfDayBegin!!) "vacation.mail.period.halfday" else "vacation.mail.period.fromto"
        val vacationStartDate = dateFormatter.getFormattedDate(vacationData.startDate)
        val vacationEndDate = dateFormatter.getFormattedDate(vacationData.endDate)
        val periodText = I18nHelper.getLocalizedMessage(periodI18nKey, vacationStartDate, vacationEndDate)
        if (vacationData.status == VacationStatus.APPROVED && configService.hrEmailadress != null) { //Send mail to HR (employee in copy)
            val subject = I18nHelper.getLocalizedMessage("vacation.mail.subject", employeeFullName) ?: ""
            val content = I18nHelper.getLocalizedMessage("vacation.mail.hr.approved", employeeFullName, periodText, replacementFullname, managerFullName, urlOfVacationEditPage)
            // sendMail(subject, content, configService.hrEmailadress, "HR-MANAGEMENT", vacationData.manager?.user, employeeUser)
        }
        // Send mail to substitutions and employee
        val subject = I18nHelper.getLocalizedMessage("vacation.mail.subject.edit", employeeFullName)
        val i18nKey = if (vacationData.status == VacationStatus.APPROVED) "vacation.mail.employee.approved" else "vacation.mail.employee.declined"
        val content = I18nHelper.getLocalizedMessage(i18nKey, employeeFullName, periodText, replacementFullname, urlOfVacationEditPage)
        val recipients = mutableListOf<PFUserDO>()
        vacationData.replacement?.user?.let { recipients.add(it) }
        employeeUser?.let { recipients.add(it) }
        // sendMail(subject, content, *recipients.toTypedArray())
    }*/

    /**
     * Sends an information to vacationer.
     */
    /*private fun sendMail(mailInfo: MailInfo, recipient: PFUserDO, receiverType: VacationMode) {
        if (recipient.id == ThreadLocalUserContext.getUserId()) {
            // Do not send e-mail to receiver for his own changes.
            return
        }
        // sendMail(subject, content, recipient = recipient)
    }*/

    internal class VacationInfo(domainService: DomainService, vacationService: VacationService, val vacation: VacationDO) {
        val link = "${domainService.domain}/react/vacation/edit/${vacation.id}"
        val employeeUser = vacation.employee?.user
        val employeeFullname = employeeUser?.getFullname() ?: translate("unknown")
        val managerFullname = vacation.manager?.user?.getFullname() ?: translate("unknown")
        val replacementFullname = vacation.replacement?.user?.getFullname() ?: translate("unknown")
        val startDate = dateFormatter.getFormattedDate(vacation.startDate)
        val endDate = dateFormatter.getFormattedDate(vacation.endDate)
        val halfDayBeginFormatted = translate(vacation.halfDayBegin)
        val halfDayEndFormatted = translate(vacation.halfDayEnd)
        val vacationSpecialFormatted = translate(vacation.special)
        val workingDays = VacationService.getVacationDays(vacation)
        val workingDaysFormatted = VacationStats.format(workingDays)
        val periodText = I18nHelper.getLocalizedMessage("vacation.mail.period", startDate, endDate, workingDaysFormatted)
        var valid: Boolean = true

        init {
            if (employeeUser == null) {
                log.warn { "Oups, employee not given. Will not send an e-mail for vacation changes: ${vacation}" }
                valid = false
            }
        }
    }

    private class MailInfo(val subject: String, val reason: String, val action: String)

    companion object {
        private const val vacationEditPagePath = "/react/vacationAccount"
        private val dateFormatter = DateTimeFormatter.instance()
    }
}
