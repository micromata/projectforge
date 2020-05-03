/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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
    fun purgeTest() {
        val tmpDir = createTempDir("BackupFilesPurginTest")
        try {
            val baseDate = LocalDate.of(2020, Month.MAY, 3)
            var current = baseDate.minusMonths(5)
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val basename = "projectforge"
            val basename2 = "database"
            while (current.isBefore(baseDate)) {
                File(tmpDir, "$basename-${dateFormatter.format(current)}_01-23_backupfile.zip").createNewFile()
                File(tmpDir, "$basename2-${dateFormatter.format(current)}_01-23_backupfile.zip").createNewFile()
                current = current.plusDays(1)
            }
            // Create oldest ones:
            File(tmpDir, "$basename-${dateFormatter.format(baseDate.minusYears(1))}_01-23_first_backupfile.zip").createNewFile()
            File(tmpDir, "$basename2-${dateFormatter.format(baseDate.minusYears(1))}_01-23_first_backupfile.zip").createNewFile()

            var files = tmpDir.listFiles()
            Assertions.assertEquals(306, files.size)
            Assertions.assertTrue(files.any { it.name == "$basename-2020-01-16_01-23_backupfile.zip" })

            BackupFilesPurging.purgeDirectory(tmpDir, baseDate = baseDate, filePrefix = "other-backup")
            files = tmpDir.listFiles()
            Assertions.assertEquals(306, files.size, "Nothing purged, because prefix didn't match.")

            BackupFilesPurging.purgeDirectory(tmpDir, baseDate = baseDate, filePrefix = basename)
            files = tmpDir.listFiles()
            Assertions.assertTrue(files.any { it.name == "$basename-2019-05-03_01-23_first_backupfile.zip" }, "Keep the oldest one.")
            Assertions.assertTrue(files.any { it.name == "$basename-2019-12-03_01-23_backupfile.zip" }, "Keep the first file of the month.")
            Assertions.assertTrue(files.any { it.name == "$basename-2020-04-03_01-23_backupfile.zip" })
            Assertions.assertFalse(files.any { it.name == "$basename-2020-04-02_01-23_backupfile.zip" })
            Assertions.assertTrue(files.any { it.name == "$basename-2020-04-01_01-23_backupfile.zip" })
            Assertions.assertTrue(files.any { it.name == "$basename-2020-03-01_01-23_backupfile.zip" })
            Assertions.assertTrue(files.any { it.name == "$basename-2020-02-01_01-23_backupfile.zip" })
            Assertions.assertEquals(189, files.size)

            BackupFilesPurging.purgeDirectory(tmpDir, baseDate = baseDate)
            files = tmpDir.listFiles()
            Assertions.assertEquals(72, files.size)

            /*
            files.sorted().forEach {
                println(it.absolutePath)
            }
            */
        } finally {
            tmpDir.deleteRecursively()
        }
    }
}
