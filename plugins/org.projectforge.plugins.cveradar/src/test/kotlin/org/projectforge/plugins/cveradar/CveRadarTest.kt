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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.jpa.MyJpaWithExtLibrariesScanner
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.core.PluginsRegistry
import org.projectforge.plugins.cveradar.CvrCaseStatusService.DeviceState
import org.projectforge.plugins.cveradar.model.CvrCaseDO
import org.projectforge.plugins.cveradar.model.CvrCaseKind
import org.projectforge.plugins.cveradar.model.CvrCaseStatus
import org.projectforge.plugins.cveradar.model.CvrClientAnswer
import org.projectforge.plugins.cveradar.model.CvrConfirmationDO
import org.projectforge.plugins.cveradar.model.CvrDeviceDO
import org.projectforge.plugins.cveradar.model.CvrTeamAnswer
import org.projectforge.plugins.cveradar.model.CvrTeamCheckDO
import org.projectforge.plugins.cveradar.model.CvrVulnerabilityDO
import org.projectforge.plugins.cveradar.rest.CloseRequest
import org.projectforge.plugins.cveradar.rest.ConfirmRequest
import org.projectforge.plugins.cveradar.rest.CveRadarRest
import org.projectforge.plugins.cveradar.rest.CvrCase
import org.projectforge.plugins.cveradar.rest.CvrCaseEntityRest
import org.projectforge.plugins.cveradar.rest.TeamCheckAnswerRequest
import org.projectforge.plugins.cveradar.rest.TeamCheckRequest
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import java.util.Date

/**
 * CVE-Radar R0: access of the security team, of readers and of ordinary users (SEC-07), the sync of the client agents
 * (EXT-04), the closure overview (CLS-02) with team checks (CLS-03) and confirmations (CLS-04), and closing (CLS-05).
 */
class CveRadarTest : CveRadarTestBase() {
    @Autowired
    private lateinit var caseDao: CvrCaseDao

    @Autowired
    private lateinit var caseRest: CvrCaseEntityRest

    @Autowired
    private lateinit var config: CveRadarConfig

    @Autowired
    private lateinit var deviceService: CvrDeviceService

    @Autowired
    private lateinit var rest: CveRadarRest

    @Autowired
    private lateinit var statusService: CvrCaseStatusService

    init {
        listOf(
            CvrCaseDO::class.java, CvrTeamCheckDO::class.java, CvrConfirmationDO::class.java,
            CvrDeviceDO::class.java, CvrVulnerabilityDO::class.java,
            org.projectforge.plugins.cveradar.model.CvrRatingDO::class.java,
        ).forEach { MyJpaWithExtLibrariesScanner.addPluginEntitiesForTestMode(it.canonicalName) }
    }

    @Test
    fun `an ordinary user sees neither cases nor devices, the security team does`() {
        val st = user("st1", CvrRole.SECURITY_TEAM)
        logon(st)
        val case = insertCase("access")
        logon(TEST_USER)
        assertThrows(AccessException::class.java) { caseDao.find(case.id) }
        assertThrows(AccessException::class.java) { caseDao.insert(newCase("refused")) }
        assertThrows(AccessException::class.java) { rest.status(case.id!!) }
        assertThrows(AccessException::class.java) { rest.devices() }
        assertThrows(AccessException::class.java) {
            rest.createTeamCheck(case.id!!, TeamCheckRequest(getGroupId(TEST_GROUP), "refused"))
        }
        logon(st)
        assertEquals(case.code, caseDao.find(case.id)!!.code)
        rest.devices()
        assertTrue(rest.status(case.id!!).canEdit)
    }

    @Test
    fun `the statistics are for readers only`() {
        logon(TEST_USER)
        assertThrows(AccessException::class.java) { rest.reportedStatistics(CvrStatisticsService.Granularity.DAY) }
        logon(user("reader2", CvrRole.READER))
        val statistics = rest.reportedStatistics(CvrStatisticsService.Granularity.MONTH)
        assertEquals(CvrStatisticsService.MONTHS.toInt(), statistics.cases.size)
        assertEquals(CvrStatisticsService.MONTHS.toInt(), statistics.vulnerabilities.size)
    }

