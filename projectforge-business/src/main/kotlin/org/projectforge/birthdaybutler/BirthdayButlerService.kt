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

package org.projectforge.birthdaybutler

import de.micromata.merlin.word.RunsProcessor
import de.micromata.merlin.word.WordDocument
import de.micromata.merlin.word.templating.Variables
import mu.KotlinLogging
import org.apache.commons.io.output.ByteArrayOutputStream
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.apache.poi.xwpf.usermodel.XWPFTableRow
import org.apache.xmlbeans.XmlException
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressStatus
import org.projectforge.business.address.ContactStatus
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.user.UserDao
import org.projectforge.common.StringHelper
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.mail.Mail
import org.projectforge.mail.MailAttachment
import org.projectforge.mail.SendMail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.util.*

private val log = KotlinLogging.logger {}

@Service
class BirthdayButlerService {
    class Response(val wordDocument: ByteArray? = null, val errorMessage: String? = null)

    class BirthdayUser(address: AddressDO, user: PFUserDO?) {
        val name: String = if (user?.nickname.isNullOrBlank()) {
            "${address.firstName} ${address.name}"
        } else {
            "${user?.nickname} ${address.name}"
        }
        val birthday: LocalDate = address.birthday ?: LocalDate.now()
    }

    @Autowired
    private lateinit var addressDao: AddressDao

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
    // For testing: @Scheduled(fixedDelay = 3600 * 1000, initialDelay = 10 * 1000)
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
            log.error { "BirthdayButlerJob aborted: ${translate(error)} + $error" }
            sendMail(month, content = error, locale = locale)
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
            sendMail(month, content = "birthdayButler.wordDocument.error", locale = locale)
        }
        log.info("BirthdayButlerJob finished.")
    }

    fun createFilename(month: Month, locale: Locale? = null): String {
        return "${translate(locale, "menu.birthdayButler")}_${
            translateMonth(
                month,
                locale
            )
        }_${getYear(month)}.docx"
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
        val wordDocument = createWordDocument(month, birthdayList, locale)
        if (wordDocument != null) {
            log.info { "Birthday list for month $month created for ${birthdayList.size} users." }
            return Response(wordDocument.toByteArray())
        }
        log.error { "Error while creating word document" }
        return Response(errorMessage = "birthdayButler.wordDocument.error")

    }

    private fun sendMail(
        month: Month,
        content: String,
        mailAttachments: List<MailAttachment>? = null,
        locale: Locale?
    ) {
        val emails = getEMailAddressesFromConfig()
        if (!emails.isNullOrEmpty()) {
            val subject = "${translate(locale, "birthdayButler.email.subject")} ${translateMonth(month, locale)}"
            emails.forEach { address ->
                val mail = Mail()
                mail.subject = subject
                mail.contentType = Mail.CONTENTTYPE_HTML
                mail.setTo(address)
                val data = mutableMapOf<String, Any?>(
                    "content" to content,
                    "month" to translateMonth(month, locale),
                    "listSize" to getBirthdayList(month)?.size
                )
                mail.content = sendMail.renderGroovyTemplate(
                    mail,
                    "mail/birthdayButlerCronMail.html",
                    data,
                    title = subject,
                    recipient = null
                )
                try {
                    sendMail.send(mail, attachments = mailAttachments)
                    log.info { "Send mail to $address" }
                } catch (ex: Exception) {
                    log.error("Error while trying to send mail to '$address': ${ex.message}", ex)
                }
            }
        }
    }

    private fun createWordDocument(
        month: Month,
        birthdayList: MutableList<BirthdayUser>,
        locale: Locale?
    ): ByteArrayOutputStream? {
        try {
            if (birthdayList.isNotEmpty()) {
                val variables = Variables()
                variables.put("table", "") // Marker for finding table (should be removed).
                variables.put("year", getYear(month))
                variables.put("month", translateMonth(month, locale = locale))
                val birthdayButlerTemplate = configurationService.getOfficeTemplateFile("BirthdayButlerTemplate.docx")
                check(birthdayButlerTemplate != null) { "BirthdayButlerTemplate.docx not found" }
                val wordDocument =
                    WordDocument(birthdayButlerTemplate.inputStream, birthdayButlerTemplate.file.name).use { document ->
                        generateBirthdayTableRows(document.document, birthdayList)
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
    private fun getBirthdayList(month: Month): MutableList<BirthdayUser>? {
        var addressList = addressDao.selectAllNotDeleted(checkAccess = false).filter {
            it.organization?.contains(
                birthdayButlerConfiguration.organization,
                ignoreCase = true
            ) == true
                    && it.addressStatus == AddressStatus.UPTODATE
                    && it.contactStatus == ContactStatus.ACTIVE
                    && it.birthday != null
        }
        if (addressList.isEmpty()) {
            log.error { "No user with organization ${birthdayButlerConfiguration.organization} found." }
            return null
        }
        addressList = addressList.filter { address ->
            address.birthday?.month == Month.values()[month.ordinal]
        }
        val activeUsers = userDao.selectAll(checkAccess = false).filter { it.hasSystemAccess() }
        val foundUsers = mutableListOf<BirthdayUser>()
        addressList.forEach { address ->
            activeUsers.firstOrNull { user ->
                address.firstName?.trim()?.equals(user.firstname?.trim(), ignoreCase = true) == true &&
                        address.name?.trim()?.equals(user.lastname?.trim(), ignoreCase = true) == true
            }?.let { user ->
                foundUsers.add(BirthdayUser(address, user))
            }
        }
        return foundUsers
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

    private fun generateBirthdayTableRows(
        templateDocument: XWPFDocument,
        birthdayList: List<BirthdayUser>
    ): XWPFTable? {
        var posTbl: XWPFTable? = null
        for (tbl in templateDocument.tables) {
            val cell = tbl.getRow(0).getCell(0)
            cell.paragraphs?.let { paragraphs ->
                for (paragraph in paragraphs) {
                    val runsProcessor = RunsProcessor(paragraph)
                    if (runsProcessor.text.contains("\${table}")) {
                        posTbl = tbl
                        break
                    }
                }
            }
        }
        if (posTbl == null) {
            log.error("Table with marker '\${table}' in first row and first column not found. Can't process invoice positions.")
            return null
        }
        var rowCounter = 2
        birthdayList.sortedBy { it.birthday.dayOfMonth }.forEach { birthdayUser ->
            createBirthdayRow(posTbl, rowCounter++, birthdayUser)
        }
        posTbl!!.removeRow(1)
        return posTbl
    }

    private fun createBirthdayRow(posTbl: XWPFTable?, rowCounter: Int, user: BirthdayUser) {
        try {
            val sourceRow = posTbl!!.getRow(1)
            val ctrow = CTRow.Factory.parse(sourceRow.ctRow.newInputStream())
            val newRow = XWPFTableRow(ctrow, posTbl)
            val variables = Variables()
            variables.put("day", "${format(user.birthday.dayOfMonth)}.${format(user.birthday.monthValue)}.")
            variables.put("name", user.name)
            for (cell in newRow.tableCells) {
                for (cellParagraph in cell.paragraphs) {
                    RunsProcessor(cellParagraph).replace(variables)
                }
            }
            posTbl.addRow(newRow, rowCounter)
        } catch (ex: IOException) {
            log.error("Error while trying to copy row: " + ex.message, ex)
        } catch (ex: XmlException) {
            log.error("Error while trying to copy row: " + ex.message, ex)
        }
    }

    companion object {
        private fun format(number: Int): String {
            return StringHelper.format2DigitNumber(number)
        }

        /**
         * Returns the year for the given month. If the month more than 2 months is in the past, the next year is returned.
         */
        private fun getYear(month: Month): Int {
            return getYear(month, LocalDateTime.now().month)
        }

        /**
         * Only for testing purposes
         */
        internal fun getYear(month: Month, currentMonth: Month): Int {
            return if (month.ordinal + 2 < currentMonth.ordinal) {
                // At the end of the year, the next year is meant.
                LocalDateTime.now().year + 1
            } else {
                return LocalDateTime.now().year
            }
        }

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
