package org.projectforge.rest.poll

import com.fasterxml.jackson.databind.ObjectMapper
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.poll.PollDO
import org.projectforge.business.poll.PollDao
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.*
import org.projectforge.rest.dto.*
import org.projectforge.rest.poll.Exel.ExcelExport
import org.projectforge.rest.poll.types.BaseType
import org.projectforge.rest.poll.types.PREMADE_QUESTIONS
import org.projectforge.rest.poll.types.Question
import org.projectforge.ui.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/poll")
class PollPageRest : AbstractDTOPagesRest<PollDO, Poll, PollDao>(PollDao::class.java, "poll.title") {

    private val log: Logger = LoggerFactory.getLogger(PollPageRest::class.java)

    @Autowired
    private lateinit var userService: UserService;

    @Autowired
    private lateinit var groupService: GroupService;

    @Autowired
    private lateinit var pollMailService: PollMailService

   /* @GetMapping("/edit/{id}")
    fun getForm(@RequestBody postData: PostData<Poll>,request: HttpServletRequest, @RequestParam("id") pollStringId: String?): ResponseEntity<ResponseAction> {
        val dto = postData.data
        val userAccess = UILayout.UserAccess(insert = true, update = true)

        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE).addVariable("data", dto).addVariable("ui", FormLayoutData(pollResponse, layout, createServerData(request)))
        )
    }

    */

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
    override fun transformFromDB(pollDO: PollDO, editMode: Boolean): Poll {
        val poll = Poll()
        poll.copyFrom(pollDO)
        if (pollDO.inputFields != null) {
            val fields = ObjectMapper().readValue(pollDO.inputFields, MutableList::class.java)
            poll.inputFields = fields.map { Question().toObject(ObjectMapper().writeValueAsString(it)) }.toMutableList()
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

    }


    override fun createEditLayout(dto: Poll, userAccess: UILayout.UserAccess): UILayout {
        val lc = LayoutContext(PollDO::class.java)
        val poll = PollDO()
        dto.copyTo(poll)
        val layout = super.createEditLayout(dto, userAccess)

        val fieldset = UIFieldset(UILength(12))
        fieldset
            .add(UIButton.createDefaultButton(
                id = "response-poll-button",
                responseAction = ResponseAction(PagesResolver.getDynamicPageUrl(ResponsePageRest::class.java, absolute = true) + "${dto.id}", targetType = TargetType.REDIRECT),
                title = "poll.response.poll"
            ))
            .add(lc, "title", "description", "location")
            .add(lc, "owner")
            .add(lc, "deadline", "date")
            .add(UISelect.createUserSelect(lc, "fullAccessUsers", true, "poll.fullAccessUsers"))
            .add(UISelect.createGroupSelect(lc, "fullAccessGroups", true, "poll.fullAccessGroups"))
            .add(UISelect.createUserSelect(lc, "attendees", true, "poll.attendees"))
            .add(UISelect.createGroupSelect(lc, "groupAttendees", true, "poll.groupAttendees"))
            .add(
                UIRow()
                    .add(
                        UICol(UILength(xs = 9, sm = 9, md = 9, lg = 9))
                            .add(UISelect("questionType", values = BaseType.values().map { UISelectValue(it, it.name) }, label = "questionType"))
                    )
                    .add(
                        UICol(UILength(xs = 3, sm = 3, md = 3, lg = 3))
                            .add(
                                UIButton.createDefaultButton(
                                    id = "add-question-button",
                                    responseAction = ResponseAction("${Rest.URL}/poll/add", targetType = TargetType.POST),
                                    title = "Eigene Frage hinzufügen"
                                )
                            )
                    )
            )
            .add(
                UIRow()
                    .add(
                        UICol(UILength(xs = 9, sm = 9, md = 9, lg = 9))
                    )
                    .add(
                        UICol(UILength(xs = 3, sm = 3, md = 3, lg = 3))
                            .add(
                                UIButton.createDefaultButton(
                                    id = "micromata-vorlage-button",
                                    responseAction = ResponseAction("${Rest.URL}/poll/addPremadeQuestions", targetType = TargetType.POST),
                                    title = "Micromata Vorlage nutzen"
                                )
                            )
                    )
            )

        layout.add(fieldset)

        addQuestionFieldset(layout, dto)

        layout.watchFields.addAll(
            arrayOf(
                "title", "description", "location", "deadline",
                "date"
            )
        )

        return LayoutUtils.processEditPage(layout, dto, this)
    }



    override fun onAfterSaveOrUpdate(request: HttpServletRequest, poll: PollDO, postData: PostData<Poll>) {
        super.onAfterSaveOrUpdate(request, poll, postData)
        val dto = postData.data
        pollMailService.sendMail(subject = "", content = "", to = "test.mail")
    }


    @PostMapping("/addAnswer/{fieldId}")
    fun addAnswerForMultipleChoice(
        @RequestBody postData: PostData<Poll>,
        @PathVariable("fieldId") fieldUid: String,
    ): ResponseEntity<ResponseAction> {
        val dto = postData.data
        val userAccess = UILayout.UserAccess(insert = true, update = true)

        val found = dto.inputFields?.find { it.uid == fieldUid }
        found?.answers?.add("")

        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE).addVariable("data", dto).addVariable("ui", createEditLayout(dto, userAccess))
        )
    }



    // PostMapping add
    @PostMapping("/add")
    fun addQuestionField(
        @RequestBody postData: PostData<Poll>
    ): ResponseEntity<ResponseAction> {
        val userAccess = UILayout.UserAccess(insert = true, update = true)
        val dto = postData.data
        val poll = PollDO()

        var type = BaseType.valueOf(dto.questionType ?: "TextQuestion")
        var question = Question(uid = UUID.randomUUID().toString(), type = type)
        if(type == BaseType.YesNoQuestion) {
            question.answers = mutableListOf("ja", "nein")
        }
        if(type == BaseType.DateQuestion) {
            question.answers = mutableListOf("Ja", "Vielleicht", "Nein")
        }

        dto.inputFields!!.add(question)

        dto.copyTo(poll)
        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE).addVariable("data", dto).addVariable("ui", createEditLayout(dto, userAccess))
        )
    }


    @PostMapping("/addPremadeQuestions")
    private fun addPremadeQuestionsField(
        @RequestBody postData: PostData<Poll>,
    ): ResponseEntity<ResponseAction> {
        val userAccess = UILayout.UserAccess(insert = true, update = true)
        val dto = postData.data
        val poll = PollDO()

        PREMADE_QUESTIONS.entries.forEach { entry ->
            dto.inputFields?.add(entry.value)
        }

        dto.copyTo(poll)
        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE).addVariable("data", dto).addVariable("ui", createEditLayout(dto, userAccess))
        )
    }


    private fun addQuestionFieldset(layout: UILayout, dto: Poll) {
        dto.inputFields?.forEachIndexed { index, field ->
            val fieldset = UIFieldset(UILength(12), title = field.type.toString())
                .add(generateDeleteButton(layout, field.uid))
                .add(UIInput("inputFields[${index}].question", label = "Frage"))
            if (field.type == BaseType.YesNoQuestion) {
                val buttons = UIGroup()
                field.answers?.forEach { answer ->
                    buttons.add(
                        UIRadioButton(
                            "YesNoQuestion[${index}].question", answer, label = answer
                        )
                    )
                }
                fieldset
                    .add(buttons)
            }

            if (field.type == BaseType.MultipleChoices) {
                val groupLayout = UIGroup()
                field.answers?.forEachIndexed { i, _ ->
                    groupLayout.add(UIInput("inputFields[${index}].answers[${i}]", label = "Antwortmöglichkeit ${i + 1}"))
                }
                groupLayout.add(
                    UIButton.createAddButton(
                        responseAction = ResponseAction(
                            "${Rest.URL}/poll/addAnswer/${field.uid}", targetType = TargetType.POST
                        )
                    )
                )
                if (field.type == BaseType.MultipleChoices) {
                    groupLayout.add(
                        UIInput(
                            "inputFields[${index}].numberOfSelect", dataType = UIDataType.INT, label = "Wie viele sollen " +
                                    "angeklickt werden können?"
                        )
                    )
                }
                fieldset.add(groupLayout)
            }

            if (field.type == BaseType.DateQuestion) {
                fieldset
                    .add(
                        UIInput(
                            "inputFields[${index}].question",
                            label = "Hast du am ... Zeit?"
                        )
                    )
            }

            layout.add(fieldset)
        }
    }

    private fun generateDeleteButton(layout: UILayout, uid:String?):UIRow {
        val row = UIRow()
        row.add(
            UICol(UILength(11))
        )
            .add(
                UICol(length = UILength(1))
                    .add(
                        UIButton.createDangerButton(
                            id = "X",
                            responseAction = ResponseAction(
                                "${Rest.URL}/poll/deleteQuestion/${uid}", targetType = TargetType.POST
                            )
                        ).withConfirmMessage(layout, confirmMessage = "Willst du wirklich diese Frage löschen?"))
            )
        return row
    }

    @PostMapping("/deleteQuestion/{uid}")
    fun removeQuestion(
        @RequestBody postData: PostData<Poll>,
        @PathVariable("uid") uid: String,
    ): ResponseEntity<ResponseAction> {
        val dto = postData.data
        val userAccess = UILayout.UserAccess(insert = true, update = true)
        val poll = PollDO()

        val matchingQuestion: Question? = dto.inputFields?.find { it.uid.equals(uid) }
        dto.inputFields?.remove(matchingQuestion)

        dto.copyTo(poll)
        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE).addVariable("data", dto).addVariable("ui", createEditLayout(dto, userAccess))
        )
    }


    @PostMapping("Export")
    fun export(request: HttpServletRequest,poll: Poll) : ResponseEntity<Resource>? {
        val ihkExporter = ExcelExport()
        val bytes: ByteArray? = ihkExporter
            .getExcel(poll)
        val filename = ("test.xlsx")

        if (bytes == null || bytes.size == 0) {
            log.error("Oops, xlsx has zero size. Filename: $filename")
            return null;
        }
        return RestUtils.downloadFile(filename, bytes)
    }

}
