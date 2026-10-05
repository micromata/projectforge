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

package org.projectforge.rest

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.ProjectForgeVersion
import org.projectforge.SystemAlertMessage
import org.projectforge.SystemStatus
import org.projectforge.business.admin.SystemDiagnosticsExport
import org.projectforge.business.address.AddressImageDao
import org.projectforge.business.book.BookDO
import org.projectforge.business.book.BookDao
import org.projectforge.business.book.BookStatus
import org.projectforge.business.jobs.CronSanityCheckJob
import org.projectforge.business.system.SystemService
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.I18nKeysUsageInterface
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.jobs.JobHandler
import org.projectforge.framework.persistence.api.ReindexSettings
import org.projectforge.framework.persistence.database.DatabaseDao
import org.projectforge.framework.persistence.database.DatabaseService
import org.projectforge.framework.persistence.database.DatabaseTester
import org.projectforge.framework.persistence.search.HibernateSearchReindexer
import org.projectforge.framework.time.DateHelper
import org.projectforge.jcr.RepoBackupService
import org.projectforge.jcr.RepoMigrationService
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.jobs.JcrBackupZipJob
import org.projectforge.rest.jobs.ReindexJob
import org.projectforge.rest.jobs.RepoMigrationJob
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * The **System** administration page (`/next/system`), successor of Wicket's removed `AdminPage` (`wa/admin`, now
 * redirected by [org.projectforge.web.OrphanedLinkFilter]).
 *
 * Like [PersonalStatisticsRest] and [SendTextMessageRest] this is a non-entity, standalone action page: it exposes
 * plain JSON so the next frontend can render the cards and buttons, and each action is a thin wrapper around the
 * existing services the Wicket page used ([SystemService], [DatabaseService], [AddressImageDao], … ).
 *
 * **Access:** the classic page was reachable through an admin-only menu entry, and every action re-checked the
 * logged-in user. These REST endpoints have no DAO access backstop, so every method checks the admin group (and,
 * for mutating actions, that the user is neither restricted nor a demo user) itself — [checkAdminAccess] /
 * [checkWriteAccess]. Unlike the Wicket page, `createMissingIndices` also gets the admin-group check here (the
 * classic page relied on its admin-only menu for that, which a direct REST call would bypass).
 */
@RestController
@RequestMapping("${Rest.URL}/system")
class SystemRest {
    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var systemService: SystemService

    @Autowired
    private lateinit var databaseService: DatabaseService

    @Autowired
    private lateinit var addressImageDao: AddressImageDao

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var bookDao: BookDao

    @Autowired
    private lateinit var jobHandler: JobHandler

    @Autowired
    private lateinit var databaseDao: DatabaseDao

    @Autowired
    private lateinit var hibernateSearchReindexer: HibernateSearchReindexer

    @Autowired
    private lateinit var repoBackupService: RepoBackupService

    @Autowired
    private lateinit var repoMigrationService: RepoMigrationService

    /**
     * The only implementation ([org.projectforge.i18n.I18nKeysUsage]) lives in projectforge-application, which is not
     * on the classpath of every (plugin/wicket) test context. Autowire it optionally so those Spring test contexts can
     * still start; the checkI18nProperties endpoint guards against a missing bean at runtime.
     */
    @Autowired(required = false)
    private var i18nKeysUsage: I18nKeysUsageInterface? = null

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var databaseTester: DatabaseTester

    /**
     * The only implementation ([org.projectforge.start.ProjectForgeEndpoints]) lives in projectforge-application, which
     * is not on the classpath of every (plugin/wicket) test context. Autowire it optionally so those Spring test
     * contexts can still start; the export2FAConfiguration endpoint guards against a missing bean at runtime.
     */
    @Autowired(required = false)
    private var systemDiagnosticsExport: SystemDiagnosticsExport? = null

    /** The initial state of the page: the current alert message, the reindex default, and the dev-mode flag. */
    class SystemAdminData(
        val alertMessage: String?,
        val reindexNewestNEntries: Int,
        val developmentMode: Boolean,
        /** The localized copy&paste maintenance-notice sample, with the current version filled in. */
        val alertMessageSample: String,
    )

