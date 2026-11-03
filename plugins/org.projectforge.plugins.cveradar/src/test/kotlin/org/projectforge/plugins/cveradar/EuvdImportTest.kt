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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.plugins.cveradar.model.CvrCpeMatchDO
import org.projectforge.plugins.cveradar.model.CvrFindingCertainty
import org.projectforge.plugins.cveradar.model.CvrImportStateDO
import org.projectforge.plugins.cveradar.model.CvrVulnerabilityDO
import org.projectforge.plugins.cveradar.sync.EuvdImportService
import org.projectforge.plugins.cveradar.sync.EuvdPage
import org.projectforge.plugins.cveradar.sync.EuvdParser
import org.projectforge.plugins.cveradar.sync.EuvdParserTest
import org.projectforge.plugins.cveradar.sync.EuvdSource
import org.projectforge.plugins.cveradar.sync.NvdCveParser
import org.projectforge.plugins.cveradar.sync.NvdCveParserTest
import org.projectforge.plugins.cveradar.sync.NvdImportService
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.Date

/**
 * The import of the EUVD (IMP-16): a vulnerability only the EUVD knows, the supplement of one the NVD made, the key
 * (CVE id or EUVD id), the threshold, the sweep of the exploited entries and the match by product names (possible only).
 * Every test has CVE and EUVD ids of its own, as the tests share the database.
 */
class EuvdImportTest : CveRadarTestBase() {
    @Autowired
    private lateinit var euvdImport: EuvdImportService

    @Autowired
    private lateinit var nvdImport: NvdImportService

    @Autowired
    private lateinit var matching: CvrMatchingService

    @Autowired
    private lateinit var vulnerabilityDao: CvrVulnerabilityDao

    /** Answers the publications on the first request only (the import asks once per week), the exploited ones on every. */
    private class FakeEuvd(private val publications: List<String>, private val exploited: List<String> = emptyList()) : EuvdSource {
        val requests = mutableListOf<String>()
        private var served = false

        override fun search(from: LocalDate?, to: LocalDate?, minScore: Double?, exploited: Boolean, page: Int): EuvdPage {
            requests.add("$from..$to exploited=$exploited")
            val items = if (exploited) {
                this.exploited
            } else if (served) {
                emptyList()
            } else {
                publications.also { served = true }
            }
            return EuvdParser.parsePage(EuvdParserTest.page(*items.toTypedArray()))
        }
    }

    @BeforeEach
    fun resetImportState() {
        persistenceService.runInTransaction { context ->
            listOf(EuvdImportService.SOURCE, EuvdImportService.SWEEP_SOURCE).forEach { source ->
                context.em.find(CvrImportStateDO::class.java, source)?.let { context.em.remove(it) }
            }
        }
    }

    private val now = Instant.parse("2026-11-03T17:00:00Z")

    private fun item(id: String, cve: String?, score: String = "8.1", products: String = "wlroots", exploited: String? = null) =
        EuvdParserTest.item(
            id = id, aliases = listOfNotNull(cve, "GHSA-$id").joinToString("\\n"), score = score,
            version = "3.1", vector = "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N", exploitedSince = exploited, product = products,
        )

    private fun nvd(cveId: String, product: String, status: String = "Analyzed") {
        persistenceService.runInTransaction { context ->
            context.em.find(CvrImportStateDO::class.java, NvdImportService.SOURCE)?.let { context.em.remove(it) }
        }
        val configurations = """{"nodes":[{"operator":"OR","negate":false,"cpeMatch":[
            {"vulnerable":true,"criteria":"cpe:2.3:a:vendor:$product:*:*:*:*:*:*:*:*","versionEndExcluding":"2.0"}]}]}"""
        val metrics = """"cvssMetricV31":[{"type":"Primary","cvssData":{"vectorString":"CVSS:3.1/x","baseScore":9.8,"baseSeverity":"CRITICAL"}}]"""
        nvdImport.import(
            { _, _, _ -> NvdCveParser.parsePage(NvdCveParserTest.page(NvdCveParserTest.cve(cveId, status = status, metrics = metrics, configurations = configurations))) },
            now,
        )
    }

    private fun indexOf(vulnerabilityId: Long) = persistenceService.executeQuery(
        "from CvrCpeMatchDO m where m.vulnerability.id = :id", CvrCpeMatchDO::class.java, Pair("id", vulnerabilityId),
    )

