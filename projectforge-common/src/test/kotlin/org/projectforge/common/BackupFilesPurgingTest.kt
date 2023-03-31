/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.common

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter

class BackupFilesPurgingTest {
  @Test
  fun purgeDailyTest() {
    val tmpDir = createTempDir("BackupFilesPurgingTest")
    try {
      val basename = "projectforge"
      val baseDate = LocalDate.of(2020, Month.MAY, 3)
      createTempFiles(tmpDir, baseDate, basename, "database")
      var files = tmpDir.listFiles()
      Assertions.assertEquals(306, files.size)
      Assertions.assertTrue(files.any { it.name == "$basename-2020-01-16$SUFFIX" })

      BackupFilesPurging.purgeDirectory(tmpDir, baseDate = baseDate, filePrefix = "other-backup")
      files = tmpDir.listFiles()
      Assertions.assertEquals(306, files.size, "Nothing purged, because prefix didn't match.")

      BackupFilesPurging.purgeDirectory(
        tmpDir,
        baseDate = baseDate,
        filePrefix = basename,
        keepDailyBackups = 30,
        keepWeeklyBackups = 0
      )
      files = tmpDir.listFiles()
      Assertions.assertTrue(
        files.any { it.name == "$basename-2019-05-03$SUFFIX" },
        "Keep the oldest one."
      )
      Assertions.assertTrue(
        files.any { it.name == "$basename-2019-12-03$SUFFIX" },
        "Keep the first file of the month."
      )
      // Keep daily until 2020-04-03, wwekly backups until 2020-05-03
      Assertions.assertTrue(files.any { it.name == "$basename-2020-04-03$SUFFIX" })
      Assertions.assertFalse(files.any { it.name == "$basename-2020-04-02$SUFFIX" })
      Assertions.assertTrue(files.any { it.name == "$basename-2020-04-01$SUFFIX" })
      Assertions.assertTrue(files.any { it.name == "$basename-2020-03-01$SUFFIX" })
      Assertions.assertTrue(files.any { it.name == "$basename-2020-02-01$SUFFIX" })
      Assertions.assertEquals(189, files.size)

      BackupFilesPurging.purgeDirectory(tmpDir, baseDate = baseDate, keepDailyBackups = 30, keepWeeklyBackups = 0)
      files = tmpDir.listFiles()
      Assertions.assertEquals(72, files.size)
    } finally {
      tmpDir.deleteRecursively()
    }
  }

  @Test
  fun purgeWeeklyTest() {
    val tmpDir = createTempDir("BackupFilesPurgingWeeklyTest")
    try {
      val basename = "projectforge"
      val baseDate = LocalDate.of(2021, Month.JANUARY, 18)
      createTempFiles(tmpDir,baseDate,  basename)
      BackupFilesPurging.purgeDirectory(tmpDir, baseDate = baseDate, keepDailyBackups = 8, keepWeeklyBackups = 4)
      // Weekly until 2020-12-21
      // Daily until 2021-01-10
      // First days of weeks: 14.12., 21.12., 28.12., 4.01., 11.01.
      val files = tmpDir.listFiles()
      Assertions.assertTrue(files.any { it.name == "$basename-2021-01-10$SUFFIX" })
      Assertions.assertFalse(files.any { it.name == "$basename-2021-01-09$SUFFIX" })
      // First day of month 01.01.
      Assertions.assertFalse(files.any { it.name == "$basename-2021-01-02$SUFFIX" })
      Assertions.assertTrue(files.any { it.name == "$basename-2021-01-01$SUFFIX" })
      Assertions.assertFalse(files.any { it.name == "$basename-2020-12-31$SUFFIX" })
      // First day of week 04.01.
      Assertions.assertFalse(files.any { it.name == "$basename-2021-01-05$SUFFIX" })
      Assertions.assertTrue(files.any { it.name == "$basename-2021-01-04$SUFFIX" })
      Assertions.assertFalse(files.any { it.name == "$basename-2021-01-03$SUFFIX" })
      // First day of week 28.12.
      Assertions.assertFalse(files.any { it.name == "$basename-2020-12-29$SUFFIX" })
      Assertions.assertTrue(files.any { it.name == "$basename-2020-12-28$SUFFIX" })
      Assertions.assertFalse(files.any { it.name == "$basename-2020-12-27$SUFFIX" })
      // First day of week 21.12.
      Assertions.assertFalse(files.any { it.name == "$basename-2020-12-22$SUFFIX" })
      Assertions.assertTrue(files.any { it.name == "$basename-2020-12-21$SUFFIX" })
      Assertions.assertFalse(files.any { it.name == "$basename-2020-12-20$SUFFIX" })
      // First day of week 14.12. (more than 4 weeks ago)
      Assertions.assertFalse(files.any { it.name == "$basename-2020-12-14$SUFFIX" })

      Assertions.assertEquals(18, files.size)
    } finally {
      tmpDir.deleteRecursively()
    }
  }

  private fun createTempFiles(tmpDir: File, baseDate: LocalDate, vararg basenames: String) {
    var current = baseDate.minusMonths(5)
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    while (current.isBefore(baseDate)) {
      basenames.forEach { basename ->
        File(tmpDir, "$basename-${dateFormatter.format(current)}$SUFFIX").createNewFile()
      }
      current = current.plusDays(1)
    }
    // Create oldest ones:
    basenames.forEach { basename ->
      File(
        tmpDir,
        "$basename-${dateFormatter.format(baseDate.minusYears(1))}$SUFFIX"
      ).createNewFile()
    }
  }

  companion object {
    const val SUFFIX = "_01-23_backupfile.zip"
  }
}
