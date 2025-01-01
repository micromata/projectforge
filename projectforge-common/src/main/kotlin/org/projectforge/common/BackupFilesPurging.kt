/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Purges old backup file by keeping e. g. monthly backups and removing daily ones.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object BackupFilesPurging {
    /**
     * @param backupDirectory The backup directory to purge.
     * @param filePrefix Purge only files with this given prefix. If not given, all files containing a date in its filename will be purged.
     * @param keepDailyBackups Keep daily backups [keepDailyBackups] days (default value is 30 days).
     * @param baseDate Optional date as base date (today is the default value).
     * @param dateFormatter The format of the date in the filenames (default value is 'yyyy-MM-dd'.
     * @param dateRegex The regex matching the [dateFormatter] (default value is '\d{4}-\d{2}-\d{2}Ä).
     */
    @JvmStatic
    @JvmOverloads
    fun purgeDirectory(backupDirectory: File,
                       filePrefix: String? = null,
                       /**
                        * Number of days to keep weekly backups.
                        */
                       keepDailyBackups: Long = 8,
                       /**
                        * Number of week to keep weekly backups.
                        */
                       keepWeeklyBackups: Long = 4,
                       /**
                        * baseDate is normally now. Set this only for test cases.
                        */
                       baseDate: LocalDate = LocalDate.now(),
                       dateFormatter: DateTimeFormatter = DATE_FORMATTER,
                       dateRegex: Regex = DATE_REGEX) {
        if (!backupDirectory.exists() || !backupDirectory.isDirectory) {
            log.error { "Can't purge directory ${backupDirectory.absolutePath}, it doesn't exist." }
            return
        }
        val monthsSet = mutableSetOf<Triple<String, Int, Month>>() // File prefix, Year, month of year
        val weeksSet = mutableSetOf<Triple<String, Int, Int>>() // File prefix, Year, week of year
        val keepDailyBackupsUntil = baseDate.minusDays(keepDailyBackups)
        val keepWeeklyBackupsUntil = baseDate.minusDays(keepWeeklyBackups * 7)
        log.info { "Keeping daily backups back until ${DATE_FORMATTER.format(keepDailyBackupsUntil)}, weekly backups until ${DATE_FORMATTER.format(keepWeeklyBackupsUntil)} and keeping monthly backups forever in ${backupDirectory.absolutePath}/${filePrefix ?: ""}*..." }
        var deletedFiles = 0
        var keptFiles = 0
        var totalFiles = 0
        backupDirectory.listFiles()?.sorted()?.let { fileList ->
            for (file in fileList) {
                if (filePrefix != null && !file.name.startsWith(filePrefix)) {
                    continue
                }
                totalFiles++
                val dateString = dateRegex.find(file.name)?.value
                dateString?.let {
                    val prefix = file.name.substringBefore(it)
                    val date = LocalDate.parse(it, dateFormatter)
                    val weekOfYear = Triple(prefix, date.year, date.get(WEEK_FIELDS.weekOfYear()))
                    val monthOfYear = Triple(prefix, date.year, date.month)
                    val firstEntryOfWeek = !weeksSet.contains(weekOfYear)
                    val firstEntryOfMonth = !monthsSet.contains(monthOfYear)
                    weeksSet.add(weekOfYear)
                    monthsSet.add(monthOfYear)
                    if (firstEntryOfMonth) {
                        // Don't remove the first file of each month.
                        keptFiles++
                    } else if (firstEntryOfWeek) {
                        // Remove first entries of week only after keepWeeklyBackupsUntil
                        if (date.isBefore(keepDailyBackupsUntil) && date.isBefore(keepWeeklyBackupsUntil)) {
                            log.info { "Deleting file '${file.absolutePath}'..." }
                            file.delete()
                            deletedFiles++
                        } else {
                            keptFiles++
                        }
                    } else if (date.dayOfMonth > 1) {
                        // Daily backup not at the beginning of a month.
                        if (date.isBefore(keepDailyBackupsUntil)) {
                            log.info { "Deleting file '${file.absolutePath}'..." }
                            file.delete()
                            deletedFiles++
                        } else {
                            keptFiles++
                        }
                    } else {
                        keptFiles++
                    }
                }
            }
        }
        log.info { "Deleted $deletedFiles/$totalFiles files ($keptFiles kept) in ${backupDirectory.absolutePath}'" }
    }

    private val DATE_REGEX = """\d{4}-\d{2}-\d{2}""".toRegex()

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val WEEK_FIELDS = WeekFields.of(DayOfWeek.MONDAY, 1)
}
