package org.projectforge.rest

import de.micromata.merlin.word.WordDocument
import de.micromata.merlin.word.templating.Variables
import mu.KotlinLogging
import org.apache.commons.io.output.ByteArrayOutputStream
import org.hibernate.annotations.Type
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressFilter
import org.projectforge.business.scripting.I18n.getString
import org.projectforge.business.teamcal.event.model.TeamEventAttachmentDO
import org.projectforge.business.user.UserDao
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
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.time.LocalDateTime
import java.time.Month
import javax.persistence.Column
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/birthdayListExporter")
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


    @GetMapping("dynamic")
    fun getForm(request: HttpServletRequest, @RequestParam("userId") userIdString: String?): FormLayoutData {
        val layout = UILayout("BirthdayListExporter")
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

        val values = ArrayList<UISelectValue<Int>>()
        for (i in months.indices) {
            values.add(UISelectValue(i + 1, months[i]))
        }
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
        LayoutUtils.process(layout) // Macht i18n und so...
        val data = BirthdayListData()
        return FormLayoutData(data, layout, createServerData(request))
    }

    @PostMapping("downloadBirthdayList")
    fun downloadBirthdayList(request: HttpServletRequest, @RequestBody postData: PostData<BirthdayListData>) : ResponseEntity<*>? {
        //validateCsrfToken(request, postData)?.let { return it }

        if (postData.data.month in 1..12) {
            val birthdaylist = getBirthdayList(postData.data.month - 1)

            return if (birthdaylist.isNotEmpty())
                RestUtils.downloadFile(
                    "Geburtstagsliste_" + Month.values()[postData.data.month - 1] + "_" + LocalDateTime.now().year + ".docx",
                    createWordDocument(birthdaylist)!!.toByteArray()
                )
            else
                ResponseEntity(
                    ResponseAction(
                        validationErrors = createValidationErrors(
                            ValidationError(
                                getString("plugins.birthdaylist.month.response.noentry"),
                                fieldId = "month"
                            )
                        )
                    ), HttpStatus.NOT_ACCEPTABLE
                )
        }
        return ResponseEntity(
            ResponseAction(
                validationErrors = createValidationErrors(
                    ValidationError(
                        getString("plugins.birthdaylist.month.response.nothingselected"),
                        fieldId = "month"
                    )
                )
            ), HttpStatus.NOT_ACCEPTABLE
        )
    }

    private fun createWordDocument(addressList: List<AddressDO>): ByteArrayOutputStream? {
        return try {
            var listDates: String = ""
            var listNames: String = ""
            val sortedList = addressList.sortedBy { it.birthday!!.dayOfMonth }
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

            if (sortedList.isNotEmpty()) {
                for (i in sortedList.indices) {
                    listDates += "\n"
                    listNames += "\n" + sortedList[i].firstName + " " + sortedList[i].name
                    if (i != 0 && sortedList[i].birthday!!.dayOfMonth == sortedList[i - 1].birthday!!.dayOfMonth) {
                        listDates += "\t"
                        continue
                    }
                    if (sortedList[i].birthday!!.dayOfMonth < 10)
                        listDates += "0"

                    listDates += sortedList[i].birthday!!.dayOfMonth.toString() + "." + sortedList[i].birthday!!.month.value.toString() + ".:"
                }
            }

            val variables = Variables()
            variables.put("listNames", listNames)
            variables.put("listDates", listDates)
            variables.put("listLength", sortedList.size)
            variables.put("year", LocalDateTime.now().year)
            variables.put("month", months[sortedList[0].birthday!!.month.value - 1])

            val birthdayListTemplate = applicationContext.getResource("classpath:officeTemplates/BirthdayListTemplate" + ".docx")
            WordDocument(birthdayListTemplate.inputStream, birthdayListTemplate.file.name).use { document ->
                document.process(variables)
                document.asByteArrayOutputStream
            }
        } catch (e: IOException) {
            null
        }
    }

    private fun getBirthdayList(month: Int): ArrayList<AddressDO> {
        val filter = AddressFilter()
        //filter.isDeleted = false
        val addressList = addressDao.getList(filter)
        val pFUserList = userDao.internalLoadAll()
        val filteredList = ArrayList<AddressDO>()

        pFUserList.forEach { user ->
            addressList.forEach { addressEntry ->
                if (addressEntry.firstName.equals(user.firstname, ignoreCase = true) && addressEntry.name.equals(user.lastname, ignoreCase = true) && addressEntry.organization.equals(
                        user.organization, ignoreCase = true) && addressEntry.birthday != null && addressEntry.birthday?.month == Month.values()[month]
                )
                    filteredList.add(addressEntry)
            }
        }
        return filteredList
    }

    //@Scheduled(cron = "0 0 8 L-2 * ?") -> Jeden vorletzten Tag des Monats
    @Scheduled(cron = "0/5 * * ? * *")
    fun sendBirthdayListJob() {
        /*val obj = mailAttachmentimpl()
        val wordDocument = createWordDocument(getBirthdayList(11))

        obj.setFilename("Test123")
        obj.setContent(wordDocument!!.toByteArray())

        val list = mutableListOf<mailAttachmentimpl>()
        list.add(obj)
        birthdayListMailService.sendMail(list)*/
    }
}