    /** A plain result message, shown by the frontend as a success toast. */
    class MessageResponse(val message: String)

    class AlertMessageRequest(var alertMessage: String? = null)

    class ReindexRequest(var newestNEntries: Int? = null, var fromDate: LocalDate? = null)

    /**
     * The id of the started job ([ReindexJob], [JcrBackupZipJob], [RepoMigrationJob]), which the frontend polls (see
     * JobsMonitorPageRest) for the progress.
     */
    class JobResponse(val jobId: Int)

    @AccessChecked("Admin group (checkIsLoggedInUserMemberOfAdminGroup)")
    @GetMapping
    fun getData(): SystemAdminData {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        return SystemAdminData(
            alertMessage = SystemAlertMessage.alertMessage,
            reindexNewestNEntries = DEFAULT_REINDEX_NEWEST_N_ENTRIES,
            developmentMode = SystemStatus.isDevelopmentMode(),
            alertMessageSample = translateMsg(
                "system.admin.alertMessage.copyAndPaste.text",
                ProjectForgeVersion.VERSION_NUMBER,
            ),
        )
    }

    // ------------------------------------------------------------------------------------------
    // Alert message (shown red on every page, not persisted).
    // ------------------------------------------------------------------------------------------

    @AccessChecked("Admin group + not restricted/demo (checkWriteAccess)")
    @PostMapping("setAlertMessage")
    fun setAlertMessage(@RequestBody request: AlertMessageRequest): MessageResponse {
        checkWriteAccess()
        log.info { "Admin user has set the alert message: \"${request.alertMessage}\"" }
        SystemAlertMessage.alertMessage = request.alertMessage
        return MessageResponse(translate("system.admin.alertMessage.setMessage"))
    }

    @AccessChecked("Admin group + not restricted/demo (checkWriteAccess)")
    @PostMapping("clearAlertMessage")
    fun clearAlertMessage(): MessageResponse {
        checkWriteAccess()
        log.info { "Admin user has cleared the alert message." }
        SystemAlertMessage.alertMessage = null
        return MessageResponse(translate("system.admin.alertMessage.clearMessage"))
    }

    // ------------------------------------------------------------------------------------------
    // Caches / configuration.
    // ------------------------------------------------------------------------------------------

    @AccessChecked("Admin group + not restricted/demo (checkWriteAccess)")
    @PostMapping("refreshCaches")
    fun refreshCaches(): MessageResponse {
        checkWriteAccess()
        log.info { "Administration: refresh all caches." }
        val refreshedCaches = systemService.refreshCaches()
        return MessageResponse(translateMsg("administration.refreshCachesDone", refreshedCaches))
    }

    @AccessChecked("Admin group + not restricted/demo (checkWriteAccess)")
    @PostMapping("rereadConfiguration")
    fun rereadConfiguration(): MessageResponse {
        checkWriteAccess()
        log.info { "Administration: reread configuration." }
        val result = systemService.rereadConfiguration()
        return MessageResponse(translateMsg("administration.rereadConfiguration", result ?: ""))
    }

    @AccessChecked("Admin group + not restricted/demo (checkWriteAccess)")
    @GetMapping("exportConfiguration")
    fun exportConfiguration(): ResponseEntity<*> {
        checkWriteAccess()
        log.info { "Administration: export configuration." }
        val filename = "config-${DateHelper.getDateAsFilenameSuffix(Date())}.xml"
        return RestUtils.downloadFile(filename, systemService.exportConfiguration() ?: "")
    }

    @AccessChecked("Admin group + not restricted/demo (checkWriteAccess)")
    @GetMapping("export2FAConfiguration")
    fun export2FAConfiguration(): ResponseEntity<*> {
        checkWriteAccess()
        log.info { "Administration: export 2FA configuration." }
        val filename = "config-2FA-${DateHelper.getDateAsFilenameSuffix(Date())}.txt"
        val systemDiagnosticsExport = this.systemDiagnosticsExport
            ?: return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("SystemDiagnosticsExport service is not available.")
        return RestUtils.downloadFile(filename, systemDiagnosticsExport.getInfo())
    }

