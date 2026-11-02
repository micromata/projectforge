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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.plugins.cveradar.model.CvrCaseKind
import org.projectforge.plugins.cveradar.model.CvrCaseStatus
import org.projectforge.plugins.cveradar.model.CvrImportStateDO
import org.projectforge.plugins.cveradar.model.CvrPriority
import org.projectforge.plugins.cveradar.rest.CaseImportRequest
import org.projectforge.plugins.cveradar.rest.CvrCaseFileRest
import org.projectforge.plugins.cveradar.sync.NvdCveParser
import org.projectforge.plugins.cveradar.sync.NvdCveParserTest
import org.projectforge.plugins.cveradar.sync.NvdCveParserTest.Companion.cve
import org.projectforge.plugins.cveradar.sync.NvdImportService
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

/**
 * A new case from the answer of an AI system (AI-01 ff.): the context for the prompt (readers), the case created by the
 * security team only, linked to the imported CVE, with the rules of a new case (code, priority reason, lengths).
 */
class CvrCaseAiImportTest : CveRadarTestBase() {
    @Autowired
    private lateinit var fileRest: CvrCaseFileRest

    @Autowired
    private lateinit var caseDao: CvrCaseDao

    @Autowired
    private lateinit var importService: NvdImportService

    @Autowired
    private lateinit var vulnerabilityDao: CvrVulnerabilityDao

    @BeforeEach
    fun resetImportState() {
        persistenceService.runInTransaction { context ->
            context.em.find(CvrImportStateDO::class.java, NvdImportService.SOURCE)?.let { context.em.remove(it) }
        }
    }

    private fun request(code: String?, kind: CvrCaseKind = CvrCaseKind.CVE, priority: CvrPriority? = null, reason: String? = null, title: String = " Remote code execution ") =
        CaseImportRequest(
            kind = kind, code = code, title = title, description = "**Summary**",
            priority = priority, priorityReason = reason, fixVersion = "9.0.90", workaround = "<p>Disable it</p>",
            techMeasures = "<ul><li>Update</li></ul>", nonTechMeasures = "<p>Nothing to do</p>",
            referenceLinks = "https://example.org/advisory Advisory\njavascript:alert(1) bad", clientAdvisory = true,
        )

    @Test
    fun `an ordinary user and a reader are refused, the security team creates the case, linked to the imported CVE`() {
        logon(TEST_ADMIN_USER)
        importService.import(
            { _, _, _ -> NvdCveParser.parsePage(NvdCveParserTest.page(cve("CVE-2026-96001", metrics = SCORE))) },
            Instant.parse("2026-10-27T10:00:00Z"),
        )
        val vulnerabilityId = vulnerabilityDao.findByCveId("CVE-2026-96001")!!.id
        logon(TEST_USER)
        assertThrows(AccessException::class.java) { fileRest.aiContext("CVE-2026-96001") }
        assertThrows(AccessException::class.java) { fileRest.importCase(request("CVE-2026-96001")) }
        val reader = user("reader", CvrRole.READER)
        logon(reader)
        val context = fileRest.aiContext(" cve-2026-96001 ")
        assertEquals(vulnerabilityId, context.vulnerability!!.id)
        assertNull(context.caseId)
        assertThrows(UserException::class.java) { fileRest.aiContext("log4shell") }
        assertThrows(AccessException::class.java) { fileRest.importCase(request("CVE-2026-96001")) }

        val team = user("team", CvrRole.SECURITY_TEAM)
        logon(team)
        val file = fileRest.importCase(request("cve-2026-96001", priority = CvrPriority.P2, reason = "Exploited"))
        val case = file.case!!
        assertEquals("CVE-2026-96001", case.code)
        assertEquals(vulnerabilityId, file.vulnerability!!.id, "linked to the imported vulnerability")
        assertEquals("Remote code execution", case.title)
        assertEquals(CvrCaseStatus.NEW, case.status)
        assertEquals(CvrPriority.P2, case.priority)
        assertEquals(team.id, case.assigneeId)
        assertEquals("9.0.90", case.fixVersion)
        assertEquals(listOf("https://example.org/advisory"), case.referenceLinks.map { it.url }, "only web links")
        assertEquals(case.id, fileRest.aiContext("CVE-2026-96001").caseId)
        assertThrows(UserException::class.java) { fileRest.importCase(request("CVE-2026-96001")) }
    }

