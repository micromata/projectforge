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

package org.projectforge.rest.fibu

import mu.KotlinLogging
import org.projectforge.business.fibu.kost.reporting.Report
import org.projectforge.business.fibu.kost.reporting.ReportDao
import org.projectforge.business.fibu.kost.reporting.ReportStorage
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.common.i18n.Priority
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.utils.FileCheck
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

private val log = KotlinLogging.logger {}

/**
 * The report objectives page (next page `/reportObjectives`, formerly Wicket's `ReportObjectivesPage`), for
 * finance and controlling only: upload of a ReportObjective XML, its evaluation over a month range and the
 * drill-down through the child objectives as BWA table.
 *
 * The evaluated report lives in the user's volatile prefs under [ReportStorage.USER_PREF_KEY] — the very entry
 * `AccountingRecordEntityRest.getReportRecords` reads for the accounting-record drill-down of a report or a
 * single BWA row. It is gone after logout or a restart.
 *
 * Refusals (no file, wrong type, unreadable XML, invalid period) are answered with HTTP 400 and the translated
 * text as body. The CSRF header of the next client is checked centrally by `RestCsrfProtection`.
 */
@RestController
@RequestMapping("${Rest.URL}/reportObjectives")
class ReportObjectivesPageRest {
    class PathEntry(val id: String?, val title: String?)

    /** A column of the BWA table: the current report (first) or one of its children. */
    class Column(val id: String?, val title: String?, val hasChildren: Boolean)

    /** A BWA row, with one amount per [Column]; formatted client-side by [scale] and [unit] (see `BwaRow`). */
    class Row(
        val no: String?,
        val id: String?,
        val title: String?,
        val indent: Int,
        val scale: Int,
        val unit: String?,
        /** Verbosity level of the row: the page shows only the important ones unless asked for all. */
        val priority: Priority?,
        val amounts: List<BigDecimal?>,
    )

    class ReportData(
        val id: String?,
        val title: String?,
        /** The evaluated period, formatted; null until the report was created. */
        val period: String?,
        /** From the root report down to the direct parent of the current report; empty for the root. */
        val path: List<PathEntry>,
        val columns: List<Column>,
        val rows: List<Row>,
    )

    class ReportObjectivesData(
        /** Name of the uploaded XML file; null if no report objective is stored or it was pasted. */
        val fileName: String? = null,
        /** First month of the period (`yyyy-MM`). */
        val fromMonth: String,
        /** Last month of the period (`yyyy-MM`). */
        val toMonth: String,
        /** True once the accounting records of the period were selected. */
        val loaded: Boolean = false,
        /** May the user open the accounting records of a report (`FIBU_DATEV_IMPORT`, see `getReportRecords`)? */
        val canShowRecords: Boolean = false,
        /** The current report; null if no report objective is stored. */
        val report: ReportData? = null,
    )

    class PeriodRequest(var fromMonth: String? = null, var toMonth: String? = null)

    class SelectRequest(var reportId: String? = null)

    /** A ReportObjective XML pasted by the user instead of uploading a file. */
    class PasteRequest(var xml: String? = null)

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var reportDao: ReportDao

    @Autowired
    private lateinit var userPrefService: UserPrefService

    @AccessChecked("FINANCE/CONTROLLING group (checkAccess)")
    @GetMapping
    fun getData(): ReportObjectivesData {
        checkAccess()
        return toData(getStorage())
    }

    @AccessChecked("FINANCE/CONTROLLING group + not restricted/demo (checkWriteAccess)")
    @PostMapping("upload")
    fun upload(@RequestParam("file") file: MultipartFile): ResponseEntity<*> {
        checkWriteAccess()
        val filename = file.originalFilename ?: "unknown"
        log.info { "Import of report objectives '$filename' (${file.size} bytes)." }
        if (file.isEmpty) {
            return ResponseEntity.badRequest().body(translate("file.upload.error.noFileSelected"))
        }
        FileCheck.checkFile(filename, file.size, "xml", megaBytes = MAX_FILE_SIZE_MB)?.let { error ->
            return ResponseEntity.badRequest().body(error)
        }
        val report = file.inputStream.use { reportDao.createReport(it) }
        return store(report, filename)
    }

    /** As [upload], but with the XML pasted into the page; the stored report then has no file name. */
    @AccessChecked("FINANCE/CONTROLLING group + not restricted/demo (checkWriteAccess)")
    @PostMapping("paste")
    fun paste(@RequestBody request: PasteRequest): ResponseEntity<*> {
        checkWriteAccess()
        val xml = request.xml?.trim()
        if (xml.isNullOrEmpty()) {
            return ResponseEntity.badRequest().body(translate("fibu.kost.reporting.error.noXml"))
        }
        if (xml.length > MAX_FILE_SIZE_MB * 1024 * 1024) {
            return ResponseEntity.badRequest().body(translate("fibu.kost.reporting.error.invalidXml"))
        }
        log.info { "Import of pasted report objectives (${xml.length} chars)." }
        val report = xml.byteInputStream(Charsets.UTF_8).use { reportDao.createReport(it) }
        return store(report, null)
    }

