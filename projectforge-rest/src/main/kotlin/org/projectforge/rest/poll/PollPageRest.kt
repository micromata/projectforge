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

package org.projectforge.rest.poll

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.poll.PollDO
import org.projectforge.business.poll.PollDao
import org.projectforge.business.poll.PollResponseDao
import org.projectforge.business.poll.filter.PollAssignment
import org.projectforge.business.poll.filter.PollAssignmentFilter
import org.projectforge.business.poll.filter.PollState
import org.projectforge.business.poll.filter.PollStateFilter
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.mail.MailAttachment
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.User
import org.projectforge.rest.poll.excel.ExcelExport
import org.projectforge.rest.poll.types.*
import org.projectforge.ui.*
import org.projectforge.ui.filter.UIFilterListElement
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*


@RestController
@RequestMapping("${Rest.URL}/poll")
class PollPageRest : AbstractDTOPagesRest<PollDO, Poll, PollDao>(PollDao::class.java, "poll.title") {

    private val log: Logger = LoggerFactory.getLogger(PollPageRest::class.java)

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var pollMailService: PollMailService

    @Autowired
    private lateinit var pollDao: PollDao

    @Autowired
    private lateinit var excelExport: ExcelExport

    @Autowired
    private lateinit var pollResponseDao: PollResponseDao

    override fun newBaseDTO(request: HttpServletRequest?): Poll {
        val result = Poll()
        result.owner = ThreadLocalUserContext.loggedInUser
        return result
    }

    override fun onBeforeMarkAsDeleted(request: HttpServletRequest, obj: PollDO, postData: PostData<Poll>) {
        val responsesToDelete = pollResponseDao.selectAll(checkAccess = false).filter {
            it.poll?.id == obj.id
        }
        responsesToDelete.forEach {
            pollResponseDao.markAsDeleted(it)
        }
        super.onBeforeMarkAsDeleted(request, obj, postData)
    }


    override fun transformForDB(dto: Poll): PollDO {
        val pollDO = PollDO()
        dto.copyTo(pollDO)
        if (dto.inputFields != null) {
            pollDO.inputFields = ObjectMapper().writeValueAsString(dto.inputFields)
        }
        return pollDO
    }


    // override fun transformForDB editMode not used
    override fun transformFromDB(obj: PollDO, editMode: Boolean): Poll {
        val poll = Poll()
        poll.copyFrom(obj)
        User.restoreDisplayNames(poll.fullAccessUsers)
        Group.restoreDisplayNames(poll.fullAccessGroups)
        User.restoreDisplayNames(poll.attendees)
        Group.restoreDisplayNames(poll.groupAttendees)
        return poll
    }

    /**
     * @return the response page.
     */
    override fun getStandardEditPage(): String {
        return "${PagesResolver.getDynamicPageUrl(PollResponsePageRest::class.java)}?pollId=:id"
    }

