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
import org.projectforge.business.poll.PollDO
import org.projectforge.business.poll.PollDao
import org.projectforge.business.poll.PollResponseDO
import org.projectforge.business.poll.PollResponseDao
import org.projectforge.business.poll.filter.PollAssignment
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
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
import org.projectforge.rest.poll.types.*
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.format.DateTimeFormatter
import java.util.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid


@RestController
@RequestMapping("${Rest.URL}/pollResponse")
class PollResponsePageRest : AbstractDynamicPageRest() {

    @Autowired
    private lateinit var pollDao: PollDao

    @Autowired
    private lateinit var pollResponseDao: PollResponseDao

    @Autowired
    private lateinit var pollMailService: PollMailService

    @Autowired
    private lateinit var userService: UserService

    private var pollId: Long? = null
    private var questionOwnerId: Long? = null

    @GetMapping("dynamic")
    fun getForm(
        request: HttpServletRequest,
        @RequestParam("pollId") pollStringId: String?,
        @RequestParam("questionOwner") delUser: String?,
        @RequestParam("returnToCaller") returnToCaller: String?,
    ): FormLayoutData {
        if (pollId == null || pollStringId != null) {
            pollId = NumberHelper.parseLong(pollStringId) ?: throw IllegalArgumentException("id not given.")
        }
        // used to load answers, is an attendee chosen by a fullAccessUser in order to answer for them or the ThreadLocal User
        val pollData = pollDao.find(pollId, checkAccess = false) ?: PollDO()

        val answerTitle: String
        if (delUser != null && pollDao.hasFullAccess(pollData) && pollDao.isAttendee(pollData, delUser.toLong())) {
            questionOwnerId = delUser.toLong()
            answerTitle = translateMsg("poll.delegationAnswers") + userService.getUser(questionOwnerId).displayName
        } else {
            questionOwnerId = ThreadLocalUserContext.loggedInUserId
            answerTitle = translateMsg("poll.yourAnswers")
        }


        val pollDto = transformPollFromDB(pollData)

        if (pollData.id == null) {
            throw AccessException("access.exception.noAccess", "Umfrage nicht gefunden.")
        }

        // allow access to attendee, full access and owner
        if (pollData.getPollAssignment().contains(PollAssignment.OTHER)) {
            throw AccessException("access.exception.noAccess", "Du darfst nicht auf diese Umfrage antworten.")
        }

        val layout = UILayout("poll.response.title")
        if (pollDao.hasFullAccess(pollData)) {
            layout.add(
                MenuItem(
                    "EDIT",
                    i18nKey = "poll.title.edit",
                    url = PagesResolver.getEditPageUrl(PollPageRest::class.java, pollDto.id),
                    type = MenuItemTargetType.REDIRECT
                )
            )
        }

        val fieldset = UIFieldset(12, title = pollDto.title + " - " + answerTitle)
        layout.add(fieldset)
        fieldset.add(UIReadOnlyField(value = pollDto.description, label = translateMsg("poll.description")))
            .add(UIReadOnlyField(value = pollDto.location, label = translateMsg("poll.location")))
            .add(UIReadOnlyField(value = pollDto.owner?.displayName, label = translateMsg("poll.owner")))
            .add(UIReadOnlyField(value = pollDto.deadline.toString(), label = translateMsg("poll.deadline")))

        /*  Aktuell nicht benutzbar auskommentiert bis es behoben wird
          if (pollDto.isFinished() && ThreadLocalUserContext.userId == questionOwnerId && pollDao.hasFullAccess(pollData)) {
              val fieldSetDelegationUser = UIFieldset(title = "poll.userDelegation")
              fieldSetDelegationUser.add(
                  UIInput(
                      id = "delegationUser",
                      label = "user",
                      dataType = UIDataType.USER
                  )
              )
                  .add(UISpacer())
                  .add(
                      UIButton.createDefaultButton(
                          id = "response-poll-button",
                          responseAction = ResponseAction(
                              RestResolver.getRestUrl(
                                  this::class.java,
                                  "showDelegatedUser"
                              ),
                              targetType = TargetType.GET
                          ),
                          title = "poll.selectUser"
                      ),
                  )
              layout.add(fieldSetDelegationUser)
          }

         */

        val pollResponse = PollResponse()
        pollResponse.poll = pollData

        pollResponseDao.selectAll(checkAccess = false).firstOrNull { response ->
            response.owner?.id == questionOwnerId
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

            if (field.type == BaseType.PollTextQuestion) {
                col.add(
                    PollPageRest.getUiElement(
                        pollDto.isFinished(),
                        "responses[$index].answers[0]",
                        "poll.question.TextQuestion",
                        UIDataType.STRING
                    )
                )
            }

            if (field.type == BaseType.PollMultiResponseQuestion || field.type == BaseType.PollSingleResponseQuestion) {
                field.answers?.forEachIndexed { index2, answer ->
                    if (pollResponse.responses?.get(index)?.answers?.getOrNull(index2) == null) {
                        pollResponse.responses?.get(index)?.answers?.add(index2, false)
                    }
                    if (field.type == BaseType.PollMultiResponseQuestion) {
                        col.add(
                            UICheckbox(
                                "responses[$index].answers[$index2]",
                                label = answer,
                            )
                        )
                    } else {
                        col.add(
                            UIRadioButton(
                                "responses[$index].answers[0]",
                                value = answer,
                                label = answer,
                            )
                        )
                    }
                }
            }

            fieldSetQuestions.add(UIRow().add(col))
            fieldset.add(fieldSetQuestions)
        }

        val backUrl = if (returnToCaller.isNullOrEmpty()) {
            PagesResolver.getListPageUrl(PollPageRest::class.java, absolute = true)
        } else {
            // Fix doubled encoding:
            returnToCaller.replace("%2F", "/")
        }
        layout.add(
            UIButton.createBackButton(
                responseAction = ResponseAction(
                    backUrl,
                    targetType = TargetType.REDIRECT
                ),
                default = true
            )
        )

        if (!pollDto.isFinished()) {
            layout.add(
                UIButton.createDefaultButton(
                    id = "addResponse",
                    title = translateMsg("poll.respond"),
                    responseAction = ResponseAction(
                        RestResolver.getRestUrl(
                            this::class.java,
                            "addResponse"
                        ) + "/?questionOwner=${questionOwnerId}", targetType = TargetType.POST
                    )
                )
            )
        }

        layout.watchFields.add("delegationUser")
        LayoutUtils.process(layout)
        return FormLayoutData(pollResponse, layout, createServerData(request))
    }

