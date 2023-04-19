package org.projectforge.rest.poll

import org.projectforge.business.group.service.GroupService
import com.fasterxml.jackson.databind.ObjectMapper
import org.projectforge.business.poll.PollDO
import org.projectforge.business.poll.PollDao
import org.projectforge.business.user.service.UserService
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.mail.MailAttachment
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.rest.VacationExportPageRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.*
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.poll.Exel.ExcelExport
import org.projectforge.rest.poll.types.BaseType
import org.projectforge.rest.poll.types.Frage
import org.projectforge.rest.dto.*
import org.projectforge.ui.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.servlet.http.HttpServletRequest
import kotlin.collections.ArrayList

@RestController
@RequestMapping("${Rest.URL}/poll")
class PollPageRest : AbstractDTOPagesRest<PollDO, Poll, PollDao>(PollDao::class.java, "poll.title") {

    private val log: Logger = LoggerFactory.getLogger(PollPageRest::class.java)

    @Autowired
    private lateinit var userService: UserService;

    @Autowired
    private lateinit var groupService: GroupService;

    @Autowired
    private lateinit var pollDao: PollDao

    @Autowired
    private lateinit var pollMailService: PollMailService

    override fun newBaseDTO(request: HttpServletRequest?): Poll {
        val result = Poll()
        result.owner = ThreadLocalUserContext.user
        return result
    }

    override fun transformForDB(dto: Poll): PollDO {
        val pollDO = PollDO()
        dto.copyTo(pollDO)
        if(dto.inputFields!= null){
            pollDO.inputFields = ObjectMapper().writeValueAsString(dto.inputFields)
        }
        return pollDO
    }


    //override fun transformForDB editMode not used
    override fun transformFromDB(obj: PollDO, editMode: Boolean): Poll {
        val poll = Poll()
        poll.copyFrom(obj)
        if(obj.inputFields!= null){
            var a = ObjectMapper().readValue(obj.inputFields, MutableList::class.java)
            poll.inputFields = a.map { Frage().toObject(ObjectMapper().writeValueAsString(it)) }.toMutableList()
        }
        User.restoreDisplayNames(poll.fullAccessUsers, userService)
        Group.restoreDisplayNames(poll.fullAccessGroups, groupService)
        User.restoreDisplayNames(poll.attendees, userService)
        Group.restoreDisplayNames(poll.groupAttendees, groupService)
        return poll
    }

    override fun createListLayout(
        request: HttpServletRequest, layout: UILayout, magicFilter: MagicFilter, userAccess: UILayout.UserAccess
    ) {
        agGridSupport.prepareUIGrid4ListPage(
            request,
            layout,
            magicFilter,
            this,
            userAccess = userAccess,
        )
            .add(lc, "title", "description", "location", "owner", "deadline", "date", "state")
        layout.add(
            MenuItem(
                "export",
                i18nKey = "poll.export.title",
                url = PagesResolver.getDynamicPageUrl(VacationExportPageRest::class.java),
                type = MenuItemTargetType.REDIRECT,
            )
        )
    }


    override fun createEditLayout(dto: Poll, userAccess: UILayout.UserAccess): UILayout {
        val lc = LayoutContext(PollDO::class.java)
        val obj = PollDO()
        dto.copyTo(obj)
        val layout = super.createEditLayout(dto, userAccess)
        layout.add(
            UIRow().add(
                UIFieldset(UILength(md = 6, lg = 4)).add(lc, "title", "description", "location")
                        .add(lc, "owner")
                        .add(lc, "deadline", "date")
                        .add(UISelect.createUserSelect(lc, "fullAccessUsers", true, "poll.fullAccessUsers"))
                        .add(UISelect.createGroupSelect(lc, "fullAccessGroups", true, "poll.fullAccessGroups"))
                        .add(UISelect.createUserSelect(lc, "attendees", true, "poll.attendees"))
                        .add(UISelect.createGroupSelect(lc, "groupAttendees", true, "poll.groupAttendees"))
            )
        )
        layout.add(
            UIRow().add(
                UIFieldset(UILength(md = 6, lg = 4)).add(
                    UIButton.createAddButton(
                        responseAction = ResponseAction("${Rest.URL}/poll/add", targetType = TargetType.POST)
                    )
                ).add(
                    UISelect("questionType", values = BaseType.values().map { UISelectValue(it, it.name) })

                )
            )
        )
        addQuestionFieldset(layout, dto)

                layout.watchFields.addAll(
            arrayOf(
                "title", "description", "location", "deadline",
                "date"
            )
        )
        return LayoutUtils.processEditPage(layout, dto, this)
    }

