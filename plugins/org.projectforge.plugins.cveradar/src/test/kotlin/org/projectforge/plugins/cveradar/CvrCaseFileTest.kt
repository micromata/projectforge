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
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.jpa.MyJpaWithExtLibrariesScanner
import org.projectforge.plugins.cveradar.model.CvrCaseDO
import org.projectforge.plugins.cveradar.model.CvrCaseStatus
import org.projectforge.plugins.cveradar.model.CvrConfirmationDO
import org.projectforge.plugins.cveradar.model.CvrDeviceDO
import org.projectforge.plugins.cveradar.model.CvrExploitability
import org.projectforge.plugins.cveradar.model.CvrImportStateDO
import org.projectforge.plugins.cveradar.model.CvrPriority
import org.projectforge.plugins.cveradar.model.CvrRatingDO
import org.projectforge.plugins.cveradar.model.CvrTeamCheckDO
import org.projectforge.plugins.cveradar.model.CvrVulnerabilityDO
import org.projectforge.plugins.cveradar.rest.CvrCaseFileRest
import org.projectforge.plugins.cveradar.rest.MassRequest
import org.projectforge.plugins.cveradar.rest.MeasuresRequest
import org.projectforge.plugins.cveradar.rest.RatingRequest
import org.projectforge.plugins.cveradar.rest.TeamCheckRequest
import org.projectforge.plugins.cveradar.rest.TriageRequest
import org.projectforge.plugins.cveradar.rest.TriageRequest.Action
import org.projectforge.plugins.cveradar.sync.NvdCveParser
import org.projectforge.plugins.cveradar.sync.NvdCveParserTest.Companion.cve
import org.projectforge.plugins.cveradar.sync.NvdImportService
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.Instant

/**
 * The file of a CVE (triage, ratings, measures): the rights of the roles (SEC-07), one rating per person, the binding
 * priority (reason, assignee or security officer), the conflict detection (TRI-04), mass actions (TRI-07) and the
 * import leaving the case alone.
 */
class CvrCaseFileTest : CveRadarTestBase() {
    @Autowired
    private lateinit var caseDao: CvrCaseDao

    @Autowired
    private lateinit var fileRest: CvrCaseFileRest

    @Autowired
    private lateinit var importService: NvdImportService

    @Autowired
    private lateinit var vulnerabilityDao: CvrVulnerabilityDao

    @Autowired
    private lateinit var teamCheckDao: CvrTeamCheckDao

    @Autowired
    private lateinit var rest: org.projectforge.plugins.cveradar.rest.CveRadarRest

    init {
        listOf(
            CvrCaseDO::class.java, CvrTeamCheckDO::class.java, CvrConfirmationDO::class.java,
            CvrDeviceDO::class.java, CvrVulnerabilityDO::class.java,
            CvrImportStateDO::class.java, CvrRatingDO::class.java,
        ).forEach { MyJpaWithExtLibrariesScanner.addPluginEntitiesForTestMode(it.canonicalName) }
    }

    /** Every test imports from the start of the import period, without watermark. */
    @BeforeEach
    fun resetImportState() {
        clock = 0L
        persistenceService.runInTransaction { context ->
            context.em.find(CvrImportStateDO::class.java, NvdImportService.SOURCE)?.let { context.em.remove(it) }
        }
    }