    override fun createListLayout(
        request: HttpServletRequest, layout: UILayout, magicFilter: MagicFilter, userAccess: UILayout.UserAccess
    ) {
        val pollLC = LayoutContext(lc)
        layout.add(
            UITable.createUIResultSetTable()
                .add(pollLC, "title", "description", "location", "owner", "deadline", "state")
        )
    }

    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        elements.add(
            UIFilterListElement("assignment", label = translate("poll.assignment"), defaultFilter = true)
                .buildValues(PollAssignment.OWNER, PollAssignment.ACCESS, PollAssignment.ATTENDEE, PollAssignment.OTHER)
        )
        elements.add(
            UIFilterListElement("status", label = translate("poll.state"), defaultFilter = true)
                .buildValues(PollState.RUNNING, PollState.FINISHED)
        )
    }

    override fun preProcessMagicFilter(target: QueryFilter, source: MagicFilter): List<CustomResultFilter<PollDO>> {
        val filters = mutableListOf<CustomResultFilter<PollDO>>()
        val assignmentFilterEntry = source.entries.find { it.field == "assignment" }
        if (assignmentFilterEntry != null) {
            assignmentFilterEntry.synthetic = true
            val values = assignmentFilterEntry.value.values
            if (!values.isNullOrEmpty()) {
                val enums = values.map { PollAssignment.valueOf(it) }
                filters.add(PollAssignmentFilter(enums))
            }
        }
        val statusFilterEntry = source.entries.find { it.field == "status" }
        if (statusFilterEntry != null) {
            statusFilterEntry.synthetic = true
            val values = statusFilterEntry.value.values
            if (!values.isNullOrEmpty()) {
                val enums = values.map { PollState.valueOf(it) }
                filters.add(PollStateFilter(enums))
            }
        }
        return filters
    }


    override fun createEditLayout(dto: Poll, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
        val fieldset = UIFieldset(UILength(12))
        layout.add(fieldset)
        if (/*dto.isFinished()*/ dto.isAlreadyCreated()) {
            layout.add(
                MenuItem(
                    "export-poll-response-button",
                    i18nKey = "poll.export.response.poll",
                    url = RestResolver.getRestUrl(
                        restClass = PollPageRest::class.java,
                        subPath = "/export",
                        params = mapOf("id" to dto.id)
                    ),
                    type = MenuItemTargetType.DOWNLOAD
                )
            )
        }

        layout.add(
            MenuItem(
                "poll-guide",
                i18nKey = "poll.guide",
                url = PagesResolver.getDynamicPageUrl(PollInfoPageRest::class.java, absolute = false),
                type = MenuItemTargetType.MODAL
            )
        )


        addDefaultParameterFields(dto, fieldset, isRunning = dto.state == PollDO.State.RUNNING)

        UILength(xs = 12, sm = 12, md = 12, lg = 12)
        val colWidth = UILength(xs = 12, sm = 12, md = 6, lg = 6)

        fieldset
            .add(
                UIRow()
                    .add(
                        UICol(colWidth)
                            .add(
                                UISelect.createUserSelect(
                                    lc,
                                    "fullAccessUsers",
                                    true,
                                    "poll.fullAccessUsers",
                                    tooltip = "poll.fullAccessUser.tooltip"
                                )
                            )
                    )
                    .add(
                        UICol(colWidth)
                            .add(
                                UISelect.createGroupSelect(
                                    lc,
                                    "fullAccessGroups",
                                    true,
                                    "poll.fullAccessGroups",
                                    tooltip = "poll.fullAccessgroups.tooltip"
                                )
                            )
                    )
            )
            .add(
                UIRow()
                    .add(
                        UICol(colWidth)
                            .add(
                                UISelect.createUserSelect(
                                    lc,
                                    "attendees",
                                    true,
                                    "poll.attendees",
                                    tooltip = "poll.attendees.tooltip"
                                )
                            )
                    )
                    .add(
                        UICol(colWidth)
                            .add(
                                UISelect.createGroupSelect(
                                    lc,
                                    "groupAttendees",
                                    true,
                                    "poll.groupAttendees",
                                    tooltip = "poll.groupAttendees.tooltip"
                                )
                            )
                    )
            )

        if (!dto.isAlreadyCreated()) {

            fieldset
                .add(
                    UIRow()
                        .add(
                            UICol(colWidth)
                                .add(
                                    UISelect(
                                        "prequestionType",
                                        values = PreType.values()
                                            .map { UISelectValue(it, translateMsg("poll.templateType" + it.name)) },
                                        label = "poll.questionTemplate",
                                        tooltip = "poll.premadeQuestion.tooltip"
                                    )
                                )
                        )
                        .add(
                            UICol(colWidth)
                                .add(
                                    UISelect(
                                        "questionType",
                                        values = BaseType.values()
                                            .map { UISelectValue(it, translateMsg("poll.questionType." + it.name)) },
                                        label = "poll.questionType",
                                        tooltip = "poll.questionType.tooltip"
                                    )
                                )
                        )
                )
                .add(
                    UIRow()
                        .add(
                            UICol(colWidth)
                                .add(
                                    UIButton.createDefaultButton(
                                        id = "template-button",
                                        title = "poll.premade.template",
                                        responseAction = ResponseAction(
                                            "${Rest.URL}/poll/addPremadeQuestions",
                                            targetType = TargetType.PUT
                                        ),
                                        default = false
                                    )
                                )
                        )
                        .add(
                            UICol(colWidth)
                                .add(
                                    UIButton.createDefaultButton(
                                        id = "add-question-button",
                                        title = "poll.button.addQuestion",
                                        responseAction = ResponseAction(
                                            "${Rest.URL}/poll/add",
                                            targetType = TargetType.PUT
                                        ),
                                        default = false
                                    )
                                )
                        )
                )
                .add(
                    UIRow()
                        .add(
                            UICol(colWidth)
                                .add(
                                    UIInput(
                                        "customemailsubject",
                                        required = false,
                                        label = "poll.email-subject-field",
                                        tooltip = "poll.email-subject-tooltip"
                                    )
                                )
                        )
                        .add(
                            UICol(colWidth)
                                .add(
                                    UISpacer()
                                )
                        )
                )
                .add(
                    UIRow()
                        .add(
                            UICol(colWidth)
                                .add(
                                    UITextArea(
                                        "customemailcontent",
                                        label = "poll.email-content-field",
                                        tooltip = "poll.email-content-tooltip",
                                        rows = 12,
                                        maxRows = 60
                                    )
                                )
                        )
                        .add(
                            UICol(colWidth)
                                .add(
                                    UISpacer()
                                )
                        )
                )

        }


        val content = "Liebe Teilnehmer:innen\n" +
                "Wir möchten Ihnen mitteilen, dass eine Umfrage erstellt wurde mit dem Titel \"{0}\", und Sie wurden herzlichst eingeladen bei dieser Abzustimmen.\n" +
                "\n" +
                "Die Umfrage zu welcher sie Eingeladen wurden, endet am {4}. Eine Kurze Beschreibung des Themas finden sie hier: \"{3}\"\n" +
                "{2}\n" +
                "Mit Freundlichen Grüßen\n" +
                "{1}"
        if (dto.customemailcontent.isNullOrEmpty()) {
            dto.customemailcontent = content
        }

        val subject = "Sie wurden zu einer Umfrage mit dem Titel \"{0}\" eingeladen."

        if (dto.customemailsubject.isNullOrEmpty()) {
            dto.customemailsubject = subject
        }

        addQuestionFieldset(layout, dto, fieldset)

        layout.watchFields.add("delegationUser")
        layout.watchFields.addAll(listOf("groupAttendees"))
        layout.watchFields.addAll(listOf("fullAccessGroups"))


        val processedLayout = LayoutUtils.processEditPage(layout, dto, this)
        if (!dto.isFinished()) {
            processedLayout.actions.filterIsInstance<UIButton>().find {
                it.id == "create"
            }?.confirmMessage = translateMsg("poll.confirmation.creation")


            if (dto.isAlreadyCreated()) {
                processedLayout.addAction(
                    UIButton.createDangerButton(
                        id = "poll-button-finish",
                        title = "poll.button.finish",
                        confirmMessage = translateMsg("poll.confirmation.finish"),
                        responseAction = ResponseAction(
                            "${Rest.URL}/poll/finish",
                            targetType = TargetType.PUT
                        ),
                        layout = layout,
                    )
                )
            }
        }

        return processedLayout
    }


    @GetMapping("/export")
    fun export(@RequestParam("id") id: String): ResponseEntity<Resource>? {
        val poll = Poll()
        val pollDo = pollDao.find(id.toLong())!!
        poll.copyFrom(pollDo)
        User.restoreDisplayNames(poll.attendees)
        val bytes: ByteArray? = excelExport
            .getExcel(poll)
        val filename = ("${poll.title}_${LocalDateTime.now().year}_Result.xlsx")

        if (bytes == null || bytes.isEmpty()) {
            log.error("Oops, xlsx is empty. Filename: $filename")
            return null
        }
        log.info("Exporting $filename")
        return RestUtils.downloadFile(filename, bytes)
    }

    @PutMapping("/finish")
    fun changeStateToFinish(
        request: HttpServletRequest,
        @RequestBody postData: PostData<Poll>,
    ): ResponseEntity<ResponseAction> {
        postData.data.state = PollDO.State.FINISHED
        postData.data.deadline = LocalDate.now()
        val responseEntity = super.saveOrUpdate(request, postData)
        val fuemailSent = false

        if (postData.data.state == PollDO.State.FINISHED) {


            val poll = Poll()
            val id: String
            id = postData.data.id.toString()
            val pollDo = pollDao.find(id.toInt())!!
            poll.copyFrom(pollDo)
            User.restoreDisplayNames(poll.attendees)

            val excel = excelExport.getExcel(poll)
            val filename = ("${postData.data.title}_${LocalDateTime.now().year}_Result.xlsx")

            val mailAttachment = object : MailAttachment {
                override fun getFilename(): String {
                    return filename
                }

                override fun getContent(): ByteArray? {
                    return excel
                }
            }

            if (fuemailSent != true) {

                val owner = userService.getUser(postData.data.owner?.id)
                val mailFrom = owner?.email.toString()
                val mailTo = pollMailService.getAllFullAccessEmails(postData.data)
                val mailSubject = translateMsg("poll.mail.ended.fullAccess.subject", postData.data.title)
                val mailContent =
                    translateMsg("poll.mail.ended.fullAccess.content", postData.data.title, owner?.displayName)

                pollMailService.sendMail(mailFrom, mailTo, mailSubject, mailContent, listOf(mailAttachment))
            }

            val owner = userService.getUser(postData.data.owner?.id)
            val mailFrom = owner?.email.toString()
            val mailTo = pollMailService.getAllAttendeesEmails(postData.data)
            val mailSubject = translateMsg("poll.mail.ended.subject", postData.data.title)
            val mailContent = translateMsg("poll.mail.ended.content", postData.data.title, owner?.displayName)
            pollMailService.sendMail(mailFrom, mailTo, mailSubject, mailContent)
        }
        return responseEntity
    }

    override fun onBeforeSaveOrUpdate(request: HttpServletRequest, obj: PollDO, postData: PostData<Poll>) {
        if (obj.inputFields.isNullOrEmpty() || obj.inputFields.equals("[]")) {
            throw AccessException("poll.error.oneQuestionRequired")
        }

        if (obj.attendeeIds.isNullOrEmpty() || obj.attendeeIds.equals("[]")) {
            throw AccessException("poll.error.oneAttendeRequired")
        }

        postData.data.customemailcontent = postData.data.customemailcontent?.replace("\n", "<br>")

        super.onBeforeSaveOrUpdate(request, obj, postData)

    }


    override fun onAfterSaveOrUpdate(request: HttpServletRequest, obj: PollDO, postData: PostData<Poll>) {

        if (postData.data.state != PollDO.State.FINISHED) {

            val owner = userService.getUser(obj.owner?.id)
            val mailFrom = owner?.email.toString()
            val mailSubject: String
            val mailContent: String

            if (postData.data.isAlreadyCreated()) {

                val mailTo = pollMailService.getAllFullAccessEmails(postData.data)

                mailSubject = translateMsg("poll.mail.update.subject", obj.description, obj.deadline)
                mailContent = translateMsg(
                    "poll.mail.update.content", obj.title, owner?.displayName
                )

                pollMailService.sendMail(mailFrom, mailTo, mailSubject, mailContent)


            } else {
                val dto = postData.data

                val subject = dto.customemailsubject
                val content = dto.customemailcontent

                if (subject.isNullOrEmpty() && content.isNullOrEmpty()) {
                    val mailTo = pollMailService.getAllMails(postData.data)

                    mailSubject = translateMsg("poll.mail.created.subject", obj.title, obj.description, obj.deadline)
                    mailContent = translateMsg(
                        "poll.mail.created.content",
                        obj.title,
                        owner?.displayName,
                        obj.id.toString(),
                    )
                    pollMailService.sendMail(mailFrom, mailTo, mailSubject, mailContent)

                } else if (subject.isNullOrEmpty() && !content.isNullOrEmpty()) {
                    val mailTo = pollMailService.getAllMails(postData.data)

                    mailSubject = translateMsg("poll.mail.created.subject", obj.title, obj.deadline)
                    mailContent = translateMsg(
                        content,
                        obj.title,
                        owner?.displayName,
                        "<p><a href=\"https://projectforge.micromata.de/react/pollResponse/dynamic/?pollId=${obj.id}\">Link</a></p>",
                        obj.description,
                        obj.deadline
                    )
                    pollMailService.sendMail(mailFrom, mailTo, mailSubject, mailContent)
                } else if (!subject.isNullOrEmpty() && content.isNullOrEmpty()) {
                    val mailTo = pollMailService.getAllMails(postData.data)

                    mailSubject = translateMsg("$subject", obj.title, obj.deadline)
                    mailContent = translateMsg(
                        "poll.mail.created.content",
                        obj.title,
                        owner?.displayName,
                        "https://projectforge.micromata.de/react/pollResponse/dynamic/?pollId=${obj.id}",
                        obj.description,
                        obj.deadline
                    )
                    pollMailService.sendMail(mailFrom, mailTo, mailSubject, mailContent)

                } else if (!subject.isNullOrEmpty() && !content.isNullOrEmpty()) {
                    val mailTo = pollMailService.getAllMails(postData.data)

                    mailSubject = translateMsg("$subject", obj.title, obj.deadline)
                    mailContent = translateMsg(
                        content,
                        obj.title,
                        owner?.displayName,
                        "<p>Hier kommst du direkt <a href=\"https://projectforge.micromata.de/react/pollResponse/dynamic/?pollId=${obj.id}\"> zur Umfrage</a></p>",
                        obj.description,
                        obj.deadline
                    )
                    pollMailService.sendMail(mailFrom, mailTo, mailSubject, mailContent)

                }


            }
        }
        super.onAfterSaveOrUpdate(request, obj, postData)
    }


    @PostMapping("/addAnswer/{fieldId}")
    fun addAnswerForMultipleChoice(
        @RequestBody postData: PostData<Poll>,
        @PathVariable("fieldId") fieldUid: String,
    ): ResponseEntity<ResponseAction> {
        val dto = postData.data

        val found = dto.inputFields?.find { it.uid == fieldUid }
        found?.answers?.add("")
        dto.owner = userService.getUser(dto.owner?.id)
        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE).addVariable("data", dto).addVariable(
                "ui",
                createEditLayout(dto, getUserAccess(dto))
            )
        )
    }

    // PostMapping add
    @PutMapping("/add")
    fun addQuestionField(
        @RequestBody postData: PostData<Poll>
    ): ResponseEntity<ResponseAction> {
        val dto = postData.data

        val type = dto.questionType?.let { BaseType.valueOf(it) } ?: BaseType.PollTextQuestion
        val question = Question(uid = UUID.randomUUID().toString(), type = type)
        if (type == BaseType.PollSingleResponseQuestion) {
            question.answers = mutableListOf("", "")
        } else if (type == BaseType.PollMultiResponseQuestion) {
            question.answers = mutableListOf("", "", "")
        }

        dto.inputFields?.add(question)
        dto.owner = userService.getUser(dto.owner?.id)
        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE).addVariable("data", dto)
                .addVariable("ui", createEditLayout(dto, getUserAccess(dto)))
        )
    }

    override fun onWatchFieldsUpdate(
        request: HttpServletRequest,
        dto: Poll,
        watchFieldsTriggered: Array<String>?
    ): ResponseEntity<ResponseAction> {
        if (watchFieldsTriggered?.get(0) == "groupAttendees") {
            val groupIds = dto.groupAttendees?.filter { it.id != null }?.map { it.id!! }?.toLongArray()
            val userIds = UserService().getUserIds(groupService.getGroupUsers(groupIds))
            val users = User.toUserList(userIds)
            User.restoreDisplayNames(users)
            val allUsers = dto.attendees?.toMutableList() ?: mutableListOf()

            users?.forEach { user ->
                if (allUsers.none { it.id == user.id }) {
                    allUsers.add(user)
                }
            }

            dto.groupAttendees = mutableListOf()
            dto.attendees = allUsers.sortedBy { it.displayName }
        }
        if (watchFieldsTriggered?.get(0) == "fullAccessGroups") {
            val groupIds = dto.fullAccessGroups?.filter { it.id != null }?.map { it.id!! }?.toLongArray()
            val userIds = UserService().getUserIds(groupService.getGroupUsers(groupIds))
            val users = User.toUserList(userIds)
            User.restoreDisplayNames(users)
            val allUsers = dto.fullAccessUsers?.toMutableList() ?: mutableListOf()

            users?.forEach { user ->
                if (allUsers.none { it.id == user.id }) {
                    allUsers.add(user)
                }
            }

            dto.fullAccessGroups = mutableListOf()
            dto.fullAccessUsers = allUsers.sortedBy { it.displayName }
        }
        dto.owner = userService.getUser(dto.owner?.id)

        // I don't know why this is necessary
        if (watchFieldsTriggered?.get(0) == "delegationUser") {
            dto.delegationUser = dto.delegationUser
        }
        dto.owner = userService.getUser(dto.owner?.id)
        return ResponseEntity.ok(
            ResponseAction(
                targetType = TargetType.UPDATE
            )
                .addVariable("ui", createEditLayout(dto, getUserAccess(dto)))
                .addVariable("data", dto)
        )
    }


    @PutMapping("/addPremadeQuestions")
    private fun addPremadeQuestionsField(
        @RequestBody postData: PostData<Poll>
    ): ResponseEntity<ResponseAction> {
        val dto = postData.data

        val ptype = dto.prequestionType?.let { PreType.valueOf(it) } ?: PreType.Neujahrsfeier
        PreQuestion(uid = UUID.randomUUID().toString(), pType = ptype)
        if (ptype == PreType.Sommerfest) {
            Sommerfest.entries.forEach { entry ->
                dto.inputFields?.add(entry.value)
            }
        } else if (ptype == PreType.Neujahrsfeier) {
            Neujahrsfeier.entries.forEach { entry ->
                dto.inputFields?.add(entry.value)
            }
        } else if (ptype == PreType.Teamessen) {
            Teamessen.entries.forEach { entry ->
                dto.inputFields?.add(entry.value)
            }
        }

        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE).addVariable("data", dto)
                .addVariable("ui", createEditLayout(dto, getUserAccess(dto)))
        )
    }

    private fun addQuestionFieldset(layout: UILayout, dto: Poll, fieldset: UIFieldset) {
        fieldset.add(UISpacer())
        dto.inputFields?.forEachIndexed { index, field ->
            val questionFieldset = UIFieldset(UILength(12), title = field.type.toString())
            if (!dto.isAlreadyCreated()) {
                questionFieldset.add(generateDeleteButton(layout, field.uid))
            }
            questionFieldset.add(
                getUiElement(
                    dto.isAlreadyCreated(),
                    "inputFields[${index}].question",
                    translateMsg("poll.question")
                )
            )

            if (field.type == BaseType.PollSingleResponseQuestion || field.type == BaseType.PollMultiResponseQuestion) {
                field.answers?.forEachIndexed { answerIndex, _ ->
                    questionFieldset.add(
                        generateSingleAndMultiResponseAnswer(
                            dto.isAlreadyCreated(),
                            index,
                            field.uid,
                            answerIndex,
                            layout,
                            field.answers!!.size
                        )
                    )
                        .add(UISpacer())
                }
                if (!dto.isAlreadyCreated()) {
                    questionFieldset.add(
                        UIRow()
                            .add(
                                UIButton.createAddButton(
                                    responseAction = ResponseAction(
                                        "${Rest.URL}/poll/addAnswer/${field.uid}", targetType = TargetType.POST
                                    ),
                                    default = false
                                )
                            )
                    )
                }
            }

            fieldset.add(questionFieldset)
        }
    }

    private fun generateSingleAndMultiResponseAnswer(
        objGiven: Boolean,
        inputFieldIndex: Int,
        questionUid: String?,
        answerIndex: Int,
        layout: UILayout,
        answerAmount: Int,
    ): UIRow {
        val row = UIRow()
        row.add(
            UICol()
                .add(
                    getUiElement(
                        objGiven,
                        "inputFields[${inputFieldIndex}].answers[${answerIndex}]",
                        translateMsg("poll.answer") + " ${answerIndex + 1}"
                    )
                )
        )
        if (!objGiven) {
            // require at least two answers
            if (answerAmount > 2) {
                row.add(
                    UICol()
                        .add(UISpacer())
                        .add(
                            UIButton.createDangerButton(
                                id = "X",
                                responseAction = ResponseAction(
                                    "${Rest.URL}/poll/deleteAnswer/${questionUid}/${answerIndex}",
                                    targetType = TargetType.POST
                                )
                            ).withConfirmMessage(layout, confirmMessage = "poll.confirmation.deleteAnswer")
                        )
                )
            }
        }

        return row
    }


    @PostMapping("/deleteAnswer/{questionUid}/{answerIndex}")
    fun deleteAnswerOfSingleAndMultipleResponseQuestion(
        @RequestBody postData: PostData<Poll>,
        @PathVariable("questionUid") questionUid: String,
        @PathVariable("answerIndex") answerIndex: Int
    ): ResponseEntity<ResponseAction> {
        val dto = postData.data

        dto.inputFields?.find { it.uid.equals(questionUid) }?.answers?.removeAt(answerIndex)
        dto.owner = userService.getUser(dto.owner?.id)
        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE).addVariable("data", dto)
                .addVariable("ui", createEditLayout(dto, getUserAccess(dto)))
        )
    }


    private fun generateDeleteButton(layout: UILayout, uid: String?): UIRow {
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
                        ).withConfirmMessage(layout, confirmMessage = "poll.confirmation.deleteQuestion")
                    )
            )
        return row
    }


    @PostMapping("/deleteQuestion/{uid}")
    fun deleteQuestion(
        @RequestBody postData: PostData<Poll>,
        @PathVariable("uid") uid: String,
    ): ResponseEntity<ResponseAction> {
        val dto = postData.data
        val userAccess = UILayout.UserAccess(insert = true, update = true)

        val matchingQuestion: Question? = dto.inputFields?.find { it.uid.equals(uid) }
        dto.inputFields?.remove(matchingQuestion)

        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE).addVariable("data", dto)
                .addVariable("ui", createEditLayout(dto, userAccess))
        )
    }


    companion object {
        /**
         *  Once created, questions should be ReadOnly
         */
        @JvmStatic
        fun getUiElement(
            isReadOnly: Boolean,
            id: String,
            label: String? = null,
            dataType: UIDataType = UIDataType.STRING
        ): UIElement {
            return if (isReadOnly)
                UIReadOnlyField(id, label = label, dataType = dataType)
            else
                UIInput(id, label = label, dataType = dataType)
        }
    }


    private fun addDefaultParameterFields(pollDto: Poll, fieldset: UIFieldset, isRunning: Boolean) {

        val colWidth = UILength(xs = 12, sm = 12, md = 6, lg = 6)
        if (isRunning) {
            fieldset
                .add(lc, "title", "description", "location")
                .add(
                    UIRow()
                        .add(
                            UICol(colWidth)
                                .add(
                                    UISelect.createUserSelect(
                                        lc,
                                        "owner",
                                        false,
                                        "poll.owner",
                                    )
                                )
                        )
                        .add(
                            UICol(colWidth)
                                .add(
                                    lc,
                                    "deadline",
                                )
                        )
                )
        } else {
            fieldset
                .add(UIReadOnlyField(value = pollDto.title, label = "title", dataType = UIDataType.STRING))
                .add(UIReadOnlyField(value = pollDto.description, label = "description", dataType = UIDataType.STRING))
                .add(UIReadOnlyField(value = pollDto.location, label = "location", dataType = UIDataType.STRING))
                .add(
                    UIReadOnlyField(
                        value = pollDto.deadline.toString(),
                        label = "deadline",
                        dataType = UIDataType.STRING
                    )
                )
                .add(UIReadOnlyField(value = pollDto.owner?.displayName, label = "owner", dataType = UIDataType.STRING))
        }
    }


    /**
     * restricts the user access accordingly
     */

    private fun getUserAccess(pollDto: Poll): UILayout.UserAccess {
        val pollDO = PollDO()
        pollDto.copyTo(pollDO)

        //Fua entfernen

        return if (!pollDao.hasFullAccess(pollDO)) {
            // no full access user
            UILayout.UserAccess(insert = false, update = false, delete = false, history = false)
        } else {
            if (!pollDto.isAlreadyCreated()) {
                // full access when creating new poll
                UILayout.UserAccess(insert = true, update = true, delete = false, history = true)
            } else {
                if (pollDto.isFinished()) {
                    // full access when viewing finished poll
                    UILayout.UserAccess(insert = false, update = false, delete = true, history = false)
                }
                // full access when viewing old poll
                UILayout.UserAccess(insert = true, update = true, delete = true, history = false)
            }
        }
    }
}
