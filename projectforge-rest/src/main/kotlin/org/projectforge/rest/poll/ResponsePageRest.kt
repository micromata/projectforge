package org.projectforge.rest.poll

import com.fasterxml.jackson.databind.ObjectMapper
import org.projectforge.business.poll.PollDO
import org.projectforge.business.poll.PollDao
import org.projectforge.business.poll.PollResponseDO
import org.projectforge.business.poll.PollResponseDao
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.access.AccessCheckerImpl.I18N_KEY_VIOLATION_USER_NOT_MEMBER_OF
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.poll.types.*
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.HttpServletRequest


@RestController
@RequestMapping("${Rest.URL}/response")
class ResponsePageRest : AbstractDynamicPageRest() {

    @Autowired
    private lateinit var pollDao: PollDao

    @Autowired
    private lateinit var pollResponseDao: PollResponseDao

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var accesscheck: AccessChecker

    @GetMapping("dynamic")
    fun getForm(request: HttpServletRequest, @RequestParam("pollid") pollStringId: String?, @RequestParam("questionOwner") delUser: String?): FormLayoutData {
        val id = NumberHelper.parseInteger(pollStringId) ?: throw IllegalArgumentException("id not given.")

        //used to load awnsers, is an attendee chosen by a fullaccessuser in order to awnser for them or the Threadlocal User
        var questionOwner: Int? = null

        val pollData = pollDao.internalGetById(id) ?: PollDO()

        if (delUser != "null" && pollDao.hasFullAccess(pollData) && pollDao.isAttendee(pollData, delUser?.toInt()))
            questionOwner = delUser?.toInt()
        else
            questionOwner = ThreadLocalUserContext.user?.id

        val questionOwnerName = userService.getUser(questionOwner).displayName
        val pollDto = transformPollFromDB(pollData)

        if (pollDto.state == PollDO.State.FINISHED) {
            throw AccessException(I18N_KEY_VIOLATION_USER_NOT_MEMBER_OF, "Umfrage wurde bereits beendet")
        }

        val layout = UILayout("poll.response.title")
        val fieldSet = UIFieldset(12, title = pollDto.title + " Antworten von " + questionOwnerName)
        fieldSet
            .add(UIReadOnlyField(value = pollDto.description, label = "Description"))
            .add(UIReadOnlyField(value = pollDto.location, label = "Location"))
            .add(UIReadOnlyField(value = pollDto.owner?.displayName, label = "Owner"))
            .add(UIReadOnlyField(value = pollDto.deadline.toString(), label = "Deadline"))

        layout.add(fieldSet)

        val pollResponse = PollResponse()
        pollResponse.poll = pollData

        pollResponseDao.internalLoadAll().firstOrNull { response ->
            response.owner?.id  == questionOwner
                    && response.poll?.id == pollData.id
        }?.let {
            pollResponse.copyFrom(it)
        }

        pollDto.inputFields?.forEachIndexed { index, field ->
            val fieldSet2 = UIFieldset(title = field.question)
            val questionAnswer = QuestionAnswer()
            questionAnswer.uid = UUID.randomUUID().toString()
            questionAnswer.questionUid = field.uid
            pollResponse.responses?.firstOrNull {
                it.questionUid == field.uid
            }.let {
                if (it == null)
                    pollResponse.responses?.add(questionAnswer)
            }

            val col = UICol()

            if (field.type == BaseType.TextQuestion) {
                col.add(UITextArea("responses[$index].answers[0]"))
            }
            if (field.type == BaseType.SingleResponseQuestion) {
                col.add(
                    UIRadioButton(
                        "responses[$index].answers[0]",
                        value = field.answers!![0],
                        label = field.answers?.get(0) ?: ""
                    )
                )
                col.add(
                    UIRadioButton(
                        "responses[$index].answers[0]",
                        value = field.answers!![1],
                        label = field.answers?.get(1) ?: ""
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
            fieldSet2.add(UIRow().add(col))
            layout.add(fieldSet2)
        }

        layout.add(
            UIButton.createDefaultButton(
                id = "addResponse",
                title = "submit",
                responseAction = ResponseAction(
                    RestResolver.getRestUrl(
                        this::class.java,
                        "addResponse"
                    ) + "/?questionOwner=${questionOwner}", targetType = TargetType.POST
                )
            )
        )

        return FormLayoutData(pollResponse, layout, createServerData(request))
    }

    @PostMapping("addResponse")
    fun addResponse(
        request: HttpServletRequest,
        @RequestBody postData: PostData<PollResponse>, @RequestParam("questionOwner") questionOwner: Int?
    ): ResponseEntity<ResponseAction>? {
        val questionOwner: Int? = questionOwner
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


    private fun transformPollFromDB(obj: PollDO): Poll {
        val poll = Poll()
        poll.copyFrom(obj)
        if (obj.inputFields != null) {
            val a = ObjectMapper().readValue(obj.inputFields, MutableList::class.java)
            poll.inputFields = a.map { Question().toObject(ObjectMapper().writeValueAsString(it)) }.toMutableList()
        }
        return poll
    }
}