    @Test
    fun `an ordinary user is refused everywhere, readers read and open, only the security team changes`() {
        val vulnerability = vulnerability("CVE-2026-93001")
        logon(TEST_USER)
        assertThrows(AccessException::class.java) { fileRest.vulnerability(vulnerability.id!!) }
        assertThrows(AccessException::class.java) { fileRest.open(vulnerability.id!!) }
        assertThrows(AccessException::class.java) { fileRest.mass(MassRequest(listOf(vulnerability.id!!), Action.TAKE_OVER)) }

        logon(user("reader", CvrRole.READER))
        val file = fileRest.vulnerability(vulnerability.id!!)
        assertNull(file.case, "not opened yet")
        assertTrue(file.permissions.canOpen)
        assertFalse(file.permissions.canEdit)
        val opened = fileRest.open(vulnerability.id!!)
        val caseId = opened.case!!.id
        assertEquals("CVE-2026-93001", opened.case.code)
        assertEquals(CvrCaseStatus.NEW, opened.case.status)
        assertEquals(opened.case.id, fileRest.open(vulnerability.id!!).case!!.id, "opening twice gives the same case")
        val lastUpdate = opened.case.lastUpdate
        assertThrows(AccessException::class.java) {
            fileRest.triage(caseId, TriageRequest(lastUpdate, Action.TAKE_OVER))
        }
        assertThrows(AccessException::class.java) { fileRest.rate(caseId, RatingRequest(lastUpdate, CvrPriority.P1, CvrExploitability.HIGH)) }
        assertThrows(AccessException::class.java) { fileRest.measures(caseId, MeasuresRequest(lastUpdate, techMeasures = "x")) }
        assertThrows(AccessException::class.java) { caseDao.update(caseDao.find(caseId, checkAccess = false)!!) }
    }

    @Test
    fun `take over, rate once per person, binding priority needs a reason and the assignee or the officer`() {
        val vulnerability = vulnerability("CVE-2026-93002", kev = "2026-10-01")
        val st1 = user("st1", CvrRole.SECURITY_TEAM)
        val st2 = user("st2", CvrRole.SECURITY_TEAM)
        val so = user("so", CvrRole.SECURITY_OFFICER)
        logon(st1)
        var file = fileRest.open(vulnerability.id!!)
        val caseId = file.case!!.id
        assertEquals(CvrPriority.P1, file.suggestion!!.priority, "CVSS 9.8 is P2, KEV raises it to P1")
        assertEquals(listOf("cvssP2", "kev"), file.suggestion.reasons)

        file = fileRest.triage(caseId, TriageRequest(file.case!!.lastUpdate, Action.TAKE_OVER))
        assertEquals(st1.id, file.case!!.assigneeId)
        assertEquals(CvrCaseStatus.IN_TRIAGE, file.case.status)

        file = fileRest.rate(caseId, RatingRequest(file.case.lastUpdate, CvrPriority.P2, CvrExploitability.HIGH, "exposed"))
        file = fileRest.rate(caseId, RatingRequest(file.case!!.lastUpdate, CvrPriority.P1, CvrExploitability.HIGH, "exposed, KEV"))
        assertEquals(1, file.ratings.size, "one rating per person, the second one changed the first")
        assertEquals(CvrPriority.P1, file.ratings.single().priority)
        logon(st2)
        file = fileRest.rate(caseId, RatingRequest(file.case!!.lastUpdate, CvrPriority.P3, CvrExploitability.LOW))
        assertEquals(2, file.ratings.size)
        assertEquals(1, file.ratings.count { it.mine })
        file = fileRest.deleteRating(caseId)
        assertEquals(1, file.ratings.size)
        file = fileRest.rate(caseId, RatingRequest(file.case!!.lastUpdate, CvrPriority.P3, CvrExploitability.LOW))
        assertEquals(2, file.ratings.size, "rated again after removing the rating")
        val reassign = assertThrows(UserException::class.java, {
            fileRest.triage(caseId, TriageRequest(file.case!!.lastUpdate, Action.TAKE_OVER))
        }, "st1 has the case: st2 can't take it over")
        assertEquals(CvrCaseFileService.ERROR_REASSIGN_RIGHTS, reassign.i18nKey)

        // The binding priority: reason needed, and only the assignee or a security officer.
        assertThrows(UserException::class.java) {
            fileRest.triage(caseId, TriageRequest(file.case!!.lastUpdate, Action.SET_PRIORITY, priority = CvrPriority.P1))
        }
        assertThrows(UserException::class.java, {
            fileRest.triage(caseId, TriageRequest(file.case!!.lastUpdate, Action.SET_PRIORITY, priority = CvrPriority.P1, reason = "KEV"))
        }, "st2 isn't the assignee")
        logon(st1)
        file = fileRest.triage(caseId, TriageRequest(file.case!!.lastUpdate, Action.SET_PRIORITY, priority = CvrPriority.P1, reason = "KEV"))
        assertEquals(CvrPriority.P1, file.case!!.priority)
        assertEquals(CvrCaseStatus.RATED, file.case.status)
        assertTrue(file.ratings.single { !it.mine }.deviates, "the rating of st2 deviates from the binding priority")
        logon(so)
        file = fileRest.triage(caseId, TriageRequest(file.case.lastUpdate, Action.SET_PRIORITY, priority = CvrPriority.P2, reason = "mitigated by the WAF"))
        assertEquals(CvrPriority.P2, file.case!!.priority)
        assertTrue(file.permissions.isSecurityOfficer)
    }

