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

package org.projectforge.rest.fibu.importer

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.fibu.KontoDao
import org.projectforge.business.fibu.datev.DatevImportService
import org.projectforge.common.DataSizeConfig
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.jobs.JobHandler
import org.projectforge.rest.config.Rest
import org.projectforge.rest.importer.AbstractImportRest
import org.projectforge.rest.importer.ImportPairEntry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream

private val log = KotlinLogging.logger {}

/**
 * The layout-free DATEV import of the chart of accounts (Kontenplan), the second tab of the next DATEV import
 * page. It reads the same original file of the tax office as [DatevRecordImportRest] (see
 * [DatevAccountExcelImporter]); the accounting records of that file are stashed for the records tab as well.
 *
 * @author Kai Reinhard
 */
@RestController
@RequestMapping("${Rest.URL}/datevAccountImport")
class DatevAccountImportRest : AbstractImportRest<DatevAccountImportDTO, DatevAccountImportStorage>() {

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var parser: DatevImportParser

    @Autowired
    private lateinit var kontoDao: KontoDao

    @Autowired
    private lateinit var configurationService: ConfigurationService

    @Autowired
    private lateinit var jobHandler: JobHandler

    override val fileExtensions = arrayOf("xlsx", "xls")

    override val maxFileUploadSizeMB = 10L // in MB, unused: see maxFileUploadSizeBytes

    /** Configured by `projectforge.max-file-size.datev` (default `10MB`), shared by both DATEV imports. */
    override val maxFileUploadSizeBytes: Long
        get() = DataSizeConfig.init(configurationService.maxFileSizeDatev).toBytes()

    override fun checkRight() {
        DatevImportService.checkLoggedinUserRight(accessChecker)
        accessChecker.checkRestrictedOrDemoUser()
        // Every request of the import page passes here, so the log subscription exists before the upload is logged.
        DatevImportLog.ensureSubscription()
    }

    override fun proceedUpload(inputStream: InputStream, filename: String): DatevAccountImportStorage {
        return parser.parseAccounts(inputStream, filename)
    }

    /**
     * Stashes the accounting records of the same file for the records tab, so they can be reconciled there without a
     * second upload. A file without month sheet leaves the records tab untouched.
     */
    override fun afterUpload(request: HttpServletRequest, file: MultipartFile, filename: String): List<String>? {
        val storage = try {
            file.inputStream.use { parser.parseRecords(it, filename) }
        } catch (ex: Exception) {
            log.info { "No accounting records in file '$filename' for the records tab: ${ex.message}" }
            return null
        }
        if (storage.readRecords.isEmpty()) {
            return null
        }
        stashStorage(request, getSessionAttributeName(DatevRecordImportRest::class.java), storage)
        log.info { "Accounting records of file '$filename' stashed for the records tab: #${storage.readRecords.size}" }
        return listOf(translateMsg("fibu.datev.import.companion.records", storage.readRecords.size))
    }

    override fun import(
        storage: DatevAccountImportStorage,
        selectedEntries: List<ImportPairEntry<DatevAccountImportDTO>>,
    ): Int {
        log.info { "Enqueueing import of #${selectedEntries.size} DATEV accounts." }
        return jobHandler.addJob(
            DatevAccountImportJob(kontoDao, selectedEntries, importStorage = storage)
        ).id
    }
}
