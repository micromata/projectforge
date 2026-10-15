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

package org.projectforge.framework.jcr

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.framework.configuration.ConfigXml
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jobs.AbstractJob
import org.projectforge.framework.jobs.JobHandler
import org.projectforge.jcr.RepoMigrationService
import org.projectforge.jcr.RepoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

private val log = KotlinLogging.logger {}

/**
 * Starts the migration of the files out of the JCR into the file store ([RepoMigrationJob]).
 *
 * With `projectforge.files.store=db` the migration is started automatically on every start-up. Already migrated files
 * are skipped by a look-up in the data base, so a run without anything to do is cheap. The report file isn't used as
 * marker: it lives in the home directory and doesn't fit any more after a restore of the data base (empty `pf_files`).
 */
@Service
class FileStoreMigrationService {
    @Autowired
    private lateinit var jobHandler: JobHandler

    @Autowired
    private lateinit var repoMigrationService: RepoMigrationService

    @Autowired
    private lateinit var repoService: RepoService

    /**
     * The report of the last migration run.
     */
    val reportFile: File
        get() = File(ConfigXml.getInstance().applicationHomeDir, REPORT_FILENAME)

    /**
     * True, if all files are stored by the file store (`projectforge.files.store=db`).
     */
    val allFilesInFileStore: Boolean
        get() = repoService.allFilesInFileStore

    fun startMigration(): AbstractJob {
        val job = RepoMigrationJob(repoMigrationService, reportFile, title = translate("system.admin.button.migrateJcrFiles"))
        return jobHandler.addJob(job)
    }

    /**
     * Called on start-up: starts the migration, if `projectforge.files.store=db`. Files migrated before are skipped.
     */
    fun autoStart() {
        if (!repoService.allFilesInFileStore) {
            return
        }
        log.info { "projectforge.files.store=db: migrating the files of the JCR to the file store (already migrated files are skipped)..." }
        startMigration()
    }

    companion object {
        const val REPORT_FILENAME = "jcr-migration-report.txt"
    }
}
