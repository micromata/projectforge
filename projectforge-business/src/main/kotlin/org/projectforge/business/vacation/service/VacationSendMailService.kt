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

package org.projectforge.business.vacation.service

import mu.KotlinLogging
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.configuration.ConfigurationServiceAccessor
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationMode
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.mail.Mail
import org.projectforge.mail.SendMail
import org.projectforge.menu.builder.MenuItemDefId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

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
  private lateinit var employeeDao: EmployeeDao

  @Autowired
  private lateinit var sendMail: SendMail

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
    val vacationInfo = VacationInfo(employeeDao, obj)
    if (!vacationInfo.valid) {
      return
    }
    if (obj.special == true && arrayOf(VacationStatus.IN_PROGRESS, VacationStatus.APPROVED).contains(obj.status)) {
      val hrEmailAddress = configurationService.hREmailadress
      if (hrEmailAddress.isNullOrBlank() || !hrEmailAddress.contains("@")) {
        log.warn { "E-Mail configuration of HR staff isn't configured. Can't notificate HR team for special vacation entries. You may configure the e-mail address under ProjectForge's menu Administration->Configuration." }
      } else {
        // Send to HR
        sendMail(vacationInfo, operationType, VacationMode.HR, null, hrEmailAddress)
      }
    }
    val vacationer = vacationInfo.employeeUser!!
    if (vacationer.id != ThreadLocalUserContext.loggedInUserId) {
      sendMail(vacationInfo, operationType, VacationMode.OWN, vacationer)
    }
    val manager = vacationInfo.managerUser!!
    if (manager.id != ThreadLocalUserContext.loggedInUserId) {
      sendMail(vacationInfo, operationType, VacationMode.MANAGER, manager)
    }
    val replacements = mutableSetOf<PFUserDO>()
    vacationInfo.replacementUser?.let { user ->
      replacements.add(user)
    }
    vacationInfo.otherReplacementUsers.forEach { user ->
      replacements.add(user)
    }
    replacements.forEach { user ->
      sendMail(vacationInfo, operationType, VacationMode.REPLACEMENT, user)
    }
  }

  private fun sendMail(
    vacationInfo: VacationInfo, operationType: OperationType, vacationMode: VacationMode, recipient: PFUserDO?,
    mailTo: String? = null
  ) {
    val mail = prepareMail(vacationInfo, operationType, vacationMode, recipient, mailTo) ?: return
    sendMail.send(mail)
  }

  /**
   * Especially for testing.
   *
   * Analyzes the changes of the given vacation. If necessary, e-mails will be send to the involved
   * employees (replacement and management).
   */
  internal fun prepareMail(
    obj: VacationDO, operationType: OperationType, vacationMode: VacationMode, recipient: PFUserDO?,
    mailTo: String? = null
  ): Mail? {
    val vacationInfo = VacationInfo(employeeDao, obj)
    return prepareMail(vacationInfo, operationType, vacationMode, recipient, mailTo)
  }

  /**
   * Especially for testing.
   *
   * Analyzes the changes of the given vacation. If necessary, e-mails will be send to the involved
   * employees (replacement and management).
   */
  private fun prepareMail(
    vacationInfo: VacationInfo, operationType: OperationType, vacationMode: VacationMode, recipient: PFUserDO?,
    mailTo: String? = null
  ): Mail? {
    if (!vacationInfo.valid) {
      return null
    }
    vacationInfo.updateI18n(recipient)
    val vacationer = vacationInfo.employeeUser!!
    val obj = vacationInfo.vacation

    val i18nArgs = arrayOf(
      vacationInfo.employeeFullname,
      vacationInfo.periodText,
      translate(recipient, "vacation.mail.modType.${operationType.name.lowercase()}")
    )
    val subject = translate(recipient, "vacation.mail.action.short", *i18nArgs)
    val operation = translate(recipient, "vacation.mail.modType.${operationType.name.lowercase()}")
    val mode = vacationMode.name.lowercase()
    val mailInfo = MailInfo(subject, operation, mode)
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
      log.error { "Oups, neither recipient nor VacationMode.HR is given to prepare mail. No notification is done." }
      return null
    }
    val data = mutableMapOf<String, Any?>("vacationInfo" to vacationInfo, "vacation" to obj, "mailInfo" to mailInfo)
    mail.content =
      sendMail.renderGroovyTemplate(mail, "mail/vacationMail.html", data, translate(recipient, "vacation"), recipient)
    return mail
  }

  // Used by template.
  @Suppress("HasPlatformType", "MemberVisibilityCanBePrivate", "unused")
  internal class VacationInfo(employeeDao: EmployeeDao, val vacation: VacationDO) {
    val link = getLinkToVacationEntry(vacation.id)
    val modifiedByUser = ThreadLocalUserContext.loggedInUser!!
    val modifiedByUserFullname = modifiedByUser.getFullname()
    val modifiedByUserMail = modifiedByUser.email
    val employeeUser = employeeDao.find(vacation.employee?.id, checkAccess = false)?.user
    var employeeFullname = employeeUser?.getFullname() ?: "unknown"
    val employeeMail = employeeUser?.email
    val managerUser = employeeDao.find(vacation.manager?.id, checkAccess = false)?.user
    var managerFullname = managerUser?.getFullname() ?: "unknown"
    val managerMail = managerUser?.email
    val replacementUser = employeeDao.find(vacation.replacement?.id, checkAccess = false)?.user
    val otherReplacementUsers = mutableListOf<PFUserDO>()
    var replacementFullname = replacementUser?.getFullname() ?: "unknown"
    var otherReplacementsFullnames: String = ""
    val replacementMail = replacementUser?.email
    var startDate = dateFormatter.getFormattedDate(vacation.startDate)
    var endDate = dateFormatter.getFormattedDate(vacation.endDate)
    lateinit var halfDayBeginFormatted: String
    lateinit var halfDayEndFormatted: String
    lateinit var vacationSpecialFormatted: String
    val workingDays = VacationService.getVacationDays(vacation)
    val workingDaysFormatted = VacationStats.format(workingDays)
    var periodText = I18nHelper.getLocalizedMessage("vacation.mail.period", startDate, endDate, workingDaysFormatted)
    var valid: Boolean = true

    /**
     * E-Mail be be sent to recipients with different locales.
     */
    fun updateI18n(recipient: PFUserDO?) {
      employeeFullname = employeeUser?.getFullname() ?: translate(recipient, "unknown")
      managerFullname = managerUser?.getFullname() ?: translate(recipient, "unknown")
      replacementFullname = replacementUser?.getFullname() ?: translate(recipient, "unknown")
      halfDayBeginFormatted = translate(recipient, vacation.halfDayBegin)
      halfDayEndFormatted = translate(recipient, vacation.halfDayEnd)
      vacationSpecialFormatted = translate(recipient, vacation.special)
      startDate = dateFormatter.getFormattedDate(recipient, vacation.startDate)
      endDate = dateFormatter.getFormattedDate(recipient, vacation.endDate)
      periodText =
        I18nHelper.getLocalizedMessage(recipient, "vacation.mail.period", startDate, endDate, workingDaysFormatted)
    }

    init {
      if (employeeUser == null) {
        log.warn { "Oups, employee not given. Will not send an e-mail for vacation changes: $vacation" }
        valid = false
      }
      if (managerUser == null) {
        log.warn { "Oups, manager not given. Will not send an e-mail for vacation changes: $vacation" }
        valid = false
      }
      vacation.otherReplacements?.forEach { employeeDO ->
        employeeDao.find(employeeDO.id, checkAccess = false)?.user?.let { user ->
          otherReplacementUsers.add(user)
        }
      }
      otherReplacementUsers.let { list ->
        otherReplacementsFullnames = list.joinToString { user ->
          user.getFullname()
        }
      }
    }

    fun formatModifiedByUser(): String {
      return formatUserWithMail(this.modifiedByUserFullname, this.modifiedByUserMail)
    }

    fun formatEmployee(): String {
      return formatUserWithMail(this.employeeFullname, this.employeeMail)
    }

    fun formatManager(): String {
      return formatUserWithMail(this.managerFullname, this.managerMail)
    }

    fun formatReplacement(): String {
      return formatUserWithMail(this.replacementFullname, this.replacementMail)
    }

    fun formatUserWithMail(name: String, mail: String? = null): String {
      return SendMail.formatUserWithMail(name, mail)
    }
  }

  @Suppress("unused")
  private class MailInfo(val subject: String, val operation: String, val mode: String)

  companion object {
    private var _linkToVacationEntry: String? = null
    private val linkToVacationEntry: String
      get() {
        if (_linkToVacationEntry == null) {
          val sendMail = ApplicationContextProvider.getApplicationContext().getBean(SendMail::class.java)
          _linkToVacationEntry = sendMail.buildUrl("$vacationEditPagePath/")
        }
        return _linkToVacationEntry!!
      }

    fun getLinkToVacationEntry(id: String): String {
      return "$linkToVacationEntry$id?returnToCaller=account"
    }

    fun getLinkToVacationEntry(id: Long?): String {
      id ?: return "???"
      return getLinkToVacationEntry(id.toString())
    }

    private val vacationEditPagePath = "${MenuItemDefId.VACATION.url}/edit"
    private val dateFormatter = DateTimeFormatter.instance()
    private var _defaultLocale: Locale? = null
    private val defaultLocale: Locale
      get() {
        if (_defaultLocale == null) {
          _defaultLocale = ConfigurationServiceAccessor.get().defaultLocale ?: Locale.getDefault()
        }
        return _defaultLocale!!
      }

    private fun translate(recipient: PFUserDO?, i18nKey: String, vararg params: Any): String {
      return I18nHelper.getLocalizedMessage(recipient, i18nKey, *params)
    }

    private fun translate(recipient: PFUserDO?, value: Boolean?): String {
      return translate(recipient, if (value == true) "yes" else "no")
    }
  }
}
