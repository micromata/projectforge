/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.birthdaybutler

import de.micromata.merlin.word.WordDocument
import de.micromata.merlin.word.templating.Variables
import mu.KotlinLogging
import org.apache.commons.io.output.ByteArrayOutputStream
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.user.UserDao
import org.projectforge.common.StringHelper
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import org.projectforge.mail.Mail
import org.projectforge.mail.MailAttachment
import org.projectforge.mail.SendMail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.IOException
import java.time.LocalDateTime
import java.time.Month
import java.util.*

private val log = KotlinLogging.logger {}

@Service
class BirthdayButlerService {
  class Response(val wordDocument: ByteArray? = null, val errorMessage: String? = null)

  @Autowired
  private lateinit var addressDao: AddressDao

  @Autowired
  private lateinit var applicationContext: ApplicationContext

  @Autowired
  private lateinit var birthdayButlerConfiguration: BirthdayButlerConfiguration

  @Autowired
  private lateinit var configurationService: ConfigurationService

  @Autowired
  private lateinit var sendMail: SendMail

  @Autowired
  private lateinit var userDao: UserDao

  fun translateMonth(month: Month, locale: Locale? = ThreadLocalUserContext.locale): String {
    return translate(locale, months[month.ordinal])
  }

  // Every month on the second last day at 8:00 AM
  @Scheduled(cron = "0 0 8 L-2 * ?")
  fun sendBirthdayButlerJob() {
    var locale = ThreadLocalUserContext.getLocale(null)
    birthdayButlerConfiguration.locale?.let {
      if (it.isNotBlank()) {
        locale = Locale(it)
      }
    }
    val month = PFDateTime.now().plusMonths(1).month // Use next month.
    log.info { "BirthdayButlerJob started: using locale '${locale.language}' and month '${month.name}'..." }
    val response = createWord(month, locale)
    val error = response.errorMessage
    if (error != null) {
      log.error { "BirthdayButlerJob aborted: ${translate(error)}" }
      return
    }
    val word = response.wordDocument
    if (word != null) {
      val attachment = object : MailAttachment {
        override fun getFilename(): String {
          return createFilename(month, locale)
        }

        override fun getContent(): ByteArray {
          return word
        }
      }
      val list = mutableListOf<MailAttachment>()
      list.add(attachment)
      sendMail(month, content = "birthdayButler.email.content", mailAttachments = list, locale = locale)
    } else {
      sendMail(month, content = "birthdayButler.email.content.noBirthdaysFound", locale = locale)
    }
    log.info("BirthdayButlerJob finished.")
  }

