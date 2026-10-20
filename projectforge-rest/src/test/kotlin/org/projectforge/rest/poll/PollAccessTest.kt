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

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.projectforge.business.poll.*
import org.projectforge.business.poll.filter.PollAssignment
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.time.LocalDate

/**
 * Who may do what with a poll: the owner and the users with full access (also by group) edit it and see all
 * answers, the attendees (also by group) answer it for themselves, everybody else sees nothing. The decision is
 * taken on the stored poll, never on the posted one.
 */
class PollAccessTest : AbstractTestBase() {
    @Autowired
    private lateinit var pollDao: PollDao

    @Autowired
    private lateinit var pollResponseDao: PollResponseDao

    @Autowired
    private lateinit var pollAnswerRest: PollAnswerRest

    @Autowired
    private lateinit var pollEntityRest: PollEntityRest

    @Autowired
    private lateinit var pollResultsService: PollResultsService

    @Autowired
    private lateinit var pollCronJobs: PollCronJobs

    private val single = PollQuestion(
        PollJson.newUid(), "Coming?", PollQuestionType.SINGLE_CHOICE,
        mutableListOf(PollOption("yes", "Yes"), PollOption("no", "No")),
    )
    private val multi = PollQuestion(
        PollJson.newUid(), "Food?", PollQuestionType.MULTIPLE_CHOICE,
        mutableListOf(PollOption("meat", "Meat"), PollOption("fish", "Fish"), PollOption("veggie", "Veggie")),
    )
    private val text = PollQuestion(PollJson.newUid(), "Remarks", PollQuestionType.TEXT)

    /**
     * Owner TEST_HR_USER, attendees TEST_USER2 and the members of group1 (user1, user2), full access for the
     * members of group2 (user1). TEST_USER is an outsider.
     */
    private fun createPoll(deadline: LocalDate = LocalDate.now().plusDays(7)): PollDO {
        val poll = PollDO().also {
            it.title = "poll access test"
            it.deadline = deadline
            it.owner = getUser(TEST_HR_USER)
            it.attendeeUserIds = "${getUser(TEST_USER2).id}"
            it.attendeeGroupIds = "${getGroup("group1").id}"
            it.fullAccessGroupIds = "${getGroup("group2").id}"
            it.questions = PollJson.toJson(listOf(single, multi, text))
        }
        pollDao.insert(poll, checkAccess = false)
        return poll
    }

    @Test
    fun `nobody takes a poll over by posting themselves with full access`() {
        val poll = createPoll()
        val outsider = logon(TEST_USER)
        val posted = pollDao.find(poll.id, checkAccess = false)!!.also {
            it.fullAccessUserIds = "${outsider.id}"
            it.title = "taken over"
        }
        assertThrows(AccessException::class.java) { pollDao.update(posted) }
        assertThrows(AccessException::class.java) { pollDao.markAsDeleted(posted) }
        assertThrows(AccessException::class.java) { pollDao.find(poll.id) }
        assertEquals("poll access test", pollDao.find(poll.id, checkAccess = false)!!.title)
    }

    @Test
    fun `groups count, for full access and for the attendees`() {
        val poll = createPoll()
        logon("user1") // member of group2: full access, and of group1: attendee
        val stored = pollDao.find(poll.id)!!
        stored.location = "Kassel"
        pollDao.update(stored)
        assertEquals(
            setOf(PollAssignment.ACCESS, PollAssignment.ATTENDEE),
            pollDao.getAssignments(stored, getUser("user1").id),
        )

        logon("user2") // member of group1: attendee
        val seen = pollDao.find(poll.id)!!
        seen.location = "Berlin"
        assertThrows(AccessException::class.java) { pollDao.update(seen) }
        assertEquals(
            setOf(getUser(TEST_USER2).id, getUser("user1").id, getUser("user2").id),
            pollDao.getAttendeeIds(poll),
        )
        // A template has no attendees:
        val template = PollDO().also { it.template = true; it.attendeeUserIds = "${getUser("user2").id}" }
        assertEquals(emptySet<Long>(), pollDao.getAttendeeIds(template))
    }

