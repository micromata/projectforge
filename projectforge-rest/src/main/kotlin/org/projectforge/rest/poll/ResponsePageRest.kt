package org.projectforge.rest.poll

import com.fasterxml.jackson.databind.ObjectMapper
import org.projectforge.business.poll.PollDO
import org.projectforge.business.poll.PollDao
import org.projectforge.business.poll.PollResponseDO
import org.projectforge.business.poll.PollResponseDao
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.User
import org.projectforge.rest.poll.types.*
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid


@RestController
@RequestMapping("${Rest.URL}/response")
class ResponsePageRest : AbstractDynamicPageRest() {

    @Autowired
    private lateinit var pollDao: PollDao

    @Autowired
    private lateinit var pollResponseDao: PollResponseDao

    @Autowired
    private lateinit var userService: UserService

    private var pollId: Int? = null
    private var delegatedUser: User? = null

    @GetMapping("dynamic")
    fun getForm(
        request: HttpServletRequest,
        @RequestParam("number") pollStringId: String?,
        @RequestParam("delegatedUser") delUser: String?,
    ): FormLayoutData {
        pollId = NumberHelper.parseInteger(pollStringId) ?: throw IllegalArgumentException("id not given.")

        //used to load answers, is an attendee chosen by a fullAccessUser in order to answer for them or the ThreadLocal User
        val pollData = pollDao.internalGetById(pollId) ?: PollDO()

        val questionOwner =
            if (delUser != null && pollDao.hasFullAccess(pollData) && pollDao.isAttendee(pollData, delUser.toInt()))
                delUser.toInt()
            else
                ThreadLocalUserContext.user?.id

        val questionOwnerName = userService.getUser(questionOwner).displayName
        val pollDto = transformPollFromDB(pollData)

        if (pollDto.state == PollDO.State.FINISHED) {
            throw AccessException("access.exception.noAccess", "poll.error.closed")
        }

        val layout = UILayout("poll.response.title")

        val fieldSetDelegationUser = UIFieldset(title = "poll.userDelegation")

        if (hasFullAccess(pollDto)) {
            fieldSetDelegationUser.add(
                UIInput(
                    id = "delegationUser",
                    label = "user",
                    dataType = UIDataType.USER
                )
            )
                .add(
                    UIButton.createDefaultButton(
                        id = "response-poll-button",
                        responseAction = ResponseAction(
                            RestResolver.getRestUrl(
                                this::class.java,
                                "showDelegatedUser"
                            ), targetType = TargetType.PUT
                        ),
                        title = "poll.response.page"
                    ),
                )
        }
        layout.add(fieldSetDelegationUser)

        val fieldSet = UIFieldset(12, title = pollDto.title + " Antworten von " + questionOwnerName)
        fieldSet
            .add(UIReadOnlyField(value = pollDto.description, label = "Description"))
            .add(UIReadOnlyField(value = pollDto.location, label = "Location"))
            .add(UIReadOnlyField(value = pollDto.owner?.displayName, label = "Owner"))
            .add(UIReadOnlyField(value = pollDto.deadline.toString(), label = "Deadline"))

        fieldSet.add(UIReadOnlyField(value = pollDto.description, label = translateMsg("poll.description")))
            .add(UIReadOnlyField(value = pollDto.location, label = translateMsg("poll.location")))
            .add(UIReadOnlyField(value = pollDto.owner?.displayName, label = translateMsg("poll.owner")))
            .add(UIReadOnlyField(value = pollDto.deadline.toString(), label = translateMsg("poll.deadline")))

        layout.add(fieldSet)

        layout.add(
            MenuItem(
                "EDIT",
                i18nKey = "poll.title.edit",
                url = PagesResolver.getEditPageUrl(PollPageRest::class.java, pollDto.id),
                type = MenuItemTargetType.REDIRECT
            )
        )

        val pollResponse = PollResponse()
        pollResponse.poll = pollData

        pollResponseDao.internalLoadAll().firstOrNull { response ->
            response.owner?.id == questionOwner
                    && response.poll?.id == pollData.id
        }?.let {
            pollResponse.copyFrom(it)
        }

        pollDto.inputFields?.forEachIndexed { index, field ->
            val fieldSetQuestions = UIFieldset(title = field.question)
            val questionAnswer = QuestionAnswer()
            questionAnswer.uid = UUID.randomUUID().toString()
            questionAnswer.questionUid = field.uid
            pollResponse.responses?.firstOrNull {
                it.questionUid == field.uid
            }.let {
                if (it == null) pollResponse.responses?.add(questionAnswer)
            }

            val col = UICol()

            if (field.type == BaseType.TextQuestion) {
                col.add(
                    PollPageRest.getUiElement(
                        pollDto.isFinished(),
                        "responses[$index].answers[0]",
                        "poll.question.textQuestion",
                        UIDataType.STRING
                    )
                )
            }
            if (field.type == BaseType.SingleResponseQuestion) {
                col.add(
                    PollPageRest.getUiElement(
                        pollDto.isFinished(),
                        "responses[$index].answers[0]",
                        "poll.question.textQuestion",
                        UIDataType.BOOLEAN
                    )
                )
                col.add(
                    PollPageRest.getUiElement(
                        pollDto.isFinished(),
                        "responses[$index].answers[0]",
                        "poll.question.textQuestion",
                        UIDataType.BOOLEAN
                    )
                )
                col.add(
                    UIRadioButton(
                        "responses[$index].answers[0]", value = field.answers!![0], label = field.answers?.get(0) ?: ""
                    )
                )
                col.add(
                    UIRadioButton(
                        "responses[$index].answers[0]", value = field.answers!![1], label = field.answers?.get(1) ?: ""
                    )
                )
            }
            if (field.type == BaseType.MultiResponseQuestion) {
                field.answers?.forEachIndexed { index2, _ ->
                    if (pollResponse.responses?.get(index)?.answers?.getOrNull(index2) == null) {
                        pollResponse.responses?.get(index)?.answers?.add(index2, false)
                    }
                    col.add(UICheckbox("responses[$index].answers[$index2]", label = field.answers?.get(index2) ?: ""))
                }
            }
            fieldSetQuestions.add(UIRow().add(col))
            layout.add(fieldSetQuestions)
        }

        layout.add(
            UIButton.createDefaultButton(
                id = "addResponse",
                title = translateMsg("poll.respond"),
                responseAction = ResponseAction(
                    RestResolver.getRestUrl(
                        this::class.java,
                        "addResponse"
                    ) + "/?questionOwner=${questionOwner}", targetType = TargetType.POST
                )
            )
        )
        layout.watchFields.add("delegationUser")
        LayoutUtils.process(layout)
        return FormLayoutData(pollResponse, layout, createServerData(request))
    }

