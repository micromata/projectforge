/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.i18n

import de.micromata.merlin.excel.ExcelWorkbook
import org.apache.poi.ss.usermodel.IndexedColors
import org.projectforge.framework.i18n.I18nKeysUsageInterface
import org.springframework.stereotype.Service
import java.io.File


fun main() {
  I18nKeysUsage(I18nKeysUsage.RUN_MODE.CREATE)
}

/**
 * Tries to get all used i18n keys from the sources (java and html). As result a file is written which will be checked
 * by AdminAction.checkI18nProperties. Unused i18n keys should be detected.
 *
 * @param runMode If CREATE, the i18n keys will be analyzed and the i18n keys file will be created (only possible if
 * sources are available). Otherwise, the i18n keys will be load from json file. If runMode is FILESYSTEM the i18n keys
 * will be load from the filesystem (source dir) instead of the classpath (in production mode).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class I18nKeysUsage(runmode: RUN_MODE? = null, useTmpFile: Boolean = false) : I18nKeysUsageInterface {
  internal val i18nKeyMap: Map<String, I18nKeyUsageEntry>
  enum class RUN_MODE { CREATE, FILESYSTEM }

  private val orderedEntries: List<I18nKeyUsageEntry>
    get() = getOrderedEntries(i18nKeyMap.values)

  init {
    if (runmode == RUN_MODE.CREATE) {
      i18nKeyMap = I18nKeysSourceAnalyzer().run(useTmpFile)
    } else {
      i18nKeyMap = I18nKeysSourceAnalyzer.readJson(runmode == RUN_MODE.FILESYSTEM, useTmpFile)
    }
  }

  internal fun writeExcelFile() {
    val excelFile = createExcelFile()
    val file = File(excelFile.filename)
    file.writeBytes(excelFile.bytes)
  }

  override fun createExcelFile(): I18nKeysUsageInterface.ExcelFile {
    val workbook = ExcelWorkbook.createEmptyWorkbook()
    val boldFont = workbook.createOrGetFont("bold", bold = true)
    val boldStyle = workbook.createOrGetCellStyle("hr", font = boldFont)
    val redFont = workbook.createOrGetFont("error-font")
    redFont.fontName = "Arial"
    redFont.color = IndexedColors.RED.index
    val redStyle = workbook.createOrGetCellStyle("red", redFont)
    redStyle.fillForegroundColor
    val sheet = workbook.createOrGetSheet("I18n keys")
    sheet.registerColumn("I18n key|60", "i18nKey")
    sheet.registerColumn("English|60", "translation")
    sheet.registerColumn("German|60", "translationDE")
    sheet.registerColumn("Bundle|bundleName|60")
    sheet.registerColumn("Classes|60")
    sheet.registerColumn("Files|60")
    sheet.createRow().fillHeadRow().setCellStyle(boldStyle)
    orderedEntries.forEach { entry ->
      val row = sheet.createRow()
      row.autoFillFromObject(entry)
      if (entry.usedInClasses.isEmpty() && entry.usedInFiles.isEmpty()) {
        row.setCellStyle(redStyle)
      }
    }
    sheet.setAutoFilter()
    val ba: ByteArray
    workbook.asByteArrayOutputStream.use { baos ->
      ba = baos.toByteArray()
    }
    return I18nKeysUsageInterface.ExcelFile(ba, "i18nKeys.xlsx")
  }

  companion object {
    internal fun getOrderedEntries(i18nKeyEntries: Collection<I18nKeyUsageEntry>): List<I18nKeyUsageEntry> {
      return i18nKeyEntries.sortedWith(Comparator { o1, o2 ->
        val compare = (o1.bundleName ?: "ZZZ").compareTo(o2.bundleName ?: "ZZZ")
        if (compare != 0) {
          return@Comparator compare
        }
        return@Comparator o1.i18nKey.compareTo(o2.i18nKey)
      })
    }
  }
}