    @Test
    fun `a CVE not imported yet, an incident with its own code, the priority reason, the lengths and the CVE id are checked`() {
        logon(user("team2", CvrRole.SECURITY_TEAM))
        val context = fileRest.aiContext("CVE-2026-96002")
        assertNull(context.vulnerability, "not imported")
        val cve = fileRest.importCase(request("CVE-2026-96002"))
        assertNull(cve.vulnerability)
        assertEquals("CVE-2026-96002", cve.case!!.code)
        // A deleted case is not offered (aiContext), and the import revives it rather than failing on its code.
        caseDao.markAsDeleted(caseDao.find(cve.case!!.id, checkAccess = false)!!, checkAccess = false)
        assertNull(fileRest.aiContext("CVE-2026-96002").caseId)
        val revived = fileRest.importCase(request("CVE-2026-96002", title = "Revived")).case!!
        assertEquals(cve.case!!.id, revived.id)
        assertEquals("Revived", revived.title)
        assertEquals(revived.id, fileRest.aiContext("CVE-2026-96002").caseId)

        val incident = fileRest.importCase(request("CVE-2026-96003", kind = CvrCaseKind.INCIDENT)).case!!
        assertEquals(CvrCaseKind.INCIDENT, incident.kind)
        assertTrue(!incident.code!!.startsWith("CVE-"), "the next incident code, not the one of the request")

        assertThrows(UserException::class.java) { fileRest.importCase(request("CVE-2026-96004", priority = CvrPriority.P1)) }
        assertThrows(UserException::class.java) { fileRest.importCase(request("not-a-cve")) }
        assertThrows(UserException::class.java) {
            fileRest.importCase(CaseImportRequest(kind = CvrCaseKind.INCIDENT, title = " "))
        }
        val tooLong = assertThrows(UserException::class.java) {
            fileRest.importCase(CaseImportRequest(kind = CvrCaseKind.INCIDENT, title = "Incident", fixVersion = "1".repeat(256)))
        }
        assertEquals(CvrCaseFileService.ERROR_AI_TOO_LONG, tooLong.i18nKey)
        assertNotNull(fileRest.importCase(CaseImportRequest(kind = CvrCaseKind.INCIDENT, title = "Incident")).case)
    }

    @Test
    fun `a case exported as a file is imported again as a new case, readers export, the security team imports`() {
        val team = user("team3", CvrRole.SECURITY_TEAM)
        logon(team)
        val original = fileRest.importCase(
            CaseImportRequest(
                kind = CvrCaseKind.INCIDENT, title = "Compromised package", description = "**npm**",
                priority = CvrPriority.P1, priorityReason = "Exploited", techMeasures = "<ul><li>Pin</li></ul>",
                referenceLinks = "https://example.org/a A", clientAdvisory = true,
                minClientAgentVersion = "2.4.1", clientAgentCheckId = "pkg-check",
            ),
        ).case!!
        logon(TEST_USER)
        assertThrows(AccessException::class.java) { fileRest.exportCase(original.id) }
        val reader = user("reader3", CvrRole.READER)
        logon(reader)
        val export = fileRest.exportCase(original.id)
        assertEquals(CvrCaseFileService.CASE_EXPORT_FORMAT, export.format)
        assertEquals("<ul><li>Pin</li></ul>", export.case.techMeasures, "as stored")
        assertEquals("pkg-check", export.case.clientAgentCheckId)
        assertThrows(AccessException::class.java) { fileRest.importCase(export.case) }

        logon(team)
        val copy = fileRest.importCase(export.case).case!!
        assertTrue(copy.id != original.id && copy.code != original.code, "a new incident with the next code")
        val again = fileRest.exportCase(copy.id).case
        assertEquals("Compromised package", again.title)
        assertEquals("**npm**", again.description)
        assertEquals(CvrPriority.P1, again.priority)
        assertEquals("https://example.org/a A", again.referenceLinks)
        assertTrue(again.clientAdvisory)
        assertEquals("2.4.1", again.minClientAgentVersion)
        assertEquals("pkg-check", again.clientAgentCheckId)
    }

    companion object {
        /** Below the minimum score of the configuration, a new CVE isn't imported. */
        private const val SCORE =
            """"cvssMetricV31":[{"type":"Primary","cvssData":{"vectorString":"CVSS:3.1/x","baseScore":9.8,"baseSeverity":"CRITICAL"}}]"""
    }
}
