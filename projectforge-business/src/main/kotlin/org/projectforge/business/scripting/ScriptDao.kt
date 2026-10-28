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

package org.projectforge.business.scripting

import de.micromata.merlin.utils.ReplaceUtils.encodeFilename
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.configuration.ApplicationDirectories
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime.Companion.now
import org.springframework.stereotype.Service
import java.io.File
import java.io.FilenameFilter
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard
 */
@Service
open class ScriptDao : AbstractScriptDao() {
    /**
     * Copy old script as script backup if modified.
     *
     * @see org.projectforge.framework.persistence.api.BaseDao.onUpdate
     */
    override fun onUpdate(obj: ScriptDO, dbObj: ScriptDO) {
        if (!Arrays.equals(dbObj.script, obj.script)) {
            obj.scriptBackup = dbObj.script
            val suffix = getScriptSuffix(obj)
            val filename =
                encodeFilename("${getBackupBasefilename(dbObj)}_${obj.name}_${now().isoStringSeconds}.$suffix", true)
            val backupDir = getBackupDir()
            ApplicationDirectories.ensureDir(backupDir)
            val file = File(backupDir, filename)
            try {
                log.info { "Writing backup of script to: " + file.absolutePath }
                file.writeText(dbObj.scriptAsString ?: "")
            } catch (ex: IOException) {
                log.error(ex) { "Error while trying to save backup file of script '" + file.absolutePath + "': " + ex.message }
            }
        }
    }

    fun getBackupFiles(obj: ScriptDO): Array<File>? {
        val baseFilename = getBackupBasefilename(obj)
        val oldBaseFilename = getOldBackupBasefilename(obj)
        val backupDir = getBackupDir()
        if (!backupDir.exists()) {
            return null
        }
        return backupDir.listFiles(FilenameFilter { _: File, name: String ->
            name.startsWith(baseFilename) || name.startsWith(
                oldBaseFilename
            )
        })
    }

    /**
     * The former versions of the script's code, newest first: one per backup file (see [onUpdate]), plus
     * the one in [ScriptDO.scriptBackup] where it is not the newest file anyway (it predates the files).
     */
    fun getBackups(obj: ScriptDO): List<ScriptBackupInfo> {
        val files = getBackupFiles(obj)
            ?.map { it to (parseBackupTimestamp(it.name) ?: it.lastModified()) }
            ?.sortedByDescending { it.second }
            ?: emptyList()
        val result = files.map { (file, timestamp) -> ScriptBackupInfo(file.name, timestamp) }.toMutableList()
        val dbBackup = obj.scriptBackupAsString
        if (!dbBackup.isNullOrEmpty() && files.firstOrNull()?.first?.readText() != dbBackup) {
            result.add(ScriptBackupInfo(ScriptBackupInfo.DB_KEY, null))
        }
        return result
    }

    /**
     * The code of a former version, named by its [ScriptBackupInfo.key]; null for a key that is none of
     * the script's backups. The key is only compared with the names of [getBackupFiles], never turned
     * into a path itself, so no other file can be read with it.
     */
    fun readBackup(obj: ScriptDO, key: String): String? {
        if (key == ScriptBackupInfo.DB_KEY) {
            return obj.scriptBackupAsString
        }
        return getBackupFiles(obj)?.firstOrNull { it.name == key }?.readText()
    }

    private fun getBackupDir(): File {
        return File(ApplicationDirectories.backupDirectory, "scripts")
    }

    private fun getBackupBasefilename(obj: ScriptDO): String {
        return "script-${obj.id}_"
    }

    private fun getOldBackupBasefilename(obj: ScriptDO): String {
        return encodeFilename("${obj.name}_", true)
    }

    fun getScriptSuffix(obj: ScriptDO): String {
        return if (ScriptExecutor.getScriptType(obj) == ScriptDO.ScriptType.GROOVY) "groovy" else "kts"
    }

    /**
     * Same as [hasAccess]: without a user right, [BaseDao.hasInsertAccess] would allow every user to add a script
     * (and to fetch the preset of a new one, which lists the variables and services available to scripts).
     */
    override fun hasInsertAccess(user: PFUserDO): Boolean {
        return hasAccess(user, null, null, OperationType.INSERT, false)
    }

    /**
     * User must be member of group controlling or finance.
     *
     * @see org.projectforge.framework.persistence.api.BaseDao.hasDeleteAccess
     */
    override fun hasAccess(
        user: PFUserDO, obj: ScriptDO?, oldObj: ScriptDO?,
        operationType: OperationType,
        throwException: Boolean
    ): Boolean {
        return accessChecker.isUserMemberOfGroup(
            user, throwException, ProjectForgeGroup.CONTROLLING_GROUP,
            ProjectForgeGroup.FINANCE_GROUP
        )
    }

    companion object {
        /** The trailing timestamp of a backup file's name, as [onUpdate] writes it (`isoStringSeconds`, UTC). */
        private val BACKUP_TIMESTAMP_REGEX = """(\d{4}-\d{2}-\d{2}_\d{2}_\d{2}_\d{2})\.[^.]+$""".toRegex()
        private val BACKUP_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm_ss")

        /**
         * When the version in a backup file was replaced, in epoch millis, read from its name
         * (`..._2022-03-05_22_06_16.groovy`); null for a name without one.
         */
        internal fun parseBackupTimestamp(filename: String): Long? {
            val match = BACKUP_TIMESTAMP_REGEX.find(filename) ?: return null
            return try {
                LocalDateTime.parse(match.groupValues[1], BACKUP_TIMESTAMP_FORMAT)
                    .toInstant(ZoneOffset.UTC).toEpochMilli()
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }
}
