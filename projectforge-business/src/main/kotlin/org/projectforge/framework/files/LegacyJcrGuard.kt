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


package org.projectforge.framework.files

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.common.EmphasizedLogSupport
import org.projectforge.common.FileUtils
import org.projectforge.files.store.FileStore
import org.projectforge.framework.configuration.ConfigXmlMigration
import org.projectforge.framework.configuration.LegacyConfigXml
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * Since 9.2 there is no JCR (Apache Oak) anymore, all files are served by the [FileStore]. The files of the JCR had to
 * be migrated with 9.1.x. This guard refuses the start, if the JCR still contains data, which wasn't migrated: the
 * files would be missing otherwise. Such an installation has to go back to 9.1.x and finish the migration there.
 */
@Service
open class LegacyJcrGuard {
    @Autowired
    private lateinit var configurationService: ConfigurationService

    @Autowired
    private lateinit var fileStore: FileStore

    /** The former directory of the JCR (default: jcr in the base dir). */
    @Value("\${projectforge.dir.jcr:}")
    private var jcrDir: String = ""

    /** Given, if the JCR was stored in a data base. */
    @Value("\${oak.datasource.url:}")
    private var oakDataSourceUrl: String = ""

    /** The storage of new files in 9.1.x: `jcr` or `db`. */
    @Value("\${projectforge.files.store:}")
    private var fileStoreMode: String = ""

    /** Starts anyway (e. g. a JCR without any files, or the DataTransfer gateway with its own, migrated JCR). */
    @Value("\${projectforge.files.ignoreLegacyJcr:false}")
    private var ignoreLegacyJcr: Boolean = false

    /**
     * @throws IllegalStateException if the JCR contains data, which wasn't migrated.
     */
    fun check() {
        val homeDir = configurationService.applicationHomeDir
        val dirs = jcrDirs(homeDir, jcrDir, configXmlJcrDirs(File(homeDir)))
        val dirWithData = dirs.firstOrNull { hasData(it) }
        val reportFile = File(homeDir, MIGRATION_REPORT_FILENAME).takeIf { it.canRead() }
        val result = evaluate(
            jcrHasData = dirWithData != null || oakDataSourceUrl.isNotBlank(),
            migrationReport = reportFile?.readText(),
            reportLastModified = reportFile?.lastModified() ?: 0L,
            fileStoreMode = fileStoreMode,
            ignoreLegacyJcr = ignoreLegacyJcr,
            filesCreatedBefore = { fileStore.countFilesCreatedBefore(it) },
        )
        val location = if (oakDataSourceUrl.isNotBlank()) {
            "oak.datasource.url=$oakDataSourceUrl"
        } else {
            (dirWithData ?: dirs.first()).absolutePath
        }
        when (result) {
            Result.NO_JCR -> {}
            Result.MIGRATED -> log.warn {
                "The former JCR ($location) isn't used anymore since 9.2 (its files were migrated by 9.1.x). It may be " +
                        "deleted after a backup."
            }

            Result.IGNORED -> log.warn {
                "The former JCR ($location) contains data, which might not have been migrated by 9.1.x. Started anyway, " +
                        "because projectforge.files.ignoreLegacyJcr=true."
            }

            Result.STORED_IN_JCR -> refuse(
                "projectforge.files.store=jcr is set: the files uploaded since then are stored in the JCR ($location) " +
                        "only and weren't migrated to the file store. Since 9.2 there is no JCR anymore: install " +
                        "9.1.x, set projectforge.files.store=db, let the migration finish without errors (see " +
                        "$MIGRATION_REPORT_FILENAME in the home directory), remove projectforge.files.store and " +
                        "update afterwards."
            )

            Result.NOT_MIGRATED -> refuse(
                "The JCR ($location) contains files, which weren't migrated to the file store. Since 9.2 there is " +
                        "no JCR anymore: install 9.1.x, let the migration finish without errors (see " +
                        "$MIGRATION_REPORT_FILENAME in the home directory) and update afterwards. If the JCR doesn't " +
                        "contain any files to keep, set projectforge.files.ignoreLegacyJcr=true."
            )
        }
    }

    private fun refuse(msg: String) {
        EmphasizedLogSupport(log, EmphasizedLogSupport.Priority.VERY_IMPORTANT, EmphasizedLogSupport.Alignment.LEFT)
            .setLogLevel(EmphasizedLogSupport.LogLevel.ERROR)
            .log(msg)
            .logEnd()
        throw IllegalStateException(msg)
    }