    @Test
    fun `no endpoint answers while the plugin isn't activated`() {
        logon(user("st7", CvrRole.SECURITY_TEAM))
        rest.checkPluginActive()
        caseRest.checkPluginActive()
        val plugins = PluginsRegistry.instance().plugins
        val plugin = plugins.single { it.id == CveRadarPlugin.ID }
        plugins.remove(plugin)
        try {
            assertThrows(AccessException::class.java) { rest.checkPluginActive() }
            assertThrows(AccessException::class.java) { caseRest.checkPluginActive() }
        } finally {
            plugins.add(plugin)
        }
    }

    @Test
    fun `a reader sees the closure overview without persons`() {
        val st = user("st2", CvrRole.SECURITY_TEAM)
        logon(st)
        val case = insertCase("reader", minVersion = "2.0")
        val checkId = rest.createTeamCheck(case.id!!, TeamCheckRequest(getGroupId(TEST_GROUP), "Project R"))
            .teamChecks.single().id
        logon(TEST_USER)
        rest.answerTeamCheck(checkId, TeamCheckAnswerRequest(CvrTeamAnswer.NOT_AFFECTED, "not used"))
        val reader = user("reader1", CvrRole.READER)
        logon(reader)
        val view = rest.status(case.id!!)
        assertNull(view.deviceRows, "no devices with persons for a reader (VIS-15)")
        assertNull(view.questions)
        assertFalse(view.canEdit)
        val readerCheck = view.teamChecks.single()
        assertEquals(CvrTeamAnswer.NOT_AFFECTED, readerCheck.answer)
        assertNull(readerCheck.answeredBy, "who answered isn't shown to a reader (VIS-10)")
        assertNull(readerCheck.comment)
        assertThrows(AccessException::class.java) { rest.devices() }
        assertThrows(AccessException::class.java) { caseDao.insert(newCase("reader-refused")) }
        logon(st)
        val stCheck = rest.status(case.id!!).teamChecks.single()
        assertEquals("not used", stCheck.comment)
        assertTrue(stCheck.answeredBy != null)
    }

    @Test
    fun `an incident gets the next id of the year, a CVE needs its id`() {
        logon(user("st3", CvrRole.SECURITY_TEAM))
        val first = insertCase("code1")
        val second = insertCase("code2")
        val prefix = "${config.caseIdPrefix}-${java.time.Year.now().value}-"
        assertTrue(first.code!!.startsWith(prefix))
        assertEquals(first.code!!.removePrefix(prefix).toInt() + 1, second.code!!.removePrefix(prefix).toInt())
        assertThrows(UserException::class.java) {
            caseDao.insert(newCase("cve").also { it.kind = CvrCaseKind.CVE; it.code = "no cve" })
        }
        val cve = newCase("cve").also { it.kind = CvrCaseKind.CVE; it.code = "cve-2026-12345" }
        caseDao.insert(cve)
        assertEquals("CVE-2026-12345", cve.code)
    }

    @Test
    fun `messages of the client agents keep the newest version and check and resolve the user`() {
        val t0 = Date(1_000_000)
        val t1 = Date(2_000_000)
        val record = { at: Date, version: String?, checkId: String?, passed: Boolean? ->
            CvrDeviceService.AgentRecord(at, TEST_USER, "SN-AGENT", "nb-test", version, checkId, passed)
        }
        assertEquals(
            1,
            deviceService.applyAgentRecords(
                listOf(record(t1, "2.6.40", "INC-CHECK", true), record(t0, "2.6.3", "INC-CHECK", false)),
            ),
        )
        deviceService.applyAgentRecords(listOf(record(t0, "2.6.1", null, null)))
        val device = deviceService.selectDevices().single { it.serialNumber == "SN-AGENT" }
        assertEquals(getUserId(TEST_USER), device.userId, "the user name is resolved to the PF user")
        assertEquals("2.6.40", device.agentVersion, "older messages don't replace the newer ones")
        assertEquals(true, deviceService.agentChecks(device)["INC-CHECK"]!!.passed)
    }

