package org.projectforge.rest

import de.micromata.merlin.word.WordDocument
import de.micromata.merlin.word.templating.Variables
import mu.KotlinLogging
import org.apache.commons.io.output.ByteArrayOutputStream
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.scripting.I18n.getString
import org.projectforge.business.user.UserDao
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.mail.MailAttachment
import org.projectforge.rest.config.BirthdayListConfiguration
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.*
import java.io.IOException
import java.time.LocalDateTime
import java.time.Month
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/birthdayList")
class BirthdayListPageRest : AbstractDynamicPageRest() {

    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var birthdayListMailService: BirthdayListMailService

    @Autowired
    private lateinit var birthdayListConfiguration: BirthdayListConfiguration

    val months = arrayOf(
        getString("calendar.month.january"),
        getString("calendar.month.february"),
        getString("calendar.month.march"),
        getString("calendar.month.april"),
        getString("calendar.month.may"),
        getString("calendar.month.june"),
        getString("calendar.month.july"),
        getString("calendar.month.august"),
        getString("calendar.month.september"),
        getString("calendar.month.october"),
        getString("calendar.month.november"),
        getString("calendar.month.december")
    )

    @GetMapping("dynamic")
    fun getForm(request: HttpServletRequest): FormLayoutData {
        val layout = UILayout(getString("menu.birthdayList"))

        val values = ArrayList<UISelectValue<Int>>()
        months.forEachIndexed { index, month -> values.add(UISelectValue(index + 1, month)) }

        layout.add(UISelect("month", required = true, values = values, label = getString("calendar.month")))
        layout.addAction(
            UIButton.createDefaultButton(
                id = "download_button",
                title = getString("download"),
                responseAction = ResponseAction(
                    RestResolver.getRestUrl(
                        this::class.java,
                        "downloadBirthdayList"
                    ), targetType = TargetType.POST
                )
            )
        )
        LayoutUtils.process(layout)
        val data = BirthdayListData()
        return FormLayoutData(data, layout, createServerData(request))
    }

    @PostMapping("downloadBirthdayList")
    fun downloadBirthdayList(
        request: HttpServletRequest,
        @RequestBody postData: PostData<BirthdayListData>
    ): ResponseEntity<*> {
        validateCsrfToken(request, postData)?.let { return it }

        if (postData.data.month in 1..12) {
            val birthdayList = getBirthdayList(postData.data.month)

            return if (birthdayList.isNotEmpty()) {
                val wordDocument = createWordDocument(birthdayList)
                if (wordDocument != null) {
                    RestUtils.downloadFile(
                        getString("menu.birthdayList") + "_" + Month.values()[postData.data.month - 1].toString()
                            .lowercase() + "_" + LocalDateTime.now().year + ".docx",
                        wordDocument.toByteArray()
                    )
                } else {
                    // error while creating word document
                    ResponseEntity(
                        ResponseAction(
                            validationErrors = createValidationErrors(
                                ValidationError(
                                    getString("birthdayList.month.response.wordDocument.error"),
                                    fieldId = "month"
                                )
                            )
                        ), HttpStatus.NOT_ACCEPTABLE
                    )
                }
            } else {
                // no user with birthday in selected month
                ResponseEntity(
                    ResponseAction(
                        validationErrors = createValidationErrors(
                            ValidationError(
                                getString("birthdayList.month.response.noEntry"),
                                fieldId = "month"
                            )
                        )
                    ), HttpStatus.NOT_ACCEPTABLE
                )
            }
        }
        // no month selected
        return ResponseEntity(
            ResponseAction(
                validationErrors = createValidationErrors(
                    ValidationError(
                        getString("birthdayList.month.response.nothingSelected"),
                        fieldId = "month"
                    )
                )
            ), HttpStatus.NOT_ACCEPTABLE
        )
    }


    private fun createWordDocument(addressList: MutableList<AddressDO>): ByteArrayOutputStream? {
        return try {
            var listDates = ""
            var listNames = ""
            val sortedList = addressList.sortedBy { it.birthday!!.dayOfMonth }

            if (sortedList.isNotEmpty()) {

                sortedList.forEachIndexed { index, address ->
                    listDates += "\n"
                    listNames += "\n" + address.firstName + " " + address.name
                    if (index != 0 && address.birthday!!.dayOfMonth == sortedList[index - 1].birthday!!.dayOfMonth) {
                        listDates += "\t"
                        return@forEachIndexed
                    }
                    if (address.birthday!!.dayOfMonth < 10)
                        listDates += "0"

                    listDates += address.birthday!!.dayOfMonth.toString() + "." + address.birthday!!.month.value.toString() + ".:"
                }

                val variables = Variables()
                variables.put("listNames", listNames)
                variables.put("listDates", listDates)
                variables.put("listLength", sortedList.size)
                variables.put("year", LocalDateTime.now().year)
                variables.put("month", months[sortedList[0].birthday!!.month.value - 1])

                val birthdayListTemplate =
                    applicationContext.getResource("classpath:officeTemplates/BirthdayListTemplate" + ".docx")
                WordDocument(birthdayListTemplate.inputStream, birthdayListTemplate.file.name).use { document ->
                    document.process(variables)
                    document.asByteArrayOutputStream
                }
            } else {
                // throw exception beacause list should not be empty
                null
            }
        } catch (e: IOException) {
            // log exeption
            null
        }
    }

    private fun getBirthdayList(month: Int): MutableList<AddressDO> {
        val filter = QueryFilter()
        val addressList = addressDao.internalGetList(filter)
        val pFUserList = userDao.internalLoadAll()
        val foundUser = mutableListOf<AddressDO>()

        if (birthdayListConfiguration.organization.isNotBlank()){
            pFUserList.forEach { user ->
                addressList.firstOrNull { address ->
                    address.firstName?.trim().equals(user.firstname?.trim(), ignoreCase = true) &&
                            address.name?.trim().equals(user.lastname?.trim(), ignoreCase = true) &&
                            address.organization?.contains(
                                birthdayListConfiguration.organization,
                                ignoreCase = true
                            ) == true
                }?.let { found ->
                    if (found.birthday?.month == Month.values()[month - 1])
                        foundUser.add(found)
                }
            }

        }
        else {
            //throw exception because organization is not set
        }
        return foundUser
    }

    // Every month on the second last day at 8:00 AM
    @Scheduled(cron = "0 0 8 L-2 * ?")
    fun sendBirthdayListJob() {
        val currentMonth = LocalDateTime.now().monthValue
        val birthdayList = getBirthdayList(currentMonth)

        if (birthdayList.isNotEmpty()) {
            val wordDocument = createWordDocument(birthdayList)

            val attachment = object : MailAttachment {
                override fun getFilename(): String {
                    return getString("menu.birthdayList") + "_" + Month.values()[currentMonth - 1].toString()
                        .lowercase() + "_" + LocalDateTime.now().year + ".docx"
                }

                override fun getContent(): ByteArray? {
                    return wordDocument?.toByteArray()
                }
            }

            val list = mutableListOf<MailAttachment>()
            list.add(attachment)
            birthdayListMailService.sendMail(
                subject = getString("birthdayList.email.subject") + " " + Month.values()[currentMonth - 1].toString().lowercase(),
                content = getString("birthdayList.email.content"),
                mailAttachments = list
            )
        } else {
            birthdayListMailService.sendMail(
                subject = getString("birthdayList.email.subject") + " " + Month.values()[currentMonth - 1].toString().lowercase(),
                content = getString("birthdayList.email.content.noBirthdaysFound"),
                mailAttachments = null
            )
        }
    }
}