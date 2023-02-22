package org.projectforge.rest

import de.micromata.merlin.word.WordDocument
import de.micromata.merlin.word.templating.Variables
import mu.KotlinLogging
import org.apache.commons.io.output.ByteArrayOutputStream
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressFilter
import org.projectforge.business.user.UserDao
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
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/birthdayListExporter")
class BirthdayListPageRest : AbstractDynamicPageRest() {

    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var birthdayListMailService: BirthdayListMailService

    @Autowired
    private lateinit var userDao: UserDao

    private var months = arrayOf(
        "Januar",
        "Februar",
        "März",
        "April",
        "Mai",
        "Juni",
        "Juli",
        "August",
        "September",
        "Oktober",
        "November",
        "Dezember"
    )

    @GetMapping("dynamic")
    fun getForm(request: HttpServletRequest, @RequestParam("userId") userIdString: String?): FormLayoutData {
        val layout = UILayout("BirthdayListExporter")

        val values = ArrayList<UISelectValue<Int>>()
        for (i in months.indices) {
            values.add(UISelectValue(i + 1, months[i]))
        }
        layout.add(UISelect("month", required = true, values = values, label = "Month"))

        layout.addAction(
            UIButton.createDefaultButton(
                id = "download_button",
                title = "Download",
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
            var addressList = addressDao.getList(AddressFilter())
            var pFUserList = userDao.internalLoadAll()
            var filteredList = ArrayList<AddressDO>()



            pFUserList.forEach { user ->
                addressList.forEach { addressEntry ->
                    if (addressEntry.firstName.equals(user.firstname) && addressEntry.name.equals(user.lastname) && addressEntry.organization.equals(
                            user.organization
                        ) && addressEntry.birthday != null && addressEntry.birthday?.month == Month.values()[postData.data.month - 1]
                    )
                        filteredList.add(addressEntry)
                }
            }

            return if (filteredList.isNotEmpty())
                RestUtils.downloadFile(
                    "Geburtstagsliste_" + Month.values()[postData.data.month - 1] + "_" + LocalDateTime.now().year + ".docx",
                    createWordDocument(filteredList)!!.toByteArray()
                )
            else
                ResponseEntity(
                    ResponseAction(
                        validationErrors = createValidationErrors(
                            ValidationError("Es gibt keine Geburtstagseinträge für diesen Monat",
                                fieldId = "month"
                            )
                        )
                    ), HttpStatus.NOT_ACCEPTABLE
                )
        }
        return ResponseEntity(
            ResponseAction(
                validationErrors = createValidationErrors(
                    ValidationError("Der Monat muss ausgewählt sein",
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

    private fun sendMail() {
        var file = File.createTempFile("stuff", ".tmp")
        file.deleteOnExit()
        var pw = PrintWriter(file)
        pw.println("this is line 1")
        pw.println("this is line 2")
        pw.close()
    }

    //@Scheduled(cron = "0 0 8 L-2 * ?") -> Jeden vorletzten Tag des Monats
    @Scheduled(cron = "0/1 * * ? * *")
    fun sendBirthdayListJob() {
        //birthdayListMailService.sendMail()
    }

}