    @Test
    fun `devices are fixed by version or check, open otherwise or confirmed by their user`() {
        val case = CvrCaseDO().also {
            it.id = 4711
            it.minClientAgentVersion = "2.6.34"
            it.clientAgentCheckId = "INC-CHECK"
            it.clientAdvisory = true
        }
        fun device(id: Long, version: String? = null, checks: String? = null, userId: Long? = null) =
            CvrDeviceDO().also {
                it.id = id
                it.agentVersion = version
                it.agentChecks = checks
                it.userId = userId
                it.lastSeen = Date()
            }
        val byVersion = device(1, version = "2.6.40")
        val byCheck = device(2, checks = """{"INC-CHECK":{"passed":true,"at":0}}""")
        val tooOld = device(3, version = "2.6.3")
        val failedCheck = device(4, version = "2.6.40", checks = """{"INC-CHECK":{"passed":false,"at":0}}""")
        val noData = device(5)
        val confirmed = device(6, userId = 99)
        val confirmation = CvrConfirmationDO().also { it.userId = 99; it.answer = CvrClientAnswer.DONE }
        val status = statusService.status(
            case,
            listOf(byVersion, byCheck, tooOld, failedCheck, noData, confirmed),
            0,
            listOf(confirmation),
            emptyList(),
        )
        val states = status.devices.associate { it.device.id to it.state }
        assertEquals(DeviceState.FIXED, states[1])
        assertEquals(DeviceState.FIXED, states[2])
        assertEquals(DeviceState.OPEN, states[3])
        assertEquals(DeviceState.OPEN, states[4], "a failed check beats the version")
        assertEquals(DeviceState.NO_DATA, states[5])
        assertEquals(DeviceState.CONFIRMED, states[6])
        assertFalse(status.closable)
        assertEquals(mapOf("devicesOpen" to 2, "devicesNoData" to 1), status.openItems.associate { it.key to it.count })
    }

    @Test
    fun `team checks are answered by the group only, then the case can be closed`() {
        val st = user("st4", CvrRole.SECURITY_TEAM)
        logon(st)
        val case = insertCase("teamcheck")
        val view = rest.createTeamCheck(case.id!!, TeamCheckRequest(getGroupId(TEST_GROUP), "Project X"))
        val checkId = view.teamChecks.single().id
        assertFalse(view.closable)
        assertThrows(UserException::class.java) { rest.close(case.id!!, CloseRequest("too early")) }

        logon(TEST_ADMIN_USER) // Not member of the group, no right.
        assertThrows(AccessException::class.java) {
            rest.answerTeamCheck(checkId, TeamCheckAnswerRequest(CvrTeamAnswer.FIXED))
        }
        logon(TEST_USER) // Member of the group, without right of the CVE-Radar.
        assertThrows(UserException::class.java, { rest.answerTeamCheck(checkId, TeamCheckAnswerRequest(CvrTeamAnswer.NOT_AFFECTED)) }, "a reason is needed")
        val answered = rest.answerTeamCheck(checkId, TeamCheckAnswerRequest(CvrTeamAnswer.FIXED, "version 2.1"))
        assertEquals(CvrTeamAnswer.FIXED, answered.answer)
        assertTrue(rest.my().teamChecks.any { it.id == checkId }, "the check is in the own view of the member")

        logon(st)
        assertTrue(rest.status(case.id!!).closable)
        assertThrows(UserException::class.java) { rest.close(case.id!!, CloseRequest(" ")) }
        val closed = rest.close(case.id!!, CloseRequest("All projects fixed."))
        assertEquals(CvrCaseStatus.CLOSED, closed.case.status)
        assertThrows(UserException::class.java, { rest.close(case.id!!, CloseRequest("other reason")) }, "closed once")
        assertEquals("All projects fixed.", caseDao.find(case.id)!!.closeReason)
    }

    @Test
    fun `not relevant closes like closed, with reason and with open items by the security officer only`() {
        val so = user("so2", CvrRole.SECURITY_OFFICER)
        logon(user("st6", CvrRole.SECURITY_TEAM))
        val case = insertCase("notrelevant")
        rest.createTeamCheck(case.id!!, TeamCheckRequest(getGroupId(TEST_GROUP), "Project Z"))
        fun errors(reason: String?) = mutableListOf<ValidationError>().also { errors ->
            val dto = CvrCase().also {
                it.copyFrom(caseDao.find(case.id)!!)
                it.status = CvrCaseStatus.NOT_RELEVANT
                it.closeReason = reason
            }
            caseRest.validate(errors, dto)
        }
        assertEquals(1, errors(null).size, "a reason is needed")
        assertEquals(1, errors("not used here").size, "open items: security officer only")
        logon(so)
        assertTrue(errors("not used here").isEmpty())
    }

