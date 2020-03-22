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
 * @author Kai Reinhard
 */
@Service
open class VacationSendMailService {
    @Autowired
    private lateinit var configurationService: ConfigurationService

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
        if (!configurationService.isSendMailConfigured) {
            log.info { "Mail server is not configured. No e-mail notification is sent." }
            return
        }
        val vacationInfo = VacationInfo(domainService, obj)
        if (!vacationInfo.valid) {
            return
        }
        if (obj.special == true && arrayOf(VacationStatus.IN_PROGRESS, VacationStatus.APPROVED).contains(obj.status)) {
            val hrEmailAddress = configurationService.hrEmailadress
            if (hrEmailAddress.isNullOrBlank() || !hrEmailAddress.contains("@")) {
                log.warn { "E-Mail configuration of HR staff isn't configured. Can't notificate HR team for special vacation entries. You may configure the e-mail address under ProjectForge's menu Administration->Configuration." }
            } else {
                // Send to HR
                sendMail(vacationInfo, operationType, VacationMode.HR, null, dbObj, hrEmailAddress)
            }
        }
        val vacationer = vacationInfo.employeeUser!!
        if (vacationer.id != ThreadLocalUserContext.getUserId()) {
            sendMail(vacationInfo, operationType, VacationMode.OWN, vacationer, dbObj)
        }
        val manager = vacationInfo.managerUser!!
        if (manager.id != ThreadLocalUserContext.getUserId()) {
            sendMail(vacationInfo, operationType, VacationMode.MANAGER, manager, dbObj)
        }
        val replacement = vacationInfo.replacementUser
        if (replacement != null) {
            sendMail(vacationInfo, operationType, VacationMode.REPLACEMENT, replacement, dbObj)
        }
    }

    private fun sendMail(vacationInfo: VacationInfo, operationType: OperationType, vacationMode: VacationMode, recipient: PFUserDO?, dbObj: VacationDO? = null,
                         mailTo: String? = null) {
        val mail = prepareMail(vacationInfo, operationType, vacationMode, recipient, dbObj, mailTo) ?: return
        sendMailService.send(mail)
    }

    /**
     * Especially for testing.
     *
     * Analyzes the changes of the given vacation. If necessary, e-mails will be send to the involved
     * employees (replacement and management).
     * @param obj The object to save.
     * @param dbObj The already existing object in the data base (if updated). For new objects dbObj is null.
     */
    internal fun prepareMail(obj: VacationDO, operationType: OperationType, vacationMode: VacationMode, recipient: PFUserDO?, dbObj: VacationDO? = null,
                             mailTo: String? = null): Mail? {
        val vacationInfo = VacationInfo(domainService, obj)
        return prepareMail(vacationInfo, operationType, vacationMode, recipient, dbObj, mailTo)
    }

    /**
     * Especially for testing.
     *
     * Analyzes the changes of the given vacation. If necessary, e-mails will be send to the involved
     * employees (replacement and management).
     * @param obj The object to save.
     * @param dbObj The already existing object in the data base (if updated). For new objects dbObj is null.
     */
    private fun prepareMail(vacationInfo: VacationInfo, operationType: OperationType, vacationMode: VacationMode, recipient: PFUserDO?, dbObj: VacationDO? = null,
                            mailTo: String? = null): Mail? {
        if (!vacationInfo.valid) {
            return null
        }
        val vacationer = vacationInfo.employeeUser!!
        val obj = vacationInfo.vacation

        val i18nArgs = arrayOf(vacationInfo.employeeFullname,
                vacationInfo.periodText,
                translate("vacation.mail.modType.${operationType.name.toLowerCase()}"))
        val subject = translateMsg("vacation.mail.action.short", *i18nArgs)
        val action: String = translateMsg("vacation.mail.action", *i18nArgs, vacationInfo.modifiedByUserFullname)
        val reason: String = translateMsg("vacation.mail.reason.${vacationMode.name.toLowerCase()}", vacationInfo.employeeFullname)
        val mailInfo = MailInfo(subject, reason, action)
        val mail = Mail()
        mail.subject = subject
        mail.contentType = Mail.CONTENTTYPE_HTML
        var vacationerAsCC = false
        if (recipient != null) {
            mail.setTo(recipient)
            if (vacationer.id != recipient.id) {
                vacationerAsCC = true
            }
        }
        if (!mailTo.isNullOrBlank()) {
            mail.setTo(mailTo)
            vacationerAsCC = true
        }
        if (vacationerAsCC) {
            mail.addCC(vacationer.email)
        }
        if (mail.to.isEmpty()) {
            log.error { "Oups, whether recipient nor VacationMode.HR is given to prepare mail. No notification is done." }
            return null
        }
        val data = mutableMapOf<String, Any>("vacationInfo" to vacationInfo, "vacation" to obj, "mailInfo" to mailInfo)
        mail.content = sendMailService.renderGroovyTemplate(mail, "mail/vacationMail.html", data, recipient)
        return mail
    }

    internal class VacationInfo(domainService: DomainService, val vacation: VacationDO) {
        val link = "${domainService.domain}/react/vacation/edit/${vacation.id}"
        val modifiedByUserFullname = ThreadLocalUserContext.getUser().getFullname()
        val employeeUser = vacation.employee?.user
        val employeeFullname = employeeUser?.getFullname() ?: translate("unknown")
        val managerUser = vacation.manager?.user
        val managerFullname = managerUser?.getFullname() ?: translate("unknown")
        val replacementUser = vacation.replacement?.user
        val replacementFullname = replacementUser?.getFullname() ?: translate("unknown")
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
            if (managerUser == null) {
                log.warn { "Oups, manager not given. Will not send an e-mail for vacation changes: ${vacation}" }
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
