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

package org.projectforge.i18n

import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.common.EmphasizedLogSupport

private val log = KotlinLogging.logger {}

class I18nKeysUsageTest {
  /**
   * Creates the i18n key usage file on each test run (should be committed to Git after modifications).
   * This file is used for download Excel-file with i18 translations and usages from running ProjectForge-App.
   */
  @Test
  fun createI18nKeysUsageFile() {
    val usage = I18nKeysUsage(
      I18nKeysUsage.RUN_MODE.CREATE,
      useTmpFile = true
    ) // Creates all i18n usages by analyzing sources and writes it to json file.
    val readUsage = I18nKeysUsage(I18nKeysUsage.RUN_MODE.FILESYSTEM, useTmpFile = true) // Loads all i18n usages from json file generated above.
    Assertions.assertEquals(
      usage.i18nKeyMap.size,
      readUsage.i18nKeyMap.size,
      "Size of analyze run should be the same after read from json."
    )
    usage.i18nKeyMap.values.forEach { entry ->
      val readEntry = readUsage.i18nKeyMap[entry.i18nKey]
      Assertions.assertNotNull(readEntry, "Entry should be load from json.")
      Assertions.assertEquals(entry.files, readEntry!!.files)
    }
  }

  @Test
  fun i18nKeysUpToDateTest() {
    val usage = I18nKeysUsage(
      I18nKeysUsage.RUN_MODE.CREATE,
      useTmpFile = true
    ) // Creates all i18n usages by analyzing sources and writes it to json file.
    val readUsage = I18nKeysUsage(useTmpFile = false) // Loads all i18n usages from json file of repository.
    if (usage.i18nKeyMap.size != readUsage.i18nKeyMap.size) {
      showErrorMessage(
        "Size of number of entries of analyze run differs in i18nKeys.json.",
        "analyzed=${usage.i18nKeyMap.size} entries",
        "file=${readUsage.i18nKeyMap.size} entries"
      )
      showErrorMessage("Size of analyze run should be the same of checked in one: ${I18nKeysSourceAnalyzer.jsonResourceFile.absolutePath}")
      return
    }
    usage.i18nKeyMap.values.forEach { entry ->
      val readEntry = readUsage.i18nKeyMap[entry.i18nKey]
      if (readEntry == null) {
        showErrorMessage("Entry '${entry.i18nKey}' found in source but not in i18nKeys.json.")
        return
      }
      if (entry.files != readEntry.files) {
        showErrorMessage(
          "List of files for entry '${entry.i18nKey}' differs in i18nKeys.json.",
          "analyzed='${entry.files}'",
          "file='${readEntry.files}'"
        )
        return
      }
      if (entry.classes != readEntry.classes) {
        showErrorMessage(
          "List of classes for entry '${entry.i18nKey}' differs in i18nKeys.json.",
          "analyzed='${entry.classes}'",
          "file='${readEntry.classes}'"
        )
        return
      }
    }
  }

  private fun showErrorMessage(vararg caused: String) {
    val messages = mutableListOf("File '${I18nKeysSourceAnalyzer.jsonResourceFile.absolutePath}' not up to date.")
    messages.addAll(caused)
    messages.add("Please re-run I18nKeysUsage.main or do:")
    messages.add("cp ${I18nKeysSourceAnalyzer.jsonTmpFile.absolutePath} ${I18nKeysSourceAnalyzer.jsonResourceFile.absolutePath}")
    val emphasizedLogSupport = EmphasizedLogSupport(
      log,
      EmphasizedLogSupport.Priority.VERY_IMPORTANT
    ).setLogLevel(EmphasizedLogSupport.LogLevel.ERROR)
    messages.forEach {
      emphasizedLogSupport.log(it)
    }
    emphasizedLogSupport.logEnd()
    messages.forEach {
      println("**** $it")
    }
  }
}
