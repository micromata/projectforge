/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.system

import org.apache.commons.io.FileUtils
import org.projectforge.business.address.BirthdayCache.Companion.instance
import org.projectforge.business.fibu.AuftragsCache
import org.projectforge.business.fibu.KontoCache
import org.projectforge.business.fibu.RechnungCache
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.jobs.CronSanityCheckJob
import org.projectforge.business.task.TaskDao
import org.projectforge.business.task.TaskTree
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.persistence.database.SchemaExport
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException

/**
 * Provides some system routines.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de), Florian Blumenstein
 */
@Service
class SystemService {
    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var systemInfoCache: SystemInfoCache

    @Autowired
    private lateinit var auftragsCache: AuftragsCache

    @Autowired
    private lateinit var cronSanityCheckJob: CronSanityCheckJob

    @Autowired
    private lateinit var rechnungCache: RechnungCache

    @Autowired
    private lateinit var kontoCache: KontoCache

    @Autowired
    private lateinit var kostCache: KostCache

    fun exportSchema(): String? {
        val exp = SchemaExport()
        val file: File
        try {
            file = File.createTempFile("projectforge-schema", ".sql")
        } catch (ex: IOException) {
            log.error(ex.message, ex)
            return ex.message
        }
        exp.exportSchema(file.path)
        val result: String
        try {
            result = FileUtils.readFileToString(file, "UTF-8")
        } catch (ex: IOException) {
            log.error(ex.message, ex)
            return ex.message
        }
        file.delete()
        return result
    }

    /**
     * Search for abandoned tasks, corrupted JCR/Data transfer etc.
     * The result will also be written in the user's personal data transfer box, if plugin data-transfer is enabled.
     *
     */
    fun checkSystemIntegrity(): String {
        val context = cronSanityCheckJob.execute()
        return context.getReportAsText()
    }

    /**
     * Refreshes the caches: TaskTree, userGroupCache and kost2.
     *
     * @return the name of the refreshed caches.
     */
    fun refreshCaches(): String {
        userGroupCache.forceReload()
        taskTree.forceReload()
        kontoCache.forceReload()
        kostCache.forceReload()
        rechnungCache.forceReload()
        auftragsCache.forceReload()
        systemInfoCache.forceReload()
        instance.forceReload()
        return "UserGroupCache, TaskTree, KontoCache, KostCache, RechnungCache, AuftragsCache, SystemInfoCache, BirthdayCache"
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SystemService::class.java)
    }
}
