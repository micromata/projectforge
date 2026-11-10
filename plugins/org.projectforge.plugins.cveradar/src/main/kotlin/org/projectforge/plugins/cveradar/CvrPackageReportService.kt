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

package org.projectforge.plugins.cveradar

import com.fasterxml.jackson.databind.ObjectMapper
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.rest.InventoryLevel
import org.projectforge.plugins.cveradar.rest.PackageReport
import org.projectforge.plugins.cveradar.rest.PackageReportFormat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat

/**
 * The report per package ([PackageReport]) of an ad-hoc check or of a node of the inventory, as JSON, PDF or Excel
 * file. Made of the finding report of [CvrAdHocCheckService] or [CvrFindingReportService], which check the access and
 * leave out what the user may not see; this service adds nothing of its own.
 */
@Service
class CvrPackageReportService {
    @Autowired
    private lateinit var adHocCheckService: CvrAdHocCheckService

    @Autowired
    private lateinit var findingReportService: CvrFindingReportService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    /** The report of a finished ad-hoc check of [user] (see [CvrAdHocCheckService.report]). */
    fun adHoc(user: PFUserDO, jobId: Int, anonymize: Boolean, exclude: Set<String>): PackageReport =
        CvrPackageReportBuilder.build(adHocCheckService.report(user, jobId, anonymize, exclude))

    /** The report of the open findings below a node (see [CvrFindingReportService.report]). */
    fun node(user: PFUserDO, level: InventoryLevel, id: Long, anonymize: Boolean, exclude: Set<String>): PackageReport =
        CvrPackageReportBuilder.build(findingReportService.report(user, level, id, anonymize, exclude))

    fun render(report: PackageReport, format: PackageReportFormat): ByteArray = when (format) {
        PackageReportFormat.JSON -> objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(report)
        PackageReportFormat.PDF -> CvrPackageReportPdf.render(report)
        PackageReportFormat.XLSX -> CvrPackageReportExcel.render(report)
    }

    /** `cve-report-<code>-<yyyy-MM-dd>.<ext>`, as the client names the finding report (`findingReportFilename`). */
    fun filename(report: PackageReport, format: PackageReportFormat): String {
        val code = (report.scope.code ?: report.scope.level?.name ?: "adhoc")
            .replace(Regex("[^A-Za-z0-9._-]+"), "-").trim('-').ifEmpty { "export" }
        return "cve-report-packages-$code-${SimpleDateFormat("yyyy-MM-dd").format(report.generated)}.${format.extension}"
    }
}