    @Test
    fun `too many agent checks drop the oldest, the json stays readable`() {
        val device = CvrDeviceDO()
        val checks = (1..200).associate { "CHECK-$it-${"x".repeat(20)}" to CvrDeviceService.AgentCheck(true, Date(it * 1000L)) }
        val json = deviceService.checksJson(device, checks)!!
        assertTrue(json.length <= 4000)
        device.agentChecks = json
        val read = deviceService.agentChecks(device)
        assertTrue(read.isNotEmpty() && read.size < 200)
        assertTrue(read.containsKey("CHECK-200-${"x".repeat(20)}"), "the newest check is kept")
        assertFalse(read.containsKey("CHECK-1-${"x".repeat(20)}"), "the oldest check is dropped")
    }

    @Test
    fun `only the security officer closes a case with open items`() {
        val so = user("so1", CvrRole.SECURITY_OFFICER)
        logon(user("st8", CvrRole.SECURITY_TEAM))
        val case = insertCase("so")
        rest.createTeamCheck(case.id!!, TeamCheckRequest(getGroupId(TEST_GROUP), "Project Y"))
        assertThrows(UserException::class.java) { rest.close(case.id!!, CloseRequest("risk accepted")) }
        logon(so)
        assertTrue(rest.status(case.id!!).canCloseWithOpenItems)
        assertEquals(CvrCaseStatus.CLOSED, rest.close(case.id!!, CloseRequest("risk accepted")).case.status)
    }

    @Test
    fun `employees see released cases and their own devices only and confirm for themselves`() {
        logon(user("st5", CvrRole.SECURITY_TEAM))
        val released = insertCase("released") { it.clientAdvisory = true; it.nonTechMeasures = "Update." }
        val internal = insertCase("internal")
        deviceService.applyAgentRecords(
            listOf(
                CvrDeviceService.AgentRecord(Date(), TEST_USER2, "SN-MY2", version = "2.6.40"),
                CvrDeviceService.AgentRecord(Date(), TEST_USER, "SN-MY1", version = "2.6.40"),
            ),
        )
        logon(TEST_USER)
        val my = rest.my()
        assertEquals(cveRadarConfig.securityOfficerGroup.trim(), my.groups.securityOfficer, "the configured groups, for the help texts")
        assertTrue(my.devices.all { it.username == TEST_USER }, "only the own devices")
        assertTrue(my.devices.any { it.serialNumber == "SN-MY1" })
        assertTrue(my.advisories.any { it.id == released.id })
        assertFalse(my.advisories.any { it.id == internal.id })
        assertThrows(UserException::class.java) { rest.confirm(internal.id!!, ConfirmRequest(CvrClientAnswer.DONE)) }
        assertThrows(UserException::class.java) { rest.confirm(released.id!!, ConfirmRequest(CvrClientAnswer.QUESTION)) }
        val after = rest.confirm(released.id!!, ConfirmRequest(CvrClientAnswer.DONE))
        assertEquals(CvrClientAnswer.DONE, after.advisories.single { it.id == released.id }.myAnswer)
        assertEquals(
            CvrClientAnswer.DONE,
            deviceService.selectConfirmations(released.id!!).single { it.userId == getUserId(TEST_USER) }.answer,
        )
    }

    private fun newCase(title: String) = CvrCaseDO().also {
        it.title = title
        it.kind = CvrCaseKind.INCIDENT
        it.status = CvrCaseStatus.NEW
    }

    private fun insertCase(
        title: String,
        minVersion: String? = null,
        init: (CvrCaseDO) -> Unit = {},
    ): CvrCaseDO {
        val case = newCase(title).also {
            it.minClientAgentVersion = minVersion
            init(it)
        }
        caseDao.insert(case)
        return case
    }
}
