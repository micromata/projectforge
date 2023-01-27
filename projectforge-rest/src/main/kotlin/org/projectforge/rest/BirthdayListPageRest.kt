package org.projectforge.rest

import de.micromata.merlin.word.WordDocument
import de.micromata.merlin.word.templating.Variables
import mu.KotlinLogging
import org.apache.commons.io.output.ByteArrayOutputStream
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.IOException
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
    fun downloadBirthdayList(request: HttpServletRequest, @RequestBody postData: PostData<BirthdayListData>) : ResponseEntity<Resource>? {
        //validateCsrfToken(request, postData)?.let { return it }

        var addressList = addressDao.getList(AddressFilter())
        addressList = addressList.filter { it.birthday?.month == Month.values()[postData.data.month - 1] }
        addressList = addressList.filter { it.organization == "Micromata GmbH" }

        //birthdayListMailService.sendMail()

        return if (addressList.isNotEmpty())
            RestUtils.downloadFile("Geburtstagsliste_" + Month.values()[postData.data.month - 1] + "_" + LocalDateTime.now().year + ".docx" , createWordDocument(addressList)!!.toByteArray())
        else
        //TODO return a message to say the list is empty
            null
    }

    private fun createWordDocument(addressList: List<AddressDO>): ByteArrayOutputStream? {
        return try {
            var listDates: String = ""
            var listNames: String = ""
            var compareSameBirthday: Int = 0
            val sortedList = addressList.sortedBy { it.birthday!!.dayOfMonth }

            if (sortedList.isNotEmpty()) {
                for (i in sortedList.indices) {
                    if (sortedList[i].birthday!!.dayOfMonth == compareSameBirthday) {
                        listDates += "\n\t"
                        listNames += "\n" + sortedList[i].firstName + " " + sortedList[i].name
                        compareSameBirthday = sortedList[i].birthday!!.dayOfMonth
                    } else if (sortedList[i].birthday!!.dayOfMonth < 10) {
                        listDates += "\n" + "0" + sortedList[i].birthday!!.dayOfMonth.toString() + "." + sortedList[i].birthday!!.month.value.toString() + ".:"
                        listNames += "\n" + sortedList[i].firstName + " " + sortedList[i].name
                        compareSameBirthday = sortedList[i].birthday!!.dayOfMonth
                    } else {
                        listDates += "\n" + sortedList[i].birthday!!.dayOfMonth.toString() + "." + sortedList[i].birthday!!.month.value.toString() + ".:"
                        listNames += "\n" + sortedList[i].firstName + " " + sortedList[i].name
                        compareSameBirthday = sortedList[i].birthday!!.dayOfMonth
                    }
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


}