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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.jobs.AbstractJob
import org.projectforge.plugins.cveradar.model.CvrCatalogKind
import org.projectforge.plugins.cveradar.model.CvrFindingCertainty
import org.projectforge.plugins.cveradar.model.CvrImportStateDO
import org.projectforge.plugins.cveradar.rest.AdHocRequest
import org.projectforge.plugins.cveradar.rest.AdHocStatus
import org.projectforge.plugins.cveradar.rest.CatalogNodeRequest
import org.projectforge.plugins.cveradar.rest.CvrAdHocRest
import org.projectforge.plugins.cveradar.rest.CvrCatalogRest
import org.projectforge.plugins.cveradar.rest.LibraryFile
import org.projectforge.plugins.cveradar.rest.PackageReportFormat
import org.projectforge.plugins.cveradar.sync.NvdCveParser
import org.projectforge.plugins.cveradar.sync.NvdCveParserTest
import org.projectforge.plugins.cveradar.sync.NvdCveParserTest.Companion.cve
import org.projectforge.plugins.cveradar.sync.NvdImportService
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

/**
 * The ad-hoc check: libraries of uploaded files against the index of all vulnerabilities, with the rules of the
 * inventory, nothing stored; every reader checks, a job and its report belong to the person who started it.
 */
class CvrAdHocCheckTest : CveRadarTestBase() {
    @Autowired
    private lateinit var catalog: CvrCatalogRest

    @Autowired
    private lateinit var catalogService: CvrCatalogService

    @Autowired
    private lateinit var importService: NvdImportService

    @Autowired
    private lateinit var rest: CvrAdHocRest

    @BeforeEach
    fun prepare() {
        persistenceService.runInTransaction { context ->
            context.em.find(CvrImportStateDO::class.java, NvdImportService.SOURCE)?.let { context.em.remove(it) }
        }
        logon(TEST_ADMIN_USER)
        catalogService.ensureEcosystems()
    }

    private fun lock(vararg packages: Pair<String, String>) =
        packages.joinToString(",", """{"lockfileVersion":3,"packages":{"":{"name":"app"},""", "}}") {
            """"node_modules/${it.first}":{"version":"${it.second}"}"""
        }

    private fun runToEnd(request: AdHocRequest): AdHocStatus {
        var status = rest.start(request)
        val jobId = status.jobId
        val until = System.currentTimeMillis() + 30_000
        while ((status.status == AbstractJob.Status.RUNNING || status.status == AbstractJob.Status.WAITING) &&
            System.currentTimeMillis() < until
        ) {
            Thread.sleep(50)
            status = rest.status(jobId)
        }
        return status
    }

