package org.projectforge.rest.poll.Exel

import de.micromata.merlin.excel.ExcelRow
import de.micromata.merlin.excel.ExcelSheet
import de.micromata.merlin.excel.ExcelWorkbook
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.util.CellRangeAddress
import org.projectforge.business.poll.PollDao
import org.projectforge.business.poll.PollResponseDao
import org.projectforge.rest.dto.User
import org.projectforge.rest.poll.Poll
import org.projectforge.rest.poll.PollResponse
import org.projectforge.rest.poll.types.BaseType
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

    private val FIRST_DATA_ROW_NUM = 2

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
                val style = workbook.createOrGetCellStyle()
                excelSheet.autosize(0)
                val emptyRow = excelSheet.getRow(0)
                var anzNewRows = 2

                anzNewRows += (poll.attendees?.size ?: 0)

                createNewRow(excelSheet, emptyRow, anzNewRows)
                setFirstRow(excelSheet, style, poll)
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

    private fun setFirstRow(excelSheet: ExcelSheet,style: CellStyle,poll: Poll){
        val excelRow = excelSheet.getRow(0)
        val excelRow1 = excelSheet.getRow(1)
        var counter = 0

        style.alignment = HorizontalAlignment.CENTER

        var merge = 0
        poll.inputFields?.forEach{question ->
            merge += 1
            if(question.type==BaseType.MultiResponseQuestion ||
                question.type==BaseType.SingleResponseQuestion ||
                question.type==BaseType.DateQuestion) {
                question.answers?.forEach { answer ->
                    counter++
                    excelRow1.getCell(counter).setCellValue(answer)
                    excelRow1.getCell(counter).setCellStyle(style)
                    excelSheet.autosize(counter)
                }
                excelRow.getCell(merge).setCellValue(question.question)
                merge += question.answers?.size ?: 0
                //excelSheet.addMergeRegion(CellRangeAddress(0,0,merge,merge + counter))
                excelSheet.autosize(merge)

            }
            else {
                excelRow.getCell(merge).setCellValue(question.question)
                excelRow.setCellStyle(style)
                excelSheet.autosize(merge)
            }
        }
        excelRow.setHeight(30F)
    }
    private fun setNewRows(excelSheet: ExcelSheet, poll:Poll, user: User, res:PollResponse?, index: Int) {

        val excelRow = excelSheet.getRow(FIRST_DATA_ROW_NUM + index)


        excelRow.getCell(0).setCellValue(user.displayName)
        excelSheet.autosize(0)
        var chell=0
        poll.inputFields?.forEachIndexed{i, question ->
            val answer = res?.responses?.find { it.questionUid == question.uid }

            answer?.answers?.forEachIndexed { ind, antwort ->
                chell++
                if (question.type == BaseType.SingleResponseQuestion || question.type == BaseType.MultiResponseQuestion){
                    if(antwort is Boolean && antwort == true){
                        excelRow.getCell(chell).setCellValue("X")
                    }
                }
                else {
                    excelRow.getCell(chell).setCellValue(antwort.toString())
                }
                excelSheet.autosize(chell)
            }
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