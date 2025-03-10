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

import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import org.projectforge.business.address.BirthdayCache.Companion.instance
import org.projectforge.business.fibu.AuftragsCache
import org.projectforge.business.fibu.AuftragsRechnungCache
import org.projectforge.business.fibu.KontoCache
import org.projectforge.business.fibu.RechnungCache
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.jobs.CronSanityCheckJob
import org.projectforge.business.task.TaskTree
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.extensions.formatMillis
import org.projectforge.common.html.Html
import org.projectforge.common.html.HtmlDocument
import org.projectforge.datatransfer.DataTransferBridge
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.database.SchemaExport
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.requiredLoggedInUser
import org.projectforge.jobs.JobListExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException

private val log = KotlinLogging.logger {}

/**
 * Provides some system routines.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de), Florian Blumenstein
 */
@Service
class SystemService {
    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var auftragsCache: AuftragsCache

    @Autowired
    private lateinit var auftragsRechnungCache: AuftragsRechnungCache

    @Autowired
    private lateinit var cronSanityCheckJob: CronSanityCheckJob

    @Autowired
    private lateinit var dataTransferBridge: DataTransferBridge

    @Autowired
    private lateinit var kontoCache: KontoCache

    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var rechnungCache: RechnungCache

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var systemInfoCache: SystemInfoCache

    @Autowired
    private lateinit var taskTree: TaskTree

    fun exportSchema(): String? {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
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
     */
    fun checkSystemIntegrity(): String {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        if (!dataTransferBridge.available) {
            return internalCheckSystemIntegrity()
        }
        val user = requiredLoggedInUser
        Thread { // Can't run it in a Kotlin coroutine, because JCR code has something like runInSession.
            val start = System.currentTimeMillis()
            log.info { "Check system integrity started..." }
            try {
                ThreadLocalUserContext.setUser(user)
                val content = internalCheckSystemIntegrity()
                val filename = CronSanityCheckJob.FILENAME
                val description = "System integrity check result"
                dataTransferBridge.putFileInUsersInBox(
                    filename = filename,
                    content = content,
                    description = description,
                    receiver = user,
                )
            } finally {
                ThreadLocalUserContext.clear()
                log.info("Checking of system integrity finished after ${(System.currentTimeMillis() - start).formatMillis()}")
            }
        }.start()
        val html = HtmlDocument(JobListExecutionContext.title).add(Html.H1(JobListExecutionContext.title))
            .add(Html.Alert(Html.Alert.Type.SUCCESS, "Checking of system integrity started."))
            .add(
                Html.Alert(type = Html.Alert.Type.INFO)
                    .add(Html.P().also { p ->
                        p.add(Html.Text("The results will be in Your "))
                            .add(
                                Html.A(
                                    dataTransferBridge.getPersonalBoxOfUserLink() ?: "???",
                                    "personal data transfer box"
                                )
                            )
                            .add(Html.Text(" in a few minutes (dependant on your ProjectForge installation)..."))
                    })
            )
            .add(Html.P("For large files in the data transfer boxes, it may take much more time (all checksums will be calculated)."))
        return html.toString()
    }

    private fun internalCheckSystemIntegrity(): String {
        val context = cronSanityCheckJob.execute()
        return context.getReportAsHtml()
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
        auftragsRechnungCache.forceReload()
        systemInfoCache.forceReload()
        instance.forceReload()
        return "UserGroupCache, TaskTree, KontoCache, KostCache, RechnungCache, AuftragsCache, AuftragsRechnungCache, SystemInfoCache, BirthdayCache"
    }
}