    @Test
    fun `libraries are checked against all vulnerabilities per file, the report names the files, nothing is stored`() {
        // A library of the catalogue with its CPE (sure) and an unknown one with the same name pattern (only possible).
        logon(user("adhoc-st", CvrRole.SECURITY_TEAM))
        val npm = catalog.tree().rows.first { it.ecosystemKey == "npm" }.id
        catalog.create(
            CatalogNodeRequest(
                kind = CvrCatalogKind.LIBRARY, name = "adhoclib", parentIds = listOf(npm), cpeVendor = "adhocvendor",
                cpeProduct = "adhoclib", purlType = "npm", purlName = "adhoclib",
            ),
        )
        val configurations = """{"nodes":[{"operator":"OR","negate":false,"cpeMatch":[
            {"vulnerable":true,"criteria":"cpe:2.3:a:adhocvendor:adhoclib:*:*:*:*:*:*:*:*","versionEndExcluding":"4.17.21"},
            {"vulnerable":true,"criteria":"cpe:2.3:a:other:namedlib:*:*:*:*:*:*:*:*","versionEndExcluding":"2.0.0"}]}]}"""
        val metrics = """"cvssMetricV31":[{"type":"Primary","cvssData":{"vectorString":"CVSS:3.1/x","baseScore":9.8,"baseSeverity":"CRITICAL"}}]"""
        logon(TEST_ADMIN_USER)
        importService.import(
            { _, _, _ -> NvdCveParser.parsePage(NvdCveParserTest.page(cve("CVE-2026-97501", metrics = metrics, configurations = configurations))) },
            Instant.parse("2026-10-27T10:00:00Z"),
        )
        val findingsBefore = persistenceService.executeQuery("from CvrFindingDO f", org.projectforge.plugins.cveradar.model.CvrFindingDO::class.java).size
        val technologiesBefore = persistenceService.executeQuery("from CvrTechnologyDO t", org.projectforge.plugins.cveradar.model.CvrTechnologyDO::class.java).size

        val reader = user("adhoc-reader", CvrRole.READER)
        logon(reader)
        val status = runToEnd(
            AdHocRequest(
                listOf(
                    LibraryFile("a/package-lock.json", lock("adhoclib" to "4.17.20", "fine" to "1.0.0")),
                    LibraryFile("b/package-lock.json", lock("adhoclib" to "4.17.21", "namedlib" to "1.5.0")),
                    LibraryFile("c.txt", "nothing to read here"),
                ),
            ),
        )
        assertEquals(AbstractJob.Status.FINISHED, status.status)
        assertEquals(listOf(2, 2, 0), status.files.map { it.total })
        assertTrue(status.files.all { it.done && it.checked == it.total }, "every file counted to its end")
        assertEquals(listOf(1, 1, 0), status.files.map { it.hitCount }, "4.17.20 is affected, 4.17.21 is not; namedlib by name only")
        assertEquals(4, status.checkedCount)
        assertEquals(1, status.vulnerabilityCount)

        val report = rest.report(status.jobId, anonymize = false)
        assertTrue(report.scope.adHoc)
        assertEquals(1, report.vulnerabilityCount)
        val found = report.vulnerabilities.single().findings
        assertEquals(listOf("a/package-lock.json", "b/package-lock.json"), found.map { it.component })
        assertEquals(CvrFindingCertainty.AFFECTED, found[0].certainty)
        assertEquals("adhoclib", found[0].technology!!.product)
        assertEquals("pkg:npm/adhoclib@4.17.20", found[0].technology!!.purl)
        assertEquals(CvrFindingCertainty.POSSIBLE, found[1].certainty)
        val anonymous = rest.report(status.jobId, anonymize = true)
        assertTrue(anonymous.anonymized)
        assertEquals(listOf("F1", "F2"), anonymous.vulnerabilities.single().findings.map { it.component })

        // The report per package: JSON, PDF and Excel of the same content.
        val packages = rest.packageReport(status.jobId, PackageReportFormat.JSON, anonymize = true)
        assertTrue(packages.headers.contentDisposition.filename!!.endsWith(".json"))
        val json = com.fasterxml.jackson.databind.ObjectMapper().readTree(packages.body!!)
        assertEquals(2, json["packageCount"].asInt())
        val adhoclib = json["packages"].first { it["name"].asText() == "adhoclib" }
        assertEquals(listOf("F1"), adhoclib["components"].map { it.asText() }, "the files are anonymized")
        assertEquals("< 4.17.21", adhoclib["vulnerabilities"][0]["affectedRange"].asText())
        val pdf = rest.packageReport(status.jobId, PackageReportFormat.PDF, anonymize = true)
        assertEquals("application/pdf", pdf.headers.contentType.toString())
        val text = com.lowagie.text.pdf.parser.PdfTextExtractor(com.lowagie.text.pdf.PdfReader(pdf.body!!)).getTextFromPage(1)
        assertTrue(text.contains("adhoclib") && text.contains("CVE-2026-97501"), text)
        val xlsx = rest.packageReport(status.jobId, PackageReportFormat.XLSX, anonymize = false)
        org.apache.poi.xssf.usermodel.XSSFWorkbook(java.io.ByteArrayInputStream(xlsx.body!!)).use { workbook ->
            val sheet = workbook.getSheetAt(1)
            assertEquals(3, sheet.physicalNumberOfRows, "header and one row per package and vulnerability")
            val row = sheet.getRow(1)
            assertEquals("CVE-2026-97501", row.getCell(3).stringCellValue)
            assertEquals("https://nvd.nist.gov/vuln/detail/CVE-2026-97501", row.getCell(3).hyperlink.address)
            assertEquals(9.8, row.getCell(5).numericCellValue, 0.001)
        }

        assertEquals(
            findingsBefore,
            persistenceService.executeQuery("from CvrFindingDO f", org.projectforge.plugins.cveradar.model.CvrFindingDO::class.java).size,
            "no finding stored",
        )
        assertEquals(
            technologiesBefore,
            persistenceService.executeQuery("from CvrTechnologyDO t", org.projectforge.plugins.cveradar.model.CvrTechnologyDO::class.java).size,
            "no technology stored",
        )
    }

    @Test
    fun `an ordinary user is refused, a job belongs to the person who started it, files are checked`() {
        logon(TEST_USER)
        assertThrows(AccessException::class.java) { rest.start(AdHocRequest(listOf(LibraryFile("l.json", lock("x" to "1.0.0"))))) }
        val owner = user("adhoc-owner", CvrRole.READER)
        logon(owner)
        assertThrows(UserException::class.java) { rest.start(AdHocRequest()) }
        val status = runToEnd(AdHocRequest(listOf(LibraryFile("l.json", lock("x" to "1.0.0")))))
        assertNotNull(status.jobId)
        assertEquals(AbstractJob.Status.FINISHED, status.status)
        assertEquals(0, rest.report(status.jobId, true).vulnerabilityCount)

        val other = user("adhoc-other", CvrRole.READER)
        logon(other)
        assertThrows(UserException::class.java) { rest.status(status.jobId) }
        assertThrows(UserException::class.java) { rest.report(status.jobId, true) }
        assertThrows(UserException::class.java) { rest.packageReport(status.jobId, PackageReportFormat.PDF, true) }
        logon(TEST_USER)
        assertThrows(AccessException::class.java) { rest.status(status.jobId) }
    }
}
