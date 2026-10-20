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

import de.micromata.merlin.excel.ExcelWorkbook
import org.projectforge.excel.ExcelUtils
import org.projectforge.business.poll.*
import org.projectforge.common.FilenameUtils
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.dto.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

/**
 * All answers to a poll, for the users with full access only (results page and Excel export, see
 * PollAnswerRest and the mail after the end): the caller checks the access.
 */
@Service
class PollResultsService {
    /** One user taking part: every attendee, and whoever else answered (e.g. a user with full access). */
    class Row(
        val user: User,
        val attendee: Boolean,
        /** When the answers were last saved, null if not answered. */
        val lastUpdate: Date?,
        val answers: List<PollAnswer>,
    )

    class Results(
        val pollId: Long,
        val title: String?,
        val questions: List<PollQuestion>,
        /** Sorted by name. */
        val rows: List<Row>,
    )

    @Autowired
    private lateinit var pollDao: PollDao

    @Autowired
    private lateinit var pollResponseDao: PollResponseDao

    fun getResults(poll: PollDO): Results {
        val pollId = poll.id!!
        val responses = pollResponseDao.getResponses(pollId).associateBy { it.owner?.id }
        val attendeeIds = pollDao.getAttendeeIds(poll)
        val userIds = attendeeIds + responses.keys.filterNotNull()
        val rows = userIds.map { userId ->
            val response = responses[userId]
            val user = User(userId)
            Row(user, attendeeIds.contains(userId), response?.lastUpdate, PollJson.answers(response?.answers))
        }
        User.restoreDisplayNames(rows.map { it.user })
        return Results(
            pollId,
            poll.title,
            PollJson.questions(poll.questions),
            rows.sortedBy { it.user.displayName?.lowercase() },
        )
    }

    fun excelFilename(poll: PollDO): String =
        FilenameUtils.createSafeFilename(poll.title ?: "poll", suffix = ".xlsx", maxlength = 100, appendTimestamp = true)

    /**
     * The answers as a table: a row per user, a column per text question and per option of a choice question
     * ("X" if chosen). The first two rows name the question and its options.
     */
    fun getExcel(results: Results, locale: Locale? = ThreadLocalUserContext.locale): ByteArray {
        ExcelWorkbook.createEmptyWorkbook(locale ?: Locale.getDefault()).use { workbook ->
            val sheet = workbook.createOrGetSheet(translate(locale, "poll.results"))
            val boldFont = ExcelUtils.createFont(workbook, "bold", bold = true)
            val boldStyle = workbook.createOrGetCellStyle("bold", font = boldFont)
            val wrapStyle = workbook.createOrGetCellStyle("wrapText")
            wrapStyle.wrapText = true
            val questionRow = sheet.createRow()
            val optionRow = sheet.createRow()
            questionRow.createCell().setCellValue(translate(locale, "poll.attendee")).setCellStyle(boldStyle)
            optionRow.createCell()
            sheet.setColumnWidth(0, 30 * 256)
            results.questions.forEach { question ->
                val first = questionRow.createCell().setCellValue(question.text).setCellStyle(boldStyle).colNumber
                if (question.type.isChoice && question.options.isNotEmpty()) {
                    question.options.forEachIndexed { index, option ->
                        if (index > 0) questionRow.createCell()
                        val col = optionRow.createCell().setCellValue(option.text).setCellStyle(boldStyle).colNumber
                        sheet.setColumnWidth(col, 15 * 256)
                    }
                    if (question.options.size > 1) {
                        questionRow.addMergeRegion(first, first + question.options.size - 1)
                    }
                } else {
                    optionRow.createCell()
                    sheet.setColumnWidth(first, 40 * 256)
                }
            }
            results.rows.forEach { userRow ->
                val row = sheet.createRow()
                row.createCell().setCellValue(userRow.user.displayName)
                val answers = userRow.answers.associateBy { it.questionUid }
                results.questions.forEach { question ->
                    val answer = answers[question.uid]
                    if (question.type.isChoice && question.options.isNotEmpty()) {
                        // One cell per option, answered or not: the columns stay those of the head rows.
                        question.options.forEach { option ->
                            val cell = row.createCell()
                            if (answer?.optionUids?.contains(option.uid) == true) cell.setCellValue("X")
                        }
                    } else {
                        row.createCell().setCellValue(answer?.text ?: "").setCellStyle(wrapStyle)
                    }
                }
            }
            return workbook.asByteArrayOutputStream.toByteArray()
        }
    }
}
