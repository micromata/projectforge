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

import mu.KotlinLogging
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
                       keepDailyBackups: Long = 30,
                       baseDate: LocalDate = LocalDate.now(),
                       dateFormatter: DateTimeFormatter = DATE_FORMATTER,
                       dateRegex: Regex = DATE_REGEX) {
        if (!backupDirectory.exists() || !backupDirectory.isDirectory) {
            log.error { "Can't purge directory ${backupDirectory.absolutePath}, it doesn't exist." }
            return
        }
        val keepDailyBackupsUntil = baseDate.minusDays(keepDailyBackups)
        log.info { "Keeping daily backups back until ${DATE_FORMATTER.format(keepDailyBackupsUntil)} and keeping monthly backups forever in ${backupDirectory.absolutePath}/${filePrefix ?: ""}*..." }
        var deletedFiles = 0
        for (file in backupDirectory.listFiles()) {
            if (filePrefix != null && !file.name.startsWith(filePrefix)) {
                continue
            }
            val dateString = dateRegex.find(file.name)?.value
            dateString?.let {
                val date = LocalDate.parse(it, dateFormatter)
                if (date.dayOfMonth > 1) {
                    // Daily backup not at the beginning of a month.
                    if (date.isBefore(keepDailyBackupsUntil)) {
                        log.info { "Deleting file '${file.absolutePath}'..." }
                        file.delete()
                        deletedFiles++
                    }
                }
            }
        }
        log.info { "Deleted $deletedFiles files in ${backupDirectory.absolutePath}'" }
    }

    private val DATE_REGEX = """\d{4}-\d{2}-\d{2}""".toRegex()

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
}
