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

package org.projectforge.business.fibu.kost.reallocation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeSalaryDO
import org.projectforge.business.fibu.KontoCache
import org.projectforge.business.fibu.KontoDO
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.contributionmargin.ContributionMarginEntryType
import org.projectforge.business.fibu.contributionmargin.ContributionMarginService
import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.business.fibu.kost.BuchungssatzDao
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.fibu.kost.SHType
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.configuration.ConfigurationDao
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

/**
 * The Kost2 reallocation with the situation of the dev doc: a sub task with time sheets on the collective kost2 S was
 * moved into the task tree of the target project T. Two employees in one month: the first one partly in the moved
 * sub task (6 of 10 hours), the second one completely (5 hours). The salary export booked 1000 and 500 on S.
 */
class Kost2ReallocationServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var service: Kost2ReallocationService

    @Autowired
    private lateinit var shareService: BuchungssatzKost2ShareService

    @Autowired
    private lateinit var shareDao: BuchungssatzKost2ShareDao

    @Autowired
    private lateinit var runDao: Kost2ReallocationDao

    @Autowired
    private lateinit var ruleDao: Kost2ReallocationRuleDao

    @Autowired
    private lateinit var buchungssatzDao: BuchungssatzDao

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var kontoCache: KontoCache

    @Autowired
    private lateinit var employeeCache: EmployeeCache

    @Autowired
    private lateinit var configurationDao: ConfigurationDao

    @Autowired
    private lateinit var contributionMarginService: ContributionMarginService

    @BeforeEach
    fun setUp() {
        val costConfigured = configurationDao.getEntry(ConfigurationParam.COST_CONFIGURED)!!
        costConfigured.booleanValue = true
        configurationDao.update(costConfigured, checkAccess = false)
    }

    @Test
    fun `detection, salary shares, rules and apply`() {
        val f = Fixture(61)
        val run = f.createRun()
        val detected = service.detectTimesheets(run)
        assertEquals(f.subTimesheets.map { it.id }.toSet(), detected.map { it.id }.toSet(), "Only the moved sub task.")

        val preview = service.preview(run.id!!, emptyList())
        assertEquals(emptyList<String>(), preview.warnings)
        assertAmounts(preview, f.salary1, f.t0 to "-600.00", f.s0 to "-400.00")
        assertAmounts(preview, f.salary2, f.t0 to "-500.00")
        assertAmounts(preview, f.revenue, f.t0 to "2000.00")
        assertAmounts(preview, f.otherCosts, f.t0 to "-300.00")
        assertEquals(f.subTimesheets.size, preview.timesheets.size)

        service.apply(run.id!!, emptyList())
        assertEquals(Kost2ReallocationStatus.APPLIED, runDao.find(run.id, checkAccess = false)!!.status)
        assertShares(f.salary1, f.t0 to "-600.00", f.s0 to "-400.00")
        assertShares(f.salary2, f.t0 to "-500.00")
        assertShares(f.revenue, f.t0 to "2000.00")
        f.subTimesheets.forEach { assertEquals(f.t0.id, timesheetDao.find(it.id, checkAccess = false)!!.kost2Id) }
        f.collectiveTimesheets.forEach { assertEquals(f.s0.id, timesheetDao.find(it.id, checkAccess = false)!!.kost2Id) }
        assertEquals(f.subTimesheets.size, service.getTimesheetLog(run).size)
        // The time sheets moved show the mark no more: a second detection finds nothing.
        assertTrue(service.detectTimesheets(run).isEmpty())
        // The effective view keeps the total.
        val expanded = shareService.expand(f.records.map { buchungssatzDao.find(it.id, checkAccess = false)!! })
        assertEquals(sum(f.records), expanded.sumOf { it.betrag!! })
        assertEquals(BigDecimal("600.00"), expanded.filter { it.kost2?.id == f.t0.id }.sumOf { it.betrag!! })
        // An applied run can't be edited, deleted or applied again.
        assertThrows<UserException> { service.apply(run.id!!, emptyList()) }
        val stored = runDao.find(run.id, checkAccess = false)!!
        stored.description = "changed"
        assertThrows<UserException> { runDao.update(stored) }
        assertThrows<UserException> { runDao.markAsDeleted(stored) }
    }

    @Test
    fun `second run on the same month, revert in reverse order`() {
        val f = Fixture(62)
        val run1 = f.createRun()
        service.apply(run1.id!!, emptyList())
        // Second run: the time sheets booked directly on the collective task, selected by hand.
        val run2 = f.createRun(withRule = false)
        val manual = service.searchTimesheets(run2, listOf(f.collectiveTask.id!!), f.user1.id, null)
        assertEquals(f.collectiveTimesheets.map { it.id }.toSet(), manual.map { it.id }.toSet())
        val preview = service.preview(run2.id!!, manual.mapNotNull { it.id })
        assertAmounts(preview, f.salary1, f.t0 to "-1000.00")
        service.apply(run2.id!!, manual.mapNotNull { it.id })
        assertShares(f.salary1, f.t0 to "-1000.00")
        f.collectiveTimesheets.forEach {
            val timesheet = timesheetDao.find(it.id, checkAccess = false)!!
            assertEquals(f.t0.id, timesheet.kost2Id)
            assertEquals(f.targetTask.id, timesheet.taskId, "Selected by hand: moved to the target task.")
        }
        // Run 1 can't be reverted before run 2 (which superseded its shares).
        assertThrows<UserException> { service.revert(run1.id!!) }
        service.revert(run2.id!!)
        assertShares(f.salary1, f.t0 to "-600.00", f.s0 to "-400.00")
        service.revert(run1.id!!)
        f.records.forEach { assertTrue(shareService.activeShares(it).isEmpty(), "Original state.") }
        (f.subTimesheets + f.collectiveTimesheets).forEach { original ->
            val timesheet = timesheetDao.find(original.id, checkAccess = false)!!
            assertEquals(original.kost2Id, timesheet.kost2Id)
            assertEquals(original.taskId, timesheet.taskId)
        }
        assertEquals(Kost2ReallocationStatus.REVERTED, runDao.find(run1.id, checkAccess = false)!!.status)
    }

    @Test
    fun `discarded shares are re-applied from the log`() {
        val f = Fixture(63)
        val run = f.createRun()
        service.apply(run.id!!, emptyList())
        logon(TEST_FINANCE_USER)
        assertFalse(service.needsReapply(runDao.find(run.id, checkAccess = false)!!))
        // A DATEV reimport changed the amount of the first salary record (unchanged here, the shares are discarded only).
        shareDao.discardForRecords(listOf(f.salary1.id!!))
        assertTrue(shareService.activeShares(f.salary1).isEmpty())
        val reloaded = runDao.find(run.id, checkAccess = false)!!
        assertTrue(service.needsReapply(reloaded))
        service.reapply(run.id!!)
        assertShares(f.salary1, f.t0 to "-600.00", f.s0 to "-400.00")
        assertShares(f.salary2, f.t0 to "-500.00")
        // Nothing left to re-apply:
        assertTrue(service.reapply(run.id!!).records.isEmpty())
    }

    @Test
    fun `a time sheet changed after the salary export gives a warning`() {
        val f = Fixture(64)
        // A collective time sheet of the first employee booked on S1 after the export (which booked all on S0):
        val timesheet = timesheetDao.find(f.collectiveTimesheets.first().id, checkAccess = false)!!
        timesheet.kost2 = f.s1
        timesheetDao.update(timesheet, checkAccess = false)
        val run = f.createRun()
        val warnings = service.preview(run.id!!, emptyList()).warnings
        assertEquals(1, warnings.size, "$warnings")
        assertTrue(warnings.first().contains(f.user1.getFullname()), warnings.first())
    }

    @Test
    fun `the contribution margin of the projects follows the run`() {
        val f = Fixture(66)
        val run = f.createRun()
        val from = LocalDate.of(YEAR, MONTH, 1)
        val until = from.plusMonths(1).minusDays(1)
        fun recordSum(projectId: Long): BigDecimal {
            return contributionMarginService.load(listOf(projectId), from, until).entries
                .filter { it.type == ContributionMarginEntryType.RECORD }
                .sumOf { it.revenue + it.costs }
        }
        val collectiveId = f.collectiveProject.id!!
        val targetId = f.targetProject.id!!
        assertEquals(BigDecimal("200.00"), recordSum(collectiveId))
        assertEquals(BigDecimal.ZERO, recordSum(targetId).stripTrailingZeros())
        service.apply(run.id!!, emptyList())
        assertEquals(BigDecimal("-400.00"), recordSum(collectiveId), "The rest of the first salary stays.")
        assertEquals(BigDecimal("600.00"), recordSum(targetId), "Salaries (-1100), revenue (2000) and costs (-300).")
        service.revert(run.id!!)
        assertEquals(BigDecimal("200.00"), recordSum(collectiveId))
        assertEquals(BigDecimal.ZERO, recordSum(targetId).stripTrailingZeros())
    }

    @Test
    fun `revert is refused after a moved time sheet was changed`() {
        val f = Fixture(67)
        val run = f.createRun()
        service.apply(run.id!!, emptyList())
        // Somebody moved a time sheet of the run to another task after the apply.
        val changed = timesheetDao.find(f.subTimesheets.first().id, checkAccess = false)!!
        changed.task = f.targetTask
        timesheetDao.update(changed, checkAccess = false)
        assertThrows<UserException> { service.revert(run.id!!) }
        // Nothing was written: the run is still applied, its shares and the other moved time sheets stay.
        assertEquals(Kost2ReallocationStatus.APPLIED, runDao.find(run.id, checkAccess = false)!!.status)
        assertShares(f.salary1, f.t0 to "-600.00", f.s0 to "-400.00")
        assertEquals(f.t0.id, timesheetDao.find(f.subTimesheets.last().id, checkAccess = false)!!.kost2Id)
    }

    @Test
    fun `rules don't chain`() {
        val f = Fixture(68)
        val run = f.createRun()
        // A second rule T0 -> S1: the revenue on S0 goes to T0 by the first rule only, not on to S1.
        ruleDao.insert(Kost2ReallocationRuleDO().also { it.run = run; it.sourceKost2 = f.t0; it.targetKost2 = f.s1 })
        val preview = service.preview(run.id!!, emptyList())
        assertAmounts(preview, f.revenue, f.t0 to "2000.00")
        assertAmounts(preview, f.otherCosts, f.t0 to "-300.00")
    }

    @Test
    fun `finance only, also for the time sheet protection`() {
        val f = Fixture(65)
        val run = f.createRun()
        logon(TEST_USER)
        assertThrows<AccessException> { service.checkAccess() }
        assertThrows<AccessException> { service.preview(run.id!!, emptyList()) }
        assertThrows<AccessException> { runDao.insert(newRun(f)) }
        assertFalse(runDao.hasLoggedInUserInsertAccess())
        assertFalse(shareDao.hasLoggedInUserInsertAccess())
        assertThrows<AccessException> {
            timesheetDao.reallocate(timesheetDao.find(f.subTimesheets.first().id, checkAccess = false)!!, f.subTask, f.t0)
        }
        // The finance user moves own time sheets on a protected task (protection until the end of the year).
        val financeUser = logon(TEST_FINANCE_USER)
        val own = persistenceService.runInTransaction { _ ->
            f.timesheet(financeUser, f.collectiveTask, f.s0, day = 20, hours = 2)
        }
        val protectedTask = taskDao.find(f.collectiveTask.id, checkAccess = false)!!
        protectedTask.protectTimesheetsUntil = LocalDate.of(YEAR, 12, 31)
        taskDao.update(protectedTask, checkAccess = false)
        val changed = timesheetDao.find(own.id, checkAccess = false)!!
        changed.kost2 = f.s1
        assertThrows<AccessException> { timesheetDao.update(changed) }
        timesheetDao.reallocate(timesheetDao.find(own.id, checkAccess = false)!!, f.targetTask, f.t0)
        assertEquals(f.t0.id, timesheetDao.find(own.id, checkAccess = false)!!.kost2Id)
    }

    private fun newRun(f: Fixture) = Kost2ReallocationDO().also {
        it.description = "Run ${f.n}"
        it.projekt = f.targetProject
        it.task = f.targetTask
        it.fromYear = YEAR
        it.fromMonth = MONTH
        it.toYear = YEAR
        it.toMonth = MONTH
    }

    private fun assertAmounts(plan: Kost2ReallocationPlan, record: BuchungssatzDO, vararg expected: Pair<Kost2DO, String>) {
        val change = plan.records.find { it.record.id == record.id }
        assertNotNull(change, "Record ${record.satznr} expected in the plan.")
        assertEquals(toMap(*expected), change!!.newAmounts.mapValues { it.value.toPlainString() })
    }

    private fun assertShares(record: BuchungssatzDO, vararg expected: Pair<Kost2DO, String>) {
        val shares = shareService.activeShares(record)
        val actual = shareService.effectiveAmounts(record, shares).mapValues { it.value.toPlainString() }
        assertEquals(toMap(*expected), actual)
    }

    private fun toMap(vararg expected: Pair<Kost2DO, String>) = expected.associate { it.first.id!! to it.second }

    private fun sum(records: List<BuchungssatzDO>) = records.sumOf { it.betrag!! }

    /**
     * The data of a test (own customer number [n], so the tests don't interfere): the collective project S (kost2
     * S0, S1) on the collective task, the target project T (kost2 T0) on the target task, a sub task of the collective
     * task moved into the target task tree.
     */
    private inner class Fixture(val n: Int) {
        lateinit var s0: Kost2DO
        lateinit var s1: Kost2DO
        lateinit var t0: Kost2DO
        lateinit var collectiveTask: TaskDO
        lateinit var targetTask: TaskDO
        lateinit var subTask: TaskDO
        lateinit var collectiveProject: ProjektDO
        lateinit var targetProject: ProjektDO
        lateinit var user1: PFUserDO
        lateinit var user2: PFUserDO
        lateinit var salary1: BuchungssatzDO
        lateinit var salary2: BuchungssatzDO
        lateinit var revenue: BuchungssatzDO
        lateinit var otherCosts: BuchungssatzDO
        val subTimesheets = mutableListOf<TimesheetDO>()
        val collectiveTimesheets = mutableListOf<TimesheetDO>()
        val records: List<BuchungssatzDO>
            get() = listOf(salary1, salary2, revenue, otherCosts)

        init {
            logon(TEST_FINANCE_USER)
            persistenceService.runInTransaction { context ->
                val kunde = KundeDO().also {
                    it.name = "Reallocation-$n"
                    it.id = n.toLong()
                    kundeDao.insert(it)
                }
                collectiveProject = initTestDB.addProjekt(kunde, 1, "Collective-$n", 0, 1)
                targetProject = initTestDB.addProjekt(kunde, 2, "Target-$n", 0)
                collectiveTask = initTestDB.addTask("rl$n-collective", "root")
                projektDao.setTask(collectiveProject, collectiveTask.id)
                projektDao.update(collectiveProject)
                targetTask = initTestDB.addTask("rl$n-target", "root")
                projektDao.setTask(targetProject, targetTask.id)
                projektDao.update(targetProject)
                subTask = initTestDB.addTask("rl$n-sub", "rl$n-collective")
                s0 = kost2Dao.getKost2(5, n, 1, 0)!!
                s1 = kost2Dao.getKost2(5, n, 1, 1)!!
                t0 = kost2Dao.getKost2(5, n, 2, 0)!!
                user1 = initTestDB.addUser("rl$n-user1")
                user2 = initTestDB.addUser("rl$n-user2")
                val em = context.em
                val kost1s = (1..2).map { i ->
                    Kost1DO().also { it.nummernkreis = 3; it.bereich = n; it.teilbereich = i; em.persist(it) }
                }
                listOf(user1, user2).forEachIndexed { index, user ->
                    val employee = EmployeeDO().also { it.user = user; it.kost1 = kost1s[index]; em.persist(it) }
                    em.persist(EmployeeSalaryDO().also {
                        it.employee = employee; it.year = YEAR; it.month = MONTH
                        it.bruttoMitAgAnteil = if (index == 0) BigDecimal("1000.00") else BigDecimal("500.00")
                    })
                }
                // Employee 1: 6 hours in the sub task, 4 hours on the collective task. Employee 2: 5 hours in the sub task.
                (1..3).forEach { subTimesheets.add(timesheet(user1, subTask, s0, day = it, hours = 2)) }
                (4..5).forEach { collectiveTimesheets.add(timesheet(user1, collectiveTask, s0, day = it, hours = 2)) }
                subTimesheets.add(timesheet(user2, subTask, s0, day = 1, hours = 5))
                val salaryKonto = konto(em, 6000)
                val salaryGegenKonto = konto(em, 3791)
                salary1 = record(em, 1, "-1000.00", salaryKonto, salaryGegenKonto, kost1s[0], s0)
                salary2 = record(em, 2, "-500.00", salaryKonto, salaryGegenKonto, kost1s[1], s0)
                revenue = record(em, 3, "2000.00", konto(em, 4400), konto(em, 10000), kost1s[0], s0)
                otherCosts = record(em, 4, "-300.00", konto(em, 4900), konto(em, 10001), kost1s[0], s0)
            }
            kostCache.forceReload()
            kontoCache.forceReload()
            employeeCache.forceReload()
            // The sub task is moved into the task tree of the target project (after the salary export).
            val sub = taskDao.find(subTask.id, checkAccess = false)!!
            sub.parentTask = targetTask
            taskDao.update(sub, checkAccess = false)
        }

        fun createRun(withRule: Boolean = true): Kost2ReallocationDO {
            logon(TEST_FINANCE_USER)
            val run = newRun(this)
            runDao.insert(run)
            if (withRule) {
                service.defaultRules(targetProject.id!!, listOf(s0.id!!)).forEach { (source, target) ->
                    ruleDao.insert(Kost2ReallocationRuleDO().also {
                        it.run = run; it.sourceKost2 = source; it.targetKost2 = target
                    })
                }
            }
            return runDao.find(run.id, checkAccess = false)!!
        }

        fun timesheet(user: PFUserDO, task: TaskDO, kost2: Kost2DO, day: Int, hours: Int): TimesheetDO {
            val timesheet = TimesheetDO()
            timesheet.task = task
            timesheet.kost2 = kost2
            timesheet.user = user
            timesheet.startTime = date(day, 8)
            timesheet.stopTime = date(day, 8 + hours)
            timesheet.description = "rl$n"
            timesheetDao.insert(timesheet, checkAccess = false)
            return timesheet
        }

        private fun konto(em: jakarta.persistence.EntityManager, nummer: Int): KontoDO {
            return em.createQuery("from KontoDO k where k.nummer = :nummer", KontoDO::class.java)
                .setParameter("nummer", nummer).resultList.firstOrNull()
                ?: KontoDO().also { it.nummer = nummer; it.bezeichnung = "Konto $nummer"; em.persist(it) }
        }

        private fun record(
            em: jakarta.persistence.EntityManager, satznr: Int, betrag: String, konto: KontoDO, gegenKonto: KontoDO,
            kost1: Kost1DO, kost2: Kost2DO,
        ): BuchungssatzDO {
            return BuchungssatzDO().also {
                it.year = YEAR; it.month = MONTH; it.satznr = n * 100 + satznr
                it.datum = LocalDate.of(YEAR, MONTH, 28)
                it.betrag = BigDecimal(betrag)
                it.sh = if (it.betrag!!.signum() < 0) SHType.SOLL else SHType.HABEN
                it.konto = konto; it.gegenKonto = gegenKonto
                it.kost1 = kost1; it.kost2 = kost2
                em.persist(it)
            }
        }

        private fun date(day: Int, hour: Int): Date =
            Date.from(LocalDateTime.of(YEAR, MONTH, day + 9, hour, 0).atZone(ZoneId.of("UTC")).toInstant())
    }

    companion object {
        /** In the past, as the contribution margin ends today. */
        private const val YEAR = 2025

        private const val MONTH = 3
    }
}