    @Test
    fun `setting the priority of an unassigned case makes the acting person its assignee`() {
        val vulnerability = vulnerability("CVE-2026-93003")
        val st = user("st3", CvrRole.SECURITY_TEAM)
        logon(st)
        val file = fileRest.open(vulnerability.id!!)
        val after = fileRest.triage(
            file.case!!.id,
            TriageRequest(file.case.lastUpdate, Action.SET_PRIORITY, priority = CvrPriority.P3, reason = "low risk"),
        )
        assertEquals(st.id, after.case!!.assigneeId)
        assertEquals(CvrCaseStatus.RATED, after.case.status)
    }

    @Test
    fun `a change over a newer version is refused, measures keep only valid links`() {
        val vulnerability = vulnerability("CVE-2026-93004")
        logon(user("st4", CvrRole.SECURITY_TEAM))
        val file = fileRest.open(vulnerability.id!!)
        val caseId = file.case!!.id
        val saved = fileRest.measures(
            caseId,
            MeasuresRequest(
                file.case.lastUpdate, techMeasures = "<p>Update</p>", nonTechMeasures = "Restart", fixVersion = " 2.3.1 ",
                workaround = "Disable it", referenceLinks = "https://example.org/a Advisory\njavascript:alert(1)\nftp://x.org/f\nhttps://example.org/b",
            ),
        )
        assertEquals("2.3.1", saved.case!!.fixVersion)
        assertEquals(listOf("https://example.org/a", "https://example.org/b"), saved.case.referenceLinks.map { it.url })
        assertEquals("Advisory", saved.case.referenceLinks.first().label)
        val ex = assertThrows(UserException::class.java) {
            fileRest.measures(caseId, MeasuresRequest(file.case.lastUpdate, techMeasures = "stale"))
        }
        assertEquals("plugins.cveradar.case.error.conflict", ex.i18nKey)
        assertEquals("<p>Update</p>", caseDao.find(caseId, checkAccess = false)!!.techMeasures)
        assertThrows(UserException::class.java) { fileRest.measures(caseId, MeasuresRequest(null, techMeasures = "no version")) }
    }

    @Test
    fun `not relevant needs a reason and open team checks need the officer, a closed case takes no more ratings`() {
        val vulnerability = vulnerability("CVE-2026-93005")
        val so = user("so5", CvrRole.SECURITY_OFFICER)
        logon(user("st5", CvrRole.SECURITY_TEAM))
        var file = fileRest.open(vulnerability.id!!)
        val caseId = file.case!!.id
        rest.createTeamCheck(caseId, TeamCheckRequest(getGroupId(TEST_GROUP), "Project A"))
        assertThrows(UserException::class.java) {
            fileRest.triage(caseId, TriageRequest(file.case!!.lastUpdate, Action.NOT_RELEVANT))
        }
        assertThrows(UserException::class.java, {
            fileRest.triage(caseId, TriageRequest(file.case!!.lastUpdate, Action.NOT_RELEVANT, reason = "not used"))
        }, "a team check is open: the officer decides")
        assertThrows(UserException::class.java) {
            fileRest.triage(caseId, TriageRequest(file.case!!.lastUpdate, Action.SET_STATUS, status = CvrCaseStatus.CLOSED))
        }
        logon(so)
        file = fileRest.triage(caseId, TriageRequest(file.case!!.lastUpdate, Action.NOT_RELEVANT, reason = "not used"))
        assertEquals(CvrCaseStatus.NOT_RELEVANT, file.case!!.status)
        assertFalse(file.permissions.canEdit)
        assertThrows(UserException::class.java) {
            fileRest.rate(caseId, RatingRequest(file.case.lastUpdate, CvrPriority.P4, CvrExploitability.NONE))
        }
        listOf(
            { fileRest.triage(caseId, TriageRequest(file.case!!.lastUpdate, Action.SET_STATUS, status = CvrCaseStatus.NEW)) },
            { fileRest.triage(caseId, TriageRequest(file.case!!.lastUpdate, Action.TAKE_OVER)) },
            { fileRest.measures(caseId, MeasuresRequest(file.case!!.lastUpdate, techMeasures = "late")) },
        ).forEach { change ->
            val ex = assertThrows(UserException::class.java) { change() }
            assertEquals(CvrCaseFileService.ERROR_CLOSED, ex.i18nKey, "a closed case isn't changed any more")
        }
    }

