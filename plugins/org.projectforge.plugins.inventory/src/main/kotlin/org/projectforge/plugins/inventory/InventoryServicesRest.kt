/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.inventory

import de.micromata.merlin.excel.ExcelCellType
import de.micromata.merlin.excel.ExcelWorkbook
import mu.KotlinLogging
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateHelper
import org.projectforge.rest.config.Rest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * For uploading address immages.
 */
@RestController
@RequestMapping(InventoryServicesRest.REST_PATH)
class InventoryServicesRest {

  enum class ExcelCol(val header: String, val width: Int) {
    ITEM("plugins.inventory.item", 10),
    OWNER("plugins.inventory.owner", 50),
    EXTERNAL_OWNER("plugins.inventory.externalOwner", 50),
    COMMENT("comment", 50),
  }

  @Autowired
  private lateinit var inventoryItemDao: InventoryItemDao

  /**
   * Exports favorites addresses.
   */
  @GetMapping(REST_EXCEL_SUB_PATH)
  fun exportFavoritesExcel(): ResponseEntity<Any> {
    log.info("Exporting inventory as Excel file.")

    ExcelWorkbook.createEmptyWorkbook(ThreadLocalUserContext.getLocale()).use { workbook ->
      val sheet = workbook.createOrGetSheet(translate("plugins.inventory.title.list"))
      val boldFont = workbook.createOrGetFont("bold", bold = true)
      val boldStyle = workbook.createOrGetCellStyle("hr", font = boldFont)
      //val decimalStyle = workbook.createOrGetCellStyle("decimal")
      //decimalStyle.dataFormat = workbook.createDataFormat().getFormat("0.0")
      val headRow = sheet.createRow()
      ExcelCol.values().forEach {
        headRow.getCell(it.ordinal, ExcelCellType.STRING)
          .setCellValue(translate(it.header))
          .setCellStyle(boldStyle)
        sheet.setColumnWidth(it.ordinal, it.width * 256)
      }
      sheet.setAutoFilter()
      /* TODO later
      val ownSkills = inventoryEntryDao.getSkills(ThreadLocalUserContext.getUser())
      skillStatisticsCache.statistics.forEach { stats ->
        val row = sheet.createRow()
        if (ownSkills.any { it.normalizedSkill == InventoryEntryDO.getNormalizedSkill(stats.skill) }) {
          row.getCell(ExcelCol.MY_SKILL.ordinal, ExcelCellType.STRING).setCellValue("*")
        }
        row.getCell(ExcelCol.SKILL.ordinal, ExcelCellType.STRING).setCellValue(stats.skill)
        row.getCell(ExcelCol.COUNTER.ordinal, ExcelCellType.INT).setCellValue(stats.totalCounter)
        row.getCell(ExcelCol.RATING_MEAN.ordinal, ExcelCellType.DOUBLE)
          .setCellValue(stats.ratingMean)
          .setCellStyle(decimalStyle)
        row.getCell(ExcelCol.INTEREST_MEAN.ordinal, ExcelCellType.DOUBLE)
          .setCellValue(stats.interestsMean)
          .setCellStyle(decimalStyle)
      }
       */
      val filename = ("Inventory_${DateHelper.getDateAsFilenameSuffix(Date())}.xlsx")
      val resource = ByteArrayResource(workbook.asByteArrayOutputStream.toByteArray())
      return ResponseEntity.ok()
        .contentType(org.springframework.http.MediaType.parseMediaType("application/octet-stream"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$filename")
        .body(resource)
    }
  }

  companion object {
    const val REST_PATH = "${Rest.URL}/inventory"
    private const val REST_EXCEL_SUB_PATH = "exportExcel"
    const val REST_EXCEL_EXPORT_PATH = "$REST_PATH/$REST_EXCEL_SUB_PATH"
  }
}