    @Test
    fun `a vulnerability only the EUVD knows has the EUVD id as key without CVE id, the NVD takes it over later`() {
        logon(TEST_ADMIN_USER)
        val source = FakeEuvd(listOf(item("EUVD-2026-990001", null, products = "eutool1")))
        val result = euvdImport.import(source, now)
        assertEquals(1, result.created)
        val vulnerability = vulnerabilityDao.findByCveId("EUVD-2026-990001")!!
        assertEquals("EUVD-2026-990001", vulnerability.euvdId)
        assertEquals(EuvdImportService.STATUS_EUVD, vulnerability.nvdStatus)
        assertNull(vulnerability.nvdLastModified)
        assertEquals(BigDecimal("8.1"), vulnerability.cvssScore)
        assertEquals("HIGH", vulnerability.severity)
        assertEquals(BigDecimal("8.1"), vulnerability.cvss31Score)
        assertNull(vulnerability.cvss40Score)
        assertEquals("An out-of-bounds read.", vulnerability.description)
        assertEquals(BigDecimal("0.00430"), vulnerability.epss)
        assertEquals("2026-10-04..2026-10-10 exploited=false", source.requests.first(), "30 days back, in pieces of a week")
        assertEquals("2026-11-01..2026-11-03 exploited=false", source.requests[source.requests.size - 2])
        assertEquals("null..null exploited=true", source.requests.last(), "then the sweep")

        // The EUVD gives it a CVE id: the key follows, nothing is duplicated.
        euvdImport.import(FakeEuvd(listOf(item("EUVD-2026-990001", "CVE-2026-990001", products = "eutool1"))), now.plusSeconds(60))
        assertNull(vulnerabilityDao.findByCveId("EUVD-2026-990001"))
        assertEquals("EUVD-2026-990001", vulnerabilityDao.findByCveId("CVE-2026-990001")!!.euvdId)
        assertEquals(1, persistenceService.executeQuery(
            "from CvrVulnerabilityDO v where v.euvdId = :id", CvrVulnerabilityDO::class.java, Pair("id", "EUVD-2026-990001"),
        ).size)

        // The NVD modifies the CVE: it takes the details over and keeps the supplement of the EUVD.
        nvd("CVE-2026-990001", "eutool1")
        val taken = vulnerabilityDao.findByCveId("CVE-2026-990001")!!
        assertEquals("Analyzed", taken.nvdStatus)
        assertEquals(BigDecimal("9.8"), taken.cvssScore)
        assertEquals("EUVD-2026-990001", taken.euvdId)
        assertEquals(BigDecimal("0.00430"), taken.epss)
        assertEquals(1, indexOf(taken.id!!).size, "the CPE of the NVD is the index")
        assertTrue(indexOf(taken.id!!).none { it.approximate }, "the entries of the EUVD are gone")
    }

    @Test
    fun `a vulnerability of the NVD only gets the EUVD id, the EPSS and the day of exploitation`() {
        logon(TEST_ADMIN_USER)
        nvd("CVE-2026-990002", "eutool2")
        val before = vulnerabilityDao.findByCveId("CVE-2026-990002")!!
        val description = before.description
        euvdImport.import(FakeEuvd(listOf(item("EUVD-2026-990002", "CVE-2026-990002", score = "5.0", exploited = "Nov 2, 2026, 10:00:00 AM"))), now)

        val after = vulnerabilityDao.findByCveId("CVE-2026-990002")!!
        assertEquals("EUVD-2026-990002", after.euvdId)
        assertEquals(BigDecimal("0.00430"), after.epss)
        assertEquals(Date.from(Instant.parse("2026-11-02T10:00:00Z")), after.exploitedSince)
        assertEquals(description, after.description, "the NVD stays the source of the details")
        assertEquals(BigDecimal("9.8"), after.cvssScore)
        assertEquals("Analyzed", after.nvdStatus)
        assertNotNull(after.nvdLastModified)
        assertTrue(indexOf(after.id!!).none { it.approximate }, "precise by CPE, no entry of the EUVD")
    }

    @Test
    fun `new vulnerabilities only from the threshold on, a second id of one CVE and duplicates in a page create one`() {
        logon(TEST_ADMIN_USER)
        val result = euvdImport.import(
            FakeEuvd(
                listOf(
                    item("EUVD-2026-990003", "CVE-2026-990003", score = "3.0"),
                    item("EUVD-2026-990004", "CVE-2026-990004", score = "0.0"),
                    item("EUVD-2026-990005", "CVE-2026-990005"),
                    item("EUVD-2026-990006", "CVE-2026-990005"),
                ),
            ),
            now,
        )
        assertEquals(1, result.created)
        assertEquals(2, result.skipped)
        assertNull(vulnerabilityDao.findByCveId("CVE-2026-990003"))
        assertNull(vulnerabilityDao.findByCveId("CVE-2026-990004"))
        assertEquals("EUVD-2026-990005", vulnerabilityDao.findByCveId("CVE-2026-990005")!!.euvdId, "the first id stays")
        assertNull(vulnerabilityDao.findByEuvdIds(listOf("EUVD-2026-990006"))["EUVD-2026-990006"])
    }