    enum class Result { NO_JCR, MIGRATED, IGNORED, NOT_MIGRATED, STORED_IN_JCR }

    companion object {
        internal const val DEFAULT_JCR_DIR = "jcr"

        /** Written by the migration of 9.1.x into the home directory. */
        internal const val MIGRATION_REPORT_FILENAME = "jcr-migration-report.txt"

        /**
         * Files of the file store created this long before the start of the migration are files of the JCR (the
         * migration took over their date of creation). The margin covers the time zone of the data base, too.
         */
        internal const val MIGRATION_START_MARGIN_MILLIS = 24 * 60 * 60 * 1000L

        private val REPORT_OK_REGEX = Regex("""^Result: OK\s*$""", RegexOption.MULTILINE)
        private val REPORT_TOTAL_REGEX = Regex("""^Files in the JCR: (\d+)\s*$""", RegexOption.MULTILINE)
        private val REPORT_DURATION_REGEX = Regex("""^Duration: (\d+) s\s*$""", RegexOption.MULTILINE)

        /**
         * @param migrationReport The report of the last migration run of 9.1.x, if any.
         * @param reportLastModified The time the report was written (end of the migration run).
         * @param fileStoreMode The property `projectforge.files.store` of 9.1.x.
         * @param filesCreatedBefore Number of files of the file store created before the given date. Only called if
         * needed (it queries the data base).
         */
        internal fun evaluate(
            jcrHasData: Boolean,
            migrationReport: String?,
            reportLastModified: Long,
            fileStoreMode: String?,
            ignoreLegacyJcr: Boolean,
            filesCreatedBefore: (Date) -> Long,
        ): Result {
            if (!jcrHasData) {
                return Result.NO_JCR
            }
            if (fileStoreMode?.trim().equals("jcr", ignoreCase = true)) {
                // 9.1.x allowed to switch back to the JCR after the migration: the files uploaded afterwards are
                // stored in the JCR only.
                return if (ignoreLegacyJcr) Result.IGNORED else Result.STORED_IN_JCR
            }
            if (migrationReport != null && REPORT_OK_REGEX.containsMatchIn(migrationReport)) {
                val filesInJcr = REPORT_TOTAL_REGEX.find(migrationReport)?.groupValues?.get(1)?.toLongOrNull() ?: 0
                if (filesInJcr == 0L) {
                    return Result.MIGRATED
                }
                // The migrated files kept their date of creation, so they are older than the migration. Without such
                // files, the data base was restored to a state before the migration (files uploaded since then
                // don't count).
                val durationMillis =
                    (REPORT_DURATION_REGEX.find(migrationReport)?.groupValues?.get(1)?.toLongOrNull() ?: 0) * 1000
                val migrationStart = reportLastModified - durationMillis - MIGRATION_START_MARGIN_MILLIS
                if (filesCreatedBefore(Date(migrationStart)) > 0) {
                    return Result.MIGRATED
                }
            }
            return if (ignoreLegacyJcr) Result.IGNORED else Result.NOT_MIGRATED
        }

        /**
         * The directories, where the JCR may be: the property, the directories of config.xml (also if taken over by
         * [ConfigXmlMigration] before) and the default one.
         */
        internal fun jcrDirs(homeDir: String, jcrDirProperty: String, configXmlDirs: List<String>): List<File> {
            return (listOf(jcrDirProperty) + configXmlDirs + DEFAULT_JCR_DIR)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { File(FileUtils.getAbsolutePath(homeDir, it)) }
                .distinctBy { it.absolutePath }
        }

        /** The JCR directories given by config.xml or config.xml.migrated* in the home dir. */
        internal fun configXmlJcrDirs(homeDir: File): List<String> {
            val files = homeDir.listFiles { file ->
                file.isFile && (file.name == LegacyConfigXml.CONFIG_XML_FILE ||
                        file.name.startsWith(LegacyConfigXml.CONFIG_XML_FILE + ConfigXmlMigration.MIGRATED_SUFFIX))
            } ?: return emptyList()
            return files.mapNotNull { file ->
                try {
                    LegacyConfigXml.read(file.readText())?.jcrDirectory?.trim()?.ifEmpty { null }
                } catch (ex: Exception) {
                    log.warn { "Couldn't read ${file.absolutePath} for the directory of the former JCR: ${ex.message}" }
                    null
                }
            }
        }

        internal fun hasData(dir: File): Boolean {
            return dir.isDirectory && !dir.listFiles().isNullOrEmpty()
        }
    }
}
