/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.multiselect

import de.micromata.merlin.excel.ExcelWorkbook
import mu.KotlinLogging
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.IdObject
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.PFDateTime
import org.springframework.core.io.ByteArrayResource
import java.io.File

private val log = KotlinLogging.logger {}

/**
 * Exports the results of mass updates
 */
object MultiSelectionExcelExport {
  fun <T : IdObject<out java.io.Serializable>> export(massUpdateContext: MassUpdateContext<T>) {
    log.info("Exporting results of mass update as Excel file.")
    ExcelWorkbook.createEmptyWorkbook(ThreadLocalUserContext.getLocale()).use { workbook ->
      val sheet = workbook.createOrGetSheet(translate("massUpdate.result.excel.title"))
      val boldFont = workbook.createOrGetFont("bold", bold = true)
      val decimalStyle = workbook.createOrGetCellStyle("decimal")
      decimalStyle.dataFormat = workbook.createDataFormat().getFormat("0.0")
      val firstRow = sheet.createRow() // First row
      sheet.registerColumns("Id|11", "Element|30")
      for (i in 0..1) {
        firstRow.createCell() // 2 empty cells.
      }
      massUpdateContext.massUpdateData.forEach { (field, param) ->
        val colNumber = firstRow.createCell().setCellValue(field).colNumber
        firstRow.createCell()
        firstRow.addMergeRegion(colNumber, colNumber + 1)
        sheet.registerColumns("old", "new")
      }
      sheet.createRow().fillHeadRow(sheet.createOrGetCellStyle("headStyle"))
      sheet.setAutoFilter()
      massUpdateContext.massUpdateObjects.forEach { massUpdateObject ->
        val row = sheet.createRow()
        row.createCell().setCellValue(massUpdateObject.id)
        row.createCell().setCellValue(massUpdateObject.identifier)
        massUpdateObject.fieldModifications.forEach { (field, modification) ->
          val oldValueCell = row.createCell()
          val newValueCell = row.createCell()
          if (modification.oldValue != modification.newValue) {
            oldValueCell.setCellValue(modification.oldValue)
            newValueCell.setCellValue(modification.newValue)
          }
        }
      }
      val filename = ("MassUpdate_${PFDateTime.now().iso4FilenamesFormatterMinutes}.xlsx")
      val resource = workbook.asByteArrayOutputStream.toByteArray()
      val file = File(filename)
      log.info { "Writing ${file.absoluteFile}" }
      file.writeBytes(resource)
    }
  }

  /*
    fun download() {
      scriptExecution.getDownloadFile(request)?.let { download ->
        script.scriptDownload = Script.ScriptDownload(download.filename, download.sizeHumanReadable)
        val availableUntil = translateMsg("scripting.download.filename.additional", download.availableUntil)
        layout.add(
          UIRow().add(
            UIFieldset(title = "scripting.download.filename.info").add(
              UIReadOnlyField(
                "scriptDownload.filenameAndSize",
                label = "scripting.download.filename",
                additionalLabel = "'$availableUntil",
              )
            )
              .add(
                UIButton.createDownloadButton(
                  responseAction = ResponseAction(
                    url = "${getRestPath()}/download",
                    targetType = TargetType.DOWNLOAD
                  )
                )
              )
          )
        )
      }

    }

    internal fun getDownloadFile(request: HttpServletRequest): ScriptExecution.DownloadFile? {
      return ExpiringSessionAttributes.getAttribute(
        request,
        EXPIRING_SESSION_ATTRIBUTE, ScriptExecution.DownloadFile::class.java
      )
    }

    data class DownloadFile(val filename: String, val bytes: ByteArray, val availableUntil: String) {
      val sizeHumanReadable
        get() = NumberHelper.formatBytes(bytes.size)
    }

    internal fun createDownloadFilename(filename: String?, extension: String): String {
      val suffix = "${DateHelper.getTimestampAsFilenameSuffix(Date())}.$extension"
      return if (filename.isNullOrBlank()) {
        "pf_scriptresult_$suffix"
      } else {
        "${filename.removeSuffix(".$extension")}_$suffix"
      }
    }
  */
  private val EXPIRING_SESSION_ATTRIBUTE = "${this::class.java.name}.downloadResultExcel"
}
