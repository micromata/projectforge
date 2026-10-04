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

import mu.KotlinLogging
import org.projectforge.business.PfCaches
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.fibu.KontoCache
import org.projectforge.business.fibu.datev.DatevImportService
import org.projectforge.business.fibu.kost.BuchungssatzDao
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.common.DataSizeConfig
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.jobs.JobHandler
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.dto.BwaStatistics
import org.projectforge.rest.importer.AbstractImportRest
import org.projectforge.rest.importer.ImportPairEntry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.InputStream

private val log = KotlinLogging.logger {}

/**
 * The layout-free DATEV import of accounting records (Buchungssätze), the successor of the removed Wicket
 * page `wa/datevImport`. It reads the tax office's original xlsx unchanged (see [DatevRecordExcelImporter]) and
 * answers the business assessment (BWA) of the importable records as view meta `bwa`, so the preview can be
 * checked against the tax office's BWA sheet before committing.
 *
 * @author Kai Reinhard
 */
@RestController
@RequestMapping("${Rest.URL}/datevRecordImport")
class DatevRecordImportRest : AbstractImportRest<DatevRecordImportDTO, DatevRecordImportStorage>() {

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var buchungssatzDao: BuchungssatzDao

    @Autowired
    private lateinit var kontoCache: KontoCache

    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var caches: PfCaches

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

    /**
     * The log viewer url of the user's DATEV import log (both tabs, see [DatevImportLog]), for the "view log" button.
     */
    @AccessChecked("FIBU_DATEV_IMPORT + not restricted/demo (checkRight)")
    @GetMapping("logViewer")
    fun logViewer(): Map<String, String?> {
        checkRight()
        return mapOf("url" to DatevImportLog.viewerUrl())
    }

    override fun proceedUpload(inputStream: InputStream, filename: String): DatevRecordImportStorage {
        val storage = DatevRecordImportStorage()
        storage.filename = filename
        DatevRecordExcelImporter(kontoCache, kostCache, caches).parse(inputStream, storage)
        return storage
    }

    override fun extraViewMeta(storage: DatevRecordImportStorage): Map<String, Any>? {
        // The BWA is only a check aid of the preview, a failure must not break the import.
        val bwa = runCatching { BwaStatistics.from(storage.buildImportableRecords()) }
            .onFailure { log.warn(it) { "Can't build the BWA of the DATEV import preview: ${it.message}" } }
            .getOrNull() ?: return null
        return mapOf("bwa" to bwa)
    }

    override fun import(
        storage: DatevRecordImportStorage,
        selectedEntries: List<ImportPairEntry<DatevRecordImportDTO>>,
    ): Int {
        log.info { "Enqueueing import of #${selectedEntries.size} DATEV accounting records." }
        return jobHandler.addJob(
            DatevRecordImportJob(
                buchungssatzDao,
                kontoCache,
                kostCache,
                selectedEntries,
                importStorage = storage,
            )
        ).id
    }
}