    private fun store(report: Report?, fileName: String?): ResponseEntity<*> {
        report ?: return ResponseEntity.badRequest().body(translate("fibu.kost.reporting.error.invalidXml"))
        val storage = ReportStorage(report)
        storage.fileName = fileName
        userPrefService.putEntry(UserPrefService.LEGACY_XML_AREA, ReportStorage.USER_PREF_KEY, storage, false)
        return ResponseEntity.ok(toData(storage))
    }

    /** Selects the accounting records of the given period, as Wicket's "create report" did. */
    @AccessChecked("FINANCE/CONTROLLING group + not restricted/demo (checkWriteAccess)")
    @PostMapping("create")
    fun create(@RequestBody request: PeriodRequest): ResponseEntity<*> {
        checkWriteAccess()
        val storage = getStorage() ?: return ResponseEntity.ok(toData(null))
        val from = parseMonth(request.fromMonth)
            ?: return ResponseEntity.badRequest().body(translate("fibu.kost.reporting.error.periodRequired"))
        val to = if (request.toMonth.isNullOrBlank()) from else parseMonth(request.toMonth)
            ?: return ResponseEntity.badRequest().body(translate("fibu.buchungssatz.error.invalidTimeperiod"))
        if (from.isAfter(to)) {
            return ResponseEntity.badRequest().body(translate("fibu.buchungssatz.error.invalidTimeperiod"))
        }
        log.info { "Create report of '${storage.fileName}' for $from - $to." }
        val root = storage.root
        val currentReportId = storage.currentReport.id
        root.setFrom(from.year, from.monthValue)
        root.setTo(to.year, to.monthValue)
        reportDao.loadReport(root)
        // The selection rebuilds the child reports, so the current one is looked up again by its id:
        storage.setCurrentReport(currentReportId)
        return ResponseEntity.ok(toData(storage))
    }

    /** Navigates to the given report (an ancestor of the current one or a child with children). */
    @AccessChecked("FINANCE/CONTROLLING group (checkAccess)")
    @PostMapping("select")
    fun select(@RequestBody request: SelectRequest): ReportObjectivesData {
        checkAccess()
        val storage = getStorage()
        val reportId = request.reportId
        if (storage != null && reportId != null) {
            storage.setCurrentReport(reportId)
        }
        return toData(storage)
    }

    /** Removes the stored report objective, so another one can be uploaded. */
    @AccessChecked("FINANCE/CONTROLLING group + not restricted/demo (checkWriteAccess)")
    @PostMapping("clear")
    fun clear(): ReportObjectivesData {
        checkWriteAccess()
        log.info { "Clear report." }
        userPrefService.removeEntry(UserPrefService.LEGACY_XML_AREA, ReportStorage.USER_PREF_KEY)
        return toData(null)
    }

    private fun getStorage(): ReportStorage? {
        return userPrefService.getEntry(
            UserPrefService.LEGACY_XML_AREA,
            ReportStorage.USER_PREF_KEY,
            ReportStorage::class.java,
        )
    }

    private fun toData(storage: ReportStorage?): ReportObjectivesData {
        val root = storage?.root
        val loaded = root?.isLoad == true
        val year = LocalDate.now().year
        // Without an evaluated period the current year is suggested, as Wicket did.
        val fromMonth = if (loaded) YearMonth.of(root.fromYear, root.fromMonth) else YearMonth.of(year, 1)
        val toMonth = if (loaded) YearMonth.of(root.toYear, root.toMonth) else YearMonth.of(year, 12)
        return ReportObjectivesData(
            fileName = storage?.fileName,
            fromMonth = fromMonth.toString(),
            toMonth = toMonth.toString(),
            loaded = loaded,
            canShowRecords = loaded &&
                    accessChecker.hasLoggedInUserRight(UserRightId.FIBU_DATEV_IMPORT, false, UserRightValue.TRUE),
            report = storage?.currentReport?.let { toReportData(it, loaded) },
        )
    }

    private fun toReportData(report: Report, loaded: Boolean): ReportData {
        val columns = mutableListOf(Column(report.id, report.title, report.hasChildren()))
        val rows = mutableListOf<Row>()
        if (loaded) {
            report.children?.forEach { columns.add(Column(it.id, it.title, it.hasChildren())) }
            val assessments = report.getChildBusinessAssessmentTable(true).businessAssessmentList.map { it.value }
            // The rows of the first BWA give the row meta data, the amounts are looked up per column:
            assessments.firstOrNull()?.rows?.forEach { row ->
                rows.add(
                    Row(
                        no = row.no,
                        id = row.id,
                        title = row.title,
                        indent = row.indent,
                        scale = row.scale,
                        unit = row.unit,
                        priority = row.priority,
                        amounts = assessments.map { it.getRow(row.id)?.amount },
                    )
                )
            }
        }
        return ReportData(
            id = report.id,
            title = report.title,
            // getZeitraum() fails as long as no period was set.
            period = if (loaded) report.zeitraum else null,
            path = report.path?.map { PathEntry(it.id, it.title) } ?: emptyList(),
            columns = columns,
            rows = rows,
        )
    }

    private fun parseMonth(value: String?): YearMonth? {
        if (value.isNullOrBlank()) {
            return null
        }
        return try {
            YearMonth.parse(value.trim())
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun checkAccess() {
        accessChecker.checkIsLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP)
    }

    private fun checkWriteAccess() {
        checkAccess()
        accessChecker.checkRestrictedOrDemoUser()
    }

    companion object {
        private const val MAX_FILE_SIZE_MB = 10L
    }
}