    @PostMapping("addResponse")
    fun addResponse(
        request: HttpServletRequest,
        @RequestBody postData: PostData<PollResponse>, @RequestParam("questionOwner") questionOwner: Int?
    ): ResponseEntity<ResponseAction>? {
        val pollResponseDO = PollResponseDO()
        postData.data.copyTo(pollResponseDO)

        pollResponseDO.owner = userService.getUser(questionOwner)
        pollResponseDao.internalLoadAll().firstOrNull { pollResponse ->
            pollResponse.owner?.id == questionOwner
                    && pollResponse.poll?.id == postData.data.poll?.id
        }?.let {
            it.responses = pollResponseDO.responses
            pollResponseDao.update(it)
            return ResponseEntity.ok(
                ResponseAction(
                    targetType = TargetType.REDIRECT,
                    url = PagesResolver.getListPageUrl(PollPageRest::class.java, absolute = true)
                )
            )
        }


        pollResponseDao.saveOrUpdate(pollResponseDO)

        return ResponseEntity.ok(
            ResponseAction(
                targetType = TargetType.REDIRECT,
                url = PagesResolver.getListPageUrl(PollPageRest::class.java, absolute = true)
            )
        )
    }

    @PutMapping("showDelegatedUser")
    fun showDelegatedUser(
        request: HttpServletRequest,
        @RequestBody postData: PostData<PollResponse>
    ): ResponseEntity<ResponseAction>? {
        return ResponseEntity.ok(
            ResponseAction(
                url = "/react/response/dynamic/?number=${pollId}&delegatedUser=${delegatedUser?.id}",
                targetType = TargetType.REDIRECT
            )
        )
    }

    @PostMapping(RestPaths.WATCH_FIELDS)
    fun watchFields(@Valid @RequestBody postData: PostData<Poll>): ResponseEntity<ResponseAction> {
        delegatedUser = postData.data.delegationUser
        return ResponseEntity.ok(ResponseAction(targetType = TargetType.UPDATE))
    }

    private fun transformPollFromDB(obj: PollDO): Poll {
        val poll = Poll()
        poll.copyFrom(obj)
        if (obj.inputFields != null) {
            val a = ObjectMapper().readValue(obj.inputFields, MutableList::class.java)
            poll.inputFields = a.map { Question().toObject(ObjectMapper().writeValueAsString(it)) }.toMutableList()
        }
        return poll
    }

    private fun hasFullAccess(dto: Poll): Boolean {
        val loggedInUser = ThreadLocalUserContext.user
        val foundUser = dto.fullAccessUsers?.any { user -> user.id == loggedInUser?.id }
        return foundUser == true || dto.owner?.id == loggedInUser?.id
    }
}