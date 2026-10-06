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

import jakarta.persistence.EntityManagerFactory
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.fibu.KontoCache
import org.projectforge.business.fibu.KontoDO
import org.projectforge.business.fibu.kost.SHType
import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.fibu.kost.reporting.ReportStorage
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.framework.access.AccessException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The report objectives page: upload, evaluation, navigation and reset of a report.
 *
 * Worth asserting is mostly the hand-over to the accounting-record drill-down: the report has to land in the
 * user prefs under [ReportStorage.USER_PREF_KEY], where `AccountingRecordEntityRest.getReportRecords` looks
 * for it, and the column ids sent to the client have to be the ids that lookup resolves.
 */
class ReportObjectivesPageRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var reportObjectivesPageRest: ReportObjectivesPageRest

    @Autowired
    private lateinit var userPrefService: UserPrefService

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    @Autowired
    private lateinit var kontoCache: KontoCache

    @Autowired
    private lateinit var kostCache: KostCache

    @Test
    fun `upload, create, drill down and clear`() {
        logon(TEST_CONTROLLING_USER)
        val uploaded = upload(XML)
        assertEquals(HttpStatus.OK, uploaded.statusCode)
        var data = uploaded.body as ReportObjectivesPageRest.ReportObjectivesData
        assertEquals("objectives.xml", data.fileName)
        assertFalse(data.loaded)
        assertEquals("ACME", data.report?.id)
        // Nothing evaluated yet, so neither a period nor a table:
        assertNull(data.report?.period)
        assertTrue(data.report!!.rows.isEmpty())

        val storage = getStorage()
        assertNotNull(storage)
        assertEquals("objectives.xml", storage!!.fileName)

        data = create("2020-01", "2020-03")
        assertTrue(data.loaded)
        assertEquals("2020-01", data.fromMonth)
        assertEquals("2020-03", data.toMonth)
        val report = data.report!!
        assertNotNull(report.period)
        assertTrue(report.path.isEmpty())
        assertEquals(
            listOf("ACME", "ACME-WEB-Portal", "ACME-Java-Migration"),
            report.columns.map { it.id },
        )
        assertEquals(listOf(true, true, false), report.columns.map { it.hasChildren })
        report.rows.forEach { assertEquals(report.columns.size, it.amounts.size, it.no) }
        // The column ids are the ones the accounting-record drill-down resolves:
        report.columns.forEach { assertNotNull(getStorage()!!.findById(it.id), it.id) }

        data = reportObjectivesPageRest.select(ReportObjectivesPageRest.SelectRequest("ACME-WEB-Portal"))
        assertEquals("ACME-WEB-Portal", data.report?.id)
        assertEquals(listOf("ACME"), data.report!!.path.map { it.id })
        assertEquals(listOf("ACME-WEB-Portal", "ACME-WEB-Portal-Frontend"), data.report.columns.map { it.id })

        // A new evaluation keeps the report the user navigated to:
        data = create("2020-02", null)
        assertEquals("ACME-WEB-Portal", data.report?.id)
        assertEquals("2020-02", data.toMonth)

        data = reportObjectivesPageRest.clear()
        assertNull(data.report)
        assertNull(data.fileName)
        assertNull(getStorage())
    }

    @Test
    fun `refusals are answered with 400`() {
        logon(TEST_CONTROLLING_USER)
        assertEquals(HttpStatus.BAD_REQUEST, upload("no xml at all").statusCode)
        assertEquals(
            HttpStatus.BAD_REQUEST,
            reportObjectivesPageRest.upload(
                MockMultipartFile("file", "objectives.txt", null, XML.toByteArray())
            ).statusCode,
        )
        assertNull(getStorage())
        upload(XML)
        val stored = getStorage()
        val response = reportObjectivesPageRest.create(ReportObjectivesPageRest.PeriodRequest("2020-05", "2020-04"))
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(
            HttpStatus.BAD_REQUEST,
            reportObjectivesPageRest.create(ReportObjectivesPageRest.PeriodRequest(null, "2020-04")).statusCode,
        )
        // Nothing was evaluated and the stored report is untouched:
        assertSame(stored, getStorage())
        assertFalse(stored!!.root.isLoad)
        reportObjectivesPageRest.clear()
    }

    @Test
    fun `pasted xml is stored like an uploaded file`() {
        logon(TEST_FINANCE_USER)
        val response = reportObjectivesPageRest.paste(ReportObjectivesPageRest.PasteRequest("  $XML\n"))
        assertEquals(HttpStatus.OK, response.statusCode, "${response.body}")
        val data = response.body as ReportObjectivesPageRest.ReportObjectivesData
        assertEquals("ACME", data.report?.id)
        assertNull(data.fileName)
        assertEquals("ACME", getStorage()?.root?.id)
        assertEquals(
            HttpStatus.BAD_REQUEST,
            reportObjectivesPageRest.paste(ReportObjectivesPageRest.PasteRequest(" ")).statusCode,
        )
        assertEquals(
            HttpStatus.BAD_REQUEST,
            reportObjectivesPageRest.paste(ReportObjectivesPageRest.PasteRequest("<foo/>")).statusCode,
        )
        // A refused paste keeps the stored report:
        assertEquals("ACME", getStorage()?.root?.id)
        reportObjectivesPageRest.clear()
    }

    /**
     * The selection formats kost1 and kost2 of every record: they (and the accounts) have to come from the caches,
     * not lazily one query per distinct cost unit.
     */
    @Test
    fun `create report with a constant number of statements, not one per cost unit`() {
        persistenceService.runInTransaction { context ->
            val em = context.em
            val art = Kost2ArtDO().also { it.id = 97L; it.name = "Report objectives test" }
            em.persist(art)
            val konto = KontoDO().also { it.nummer = 4400; it.bezeichnung = "Revenue" }
            val gegenKonto = KontoDO().also { it.nummer = 10000; it.bezeichnung = "Customer" }
            em.persist(konto)
            em.persist(gegenKonto)
            // One record in January, six in February, each with its own kost1 and kost2:
            (0..6).forEach { i ->
                val kost1 = Kost1DO().also { it.nummernkreis = 3; it.bereich = 1; it.teilbereich = 1; it.endziffer = i }
                val kost2 = Kost2DO().also {
                    it.nummernkreis = 5; it.bereich = 20; it.teilbereich = i; it.kost2Art = art
                }
                em.persist(kost1)
                em.persist(kost2)
                val month = if (i == 0) 1 else 2
                em.persist(BuchungssatzDO().also {
                    it.year = 2030; it.month = month; it.satznr = i + 1
                    it.datum = LocalDate.of(2030, month, 15)
                    it.betrag = BigDecimal(100 + i); it.sh = SHType.HABEN
                    it.konto = konto; it.gegenKonto = gegenKonto
                    it.kost1 = kost1; it.kost2 = kost2
                })
            }
        }
        // Inserted directly, so the caches don't know them yet:
        kostCache.forceReload()
        kontoCache.forceReload()
        logon(TEST_CONTROLLING_USER)
        upload(XML)
        val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        val wasEnabled = statistics.isStatisticsEnabled
        statistics.isStatisticsEnabled = true
        try {
            fun statementsOf(month: String): Long {
                statistics.clear()
                assertTrue(create(month, month).loaded)
                return statistics.prepareStatementCount
            }
            statementsOf("2030-01") // Warms up caches (users, rights) not belonging to the report itself.
            assertEquals(statementsOf("2030-01"), statementsOf("2030-02"))
        } finally {
            statistics.isStatisticsEnabled = wasEnabled
            reportObjectivesPageRest.clear()
        }
    }

    @Test
    fun `only finance and controlling`() {
        logon(TEST_USER)
        assertThrows<AccessException> { reportObjectivesPageRest.getData() }
        assertThrows<AccessException> { upload(XML) }
        assertThrows<AccessException> { reportObjectivesPageRest.paste(ReportObjectivesPageRest.PasteRequest(XML)) }
    }

    private fun upload(xml: String) =
        reportObjectivesPageRest.upload(MockMultipartFile("file", "objectives.xml", null, xml.toByteArray()))

    private fun create(from: String, to: String?): ReportObjectivesPageRest.ReportObjectivesData {
        val response = reportObjectivesPageRest.create(ReportObjectivesPageRest.PeriodRequest(from, to))
        assertEquals(HttpStatus.OK, response.statusCode, "${response.body}")
        return response.body as ReportObjectivesPageRest.ReportObjectivesData
    }

    private fun getStorage(): ReportStorage? =
        userPrefService.getEntry(UserPrefService.LEGACY_XML_AREA, ReportStorage.USER_PREF_KEY, ReportStorage::class.java)

    companion object {
        private val XML = """
            <ReportObjective title="Customer ACME" id="ACME" suppressOther="true" suppressDuplicates="true">
              <kost2-include>5.*</kost2-include>
              <ReportObjective title="Project ACME-WEB-Portal" id="ACME-WEB-Portal" suppressOther="true" suppressDuplicates="true">
                <kost2-include>5.020.01.*</kost2-include>
                <ReportObjective title="Frontend" id="ACME-WEB-Portal-Frontend">
                  <kost2-include>5.020.01.01</kost2-include>
                </ReportObjective>
              </ReportObjective>
              <ReportObjective title="Project ACME-Java-Migration" id="ACME-Java-Migration">
                <kost2-include>5.020.02.*</kost2-include>
              </ReportObjective>
            </ReportObjective>
        """.trimIndent()
    }
}