    // ------------------------------------------------------------------------------------------
    // Checks.
    // ------------------------------------------------------------------------------------------

    @AccessChecked("Admin group + not restricted/demo (checkWriteAccess)")
    @GetMapping("checkSystemIntegrity")
    fun checkSystemIntegrity(): ResponseEntity<*> {
        checkWriteAccess()
        log.info { "Administration: check system integrity." }
        // Returns an HTML "started" page; the real report is delivered to the user's data transfer box.
        return RestUtils.downloadFile(CronSanityCheckJob.FILENAME, systemService.checkSystemIntegrity())
    }

    // ------------------------------------------------------------------------------------------
    // Database actions.
    // ------------------------------------------------------------------------------------------

    @AccessChecked("Admin group + not restricted/demo (checkWriteAccess)")
    @PostMapping("createMissingIndices")
    fun createMissingIndices(): MessageResponse {
        checkWriteAccess()
        log.info { "Administration: create missing database indices." }
        val counter = databaseService.createMissingIndices()
        return MessageResponse(translateMsg("administration.missingDatabaseIndicesCreated", counter.toString()))
    }

    @AccessChecked("Admin group + not restricted/demo (checkWriteAccess)")
    @GetMapping("exportSchema")
    fun exportSchema(): ResponseEntity<*> {
        checkWriteAccess()
        log.info { "Administration: schema export." }
        val filename = "projectforge_schema${DateHelper.getDateAsFilenameSuffix(Date())}.sql"
        return RestUtils.downloadFile(filename, systemService.exportSchema() ?: "")
    }

    @AccessChecked("Admin group + not restricted/demo (checkWriteAccess)")
    @GetMapping("optimizeAddressImages")
    fun optimizeAddressImages(): ResponseEntity<*> {
        checkWriteAccess()
        log.info { "Administration: shrink all address images and rebuild previews." }
        val filename = "address-image-processing-${DateHelper.getDateAsFilenameSuffix(Date())}.txt"
        return RestUtils.downloadFile(filename, addressImageDao.shrinkAllImagesAndRebuildPreviews())
    }

    @AccessChecked("Admin group + not restricted/demo (checkWriteAccess)")
    @PostMapping("resetIdpPasswordSync")
    fun resetIdpPasswordSync(): MessageResponse {
        checkWriteAccess()
        log.info { "Administration: reset IdP password sync flag for all users." }
        val count = userDao.resetIdpPasswordSync()
        return MessageResponse(translateMsg("administration.idpPasswordSyncReset", count.toString()))
    }

    // ------------------------------------------------------------------------------------------
    // Reindex (through the background-job infrastructure, so the frontend can show a progress bar).
    // ------------------------------------------------------------------------------------------