    @Test
    fun `a deleted case is revived when the file is opened again`() {
        val vulnerability = vulnerability("CVE-2026-93010")
        logon(user("st10", CvrRole.SECURITY_TEAM))
        val caseId = fileRest.open(vulnerability.id!!).case!!.id
        caseDao.markAsDeleted(caseDao.find(caseId, checkAccess = false)!!, checkAccess = false)
        assertNull(fileRest.vulnerability(vulnerability.id!!).case)
        val reopened = fileRest.open(vulnerability.id!!)
        assertEquals(caseId, reopened.case!!.id)
        assertFalse(caseDao.find(caseId, checkAccess = false)!!.deleted)
    }

    @Test
    fun `mass actions open the files, report per vulnerability and skip closed ones`() {
        val a = vulnerability("CVE-2026-93006")
        val b = vulnerability("CVE-2026-93007")
        val st = user("st6", CvrRole.SECURITY_TEAM)
        logon(st)
        val closed = fileRest.open(b.id!!)
        fileRest.triage(closed.case!!.id, TriageRequest(closed.case.lastUpdate, Action.NOT_RELEVANT, reason = "duplicate"))
        val results = fileRest.mass(MassRequest(listOf(a.id!!, b.id!!, -5), Action.SET_PRIORITY, priority = CvrPriority.P2, reason = "same advisory"))
        assertEquals(listOf(true, false, false), results.map { it.ok })
        assertEquals(st.id, fileRest.vulnerability(a.id!!).case!!.assigneeId)
        assertEquals(CvrPriority.P2, fileRest.vulnerability(a.id!!).case!!.priority)
        assertThrows(UserException::class.java) { fileRest.mass(MassRequest(listOf(a.id!!), Action.SET_STATUS)) }
        logon(user("reader2", CvrRole.READER))
        assertThrows(AccessException::class.java) { fileRest.mass(MassRequest(listOf(a.id!!), Action.TAKE_OVER)) }
    }

    @Test
    fun `the file of a team check is for its group and the security team only`() {
        val vulnerability = vulnerability("CVE-2026-93008")
        logon(user("st7", CvrRole.SECURITY_TEAM))
        val caseId = fileRest.open(vulnerability.id!!).case!!.id
        rest.createTeamCheck(caseId, TeamCheckRequest(getGroupId(TEST_GROUP), "Project B"))
        val check = teamCheckDao.selectByCase(caseId).single()
        assertEquals("Project B", fileRest.teamCheck(check.id!!).subject)
        initTestDB.addGroup("cveradar-other-group")
        rest.createTeamCheck(caseId, TeamCheckRequest(getGroupId("cveradar-other-group"), "Project C"))
        val other = teamCheckDao.selectByCase(caseId).single { it.subject == "Project C" }
        logon(user("reader3", CvrRole.READER))
        assertThrows(AccessException::class.java) { fileRest.teamCheck(check.id!!) }
        logon(TEST_USER) // Member of TEST_GROUP, not of the group asked by "Project C".
        assertEquals("Project B", fileRest.teamCheck(check.id!!).subject)
        assertThrows(AccessException::class.java) { fileRest.teamCheck(other.id!!) }
    }