    override fun onAfterSaveOrUpdate(request: HttpServletRequest, obj: PollDO, postData: PostData<Poll>) {
        super.onAfterSaveOrUpdate(request, obj, postData)
        val dto = postData.data
        pollMailService.sendMail(subject = "", content = "", to = "test.mail")
    }

    @PostMapping("/addAntwort/{fieldId}")
    fun addAntwortFeld(
        @RequestBody postData: PostData<Poll>,
        @PathVariable("fieldId") fieldUid: String,
    ): ResponseEntity<ResponseAction> {
        val dto = postData.data
        val userAccess = UILayout.UserAccess(insert = true, update = true)

        val found = dto.inputFields?.find { it.uid == fieldUid }
        found?.antworten?.add("")

        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE).addVariable("data", dto).addVariable("ui", createEditLayout(dto, userAccess))
        )
    }

    /**
     * Method to end polls after deadline
     */
    fun cronEndPolls() {

        var mail = "";
        var header = "";

        val polls = pollDao.internalLoadAll()
        val list = ArrayList<MailAttachment>()
        // set State.FINISHED for all old polls
        polls.forEach {
            if (it.deadline?.isBefore(LocalDate.now().minusDays(1)) == true) {
                it.state = PollDO.State.FINISHED
                // check if state is open or closed

                val ihkExporter = ExcelExport()

                val poll = Poll()
                poll.copyFrom(it)

                val exel = ihkExporter
                    .getExcel(poll)

                val attachment = object : MailAttachment {
                    override fun getFilename(): String {
                        return it.title+ "_" + LocalDateTime.now().year +"_Result"+ ".xlsx"
                    }

                    override fun getContent(): ByteArray? {
                        return exel
                    }
                }
                list.add(attachment)


                header = "Umfrage ist abgelaufen"
                mail ="""
                        Die Umfrage ist zu ende. Hier die ergebnisse.
                     """.trimMargin()

                pollDao.internalSaveOrUpdate(it)
            }

        }

        try {

            // erstell mir eine funktion, die alles deadlines mir gibt die in der zukunft liegen
            val pollsInFuture = polls.filter { it.deadline?.isAfter(LocalDate.now()) ?: false}
            pollsInFuture.forEach{
                val daysDifference = ChronoUnit.DAYS.between(LocalDate.now(), it.deadline)
                if(daysDifference == 1L || daysDifference == 7L){
                    header = "Umfrage Endet in $daysDifference Tage"
                    mail ="""
                    Sehr geehrter Teilnehmer,wir laden Sie herzlich dazu ein, an unserer Umfrage zum Thema ${it.title} teilzunehmen. 
                    Ihre Meinung ist uns sehr wichtig und wir würden uns freuen, wenn Sie uns dabei helfen könnten,
                    unsere Forschungsergebnisse zu verbessern. Für diese Umfrage ist ${it ///owmer
                    }zuständig.
                    Bei Fragen oder Anmerkungen können Sie sich gerne an ihn wenden.
                    Bitte beachten Sie, dass das Enddatum für die Teilnahme an dieser Umfrage der ${it.deadline.toString()} ist.
                    Wir würden uns freuen, wenn Sie sich die Zeit nehmen könnten, um diese Umfrage auszufüllen.
                    Vielen Dank im Voraus für Ihre Unterstützung.
                    
                    Mit freundlichen Grüßen,${it  ///owmer
                    }
                     """.trimMargin()
                }
            }



            if(mail.isNotEmpty()){
                pollMailService.sendMail(to="test", subject = header, content = mail, mailAttachments = list)
            }
        }
        catch (e:Exception) {
            log.error(e.toString())
        }
    }


    /**
     * Cron job for daily stuff
     */
    @Scheduled(cron = "0 0 1 * * *") // 1am everyday
    fun dailyCronJobs() {
        cronDeletePolls()
        cronEndPolls()
    }


    /**
     * Method to delete old polls
     */
    fun cronDeletePolls() {
        // check if poll end in Future
        val polls = pollDao.internalLoadAll()
        val pollsMoreThanOneYearPast = polls.filter { it.created?.before(Date.from(LocalDate.now().minusYears(1).atStartOfDay(
            ZoneId.systemDefault()
        ).toInstant())) ?: false }
        pollsMoreThanOneYearPast.forEach {
            pollDao.delete(it)
        }
    }

    // PostMapping add
    @PostMapping("/add")
    fun addQuestionField(
        @RequestBody postData: PostData<Poll>,
    ): ResponseEntity<ResponseAction> {
        val userAccess = UILayout.UserAccess(insert = true, update = true)
        val dto = postData.data
        var type = BaseType.valueOf(dto.questionType ?: "FreiTextFrage")


        val poll = PollDO()
        if (dto.inputFields == null) {
            dto.inputFields = mutableListOf()
        }

        var frage = Frage(uid = UUID.randomUUID().toString(), type = type)
        if(type== BaseType.JaNeinFrage) {
            frage.antworten = mutableListOf("ja", "nein")
        }
        if(type== BaseType.DatumsAbfrage) {
            frage.antworten = mutableListOf("Ja", "Vielleicht", "Nein")
        }

        dto.inputFields!!.add(frage)

        dto.copyTo(poll)
        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE).addVariable("data", dto).addVariable("ui", createEditLayout(dto, userAccess))
        )
    }


    private fun addQuestionFieldset(layout: UILayout, dto: Poll) {

        dto.inputFields?.forEachIndexed { index, field ->
            val feld = UIRow()
            if (field.type == BaseType.JaNeinFrage) {
                val groupLayout = UIGroup()
                field.antworten?.forEach { antwort ->
                    groupLayout.add(
                        UIRadioButton(
                            "JaNeinRadio", antwort, label = antwort
                        )
                    )
                }
                feld.add(
                    UIFieldset(UILength(md = 6, lg = 4), title = field.type.toString()).add(UIInput("inputFields[${index}].question")).add
                        (groupLayout)
                )
            }

            if (field.type == BaseType.FreiTextFrage) {
                feld.add(
                    UIFieldset(UILength(md = 6, lg = 4), title = field.type.toString()).add(UIInput("inputFields[${index}].question"))
                )
            }

            if (field.type == BaseType.MultipleChoices || field.type == BaseType.DropDownFrage) {
                val f = UIFieldset(UILength(md = 6, lg = 4), title = field.type.toString())
                    .add(UIInput("inputFields[${index}].question", label = "Die Frage"))
                field.antworten?.forEachIndexed { i, _ ->
                    f.add(UIInput("inputFields[${index}].antworten[${i}]", label = "AntwortMöglichkeit ${i + 1}"))
                }
                f.add(
                    UIButton.createAddButton(
                        responseAction = ResponseAction(
                            "${Rest.URL}/poll/addAntwort/${field.uid}", targetType = TargetType.POST
                        )
                    )
                )
                if (field.type == BaseType.MultipleChoices) {
                    f.add(
                        UIInput(
                            "inputFields[${index}].numberOfSelect", dataType = UIDataType.INT, label = "Wie viele Sollen " +
                                    "angeklickt werden können "
                        )
                    )
                }
                feld.add(f)
            }
            if (field.type == BaseType.DatumsAbfrage) {
                feld.add(
                    UIFieldset(UILength(md = 6, lg = 4), title = field.type.toString()).add(
                        UIInput(
                            "inputFields[${index}].question",
                            label = "Hast du am ... Zeit?"
                        )
                    )

                )
            }
            layout.add(feld)
        }
    }


    @PostMapping("Export")
    fun export(request: HttpServletRequest,poll: Poll) : ResponseEntity<Resource>? {
        val ihkExporter = ExcelExport()
        val bytes: ByteArray? = ihkExporter
            .getExcel(poll)
        val filename = ("test.xlsx")

        if (bytes == null || bytes.size == 0) {
            log.error("Oups, xlsx has zero size. Filename: $filename")
            return null;
        }
        return RestUtils.downloadFile(filename, bytes)
    }

    // create a update layout funktion, welche die layout nummer updatet und zurück gibt es soll für jeden Frage Basistyp eine eigene funktion haben


    /*dto.inputFields?.forEachIndexed { field, index ->
            if (field.type == msc) {
             layout.add() //
             "type[$index]"
              Id: name
            }
        }
        layout.add(UIRow().add(UIFieldset(UILength(md = 6, lg = 4))
            .add(lc, "name")))
    */
}