    @AccessChecked("Admin group + not restricted/demo (checkWriteAccess)")
    @PostMapping("reindex")
    fun reindex(@RequestBody request: ReindexRequest): JobResponse {
        checkWriteAccess()
        log.info { "Administration: re-index (newestNEntries=${request.newestNEntries}, fromDate=${request.fromDate})." }
        val fromDate = request.fromDate?.let { Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant()) }
        val settings = ReindexSettings(fromDate, request.newestNEntries)
        val job = jobHandler.addJob(
            ReindexJob(
                databaseDao = databaseDao,
                classes = hibernateSearchReindexer.indexedEntityClasses,
                settings = settings,
                adminRequired = true,
                title = translate("system.admin.button.reindex"),
            )
        )
        return JobResponse(job.id)
    }

    // ------------------------------------------------------------------------------------------
    // JCR replacement: backup ZIP as source for the import of the entity files, DataTransfer files out of Oak.
    // ------------------------------------------------------------------------------------------

    @AccessChecked("Admin group + not restricted/demo (checkWriteAccess)")
    @PostMapping("createJcrBackupZip")
    fun createJcrBackupZip(): JobResponse {
        checkWriteAccess()
        log.info { "Administration: create JCR backup ZIP." }
        val job = jobHandler.addJob(
            JcrBackupZipJob(repoBackupService, title = translate("system.admin.button.createJcrBackupZip"))
        )
        return JobResponse(job.id)
    }

    @AccessChecked("Admin group + not restricted/demo (checkWriteAccess)")
    @PostMapping("migrateJcrFileSystemPaths")
    fun migrateJcrFileSystemPaths(): JobResponse {
        checkWriteAccess()
        log.info { "Administration: move DataTransfer files out of the JCR into the file system." }
        val job = jobHandler.addJob(
            RepoMigrationJob(repoMigrationService, title = translate("system.admin.button.migrateJcrFileSystemPaths"))
        )
        return JobResponse(job.id)
    }

    // ------------------------------------------------------------------------------------------
    // Development tools.
    // ------------------------------------------------------------------------------------------

    @AccessChecked("Admin group + not restricted/demo (checkWriteAccess)")
    @GetMapping("checkI18nProperties")
    fun checkI18nProperties(): ResponseEntity<*> {
        checkWriteAccess()
        log.info { "Administration: check i18n properties." }
        val i18nKeysUsage = this.i18nKeysUsage
            ?: return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("I18nKeysUsage service is not available.")
        val excelFile = i18nKeysUsage.createExcelFile()
        return RestUtils.downloadFile(excelFile.filename, excelFile.bytes)
    }

    @AccessChecked("Admin group + not restricted/demo (checkWriteAccess)")
    @GetMapping("debugUserGroupCache")
    fun debugUserGroupCache(): ResponseEntity<*> {
        checkWriteAccess()
        log.info { "Administration: debug UserGroupCache." }
        val filename = "userGroupCache-${DateHelper.getDateAsFilenameSuffix(Date())}.json"
        return RestUtils.downloadFile(filename, userGroupCache.internalGetStateAsJson())
    }

    @AccessChecked("Dev mode only + Admin group + not restricted/demo (checkWriteAccess)")
    @PostMapping("testDatabase")
    fun testDatabase(): MessageResponse {
        checkDevelopmentMode()
        checkWriteAccess()
        log.info { "Administration: test database." }
        databaseTester.test()
        return MessageResponse(translate("system.admin.development.databaseTestStarted"))
    }

    @AccessChecked("Dev mode only + Admin group + not restricted/demo (checkWriteAccess)")
    @PostMapping("createTestBooks")
    fun createTestBooks(): MessageResponse {
        checkDevelopmentMode()
        checkWriteAccess()
        log.info { "Administration: create $NUMBER_OF_TEST_BOOKS test books." }
        // Find the lowest series number not yet used, so repeated runs don't collide on the title.
        var number = 1
        while (databaseService.queryForInt("select count(*) from t_book where title like 'title.$number.%'") > 0) {
            number++
        }
        val list = (1..NUMBER_OF_TEST_BOOKS).map { i ->
            BookDO().also { book ->
                book.title = testValue("title", number, i)
                book.abstractText = testValue("abstractText", number, i)
                book.authors = testValue("authors", number, i)
                book.comment = testValue("comment", number, i)
                book.editor = testValue("editor", number, i)
                book.isbn = testValue("isbn", number, i)
                book.keywords = testValue("keywords", number, i)
                book.publisher = testValue("publisher", number, i)
                book.signature = testValue("signature", number, i)
                book.status = BookStatus.PRESENT
                book.yearOfPublishing = "2001"
            }
        }
        bookDao.insert(list)
        return MessageResponse(
            translateMsg("system.admin.development.testObjectsCreated", NUMBER_OF_TEST_BOOKS.toString(), "BookDO")
        )
    }

    private fun testValue(basename: String, number: Int, counter: Int) = "$basename.$number.$counter"

    // ------------------------------------------------------------------------------------------

    private fun checkAdminAccess() {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
    }

    private fun checkWriteAccess() {
        checkAdminAccess()
        accessChecker.checkRestrictedOrDemoUser()
    }

    private fun checkDevelopmentMode() {
        if (!SystemStatus.isDevelopmentMode()) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Only available in development mode.")
        }
    }

    companion object {
        private const val DEFAULT_REINDEX_NEWEST_N_ENTRIES = 1000
        private const val NUMBER_OF_TEST_BOOKS = 100
    }
}