  fun createFilename(month: Month, locale: Locale? = null): String {
    return "${translate(locale, "menu.birthdayButler")}_${
      translateMonth(
        month,
        locale
      )
    }_${LocalDateTime.now().year}.docx"
  }

  /**
   * @param month month of the year
   * @return response with word document or error message
   */
  fun createWord(month: Month, locale: Locale? = ThreadLocalUserContext.locale): Response {
    if (!birthdayButlerConfiguration.isConfigured()) {
      log.error { "Organization property is not set." }
      return Response(errorMessage = "birthdayButler.organization.notSet")
    }
    val birthdayList = getBirthdayList(month)
      ?: return Response(errorMessage = "birthdayButler.organization.noMatchingUser")
    if (birthdayList.isEmpty()) {
      log.info { "No user with birthday in selected month" }
      return Response(errorMessage = "birthdayButler.month.response.noEntry")
    }
    val wordDocument = createWordDocument(birthdayList, locale)
    if (wordDocument != null) {
      log.info { "Birthday list for month $month created for ${birthdayList.size} users." }
      return Response(wordDocument.toByteArray())
    }
    log.error { "Error while creating word document" }
    return Response(errorMessage = "birthdayButler.wordDocument.error")

  }

  private fun sendMail(month: Month, content: String, mailAttachments: List<MailAttachment>? = null, locale: Locale?) {
    val emails = getEMailAddressesFromConfig()
    if (!emails.isNullOrEmpty()) {
      val subject = "${translate(locale, "birthdayButler.email.subject")} ${translateMonth(month, locale)}"
      emails.forEach { address ->
        val mail = Mail()
        mail.subject = subject
        mail.contentType = Mail.CONTENTTYPE_HTML
        mail.setTo(address)
        mail.content = translate(content)
        try {
          sendMail.send(mail, attachments = mailAttachments)
          log.info { "Send mail to $address" }
        } catch (ex: Exception) {
          log.error("error while trying to send mail to '$address': ${ex.message}", ex)
        }
      }
    }
  }

  private fun createWordDocument(addressList: MutableList<AddressDO>, locale: Locale?): ByteArrayOutputStream? {
    try {
      val listDates = StringBuilder()
      val listNames = StringBuilder()
      val sortedList = addressList.sortedBy { it.birthday!!.dayOfMonth }

      if (sortedList.isNotEmpty()) {
        sortedList.forEachIndexed { index, address ->
          listDates.append("\n")
          listNames.append("\n ${address.firstName} ${address.name}")
          if (index != 0 && address.birthday!!.dayOfMonth == sortedList[index - 1].birthday!!.dayOfMonth) {
            listDates.append("\t")
            return@forEachIndexed
          }
          if (address.birthday!!.dayOfMonth < 10) {
            listDates.append("0")
          }
          listDates.append(address.birthday!!.dayOfMonth).append(".")
          if (address.birthday!!.month.value < 10) {
            listDates.append("0")
          }
          listDates.append(address.birthday!!.month.value).append(".:")
        }

        val variables = Variables()
        variables.put("listNames", listNames.toString())
        variables.put("listDates", listDates.toString())
        variables.put("listLength", sortedList.size)
        variables.put("year", LocalDateTime.now().year)
        variables.put("month", translateMonth(sortedList[0].birthday!!.month, locale = locale))
        val birthdayButlerTemplate = configurationService.getOfficeTemplateFile("BirthdayButlerTemplate.docx")
        check(birthdayButlerTemplate != null) { "BirthdayButlerTemplate.docx not found" }
        val wordDocument =
          WordDocument(birthdayButlerTemplate.inputStream, birthdayButlerTemplate.file.name).use { document ->
            document.process(variables)
            document.asByteArrayOutputStream
          }
        log.info { "Birthday list created" }
        return wordDocument
      } else {
        throw IOException("Empty birthday list")
      }
    } catch (e: IOException) {
      log.error { "Error while creating word document" + e.message }
      return null
    }
  }

  /**
   * return list of users with birthday in selected month. Null, if no address with matching company found or empty, if no address with birthday in selected month found.
   */
  private fun getBirthdayList(month: Month): MutableList<AddressDO>? {
    val queryFilter = QueryFilter()
    var addressList = addressDao.internalGetList(queryFilter).filter {
      it.organization?.contains(
        birthdayButlerConfiguration.organization,
        ignoreCase = true
      ) == true
    }
    if (addressList.isEmpty()) {
      log.error { "No user with organization ${birthdayButlerConfiguration.organization} found." }
      return null
    }
    addressList = addressList.filter { address ->
      address.birthday?.month == Month.values()[month.ordinal]
    }
    val pFUserList = userDao.internalLoadAll()
    val foundUser = mutableListOf<AddressDO>()
    addressList.forEach { address ->
      pFUserList.firstOrNull { user ->
        address.firstName?.trim().equals(user.firstname?.trim(), ignoreCase = true) &&
            address.name?.trim().equals(user.lastname?.trim(), ignoreCase = true)
        foundUser.add(address)
      }
    }
    return foundUser
  }

  private fun getEMailAddressesFromConfig(): MutableList<String>? {
    val validEmailAddresses = mutableListOf<String>()
    this.birthdayButlerConfiguration.emailAddresses
      ?.split(",")
      ?.filter { StringHelper.isEmailValid(it.trim()) }
      ?.forEach { address ->
        validEmailAddresses.add(address.trim())
      }
    return validEmailAddresses.ifEmpty { null }
  }

  companion object {
    private val months = arrayOf(
      "calendar.month.january",
      "calendar.month.february",
      "calendar.month.march",
      "calendar.month.april",
      "calendar.month.may",
      "calendar.month.june",
      "calendar.month.july",
      "calendar.month.august",
      "calendar.month.september",
      "calendar.month.october",
      "calendar.month.november",
      "calendar.month.december"
    )
  }
}
