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

package org.projectforge.rest.poll.excel

import de.micromata.merlin.excel.ExcelRow
import de.micromata.merlin.excel.ExcelSheet
import de.micromata.merlin.excel.ExcelWorkbook
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.util.CellRangeAddress
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.poll.PollResponseDao
import org.projectforge.business.poll.PollResponseDO
import org.projectforge.business.user.service.UserService
import org.projectforge.excel.ExcelUtils
import org.projectforge.rest.dto.User
import org.projectforge.rest.poll.Poll
import org.projectforge.rest.poll.types.BaseType
import org.projectforge.rest.poll.types.PollResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.IOException
import java.util.*

@Service
class ExcelExport {

    private val log: Logger = LoggerFactory.getLogger(ExcelExport::class.java)

    private val FIRST_DATA_ROW_NUM = 2

    @Autowired
    private lateinit var pollResponseDao: PollResponseDao

    @Autowired
    private lateinit var groupService: GroupService


    fun getExcel(poll: Poll): ByteArray? {
        log.info("Starting Excel export for poll with ID: ${poll.id}")

        val responses = pollResponseDao.selectAll(checkAccess = false).filter { it.poll?.id == poll.id }
        log.info("Loaded ${responses.size} responses for poll ID: ${poll.id}")

        try {
            ExcelWorkbook.createEmptyWorkbook().use { workbook ->
                log.info("Excel template loaded successfully")
                
                // Sheet 1: Summary
                createSummarySheet(workbook, poll, responses)

                // Sheet 2: Detailed Answers
                createDetailedResponsesSheet(workbook, poll, responses)

                // Sheet 3: Question Statistics
                createQuestionStatisticsSheet(workbook, poll, responses)

                // Sheet 4: Missing Answers
                createMissingResponsesSheet(workbook, poll, responses)
                
                return returnByteFile(workbook).also {
                    log.info("Excel export for poll ID: ${poll.id} completed successfully")
                }
            }
        } catch (e: NullPointerException) {
            log.error("NullPointerException occurred while exporting Excel for poll ID: ${poll.id}", e)
        } catch (e: IOException) {
            log.error("IOException occurred while exporting Excel for poll ID: ${poll.id}", e)
        }
        log.warn("Excel export for poll ID: ${poll.id} failed")
        return null
    }


    private fun setFirstRow(excelSheet: ExcelSheet, style: CellStyle, poll: Poll) {
        val excelRow = excelSheet.getRow(0)
        val excelRow1 = excelSheet.getRow(1)


        style.alignment = HorizontalAlignment.CENTER

        var merge = 1
        poll.inputFields?.forEach { question ->
            val answers = question.answers
            if (question.type == BaseType.PollMultiResponseQuestion || question.type == BaseType.PollSingleResponseQuestion) {
                var counter = merge
                question.answers?.forEach { answer ->
                    excelRow1.getCell(counter).setCellValue(answer)
                    excelRow1.getCell(counter).setCellStyle(style)
                    excelSheet.autosize(counter)
                    counter++
                }
                excelRow.getCell(merge).setCellValue(question.question)
                excelSheet.autosize(merge)
                counter--
                answers?.size?.let {
                    if (it >= 2) {
                        excelSheet.addMergeRegion(CellRangeAddress(0, 0, merge, counter))
                    }
                }
                merge = counter
            } else {
                excelRow.getCell(merge).setCellValue(question.question)
                excelRow.setCellStyle(style)
                excelSheet.autosize(merge)
            }
            merge += 1
        }
        excelRow.setHeight(30F)
    }
    
