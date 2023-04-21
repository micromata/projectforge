package org.projectforge.rest.poll.Exel

import de.micromata.merlin.excel.ExcelRow
import de.micromata.merlin.excel.ExcelSheet
import de.micromata.merlin.excel.ExcelWorkbook
import org.projectforge.business.poll.PollDao
import org.projectforge.business.poll.PollResponseDao
import org.projectforge.rest.dto.User
import org.projectforge.rest.poll.Poll
import org.projectforge.rest.poll.PollResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.IOException
import java.util.*

@Service
class ExcelExport {

    private val log: Logger = LoggerFactory.getLogger(ExcelExport::class.java)

    private val FIRST_DATA_ROW_NUM = 1

    @Autowired
    private lateinit var pollResponseDao: PollResponseDao
    @Autowired
    private lateinit var pollDao: PollDao


    fun getExcel(poll: Poll): ByteArray? {
        val responses = pollResponseDao.internalLoadAll().filter { it.poll?.id == poll.id }

        val classPathResource = ClassPathResource("officeTemplates/PollResultTemplate" + ".xlsx")

        try {
            ExcelWorkbook(classPathResource.inputStream, classPathResource.file.name).use { workbook ->
                val excelSheet = workbook.getSheet(0)
                excelSheet.autosize(0)
                val emptyRow = excelSheet.getRow(0)
                var anzNewRows = 1

                anzNewRows += (poll.attendees?.size ?: 0)

                createNewRow(excelSheet, emptyRow, anzNewRows)
                setFirstRow(excelSheet, poll)
                poll.attendees?.sortedBy { it.displayName }
                poll.attendees?.forEachIndexed { index, user ->
                    val res = PollResponse()
                    responses.find { it.owner?.id == user.id }?.let { res.copyFrom(it) }
                    setNewRows(excelSheet,poll, user, res, index)
                }

                return returnByteFile(excelSheet)
            }
        } catch (e: NullPointerException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun setFirstRow(excelSheet: ExcelSheet, poll: Poll){
        val excelRow = excelSheet.getRow(0)
        poll.inputFields?.forEachIndexed{i, question ->
            excelRow.getCell(i+1).setCellValue(question.question)
        }
        excelRow.setHeight(20F)
    }
    private fun setNewRows(excelSheet: ExcelSheet, poll:Poll, user: User, res:PollResponse?, index: Int) {



        val excelRow = excelSheet.getRow(FIRST_DATA_ROW_NUM + index)

        excelRow.getCell(0).setCellValue(user.displayName)

        poll.inputFields?.forEachIndexed{i, question ->
            val answer = res?.responses?.find { it.uid == question.uid }
            excelRow.getCell(i+1).setCellValue(answer?.answers.toString())
        }

        excelRow.setHeight(20F)
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
    private fun returnByteFile(excelSheet: ExcelSheet): ByteArray? {
        excelSheet.excelWorkbook.use { workbook ->
            val byteArrayOutputStream = workbook.asByteArrayOutputStream
            return byteArrayOutputStream.toByteArray()
        }
    }
}