/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.poll.*
import org.projectforge.business.poll.filter.PollAssignment
import org.projectforge.business.poll.filter.PollAssignmentFilter
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.User
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.ValidationError
import org.projectforge.ui.filter.UIFilterListElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

private val log = KotlinLogging.logger {}

/**
 * The polls (menu "Polls"): list and edit of the next pages (components/features/poll). The answers of a user are
 * given on a page of their own (see [PollAnswerRest]).
 *
 * Who may do what is decided by [PollDao]: everybody creates polls, the owner and the users with full access edit
 * them and see the results, the attendees answer them.
 */
@RestController
@RequestMapping("${Rest.URL}/poll")
class PollEntityRest :
    AbstractDTOEntityRest<PollDO, Poll, PollDao>(PollDao::class.java, "poll.title", cloneSupport = CloneSupport.CLONE) {

    @Autowired
    private lateinit var pollResponseDao: PollResponseDao

    @Autowired
    private lateinit var pollMailService: PollMailService

    @Autowired
    private lateinit var pollResultsService: PollResultsService

    override fun transformFromDB(obj: PollDO, editMode: Boolean): Poll {
        val poll = Poll()
        poll.copyFrom(obj)
        poll.restoreDisplayNames()
        val userId = ThreadLocalUserContext.loggedInUserId
        poll.fullAccess = baseDao.hasFullAccess(obj, userId)
        poll.attendee = baseDao.isAttendee(obj, userId)
        return poll
    }

    override fun transformForDB(dto: Poll): PollDO {
        val poll = PollDO()
        dto.copyTo(poll)
        return poll
    }

    override fun newBaseDTO(request: HttpServletRequest?): Poll {
        val poll = Poll()
        poll.owner = User.getUser(ThreadLocalUserContext.loggedInUserId)
        poll.deadline = LocalDate.now().plusWeeks(2)
        poll.fullAccess = true
        return poll
    }

    /** A copy is a new, running poll with the questions of this one (new uids) and without its deadline. */
    override fun prepareClone(dto: Poll): Poll {
        val clone = super.prepareClone(dto)
        clone.owner = User.getUser(ThreadLocalUserContext.loggedInUserId)
        clone.state = PollDO.State.RUNNING
        clone.template = false
        clone.deadline = null
        clone.questions = PollJson.withNewUids(dto.questions)
        clone.fullAccess = true
        return clone
    }

    override fun validate(validationErrors: MutableList<ValidationError>, dto: Poll) {
        if (dto.questions.isEmpty()) {
            validationErrors.add(ValidationError(translate("poll.error.oneQuestionRequired"), fieldId = "questions"))
        }
        dto.questions.forEachIndexed { index, question ->
            if (question.text.isNullOrBlank()) {
                validationErrors.add(ValidationError(translate("poll.error.questionText"), fieldId = "questions[$index].text"))
            }
            if (question.type.isChoice && question.options.count { !it.text.isNullOrBlank() } < 2) {
                validationErrors.add(ValidationError(translate("poll.error.twoOptionsRequired"), fieldId = "questions[$index].options"))
            }
        }
        if (dto.template) {
            // Never answered: no deadline needed.
            return
        }
        if (dto.attendeeUsers.isNullOrEmpty() && dto.attendeeGroups.isNullOrEmpty()) {
            validationErrors.add(ValidationError(translate("poll.error.oneAttendeeRequired"), fieldId = "attendeeUsers"))
        }
        val deadline = dto.deadline
        if (deadline == null) {
            validationErrors.add(ValidationError.createFieldRequired(PollDO::class.java, "deadline"))
        } else if (isInvited(dto) && deadline.isBefore(LocalDate.now())) {
            validationErrors.add(ValidationError(translate("poll.error.deadlineInPast"), fieldId = "deadline"))
        }
    }

    /** Whether saving the poll invites its attendees: a new one, or a template becoming a poll. */
    private fun isInvited(dto: Poll): Boolean {
        val id = dto.id ?: return !dto.template
        return !dto.template && baseDao.find(id, checkAccess = false)?.template == true
    }

    /**
     * Owner, state and the uids of the questions are decided here, not by the posted data: a new poll belongs to its
     * creator and runs (a template is in state template), a stored one keeps its owner (see [finish] for the end).
     */
    override fun onBeforeSaveOrUpdate(request: HttpServletRequest, obj: PollDO, postData: PostData<Poll>) {
        val dbObj = obj.id?.let { baseDao.find(it, checkAccess = false) }
        if (dbObj == null) {
            obj.owner = ThreadLocalUserContext.loggedInUser
        } else {
            obj.owner = dbObj.owner
            if (dbObj.template && !obj.template) {
                // A template becomes a poll: invited like a new one (see onAfterSaveOrUpdate). Kept in the request,
                // this bean is shared by all requests.
                request.setAttribute(INVITE_ATTRIBUTE, true)
            }
        }
        obj.state = if (obj.template) PollDO.State.TEMPLATE else PollDO.State.RUNNING
        val questions = PollJson.questions(obj.questions)
        questions.forEach { question ->
            if (question.uid.isNullOrBlank()) question.uid = PollJson.newUid()
            question.options.removeIf { it.text.isNullOrBlank() }
            question.options.forEach { if (it.uid.isNullOrBlank()) it.uid = PollJson.newUid() }
            if (!question.type.isChoice) question.options.clear()
        }
        obj.questions = PollJson.toJson(questions)
        super.onBeforeSaveOrUpdate(request, obj, postData)
    }

    override fun onAfterSaveOrUpdate(request: HttpServletRequest, obj: PollDO, postData: PostData<Poll>) {
        super.onAfterSaveOrUpdate(request, obj, postData)
        val invite = postData.data.id == null || request.getAttribute(INVITE_ATTRIBUTE) == true
        if (invite && !obj.template) {
            pollMailService.sendInvitation(obj, postData.data.invitationSubject, postData.data.invitationText)
        }
    }

    /**
     * The responses go with the poll: after it is deleted, so only after the access check of [PollDao] (the responses
     * are deleted without one).
     */
    override fun onAfterMarkAsDeleted(request: HttpServletRequest, obj: PollDO, postData: PostData<Poll>): ResponseAction {
        obj.id?.let { pollId ->
            pollResponseDao.getResponses(pollId).forEach { pollResponseDao.markAsDeleted(it, checkAccess = false) }
        }
        return super.onAfterMarkAsDeleted(request, obj, postData)
    }

    /** The responses come back with the poll (they are deleted with it only, see [onAfterMarkAsDeleted]). */
    override fun onAfterUndelete(request: HttpServletRequest, obj: PollDO, postData: PostData<Poll>): ResponseAction {
        obj.id?.let { pollId ->
            pollResponseDao.getResponses(pollId, deleted = true).forEach { pollResponseDao.undelete(it, checkAccess = false) }
        }
        return super.onAfterUndelete(request, obj, postData)
    }

    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        elements.add(
            UIFilterListElement("assignment", label = translate("poll.assignment"), defaultFilter = true)
                .buildValues(PollAssignment.OWNER, PollAssignment.ACCESS, PollAssignment.ATTENDEE)
        )
        elements.add(
            UIFilterListElement("state", label = translate("poll.state"), defaultFilter = true)
                .buildValues(PollDO.State::class.java)
        )
    }

    override fun preProcessMagicFilter(target: QueryFilter, source: MagicFilter): List<CustomResultFilter<PollDO>>? {
        val filters = mutableListOf<CustomResultFilter<PollDO>>()
        source.entries.find { it.field == "assignment" }?.let { entry ->
            entry.synthetic = true
            val values = entry.value.values?.mapNotNull { value -> PollAssignment.entries.find { it.name == value } }
            if (!values.isNullOrEmpty()) {
                filters.add(PollAssignmentFilter(values, baseDao))
            }
        }
        // Nothing chosen: all of them, the templates included (in state template, see onBeforeSaveOrUpdate).
        val states = source.entries.find { it.field == "state" }?.let { entry ->
            entry.synthetic = true
            entry.value.values?.mapNotNull { value -> PollDO.State.entries.find { it.name == value } }
        }.orEmpty().toSet()
        // The former filter "templates" (a boolean), still in the stored filters of some users: ignored, the state
        // filter has taken its place (and the user couldn't remove it anymore).
        source.entries.find { it.field == "template" }?.synthetic = true
        if (states.isNotEmpty() && states.size < PollDO.State.entries.size) {
            target.add(QueryFilter.isIn("state", states))
        }
        return filters
    }

    /**
     * Ends the poll before its deadline (the deadline is set to today) and mails the end, the results to the users
     * with full access.
     */
    @PostMapping("finish/{id}")
    @AccessChecked("DAO: update access (full access to the running poll)")
    fun finish(@PathVariable("id") id: Long): ResponseEntity<Poll> {
        val poll = baseDao.find(id) ?: throw AccessException("access.exception.noAccess")
        if (poll.template || !baseDao.hasLoggedInUserUpdateAccess(poll, poll, false)) {
            throw AccessException(AccessException.I18N_KEY_STANDARD, "PollDO", "finish")
        }
        finishAndMail(poll)
        return ResponseEntity.ok(transformFromDB(poll, true))
    }

    /** Finishes the poll and mails its end (also used by [PollCronJobs] after the deadline). */
    internal fun finishAndMail(poll: PollDO) {
        poll.state = PollDO.State.FINISHED
        if (poll.deadline?.isAfter(LocalDate.now()) != false) {
            poll.deadline = LocalDate.now()
        }
        baseDao.update(poll, checkAccess = false)
        log.info { "Poll ${poll.id} finished." }
        val excel = try {
            pollResultsService.getExcel(pollResultsService.getResults(poll))
        } catch (ex: Exception) {
            log.error(ex) { "Excel of the results of poll ${poll.id} failed: ${ex.message}" }
            null
        }
        pollMailService.sendFinished(poll, pollResultsService.excelFilename(poll), excel)
    }

    /** The answers of all users as Excel file, for the users with full access only. */
    @GetMapping("exportAsExcel/{id}")
    @AccessChecked("full access to the poll (PollDao.hasFullAccess)")
    fun exportAsExcel(@PathVariable("id") id: Long): ResponseEntity<Resource> {
        val poll = baseDao.find(id) ?: throw AccessException("access.exception.noAccess")
        if (!baseDao.hasFullAccess(poll, ThreadLocalUserContext.loggedInUserId)) {
            throw AccessException(AccessException.I18N_KEY_STANDARD, "PollDO", "exportAsExcel")
        }
        val excel = pollResultsService.getExcel(pollResultsService.getResults(poll))
        return RestUtils.downloadFile(pollResultsService.excelFilename(poll), excel)
    }

    /** A template offered by the edit page: a built-in one ([key]) or a poll marked as template ([pollId]). */
    class TemplateInfo(val title: String?, val key: String? = null, val pollId: Long? = null)

    /** The built-in templates and the templates (polls) the logged-in user has full access to. */
    @GetMapping("templates")
    @AccessChecked("logged-in user; template polls: full access (PollDao.hasFullAccess)")
    fun getTemplates(): List<TemplateInfo> {
        val locale = ThreadLocalUserContext.locale
        val userId = ThreadLocalUserContext.loggedInUserId
        val builtIn = PollTemplates.templates.map { TemplateInfo(it.title?.get(locale), key = it.key) }
        val polls = baseDao.select(QueryFilter().also { it.add(QueryFilter.eq("template", true)) }, checkAccess = false)
            .filter { !it.deleted && baseDao.hasFullAccess(it, userId) }
            .sortedBy { it.title?.lowercase() }
            .map { TemplateInfo(it.title, pollId = it.id) }
        return polls + builtIn
    }

    /** The questions of a template (see [getTemplates]) with new uids, in the language of the logged-in user. */
    @GetMapping("templates/questions")
    @AccessChecked("logged-in user; template polls: full access (PollDao.hasFullAccess)")
    fun getTemplateQuestions(
        @RequestParam("key", required = false) key: String?,
        @RequestParam("pollId", required = false) pollId: Long?,
    ): List<PollQuestion> {
        if (pollId != null) {
            val poll = baseDao.find(pollId) ?: throw AccessException("access.exception.noAccess")
            if (!baseDao.hasFullAccess(poll, ThreadLocalUserContext.loggedInUserId)) {
                throw AccessException(AccessException.I18N_KEY_STANDARD, "PollDO", "templates")
            }
            return PollJson.withNewUids(PollJson.questions(poll.questions))
        }
        val template = PollTemplates.get(key) ?: return emptyList()
        return PollTemplates.questions(template, ThreadLocalUserContext.locale)
    }

    companion object {
        /** Request attribute: the saved poll was a template until now, so its attendees are invited. */
        private const val INVITE_ATTRIBUTE = "poll.invite"
    }
}