    @Test
    fun `only attendees answer, for themselves, while the poll is open`() {
        val poll = createPoll()
        logon(TEST_USER)
        assertThrows(AccessException::class.java) { save(poll, null, answer(single, "yes")) }

        logon(TEST_USER2)
        assertEquals(HttpStatus.OK, save(poll, null, answer(single, "yes")).statusCode)
        assertEquals(HttpStatus.OK, save(poll, null, answer(single, "no"), answer(multi, "meat", "fish")).statusCode)
        val responses = pollResponseDao.getResponses(poll.id!!)
        assertEquals(1, responses.size, "A second save updates the response.")
        val answers = PollJson.answers(responses.single().answers)
        assertEquals(listOf("no"), answers.single { it.questionUid == single.uid }.optionUids)
        assertEquals(listOf("meat", "fish"), answers.single { it.questionUid == multi.uid }.optionUids)
        // Not for others without full access, and one option of a single choice question at most:
        assertThrows(AccessException::class.java) { save(poll, getUser("user2").id, answer(single, "yes")) }
        assertEquals(HttpStatus.BAD_REQUEST, save(poll, null, answer(single, "yes", "no")).statusCode)

        logon("user1") // full access: answers for an attendee
        assertEquals(HttpStatus.OK, save(poll, getUser("user2").id, answer(single, "yes")).statusCode)
        assertEquals(getUser("user2").id, pollAnswerRest.getAnswers(poll.id!!, getUser("user2").id).answerOwner.id)
        // But not for an outsider:
        assertThrows(AccessException::class.java) { save(poll, getUser(TEST_USER).id, answer(single, "yes")) }

        // Finished: no more answers, and no more changes of the poll.
        pollEntityRest.finishAndMail(pollDao.find(poll.id, checkAccess = false)!!)
        logon(TEST_USER2)
        assertThrows(AccessException::class.java) { save(poll, null, answer(single, "yes")) }
        logon(TEST_HR_USER)
        val finished = pollDao.find(poll.id)!!
        assertEquals(PollDO.State.FINISHED, finished.state)
        assertThrows(AccessException::class.java) { pollDao.update(finished.also { it.title = "changed" }) }
    }

    @Test
    fun `no answers after the deadline`() {
        val poll = createPoll(deadline = LocalDate.now().minusDays(1))
        logon(TEST_USER2)
        assertThrows(AccessException::class.java) { save(poll, null, answer(single, "yes")) }
    }

    @Test
    fun `the results are for the users with full access only`() {
        val poll = createPoll()
        logon(TEST_USER2)
        save(poll, null, answer(multi, "fish"), answer(text, text = "See you"))
        assertThrows(AccessException::class.java) { pollAnswerRest.getResults(poll.id!!) }
        assertThrows(AccessException::class.java) { pollEntityRest.exportAsExcel(poll.id!!) }

        logon("user1")
        val results = pollAnswerRest.getResults(poll.id!!)
        assertEquals(3, results.rows.size) // TEST_USER2, user1, user2
        assertEquals(HttpStatus.OK, pollEntityRest.exportAsExcel(poll.id!!).statusCode)

        // The columns of an answer don't depend on the answers given before it (the single choice question is
        // unanswered here):
        val sheet = XSSFWorkbook(pollResultsService.getExcel(results).inputStream()).getSheetAt(0)
        assertEquals("Coming?", sheet.getRow(0).getCell(1).stringCellValue)
        assertEquals("Food?", sheet.getRow(0).getCell(3).stringCellValue)
        assertEquals("Remarks", sheet.getRow(0).getCell(6).stringCellValue)
        val row = (2..sheet.lastRowNum).map { sheet.getRow(it) }
            .single { it.getCell(0).stringCellValue == getUser(TEST_USER2).displayName }
        assertEquals("", row.getCell(1)?.stringCellValue ?: "")
        assertEquals("X", row.getCell(4).stringCellValue)
        assertEquals("See you", row.getCell(6).stringCellValue)
    }

    @Test
    fun `the daily job finishes a poll after its deadline, once`() {
        val poll = createPoll(deadline = LocalDate.now().minusDays(1))
        pollCronJobs.run(null, LocalDate.now())
        val finished = pollDao.find(poll.id, checkAccess = false)!!
        assertEquals(PollDO.State.FINISHED, finished.state)
        val lastUpdate = finished.lastUpdate
        assertNotNull(lastUpdate)
        pollCronJobs.run(null, LocalDate.now())
        assertEquals(lastUpdate, pollDao.find(poll.id, checkAccess = false)!!.lastUpdate)
    }

    @Test
    fun `answers to unknown questions and options are dropped`() {
        val sanitized = PollAnswerRest.sanitize(
            listOf(single, text),
            listOf(
                PollAnswer(single.uid, optionUids = mutableListOf("yes", "unknown")),
                PollAnswer("unknown question", text = "x"),
                PollAnswer(text.uid, text = "  "),
            ),
        )!!
        assertEquals(1, sanitized.size)
        assertEquals(listOf("yes"), sanitized.single().optionUids)
        assertNull(PollAnswerRest.sanitize(listOf(text), listOf(PollAnswer(text.uid, text = "x".repeat(5000)))))
    }

    private fun answer(question: PollQuestion, vararg options: String, text: String? = null) =
        PollAnswer(question.uid, text = text, optionUids = options.toMutableList())

    private fun save(poll: PollDO, ownerId: Long?, vararg answers: PollAnswer) =
        pollAnswerRest.save(PollAnswerRest.SaveRequest(poll.id, ownerId, answers.toList()))
}