    @PostMapping("addResponse/")
    fun addResponse(
        @RequestBody postData: PostData<PollResponse>,
        @RequestParam("questionOwner") questionOwner: Long?
    ): ResponseEntity<ResponseAction> {
        val pollResponseDO = PollResponseDO()
        
        // most ugly workaround in existence - for now - TODO mnuhn
        val poll = pollDao.find(pollId, checkAccess = false)
        val inputFields = ObjectMapper().readValue(poll!!.inputFields, MutableList::class.java)
        postData.data.responses?.forEachIndexed { index, response ->
            val question = inputFields[index] as Map<*, *>
            if (question["type"] != "PollTextQuestion") {
                val originalAnswers = question["answers"] as List<*>
                postData.data.responses?.get(index)?.answers?.forEachIndexed { index2, answer ->
                    if (answer != false) {
                        val originalIndex = originalAnswers.indexOf(answer)
                        if (originalIndex != -1) {
                            postData.data.responses?.get(index)?.answers?.set(originalIndex, true)
                        }
                    }
                }
            }
        }
        
        postData.data.copyTo(pollResponseDO)
        pollResponseDO.owner = userService.getUser(questionOwner)

        val existingResponse = pollResponseDao.selectAll(checkAccess = false)
            .firstOrNull { response ->
                response.owner?.id == questionOwner && response.poll?.id == postData.data.poll?.id
            }

        pollResponseDao.insertOrUpdate(pollResponseDO)

        if (ThreadLocalUserContext.loggedInUser != pollResponseDO.owner) {
            sendMailResponseToOwner(pollResponseDO, ThreadLocalUserContext.loggedInUser!!)
        }

        return ResponseEntity.ok(
            ResponseAction(
                targetType = TargetType.REDIRECT,
                url = PagesResolver.getListPageUrl(PollPageRest::class.java, absolute = true)
            )
        )
    }

    private fun sendMailResponseToOwner(pollResponseDO: PollResponseDO, canedUser: PFUserDO) {
        val emailList = ArrayList<String>()
        pollResponseDO.owner?.email?.let { emailList.add(it) }
        pollMailService.sendMail(
            canedUser.email.toString(), emailList,
            translateMsg("poll.response.mail.update.subject", canedUser.displayName),
            translateMsg(
                "poll.response.mail.update.content",
                pollResponseDO.owner?.displayName,
                pollResponseDO.poll?.title,
                canedUser.displayName,
                pollResponseDO.poll?.deadline?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")).toString()
            )
        )
    }

    @GetMapping("showDelegatedUser")
    fun showDelegatedUser(): ResponseEntity<ResponseAction>? {
        val poll = pollDao.find(pollId, checkAccess = false)
        val attendees = listOfNotNull(
            poll!!.attendeeIds,
            poll.fullAccessUserIds,
            poll.owner?.id
        )
        val joinedAttendeeIds = attendees.joinToString(", ")
        if (questionOwnerId == ThreadLocalUserContext.loggedInUserId) {
            return ResponseEntity.ok(
                ResponseAction()
            )
        }
        if (joinedAttendeeIds.split(", ").any { it.toLong() == questionOwnerId }) {
            return ResponseEntity.ok(
                ResponseAction(
                    url = "/react/response/dynamic?pollId=${pollId}&questionOwner=${questionOwnerId}",
                    targetType = TargetType.REDIRECT
                )
            )
        } else {
            return ResponseEntity.badRequest().body(
                ResponseAction(
                    url = "/react/poll",
                    targetType = TargetType.REDIRECT,
                    message = ResponseAction.Message("poll.exception.noAttendee")
                )
            )
        }
    }


    @PostMapping(RestPaths.WATCH_FIELDS)
    fun watchFields(@Valid @RequestBody postData: PostData<Poll>): ResponseEntity<ResponseAction> {
        questionOwnerId = postData.data.delegationUser?.id
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
}