    private fun setAnswerRow(excelSheet: ExcelSheet, poll: Poll, user: User, res: PollResponse?, rowNumber: Int) {
        val excelRow = excelSheet.getRow(rowNumber)
        
        // set username to first cell of row
        excelRow.getCell(0).setCellValue(user.displayName)
        
        var cell = 1
        excelSheet.autosize(0)
        
        var largestAnswer = ""
        
        // each question gets iterated once
        poll.inputFields?.forEachIndexed { _, question ->
            val choices = res?.responses?.find { it.questionUid == question.uid }
            
            // if choices is null skip
            if (choices != null) {

                var index: Int

                // each answer-field oft the question is iterated
                question.answers?.forEachIndexed { ind, answer ->
                    index = question.answers!!.size - 1
                    excelSheet.autosize(cell)

                    when (question.type) {
                        BaseType.PollTextQuestion -> {
                            if (choices?.answers?.isNotEmpty() == true) {
                                excelRow.getCell(cell).setCellValue(choices.answers?.get(0).toString())
                                if (countLines(answer) > countLines(largestAnswer)) {
                                    largestAnswer = answer
                                }
                            }
                            cell++;
                        }

                        BaseType.PollSingleResponseQuestion, BaseType.PollMultiResponseQuestion -> {
                            if (choices?.answers?.get(ind).toString() == "true") {
                                excelRow.getCell(cell).setCellValue("X")
                            }
                            cell++;
                        }

                        else -> {
                            log.error("Unknown BaseType on Poll Excel Export: ${question.type}")
                        }
                    }

                }
            }
        }

        largestAnswer = "i"
        val puffer: String = largestAnswer
        var counterOfBreaking = 0
        var counterOfOverlength = 0

        val pufferSplit: Array<String> =
            puffer.split("\r\n|\r|\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        // check for line-breaks
        for (i in pufferSplit.indices) {
            counterOfBreaking++
            counterOfOverlength += pufferSplit[i].length / 20
        }
        excelRow.setHeight((14 + counterOfOverlength * 14 + counterOfBreaking * 14).toFloat())
    }

    private fun countLines(str: String): Int {
        val lines = str.split("\r\n|\r|\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        return lines.size
    }

    private fun createNewRow(excelSheet: ExcelSheet?, emptyRow: ExcelRow?, anzNewRows: Int) {
        if (excelSheet == null || emptyRow == null) {
            log.error("in createNewRow(...) excelSheet or emptyRow is null")
            return
        }
        for (i in 1 until anzNewRows) {
            Objects.requireNonNull(
                excelSheet.getRow(FIRST_DATA_ROW_NUM)
            ).copyAndInsert(
                emptyRow.sheet
            )
        }
    }

    private fun returnByteFile(workbook: ExcelWorkbook): ByteArray? {
        workbook.use { wb ->
            val byteArrayOutputStream = wb.asByteArrayOutputStream
            return byteArrayOutputStream.toByteArray()
        }
    }

    /**
     * Creates summary sheet with overall statistics
     */
    private fun createSummarySheet(workbook: ExcelWorkbook, poll: Poll, responses: List<PollResponseDO>) {
        val sheet = workbook.createOrGetSheet("Zusammenfassung")

        var currentRow = 0

        val titleRow = sheet.getRow(currentRow++)
        titleRow.getCell(0).setCellValue("Umfrage-Auswertung: ${poll.title}")
        titleRow.setHeight(30F)

        val titleFont = ExcelUtils.createFont(workbook, "titleFont", bold = true, heightInPoints = 16)
        val titleStyle = workbook.createOrGetCellStyle("titleStyle")
        titleStyle.setFont(titleFont)
        titleRow.getCell(0).setCellStyle(titleStyle)

        currentRow++

        val infoHeaderRow = sheet.getRow(currentRow++)
        infoHeaderRow.getCell(0).setCellValue("Allgemeine Informationen")
        val headerFont = ExcelUtils.createFont(workbook, "headerFont", bold = true, heightInPoints = 12)
        val headerStyle = workbook.createOrGetCellStyle("headerStyle")
        headerStyle.setFont(headerFont)
        infoHeaderRow.getCell(0).setCellStyle(headerStyle)

        sheet.getRow(currentRow).getCell(0).setCellValue("Beschreibung:")
        sheet.getRow(currentRow++).getCell(1).setCellValue(poll.description ?: "")

        sheet.getRow(currentRow).getCell(0).setCellValue("Deadline:")
        sheet.getRow(currentRow++).getCell(1).setCellValue(poll.deadline.toString())

        sheet.getRow(currentRow).getCell(0).setCellValue("Ersteller:")
        sheet.getRow(currentRow++).getCell(1).setCellValue(poll.owner?.displayName ?: "")

        sheet.getRow(currentRow).getCell(0).setCellValue("Ort:")
        sheet.getRow(currentRow++).getCell(1).setCellValue(poll.location ?: "")

        currentRow++

        val totalAttendees = poll.attendees?.size ?: 0
        val totalResponses = responses.size
        val responseRate = if (totalAttendees > 0) {
            (totalResponses.toDouble() / totalAttendees * 100).toInt()
        } else {
            0
        }

        val statsHeaderRow = sheet.getRow(currentRow++)
        statsHeaderRow.getCell(0).setCellValue("Statistiken")
        statsHeaderRow.getCell(0).setCellStyle(headerStyle)

        val tableHeaderFont = ExcelUtils.createFont(workbook, "tableHeaderFont", bold = true)
        val tableHeaderStyle = workbook.createOrGetCellStyle("tableHeaderStyle")
        tableHeaderStyle.fillForegroundColor = IndexedColors.LIGHT_BLUE.index
        tableHeaderStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        tableHeaderStyle.setFont(tableHeaderFont)

        val statsTableHeaderRow = sheet.getRow(currentRow++)
        statsTableHeaderRow.getCell(0).setCellValue("Metrik")
        statsTableHeaderRow.getCell(1).setCellValue("Wert")
        statsTableHeaderRow.getCell(0).setCellStyle(tableHeaderStyle)
        statsTableHeaderRow.getCell(1).setCellStyle(tableHeaderStyle)

        sheet.getRow(currentRow).getCell(0).setCellValue("Gesamte Teilnehmer")
        sheet.getRow(currentRow++).getCell(1).setCellValue(totalAttendees.toDouble())

        sheet.getRow(currentRow).getCell(0).setCellValue("Erhaltene Antworten")
        sheet.getRow(currentRow++).getCell(1).setCellValue(totalResponses.toDouble())

        sheet.getRow(currentRow).getCell(0).setCellValue("Fehlende Antworten")
        sheet.getRow(currentRow++).getCell(1).setCellValue((totalAttendees - totalResponses).toDouble())

        sheet.getRow(currentRow).getCell(0).setCellValue("Rücklaufquote")
        sheet.getRow(currentRow++).getCell(1).setCellValue("$responseRate%")

        currentRow++

        val questionsHeaderRow = sheet.getRow(currentRow++)
        questionsHeaderRow.getCell(0).setCellValue("Fragen-Auswertung")
        questionsHeaderRow.getCell(0).setCellStyle(headerStyle)

        poll.inputFields?.forEach { question ->
            currentRow++
            val questionRow = sheet.getRow(currentRow++)
            questionRow.getCell(0).setCellValue(question.question ?: "")
            val questionFont = ExcelUtils.createFont(workbook, "questionFont", bold = true)
            val questionStyle = workbook.createOrGetCellStyle("questionStyle")
            questionStyle.setFont(questionFont)
            questionRow.getCell(0).setCellStyle(questionStyle)

            when (question.type) {
                BaseType.PollSingleResponseQuestion, BaseType.PollMultiResponseQuestion -> {
                    val answerCounts = calculateAnswerDistribution(question, responses)

                    val answerHeaderRow = sheet.getRow(currentRow++)
                    answerHeaderRow.getCell(1).setCellValue("Antwort")
                    answerHeaderRow.getCell(2).setCellValue("Anzahl")
                    answerHeaderRow.getCell(3).setCellValue("Prozent")
                    answerHeaderRow.getCell(1).setCellStyle(tableHeaderStyle)
                    answerHeaderRow.getCell(2).setCellStyle(tableHeaderStyle)
                    answerHeaderRow.getCell(3).setCellStyle(tableHeaderStyle)

                    answerCounts.forEach { (answer, count) ->
                        val row = sheet.getRow(currentRow++)
                        row.getCell(1).setCellValue(answer)
                        row.getCell(2).setCellValue(count.toDouble())
                        val percentage = if (totalResponses > 0) {
                            (count.toDouble() / totalResponses * 100).toInt()
                        } else {
                            0
                        }
                        row.getCell(3).setCellValue("$percentage%")
                    }
                }
                BaseType.PollTextQuestion -> {
                    sheet.getRow(currentRow++).getCell(1).setCellValue("Freitextantworten (siehe Detail-Sheet)")
                }
                else -> {
                    sheet.getRow(currentRow++).getCell(1).setCellValue("Unbekannter Fragetyp")
                }
            }
        }

        for (i in 0..5) {
            sheet.autosize(i)
        }
    }

    /**
     * Creates detailed responses sheet (improved version of original)
     */
    private fun createDetailedResponsesSheet(workbook: ExcelWorkbook, poll: Poll, responses: List<PollResponseDO>) {
        val excelSheet = workbook.createOrGetSheet("Detaillierte Antworten")
        val style = workbook.createOrGetCellStyle()
        excelSheet.autosize(0)
        val emptyRow = excelSheet.getRow(0)
        var anzNewRows = 2

        anzNewRows += (poll.attendees?.size ?: 0)

        createNewRow(excelSheet, emptyRow, anzNewRows)
        setFirstRow(excelSheet, style, poll)

        excelSheet.poiSheet.createFreezePane(1, 2)

        poll.attendees?.sortedBy { it.displayName }
        poll.attendees?.forEachIndexed { index, user ->
            val res = PollResponse()
            responses.find { it.owner?.id == user.id }?.let { res.copyFrom(it) }
            setAnswerRow(excelSheet, poll, user, res, index + FIRST_DATA_ROW_NUM)
        }

        val fullAccessUser = poll.fullAccessUsers?.toMutableList() ?: mutableListOf()
        val accessGroupIds = poll.fullAccessGroups?.filter { it.id != null }?.map { it.id!! }?.toLongArray()
        val accessUserIds = UserService().getUserIds(groupService.getGroupUsers(accessGroupIds))
        val accessUsers = User.toUserList(accessUserIds)
        User.restoreDisplayNames(accessUsers)

        accessUsers?.forEach { user ->
            if (fullAccessUser.none { it.id == user.id }) {
                fullAccessUser.add(user)
            }
        }

        val owner = User.getUser(poll.owner?.id, false)
        if (owner != null) {
            fullAccessUser.add(owner)
        }

        User.restoreDisplayNames(fullAccessUser)
        fullAccessUser.forEachIndexed { _, user ->
            val number = (anzNewRows)
            if (poll.attendees?.map { it.id }?.contains(user.id) == false) {
                val res = PollResponse()
                responses.find { it.owner?.id == user.id }?.let { res.copyFrom(it) }
                if (res.id != null) {
                    setAnswerRow(excelSheet, poll, user, res, number)
                }
            }
        }
    }

    /**
     * Creates question statistics sheet
     */
    private fun createQuestionStatisticsSheet(workbook: ExcelWorkbook, poll: Poll, responses: List<PollResponseDO>) {
        val sheet = workbook.createOrGetSheet("Fragen-Statistiken")

        var currentRow = 0

        val headerRow = sheet.getRow(currentRow++)
        headerRow.getCell(0).setCellValue("Frage")
        headerRow.getCell(1).setCellValue("Typ")
        headerRow.getCell(2).setCellValue("Antworten")
        headerRow.getCell(3).setCellValue("Leer")
        headerRow.getCell(4).setCellValue("Antwortrate")

        val headerFont = ExcelUtils.createFont(workbook, "statsHeaderFont", bold = true)
        val headerStyle = workbook.createOrGetCellStyle("statsHeaderStyle")
        headerStyle.fillForegroundColor = IndexedColors.LIGHT_BLUE.index
        headerStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        headerStyle.setFont(headerFont)

        for (i in 0..4) {
            headerRow.getCell(i).setCellStyle(headerStyle)
        }

        poll.inputFields?.forEach { question ->
            val row = sheet.getRow(currentRow++)
            row.getCell(0).setCellValue(question.question ?: "")
            row.getCell(1).setCellValue(question.type.toString())

            val answeredCount = responses.count { response ->
                val pollResponse = PollResponse()
                pollResponse.copyFrom(response)
                pollResponse.responses?.any { it.questionUid == question.uid } == true
            }

            row.getCell(2).setCellValue(answeredCount.toDouble())
            row.getCell(3).setCellValue((responses.size - answeredCount).toDouble())

            val answerRate = if (responses.isNotEmpty()) {
                (answeredCount.toDouble() / responses.size * 100).toInt()
            } else {
                0
            }
            row.getCell(4).setCellValue("$answerRate%")
        }

        for (i in 0..4) {
            sheet.autosize(i)
        }
    }

    /**
     * Creates missing responses sheet
     */
    private fun createMissingResponsesSheet(workbook: ExcelWorkbook, poll: Poll, responses: List<PollResponseDO>) {
        val sheet = workbook.createOrGetSheet("Fehlende Antworten")

        var currentRow = 0

        val headerRow = sheet.getRow(currentRow++)
        headerRow.getCell(0).setCellValue("Name")
        headerRow.getCell(1).setCellValue("E-Mail")
        headerRow.getCell(2).setCellValue("Status")

        val headerFont = ExcelUtils.createFont(workbook, "missingHeaderFont", bold = true, color = IndexedColors.WHITE.index)
        val headerStyle = workbook.createOrGetCellStyle("missingHeaderStyle")
        headerStyle.fillForegroundColor = IndexedColors.RED.index
        headerStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        headerStyle.setFont(headerFont)

        for (i in 0..2) {
            headerRow.getCell(i).setCellStyle(headerStyle)
        }

        val respondedUserIds = responses.map { it.owner?.id }.toSet()

        val missingStyle = workbook.createOrGetCellStyle()
        missingStyle.fillForegroundColor = IndexedColors.LIGHT_ORANGE.index
        missingStyle.fillPattern = FillPatternType.SOLID_FOREGROUND

        poll.attendees?.sortedBy { it.displayName }?.forEach { attendee ->
            if (!respondedUserIds.contains(attendee.id)) {
                val row = sheet.getRow(currentRow++)
                row.getCell(0).setCellValue(attendee.displayName ?: "")
                row.getCell(1).setCellValue(attendee.email ?: "")
                row.getCell(2).setCellValue("Keine Antwort")

                for (i in 0..2) {
                    row.getCell(i).setCellStyle(missingStyle)
                }
            }
        }

        for (i in 0..2) {
            sheet.autosize(i)
        }
    }

    /**
     * Calculates answer distribution for single/multi choice questions
     */
    private fun calculateAnswerDistribution(question: org.projectforge.rest.poll.types.Question, responses: List<PollResponseDO>): Map<String, Int> {
        val answerCounts = mutableMapOf<String, Int>()
        question.answers?.forEach { answer -> answerCounts[answer] = 0 }

        responses.forEach { response ->
            val pollResponse = PollResponse()
            pollResponse.copyFrom(response)

            pollResponse.responses?.find { it.questionUid == question.uid }?.let { answer ->
                answer.answers?.forEachIndexed { index, selected ->
                    if (selected.toString() == "true" && index < question.answers!!.size) {
                        val answerText = question.answers!![index]
                        answerCounts[answerText] = answerCounts.getOrDefault(answerText, 0) + 1
                    }
                }
            }
        }

        return answerCounts
    }
}