    @Test
    fun `the import keeps the case, links a case opened before the CVE was known and reads the KEV date and references`() {
        logon(user("st8", CvrRole.SECURITY_TEAM))
        val early = caseDao.let {
            val case = CvrCaseDO().also { c ->
                c.kind = org.projectforge.plugins.cveradar.model.CvrCaseKind.CVE
                c.code = "CVE-2026-93009"
                c.title = "early"
                c.status = CvrCaseStatus.NEW
            }
            it.insert(case)
            case
        }
        assertNull(caseDao.find(early.id)!!.vulnerability, "the CVE isn't imported yet")
        importService.import({ _, _, _ -> NvdCveParser.parsePage(cvePage(cve("CVE-2026-93009", metrics = SCORE, kevAdded = "2026-09-30"))) }, NOW.plusSeconds(clock++))
        val vulnerability = vulnerabilityDao.findByCveId("CVE-2026-93009")!!
        assertEquals(early.id, caseDao.findByVulnerabilityId(vulnerability.id!!)?.id, "the case is found by the CVE")
        assertNotNull(vulnerability.kevAdded)
        val file = fileRest.vulnerability(vulnerability.id!!)
        assertEquals("https://example.org/advisory", file.vulnerability!!.remediationReferences.single().url)
        assertTrue(file.vulnerability.otherReferences.isEmpty())

        // A changed document updates the NVD data and leaves the case alone.
        val opened = fileRest.triage(early.id!!, TriageRequest(caseDao.find(early.id)!!.lastUpdate, Action.TAKE_OVER))
        importService.import(
            { _, _, _ ->
                NvdCveParser.parsePage(cvePage(cve("CVE-2026-93009", metrics = SCORE, lastModified = "2026-10-25T08:00:00.000")))
            },
            NOW.plusSeconds(clock++ + 60),
        )
        val after = fileRest.vulnerability(vulnerability.id!!)
        assertEquals(opened.case!!.assigneeId, after.case!!.assigneeId)
        assertEquals(CvrCaseStatus.IN_TRIAGE, after.case.status)
        assertNull(after.vulnerability!!.kevAdded, "the NVD no longer lists it")
    }

    @Test
    fun `the suggested priority follows the CVSS and the KEV catalogue`() {
        fun suggest(cvss: String?, kev: Boolean) = CvrPriorityRule.suggest(cvss?.let { BigDecimal(it) }, kev).priority
        assertEquals(CvrPriority.P2, suggest("9.0", false))
        assertEquals(CvrPriority.P3, suggest("7.5", false))
        assertEquals(CvrPriority.P4, suggest("5.0", false))
        assertEquals(CvrPriority.P4, suggest(null, false))
        assertEquals(CvrPriority.P1, suggest("9.8", true))
        assertEquals(CvrPriority.P2, suggest("7.5", true))
        assertEquals(CvrPriority.P3, suggest("3.0", true))
    }

    private fun vulnerability(cveId: String, kev: String? = null): CvrVulnerabilityDO {
        logon(TEST_ADMIN_USER)
        vulnerabilityDao.findByCveId(cveId)?.let { return it }
        importService.import(
            { _, _, _ -> NvdCveParser.parsePage(cvePage(cve(cveId, metrics = SCORE, kevAdded = kev))) },
            NOW.plusSeconds(clock++),
        )
        return vulnerabilityDao.findByCveId(cveId)!!
    }

    /** Every import needs a later end of its range than the watermark of the one before. */
    private var clock = 0L

    private fun cvePage(vararg cves: String) = org.projectforge.plugins.cveradar.sync.NvdCveParserTest.page(*cves)

    companion object {
        private val NOW: Instant = Instant.parse("2026-10-27T10:00:00Z")
        private const val SCORE =
            """"cvssMetricV31":[{"type":"Primary","cvssData":{"vectorString":"CVSS:3.1/x","baseScore":9.8,"baseSeverity":"CRITICAL"}}]"""
    }
}
