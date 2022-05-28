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

package org.projectforge.i18n

import de.micromata.merlin.excel.ExcelWorkbook
import org.projectforge.framework.i18n.I18nKeysUsageInterface
import org.springframework.stereotype.Service
import java.io.File


fun main() {
  I18nKeysUsage(true).writeExcelFile()
}

/**
 * Tries to get all used i18n keys from the sources (java and html). As result a file is written which will be checked
 * by AdminAction.checkI18nProperties. Unused i18n keys should be detected.
 *
 * @param create If true, the i18n keys will be analyzed and the i18n keys file will be created (only possible if
 * sources are available)
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
class I18nKeysUsage(create: Boolean = false) : I18nKeysUsageInterface {
  private val i18nKeyMap: Map<String, I18nKeyUsageEntry>

  private val orderedEntries: List<I18nKeyUsageEntry>
    get() = getOrderedEntries(i18nKeyMap.values)

  init {
    if (create) {
      i18nKeyMap = I18nKeysSourceAnalyzer().run()
    } else {
      // TODO
      i18nKeyMap = I18nKeysSourceAnalyzer.readJson()
    }
  }

  internal fun writeExcelFile() {
    val excelFile = createExcelFile()
    val file = File(excelFile.filename)
    file.writeBytes(excelFile.bytes)
  }

  override fun createExcelFile(): I18nKeysUsageInterface.ExcelFile {
    val workbook = ExcelWorkbook.createEmptyWorkbook()
    val sheet = workbook.createOrGetSheet("I18n keys")
    sheet.registerColumn("I18n key|60", "i18nKey")
    sheet.registerColumn("English|60", "translation")
    sheet.registerColumn("German|60", "translationDE")
    sheet.registerColumn("Bundle|bundleName|60")
    sheet.registerColumn("Classes|60")
    sheet.registerColumn("Files|60")
    sheet.createRow().fillHeadRow()
    orderedEntries.forEach { entry ->
      val row = sheet.createRow()
      row.autoFillFromObject(entry)
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
        val compare = (o1.bundleName ?: "ZZZ").compareTo(o2.bundleName ?: "ZZZx")
        if (compare != 0) {
          return@Comparator compare
        }
        return@Comparator o1.i18nKey.compareTo(o2.i18nKey)
      })
    }
  }
}