    @Test
    fun `the watermark follows the weeks read, the next run reads the last days again, the sweep runs once a day and creates nothing`() {
        logon(TEST_ADMIN_USER)
        val source = FakeEuvd(emptyList(), exploited = listOf(item("EUVD-2026-990007", "CVE-2026-990007", exploited = "Nov 2, 2026, 10:00:00 AM")))
        val first = euvdImport.import(source, now)
        assertTrue(first.swept)
        assertEquals(1, first.skipped, "not stored: the sweep creates no vulnerability")
        assertNull(vulnerabilityDao.findByCveId("CVE-2026-990007"))
        val state = persistenceService.find(CvrImportStateDO::class.java, EuvdImportService.SOURCE)!!
        assertEquals(Date.from(now), state.lastModWatermark, "capped at now")

        source.requests.clear()
        val second = euvdImport.import(source, now.plusSeconds(3600))
        assertFalse(second.swept, "less than a day since the last sweep")
        assertEquals("2026-10-27..2026-11-02 exploited=false", source.requests.first(), "7 days before the watermark day, in pieces of a week")
        assertEquals(2, source.requests.size)
    }

    @Test
    fun `the product names of the EUVD match as possible only, even by CPE and version, until the NVD gives a CPE`() {
        logon(TEST_ADMIN_USER)
        euvdImport.import(FakeEuvd(listOf(item("EUVD-2026-990008", null, products = "eutool8"))), now)
        val id = vulnerabilityDao.findByCveId("EUVD-2026-990008")!!.id!!
        val index = indexOf(id)
        assertEquals(1, index.size)
        assertTrue(index.single().approximate)
        assertEquals("eutool8", index.single().product)

        fun hit(vendor: String) = matching.match(
            listOf(CvrMatchingService.Query("cpe:2.3:a:$vendor:eutool8:1.5:*:*:*:*:*:*:*", vendor, "eutool8", "1.5", null)),
        ).single().filter { it.vulnerabilityId == id }
        assertEquals(CvrFindingCertainty.POSSIBLE, hit("wlroots").single().certainty, "the vendor of the EUVD")
        assertTrue(hit("someothervendor").isEmpty(), "the names alone are too common without the vendor")

        // Again: the index doesn't change.
        euvdImport.import(FakeEuvd(listOf(item("EUVD-2026-990008", null, products = "eutool8"))), now.plusSeconds(60))
        assertEquals(index.single().id, indexOf(id).single().id)

        // The NVD has a CPE for it (and the key of the CVE): sure.
        euvdImport.import(FakeEuvd(listOf(item("EUVD-2026-990008", "CVE-2026-990008", products = "eutool8"))), now.plusSeconds(120))
        nvd("CVE-2026-990008", "eutool8")
        val sure = matching.match(
            listOf(CvrMatchingService.Query("cpe:2.3:a:vendor:eutool8:1.5:*:*:*:*:*:*:*", "vendor", "eutool8", "1.5", null)),
        ).single().filter { it.vulnerabilityId == id }
        assertEquals(CvrFindingCertainty.AFFECTED, sure.single().certainty)
    }

    @Test
    fun `the NVD made a record for a CVE the EUVD knew without it, the EUVD id moves to it`() {
        logon(TEST_ADMIN_USER)
        euvdImport.import(FakeEuvd(listOf(item("EUVD-2026-990009", null, products = "eutool9"))), now)
        nvd("CVE-2026-990009", "eutool9")
        // Does not fail on the unique EUVD id, a page with two ids of one CVE neither.
        euvdImport.import(
            FakeEuvd(listOf(item("EUVD-2026-990009", "CVE-2026-990009", products = "eutool9"), item("EUVD-2026-990010", "CVE-2026-990009", products = "eutool9"))),
            now.plusSeconds(60),
        )
        assertEquals("EUVD-2026-990009", vulnerabilityDao.findByCveId("CVE-2026-990009")!!.euvdId)
    }
}
