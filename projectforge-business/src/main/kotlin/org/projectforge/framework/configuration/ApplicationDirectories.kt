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


package org.projectforge.framework.configuration

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.common.FileUtils
import java.io.File

private val log = KotlinLogging.logger {}

/**
 * The directories of ProjectForge in its base dir, configured by the properties `projectforge.dir.*` (absolute or
 * relative to the base dir, the default sub directory if not given). Set on startup by
 * [org.projectforge.business.configuration.ConfigurationService]; test cases without Spring context use the default
 * names relative to the working directory.
 */
object ApplicationDirectories {
    enum class Dir(
        /** The key of the property in projectforge.properties. */
        val property: String,
        /** The default sub directory of the base dir. */
        val defaultName: String,
    ) {
        /** Working files, e.g. states of synchronizations. */
        WORK("projectforge.dir.work", "work"),

        /** Temporary files, e.g. debug logs. */
        TMP("projectforge.dir.tmp", "tmp"),

        /** Backups, e.g. of the data base and of scripts. */
        BACKUP("projectforge.dir.backup", "backup"),
    }

    private val dirs = mutableMapOf<Dir, File>()

    @JvmStatic
    val workingDirectory: File
        get() = get(Dir.WORK)

    @JvmStatic
    val tempDirectory: File
        get() = get(Dir.TMP)

    @JvmStatic
    val backupDirectory: File
        get() = get(Dir.BACKUP)

    @JvmStatic
    fun get(dir: Dir): File {
        synchronized(dirs) {
            return dirs[dir] ?: File(dir.defaultName)
        }
    }

    /**
     * Resolves the directories against the base dir and creates them, if missing.
     * @param values The configured values by directory (blank or missing: the default name).
     */
    fun init(applicationHomeDir: String, values: Map<Dir, String?>) {
        synchronized(dirs) {
            Dir.entries.forEach { dir ->
                val value = values[dir]?.trim()?.ifEmpty { null } ?: dir.defaultName
                val file = File(FileUtils.getAbsolutePath(applicationHomeDir, value))
                ensureDir(file)
                dirs[dir] = file
            }
        }
    }

    /** For test cases only. */
    fun internalSet(dir: Dir, file: File) {
        synchronized(dirs) {
            dirs[dir] = file
        }
    }

    /**
     * Creates the given directory, if missing.
     * @return true, if the directory exists and is readable.
     */
    @JvmStatic
    fun ensureDir(dir: File): Boolean {
        if (!dir.exists()) {
            log.info { "Creating directory $dir" }
            dir.mkdirs()
        }
        if (!dir.canRead()) {
            log.error { "Can't create directory: $dir" }
            return false
        }
        return true
    }
}
