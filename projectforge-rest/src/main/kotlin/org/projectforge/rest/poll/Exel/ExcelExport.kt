package org.projectforge.rest.poll.Exel

import de.micromata.merlin.excel.ExcelRow
import de.micromata.merlin.excel.ExcelSheet
import de.micromata.merlin.excel.ExcelWorkbook
import org.projectforge.rest.poll.Poll
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import java.io.IOException
import java.time.LocalDate
import java.util.*


class ExcelExport {

    private val log: Logger = LoggerFactory.getLogger(ExcelExport::class.java)

    private val FIRST_DATA_ROW_NUM = 5


    fun getExcel(obj: Poll): ByteArray? {
        //var excelSheet: ExcelSheet? = null
        //var emptyRow: ExcelRow? = null

        val classPathResource =  ClassPathResource("officeTemplates/PollResultTemplate" + ".xlsx")


        try {
            ExcelWorkbook(classPathResource.inputStream, classPathResource.file.name).use { workbook ->
                val excelSheet = workbook.getSheet(0)
                val emptyRow = excelSheet.getRow(5)
                val anzNewRows = 3
                //excelSheet.getRow(0).getCell(0).setCellValue(contentOfCell)
                createNewRow(excelSheet, emptyRow, anzNewRows)
                var hourCounter = 0.0
                for (i in 0 until anzNewRows) {
                    hourCounter = setNewRows(hourCounter, excelSheet, i)
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

    private fun setNewRows(hourCounter: Double, excelSheet: ExcelSheet, cell: Int): Double {
        val hourCounter = hourCounter
        val description: String = ""

        val excelRow = excelSheet.getRow(FIRST_DATA_ROW_NUM + cell)

        excelRow.getCell(0).setCellValue("test1")
        excelRow.getCell(1).setCellValue("test2")
        excelRow.getCell(3).setCellValue("test3")
        excelRow.getCell(4).setCellValue("test4")
        excelRow.getCell(5).setCellValue("test5")


        val puffer = description
        var counterOfBreaking = 0
        var counterOfOverlength = 0
        val pufferSplit = puffer.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        // check for line-breaks
        for (i in pufferSplit.indices) {
            counterOfBreaking++
            counterOfOverlength += pufferSplit[i].length / 70
        }
        excelRow.setHeight((14 + counterOfOverlength * 14 + counterOfBreaking * 14).toFloat())
        return hourCounter
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