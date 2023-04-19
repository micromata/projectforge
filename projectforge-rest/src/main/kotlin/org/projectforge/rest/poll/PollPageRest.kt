package org.projectforge.rest.poll

import com.fasterxml.jackson.databind.ObjectMapper
import org.projectforge.business.poll.PollDO
import org.projectforge.business.poll.PollDao
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.*
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.poll.Exel.ExcelExport
import org.projectforge.rest.poll.types.BaseType
import org.projectforge.rest.poll.types.PREMADE_QUESTIONS
import org.projectforge.rest.poll.types.Question
import org.projectforge.ui.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/poll")
class PollPageRest : AbstractDTOPagesRest<PollDO, Poll, PollDao>(PollDao::class.java, "poll.title") {

    private val log: Logger = LoggerFactory.getLogger(PollPageRest::class.java)

    @Autowired
    private lateinit var pollDao: PollDao
    @Autowired
    private lateinit var pollMailService: PollMailService

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
            var a = ObjectMapper().readValue(pollDO.inputFields, MutableList::class.java)
            poll.inputFields = a.map { Question().toObject(ObjectMapper().writeValueAsString(it)) }.toMutableList()
        }
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
            .add(lc, "title", "description", "location", "owner", "deadline", "state")
    }


    override fun createEditLayout(dto: Poll, userAccess: UILayout.UserAccess): UILayout {
        val lc = LayoutContext(PollDO::class.java)
        val poll = PollDO()
        dto.copyTo(poll)
        val layout = super.createEditLayout(dto, userAccess)
        layout.add(
            UIRow().add(
                UIFieldset(UILength(md = 6, lg = 4)).add(lc, "title", "description", "location", "owner", "deadline")
            )
        )
        layout.add(
            UIRow().add(
                UIFieldset(UILength(md = 6, lg = 4)).add(
                    UIButton.createDefaultButton(
                        id = "add-question-button",
                        responseAction = ResponseAction("${Rest.URL}/poll/add", targetType = TargetType.POST),
                        title = "Eigene Frage hinzufügen"
                    )
                ).add(
                    UISelect("questionType", values = BaseType.values().map { UISelectValue(it, it.name) })
                )
            )
        )

        layout.add(
            UIRow().add(
                UIFieldset(UILength(md = 6, lg = 4)).add(
                    UIButton.createDefaultButton(
                        id = "micromata-vorlage-button",
                        responseAction = ResponseAction("${Rest.URL}/poll/addPremadeQuestions", targetType = TargetType.POST),
                        title = "Micromata Vorlage nutzen"
                    )
                )
            )
        )

        addQuestionFieldset(layout, dto)

        layout.watchFields.addAll(
            arrayOf(
                "title", "description", "location", "deadline"
            )
        )

        return LayoutUtils.processEditPage(layout, dto, this)
    }


    //TODO refactor this whole file into multiple smaller files

    override fun onWatchFieldsUpdate(
        request: HttpServletRequest, dto: Poll, watchFieldsTriggered: Array<String>?
    ): ResponseEntity<ResponseAction> {
        val title = dto.title
        val description = dto.description
        val location = dto.location
        val deadline = dto.deadline

        val userAccess = UILayout.UserAccess()
        val poll = PollDO()
        dto.copyTo(poll)
        checkUserAccess(poll, userAccess)
        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE).addVariable("data", dto).addVariable("ui", createEditLayout(dto, userAccess))
        )
    }


    override fun onAfterSaveOrUpdate(request: HttpServletRequest, poll: PollDO, postData: PostData<Poll>) {
        super.onAfterSaveOrUpdate(request, poll, postData)
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
            val row = UIRow()
            if (field.type == BaseType.YesNoQuestion) {
                val groupLayout = UIGroup()
                field.answers?.forEach { answer ->
                    groupLayout.add(
                        UIRadioButton(
                            "YesNoQuestion[${index}].question", answer, label = answer
                        )
                    )
                }
                row.add(
                    UIFieldset(UILength(md = 6, lg = 4), title = field.type.toString()).add(UIInput("inputFields[${index}].question")).add
                        (groupLayout)
                )
            }

            if (field.type == BaseType.TextQuestion) {
                row.add(
                    UIFieldset(UILength(md = 6, lg = 4), title = field.type.toString()).add(UIInput("inputFields[${index}].question"))
                )
            }

            if (field.type == BaseType.MultipleChoices || field.type == BaseType.DropDownQuestion) {
                val f = UIFieldset(UILength(md = 6, lg = 4), title = field.type.toString())
                    .add(UIInput("inputFields[${index}].question", label = "Die Frage"))
                field.answers?.forEachIndexed { i, _ ->
                    f.add(UIInput("inputFields[${index}].answers[${i}]", label = "Antwortmöglichkeit ${i + 1}"))
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
                            "inputFields[${index}].numberOfSelect", dataType = UIDataType.INT, label = "Wie viele sollen " +
                                    "angeklickt werden können?"
                        )
                    )
                }
                row.add(f)
            }

            if (field.type == BaseType.DateQuestion) {
                row.add(
                    UIFieldset(UILength(md = 6, lg = 4), title = field.type.toString()).add(
                        UIInput(
                            "inputFields[${index}].question",
                            label = "Hast du am ... Zeit?"
                        )
                    )

                )
            }

            layout.add(row)
        }
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

    // create a update layout funktion, welche das layout nummr updatet und zurück gibt es soll für jeden Frage Basistyp eine eigene funktion haben


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
