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

import org.projectforge.business.PfCaches
import org.projectforge.business.poll.*
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.dto.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

/**
 * The answers to a poll (next page `/poll/{id}/answer`) and its results (tab of the edit page).
 *
 * An attendee answers for themselves, a user with full access for themselves or on behalf of an attendee. The poll,
 * the owner of the answers and the stored response are taken from the database and the logged-in user, never from
 * the posted data: [PollResponseDao] checks the write (and that the poll is still open).
 */
@RestController
@RequestMapping("${Rest.URL}/pollAnswer")
class PollAnswerRest {
    class AnswerData(
        val pollId: Long,
        val title: String?,
        val description: String?,
        val location: String?,
        val owner: User?,
        val deadline: LocalDate?,
        val state: PollDO.State,
        /** Whether answers are taken (running, deadline not passed). */
        val open: Boolean,
        val questions: List<PollQuestion>,
        /** Whose answers these are: the logged-in user or the attendee answered for. */
        val answerOwner: User,
        val answers: List<PollAnswer>,
        /** When the answers were saved last, null if not yet. */
        val lastUpdate: Date?,
        /** Whether the logged-in user has full access: edits the poll, sees the results, answers for attendees. */
        val fullAccess: Boolean,
        /** The attendees, to answer for one of them (full access only). */
        val attendees: List<User>?,
    )

    class SaveRequest(
        var pollId: Long? = null,
        /** The attendee answered for, null for the logged-in user. */
        var ownerId: Long? = null,
        var answers: List<PollAnswer> = emptyList(),
    )

    @Autowired
    private lateinit var pollDao: PollDao

    @Autowired
    private lateinit var pollResponseDao: PollResponseDao

    @Autowired
    private lateinit var pollMailService: PollMailService

    @Autowired
    private lateinit var pollResultsService: PollResultsService

    @GetMapping("{pollId}")
    @AccessChecked("attendee or full access (PollDao select access); for another user: full access and attendee")
    fun getAnswers(@PathVariable("pollId") pollId: Long, @RequestParam("owner", required = false) ownerId: Long?): AnswerData {
        val poll = getPoll(pollId)
        return answerData(poll, checkOwner(poll, ownerId))
    }

    @PostMapping("save")
    @AccessChecked("PollResponseDao: attendee for themselves, full access for an attendee, poll open")
    fun save(@RequestBody request: SaveRequest): ResponseEntity<AnswerData> {
        val poll = getPoll(request.pollId ?: return ResponseEntity.badRequest().build())
        val ownerId = checkOwner(poll, request.ownerId)
        val answers = sanitize(PollJson.questions(poll.questions), request.answers)
            ?: return ResponseEntity.badRequest().build()
        val response = pollResponseDao.getResponse(poll.id!!, ownerId) ?: PollResponseDO()
        response.poll = poll
        response.owner = PfCaches.instance.getUser(ownerId)
        response.answers = PollJson.toJson(answers)
        response.deleted = false
        pollResponseDao.insertOrUpdate(response)
        val loggedInUser = ThreadLocalUserContext.loggedInUser!!
        if (ownerId != loggedInUser.id) {
            pollMailService.sendAnsweredOnBehalf(poll, ownerId, loggedInUser)
        }
        return ResponseEntity.ok(answerData(poll, ownerId))
    }

    /** All answers to the poll, for the users with full access. */
    @GetMapping("results/{pollId}")
    @AccessChecked("full access to the poll (PollDao.hasFullAccess)")
    fun getResults(@PathVariable("pollId") pollId: Long): PollResultsService.Results {
        val poll = getPoll(pollId)
        if (!pollDao.hasFullAccess(poll, ThreadLocalUserContext.loggedInUserId)) {
            throw AccessException(AccessException.I18N_KEY_STANDARD, "PollDO", "results")
        }
        return pollResultsService.getResults(poll)
    }

    /** The poll, if the logged-in user may see it (attendee or full access). */
    private fun getPoll(pollId: Long): PollDO {
        val poll = pollDao.find(pollId) ?: throw AccessException("access.exception.noAccess")
        if (poll.template) {
            throw AccessException(AccessException.I18N_KEY_STANDARD, "PollDO", "answer")
        }
        return poll
    }

    /** The logged-in user, or the attendee a user with full access answers for. */
    private fun checkOwner(poll: PollDO, ownerId: Long?): Long {
        val userId = ThreadLocalUserContext.loggedInUserId!!
        if (ownerId == null || ownerId == userId) {
            return userId
        }
        if (!pollDao.hasFullAccess(poll, userId) || !pollDao.isAttendee(poll, ownerId)) {
            throw AccessException(AccessException.I18N_KEY_STANDARD, "PollDO", "answer")
        }
        return ownerId
    }

    private fun answerData(poll: PollDO, ownerId: Long): AnswerData {
        val userId = ThreadLocalUserContext.loggedInUserId
        val fullAccess = pollDao.hasFullAccess(poll, userId)
        val response = pollResponseDao.getResponse(poll.id!!, ownerId)?.takeIf { !it.deleted }
        val attendees = if (fullAccess) {
            pollDao.getAttendeeIds(poll).map { User(it) }.also { User.restoreDisplayNames(it) }
                .sortedBy { it.displayName?.lowercase() }
        } else {
            null
        }
        val owner = poll.owner?.id?.let { User(it) }?.also { User.restoreDisplayNames(listOf(it)) }
        val answerOwner = User(ownerId).also { User.restoreDisplayNames(listOf(it)) }
        return AnswerData(
            pollId = poll.id!!,
            title = poll.title,
            description = poll.description,
            location = poll.location,
            owner = owner,
            deadline = poll.deadline,
            state = poll.state,
            open = PollResponseDao.isOpen(poll),
            questions = PollJson.questions(poll.questions),
            answerOwner = answerOwner,
            answers = PollJson.answers(response?.answers),
            lastUpdate = response?.lastUpdate,
            fullAccess = fullAccess,
            attendees = attendees,
        )
    }

    companion object {
        /** Max length of a text answer. */
        const val MAX_TEXT_LENGTH = 4000

        /**
         * The answers to the questions of the poll only: a text for a text question, known options of a choice
         * question, one at most of a single choice question. Unanswered questions are left out.
         * @return null if an answer is invalid (several options of a single choice question, a text too long).
         */
        internal fun sanitize(questions: List<PollQuestion>, answers: List<PollAnswer>): List<PollAnswer>? {
            val byQuestion = answers.associateBy { it.questionUid }
            val result = mutableListOf<PollAnswer>()
            questions.forEach { question ->
                val answer = byQuestion[question.uid] ?: return@forEach
                if (question.type.isChoice) {
                    val known = question.options.mapNotNull { it.uid }.toSet()
                    val chosen = answer.optionUids.filter { known.contains(it) }.distinct()
                    if (question.type == PollQuestionType.SINGLE_CHOICE && chosen.size > 1) {
                        return null
                    }
                    if (chosen.isNotEmpty()) {
                        result.add(PollAnswer(question.uid, optionUids = chosen.toMutableList()))
                    }
                } else {
                    val text = answer.text?.trim()
                    if ((text?.length ?: 0) > MAX_TEXT_LENGTH) {
                        return null
                    }
                    if (!text.isNullOrEmpty()) {
                        result.add(PollAnswer(question.uid, text = text))
                    }
                }
            }
            return result
        }
    }
}
