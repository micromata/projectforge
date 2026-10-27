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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.fibu.EmployeeSalaryDO
import org.projectforge.business.fibu.KontoCache
import org.projectforge.business.fibu.MonthlyEmployeeReport
import org.projectforge.business.fibu.MonthlyEmployeeReportDao
import org.projectforge.business.fibu.datev.EmployeeSalarySplit
import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.business.fibu.kost.BuchungssatzDao
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.task.TaskTree
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.timesheet.TimesheetKost2Mapper
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.ge
import org.projectforge.framework.persistence.api.QueryFilter.Companion.isIn
import org.projectforge.framework.persistence.api.QueryFilter.Companion.le
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberFormatter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.ZoneId
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * The Kost2 reallocation (finance only), see `dev-docs/kost2-reallocation.md`: a run reallocates the accounting
 * records of a period from collective cost units (kost2 A) to a target project (kost2 B).
 *
 * - **Salary records** (account 6000 / counter account 3791, one per employee, month and kost2) are split by the hours
 *   of the time sheets to move: share B = effective amount on A × millis of the moved time sheets on A / current
 *   millis on A (the working time fraction is the same for all time sheets of a kost2). The time sheets to move are
 *   detected by their task in the task tree of the target project with a kost2 of another project, or are selected by
 *   hand (booked directly on a collective task, they get the target task).
 * - **Rules A -> B** move the other records of the source kost2 (optionally of an account range) completely. Salary
 *   records are left to the time sheets, unless the account range of a rule covers the salary account explicitly.
 *
 * Applying a run stores the shares first and moves the time sheets afterwards (with a log entry each): the detection
 * of the time sheets needs their old kost2. A later run on the same month calculates on the effective amounts (the
 * remaining share on A belongs to the time sheets still on A), so it needs no back calculation. A re-apply restores the
 * shares discarded by a DATEV reimport from the log. A revert restores the previous shares and the time sheets.
 */
@Service
open class Kost2ReallocationService {
    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var buchungssatzDao: BuchungssatzDao

    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var employeeCache: EmployeeCache

    @Autowired
    private lateinit var kontoCache: KontoCache

    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var monthlyEmployeeReportDao: MonthlyEmployeeReportDao

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var runDao: Kost2ReallocationDao

    @Autowired
    private lateinit var ruleDao: Kost2ReallocationRuleDao

    @Autowired
    private lateinit var shareDao: BuchungssatzKost2ShareDao

    @Autowired
    private lateinit var shareService: BuchungssatzKost2ShareService

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var timesheetLogDao: Kost2ReallocationTimesheetDao

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    /** Everything here is for the finance group only. */
    open fun checkAccess() {
        accessChecker.checkIsLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP)
    }

    /** The run from the database, never from the request. */
    open fun getRun(runId: Long): Kost2ReallocationDO {
        checkAccess()
        return runDao.find(runId, checkAccess = false)?.takeIf { !it.deleted }
            ?: throw UserException("fibu.kost2Reallocation.error.runNotFound")
    }

    /**
     * The default rules for the given source kost2: each one to the kost2 of the target project with the same kost2
     * type. Source kost2 without such a kost2 of the target project are missing in the result.
     */
    open fun defaultRules(projektId: Long, sourceKost2Ids: Collection<Long>): List<Pair<Kost2DO, Kost2DO>> {
        checkAccess()
        val targetKost2s = kostCache.getActiveKost2ForProjekt(projektId)
        return sourceKost2Ids.distinct().mapNotNull { sourceId ->
            val source = caches.getKost2(sourceId) ?: return@mapNotNull null
            if (source.projekt?.id == projektId) {
                return@mapNotNull null
            }
            TimesheetKost2Mapper.sameKost2Art(source, targetKost2s)?.let { source to it }
        }
    }

    /**
     * The time sheets of the period detected for the run: their task is in the task tree of the target project, but
     * their kost2 belongs to another project (booked before the move of the task tree on the collective kost2).
     */
    open fun detectTimesheets(run: Kost2ReallocationDO): List<TimesheetDO> {
        checkAccess()
        val projekt = caches.getProjekt(run.projekt?.id) ?: return emptyList()
        val projektTaskId = projekt.task?.id ?: return emptyList()
        val taskIds = taskTree.getDescendantTaskIds(projektTaskId, true).filterNotNull()
        return selectTimesheets(run, taskIds).filter { timesheet ->
            val kost2 = caches.getKost2(timesheet.kost2Id) ?: return@filter false
            kost2.projekt?.id != projekt.id
        }
    }

    /**
     * Time sheets of the period on the given (collective) tasks and their sub tasks, outside the task tree of the target
     * project, for the selection by hand (mixed case). Optionally of a user only and with the given text in the
     * description.
     */
    open fun searchTimesheets(
        run: Kost2ReallocationDO, taskIds: Collection<Long>, userId: Long?, text: String?,
    ): List<TimesheetDO> {
        checkAccess()
        val projekt = caches.getProjekt(run.projekt?.id) ?: return emptyList()
        val excluded = projekt.task?.id?.let { taskTree.getDescendantTaskIds(it, true).filterNotNull().toSet() }.orEmpty()
        val allTaskIds = taskIds.flatMap { taskTree.getDescendantTaskIds(it, true).filterNotNull() }.toSet() - excluded
        val search = text?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        return selectTimesheets(run, allTaskIds).filter { timesheet ->
            timesheet.kost2Id != null && (userId == null || timesheet.userId == userId) &&
                    (search == null || timesheet.description?.lowercase()?.contains(search) == true)
        }
    }

    /** The calculation of the run without any change (preview). */
    open fun preview(runId: Long, manualTimesheetIds: Collection<Long>): Kost2ReallocationPlan {
        val run = getRun(runId)
        return if (run.status == Kost2ReallocationStatus.DRAFT) {
            calculateDraft(run, manualTimesheetIds)
        } else {
            calculateReapply(run)
        }
    }

    /**
     * Applies the draft: stores the shares, then moves the time sheets (with a log entry each) and marks the run as
     * applied. All or nothing.
     */
    open fun apply(runId: Long, manualTimesheetIds: Collection<Long>): Kost2ReallocationPlan {
        getRun(runId)
        val (run, plan) = persistenceService.runInTransaction { context ->
            val run = lockRun(context, runId, Kost2ReallocationStatus.DRAFT, "fibu.kost2Reallocation.error.notDraft")
            val plan = calculateDraft(run, manualTimesheetIds)
            persistShares(run, plan)
            plan.timesheets.forEach { move -> moveTimesheet(run, move) }
            run.status = Kost2ReallocationStatus.APPLIED
            run.appliedAt = Date()
            runDao.runChangingStatus { runDao.update(run) }
            run to plan
        }
        log.info {
            "Kost2 reallocation run #${run.id} '${run.description}' applied: ${plan.records.size} records, ${plan.timesheets.size} time sheets."
        }
        return plan
    }

    /**
     * Restores the shares of the applied run discarded by a DATEV reimport (the kost2 or the amount of a record
     * changed), calculated from the log of the moved time sheets and the rules. The time sheets stay as they are.
     * Earlier runs on the same records are to be re-applied first.
     */
    open fun reapply(runId: Long): Kost2ReallocationPlan {
        getRun(runId)
        val (run, plan) = persistenceService.runInTransaction { context ->
            val run = lockRun(context, runId, Kost2ReallocationStatus.APPLIED, "fibu.kost2Reallocation.error.notApplied")
            val plan = calculateReapply(run)
            persistShares(run, plan)
            run to plan
        }
        log.info { "Kost2 reallocation run #${run.id} '${run.description}' re-applied: ${plan.records.size} records." }
        return plan
    }

    /**
     * Reverts the applied run: deletes its shares, restores the shares superseded by it and moves the time sheets back
     * (old task and kost2 of the log). Refused, if a later run changed the same records or time sheets, or if a moved
     * time sheet was changed since (its task or kost2 isn't the one of the run anymore). Deleted time sheets stay as
     * they are.
     */
    open fun revert(runId: Long) {
        getRun(runId)
        persistenceService.runInTransaction { context ->
            val run = lockRun(context, runId, Kost2ReallocationStatus.APPLIED, "fibu.kost2Reallocation.error.notApplied")
            val shares = shareDao.selectByRun(runId)
            if (shares.any { it.run?.id == runId && it.supersededByRun != null }) {
                throw UserException("fibu.kost2Reallocation.error.revertLaterRun")
            }
            val logEntries = timesheetLogDao.selectByRun(runId)
            val laterRunIds = laterAppliedRuns(run).mapNotNull { it.id }.toSet()
            if (laterRunIds.isNotEmpty() &&
                timesheetLogDao.selectByTimesheets(logEntries.mapNotNull { it.timesheet?.id }).any { it.run?.id in laterRunIds }
            ) {
                throw UserException("fibu.kost2Reallocation.error.revertLaterRun")
            }
            // All checks before the first change: the moved time sheets must still be as the run left them.
            val restores = logEntries.mapNotNull { entry ->
                val timesheet = timesheetDao.find(entry.timesheet?.id, checkAccess = false)
                    ?: throw UserException("fibu.kost2Reallocation.error.timesheetNotFound", entry.timesheet?.id)
                if (timesheet.deleted) {
                    return@mapNotNull null
                }
                val oldTask = taskTree.getTaskById(entry.oldTask?.id)
                if (oldTask == null || timesheet.taskId != entry.newTask?.id || timesheet.kost2Id != entry.newKost2?.id) {
                    throw UserException("fibu.kost2Reallocation.error.revertTimesheetChanged", describe(timesheet))
                }
                Triple(timesheet, oldTask, caches.getKost2(entry.oldKost2?.id))
            }
            shares.forEach { share ->
                if (share.run?.id == runId) {
                    shareDao.markAsDeleted(share)
                } else {
                    share.supersededByRun = null
                    shareDao.update(share)
                }
            }
            restores.forEach { (timesheet, oldTask, oldKost2) -> timesheetDao.reallocate(timesheet, oldTask, oldKost2) }
            run.status = Kost2ReallocationStatus.REVERTED
            runDao.runChangingStatus { runDao.update(run) }
            log.info {
                "Kost2 reallocation run #$runId '${run.description}' reverted: ${shares.size} shares, ${restores.size} time sheets."
            }
        }
    }

    /**
     * Locks the row of the run until the end of the transaction, if it has the expected status (a concurrent apply,
     * re-apply or revert of the same run waits and sees the changed status then), and loads it afterwards.
     */
    private fun lockRun(
        context: PfPersistenceContext, runId: Long, expected: Kost2ReallocationStatus, errorKey: String,
    ): Kost2ReallocationDO {
        val count = context.executeUpdate(
            "update Kost2ReallocationDO r set r.status = r.status where r.id = :id and r.status = :status and r.deleted = false",
            Pair("id", runId),
            Pair("status", expected),
        )
        val run = getRun(runId)
        if (count == 0 || run.status != expected) {
            throw UserException(errorKey)
        }
        return run
    }

    /** The not deleted rules of the run. */
    open fun getRules(run: Kost2ReallocationDO): List<Kost2ReallocationRuleDO> {
        return run.id?.let { ruleDao.selectByRun(it) }.orEmpty()
    }

    /** The log of the time sheets moved by the run. */
    open fun getTimesheetLog(run: Kost2ReallocationDO): List<Kost2ReallocationTimesheetDO> {
        return run.id?.let { timesheetLogDao.selectByRun(it) }.orEmpty()
    }

    /** Is the applied run to be re-applied (some of its shares were discarded by a DATEV reimport)? */
    open fun needsReapply(run: Kost2ReallocationDO): Boolean {
        return run.status == Kost2ReallocationStatus.APPLIED && recordIdsToReapply(run).isNotEmpty()
    }

    private fun calculateDraft(run: Kost2ReallocationDO, manualTimesheetIds: Collection<Long>): Kost2ReallocationPlan {
        val context = Context()
        val warnings = mutableListOf<String>()
        val moves = moves(context, run, manualTimesheetIds, warnings)
        return calculate(context, run, moves, reapply = false, initialWarnings = warnings)
    }

    /** The monthly reports of one calculation (each one is needed several times). */
    private inner class Context {
        private val reports = mutableMapOf<Triple<Long, Int, Int>, MonthlyEmployeeReport?>()

        fun report(userId: Long, year: Int, month: Int): MonthlyEmployeeReport? {
            return reports.getOrPut(Triple(userId, year, month)) {
                userGroupCache.getUser(userId)?.let { user -> monthlyEmployeeReportDao.getReport(year, month, user, false) }
            }
        }
    }

    /**
     * The time sheets to move: the detected ones and the ones selected by hand (the latter onto the target task of the
     * run), each to the kost2 of the target project given by the rule of its kost2 or of the same kost2 type. Time
     * sheets without target kost2 are left out (with a warning).
     */
    private fun moves(
        context: Context, run: Kost2ReallocationDO, manualTimesheetIds: Collection<Long>, warnings: MutableList<String>,
    ): List<Kost2ReallocationPlan.TimesheetMove> {
        val detected = detectTimesheets(run)
        val detectedIds = detected.mapNotNull { it.id }.toSet()
        val manualIds = manualTimesheetIds.toSet() - detectedIds
        val manual = if (manualIds.isEmpty()) emptyList() else {
            timesheetDao.select(manualIds, checkAccess = false).orEmpty().filter { !it.deleted && it.kost2Id != null }
        }
        val targetTask = if (manual.isNotEmpty()) {
            taskTree.getTaskById(run.task?.id) ?: throw UserException("fibu.kost2Reallocation.error.targetTaskRequired")
        } else null
        val rules = getRules(run)
        val targetKost2s = kostCache.getActiveKost2ForProjekt(run.projekt?.id)
        val result = mutableListOf<Kost2ReallocationPlan.TimesheetMove>()
        val timesheets = detected.map { it to false } + manual.map { it to true }
        // The month of a time sheet and its attendance millis as counted by the salary export.
        val attendances = attendances(context, run, timesheets.map { it.first })
        timesheets.forEach { (timesheet, isManual) ->
            val oldKost2 = caches.getKost2(timesheet.kost2Id) ?: return@forEach
            val newKost2 = rules.find { it.sourceKost2?.id == oldKost2.id }?.targetKost2?.id?.let { caches.getKost2(it) }
                ?: TimesheetKost2Mapper.sameKost2Art(oldKost2, targetKost2s)
            if (newKost2 == null) {
                warnings.add(translateMsg("fibu.kost2Reallocation.warning.noTargetKost2", describe(timesheet), oldKost2.formattedNumber))
                return@forEach
            }
            val attendance = attendances[timesheet.id]
            if (attendance == null) {
                // Not counted by the salary export of a month of the run (e. g. outside the period).
                warnings.add(translateMsg("fibu.kost2Reallocation.warning.timesheetOutsidePeriod", describe(timesheet)))
                return@forEach
            }
            result.add(
                Kost2ReallocationPlan.TimesheetMove(
                    timesheet = timesheet,
                    timesheetId = timesheet.id!!,
                    userId = timesheet.userId!!,
                    year = attendance.year,
                    month = attendance.month,
                    oldTaskId = timesheet.taskId!!,
                    newTaskId = if (isManual) targetTask!!.id!! else timesheet.taskId!!,
                    oldKost2Id = oldKost2.id!!,
                    newKost2Id = newKost2.id!!,
                    millis = attendance.millis,
                    manual = isManual,
                )
            )
        }
        return result
    }

    private class Attendance(val year: Int, val month: Int, val millis: Long)

    /** The month (of the salary export) and the attendance millis of the given time sheets, by time sheet id. */
    private fun attendances(
        context: Context, run: Kost2ReallocationDO, timesheets: List<TimesheetDO>,
    ): Map<Long?, Attendance> {
        val result = mutableMapOf<Long?, Attendance>()
        timesheets.mapNotNull { it.userId }.distinct().forEach { userId ->
            months(run).forEach { (year, month) ->
                context.report(userId, year, month)?.kost2TimesheetAttendances?.values?.forEach {
                    result[it.timesheetId] = Attendance(year, month, it.millis)
                }
            }
        }
        return timesheets.mapNotNull { it.id }.mapNotNull { id -> result[id]?.let { id to it } }.toMap()
    }

    /** User, year, month and the old kost2 of moved time sheets. */
    private data class MonthKey(val userId: Long, val year: Int, val month: Int, val kost2Id: Long)

    /**
     * Calculates the new effective amounts of the records of the period: first the salary records by the moves, then
     * the rules.
     * @param restrictRecordIds Only these records (re-apply).
     * @param extraMillis The millis moved away from A already, to add to the current millis on A (re-apply).
     */
    private fun calculate(
        context: Context, run: Kost2ReallocationDO, moves: List<Kost2ReallocationPlan.TimesheetMove>, reapply: Boolean,
        restrictRecordIds: Set<Long>? = null, extraMillis: (MonthKey) -> Long = { 0L },
        initialWarnings: List<String> = emptyList(),
    ): Kost2ReallocationPlan {
        val warnings = initialWarnings.toMutableList()
        val records = selectRecords(run).filter { restrictRecordIds == null || it.id in restrictRecordIds }
        val sharesByRecord = shareService.activeSharesByRecordId(records.mapNotNull { it.id })
        val initial = records.associate { it.id!! to shareService.effectiveAmounts(it, sharesByRecord[it.id]) }
        val working = initial.mapValues { (_, amounts) -> LinkedHashMap(amounts) }
        val salaryRecordIds = records.filter { isSalaryRecord(it) }.mapNotNull { it.id }.toSet()
        // Salary records: split by the millis of the moved time sheets.
        moves.groupBy { MonthKey(it.userId, it.year, it.month, it.oldKost2Id) }.forEach { (key, group) ->
            val kost1Id = employeeCache.getEmployeeByUserId(key.userId)?.kost1?.id
            val userName = userGroupCache.getUser(key.userId)?.getFullname() ?: "#${key.userId}"
            val kost2Number = caches.getKost2(key.kost2Id)?.formattedNumber ?: "#${key.kost2Id}"
            if (kost1Id == null) {
                warnings.add(translateMsg("fibu.kost2Reallocation.warning.noKost1", userName))
                return@forEach
            }
            val salaryRecords = records.filter { record ->
                record.id in salaryRecordIds && record.kost1?.id == kost1Id && record.year == key.year &&
                        record.month == key.month && working[record.id]!!.containsKey(key.kost2Id)
            }
            if (salaryRecords.isEmpty()) {
                warnings.add(
                    translateMsg("fibu.kost2Reallocation.warning.noSalaryRecord", userName, "${key.year}-${key.month}", kost2Number)
                )
                return@forEach
            }
            val current = context.report(key.userId, key.year, key.month)?.kost2TimesheetAttendances?.values
                ?.filter { it.kost2Id == key.kost2Id }?.sumOf { it.millis } ?: 0L
            val basis = current + extraMillis(key)
            val selectedByTarget = group.groupBy { it.newKost2Id }.mapValues { (_, list) -> list.sumOf { it.millis } }
            val selected = selectedByTarget.values.sum()
            val rest = basis - selected
            if (rest < 0) {
                warnings.add(translateMsg("fibu.kost2Reallocation.warning.millisExceeded", userName, "${key.year}-${key.month}", kost2Number))
            }
            salaryRecords.forEach { record ->
                val amounts = working[record.id]!!
                val amountA = amounts.remove(key.kost2Id) ?: return@forEach
                val targets = selectedByTarget.keys.toList()
                val parts = BuchungssatzKost2ShareService.splitByMillis(
                    amountA, targets.map { selectedByTarget[it]!! } + maxOf(rest, 0L),
                )
                targets.forEachIndexed { index, kost2Id ->
                    amounts[kost2Id] = (amounts[kost2Id] ?: BigDecimal.ZERO) + parts[index]
                }
                if (parts.last().signum() != 0) {
                    amounts[key.kost2Id] = (amounts[key.kost2Id] ?: BigDecimal.ZERO) + parts.last()
                }
            }
        }
        // Rules A -> B: the other records (salary records only, if the account range covers them explicitly). The rules
        // read the amounts before the rules, so they don't chain (A -> B and B -> C moves A to B only), and each source
        // moves by its first matching rule.
        val rules = getRules(run)
        records.forEach { record ->
            val amounts = working[record.id] ?: return@forEach
            val before = LinkedHashMap(amounts)
            val kontoNummer = kontoCache.getKontoIfNotInitialized(record.konto)?.nummer
            val salary = record.id in salaryRecordIds
            val moved = mutableSetOf<Long>()
            val additions = LinkedHashMap<Long, BigDecimal>()
            rules.forEach { rule ->
                val sourceId = rule.sourceKost2?.id ?: return@forEach
                val targetId = rule.targetKost2?.id ?: return@forEach
                if (sourceId in moved || !before.containsKey(sourceId) || !rule.matchesKonto(kontoNummer)) {
                    return@forEach
                }
                if (salary && (kontoNummer == null || !rule.coversKontoExplicitly(kontoNummer))) {
                    return@forEach
                }
                moved.add(sourceId)
                additions[targetId] = (additions[targetId] ?: BigDecimal.ZERO) + before[sourceId]!!
            }
            moved.forEach { amounts.remove(it) }
            additions.forEach { (kost2Id, amount) -> amounts[kost2Id] = (amounts[kost2Id] ?: BigDecimal.ZERO) + amount }
        }
        val changes = records.mapNotNull { record ->
            val old = initial[record.id]!!
            val new = working[record.id]!!.filterValues { it.signum() != 0 }.ifEmpty { working[record.id]!! }
            if (sameAmounts(old, new)) null else {
                Kost2ReallocationPlan.RecordChange(record, old, new, record.id in salaryRecordIds)
            }
        }
        if (!reapply) {
            warnings.addAll(plausibilityWarnings(context, moves, records, salaryRecordIds, initial))
        }
        return Kost2ReallocationPlan(run, changes, if (reapply) emptyList() else moves, warnings)
    }

    /**
     * The salary split recalculated from the current time sheets (as the salary export does) is compared with the
     * effective amounts of the stored salary records of the employees and months of the moved time sheets. A difference
     * means that time sheets were changed after the export.
     */
    private fun plausibilityWarnings(
        context: Context, moves: List<Kost2ReallocationPlan.TimesheetMove>, records: List<BuchungssatzDO>, salaryRecordIds: Set<Long>,
        effective: Map<Long, Map<Long, BigDecimal>>,
    ): List<String> {
        val warnings = mutableListOf<String>()
        moves.map { Triple(it.userId, it.year, it.month) }.distinct().forEach { (userId, year, month) ->
            val employee = employeeCache.getEmployeeByUserId(userId) ?: return@forEach
            val kost1Id = employee.kost1?.id ?: return@forEach
            val userName = userGroupCache.getUser(userId)?.getFullname() ?: "#$userId"
            val salary = persistenceService.executeQuery(
                "from EmployeeSalaryDO s where s.employee.id = :employeeId and s.year = :year and s.month = :month and s.deleted = false",
                EmployeeSalaryDO::class.java,
                Pair("employeeId", employee.id),
                Pair("year", year),
                Pair("month", month),
            ).firstOrNull()
            val brutto = salary?.bruttoMitAgAnteil
            val report = context.report(userId, year, month)
            if (brutto == null || report == null) {
                warnings.add(translateMsg("fibu.kost2Reallocation.warning.noSalary", userName, "$year-$month"))
                return@forEach
            }
            val expected = EmployeeSalarySplit.split(brutto, report).associate { it.kost2.id!! to it.amount }
            val stored = mutableMapOf<Long, BigDecimal>()
            records.filter {
                it.id in salaryRecordIds && it.kost1?.id == kost1Id && it.year == year && it.month == month
            }.forEach { record ->
                effective[record.id]?.forEach { (kost2Id, amount) ->
                    stored[kost2Id] = (stored[kost2Id] ?: BigDecimal.ZERO) + amount
                }
            }
            val differences = (expected.keys + stored.keys).filter { kost2Id ->
                val exp = expected[kost2Id] ?: BigDecimal.ZERO
                val act = (stored[kost2Id] ?: BigDecimal.ZERO).abs()
                (exp.abs() - act).abs() > TOLERANCE
            }
            if (differences.isNotEmpty()) {
                val details = differences.joinToString("; ") { kost2Id ->
                    val number = caches.getKost2(kost2Id)?.formattedNumber ?: "#$kost2Id"
                    "$number: ${format(expected[kost2Id])} / ${format(stored[kost2Id]?.abs())}"
                }
                warnings.add(translateMsg("fibu.kost2Reallocation.warning.salaryMismatch", userName, "$year-$month", details))
            }
        }
        return warnings
    }

    /**
     * The re-apply of an applied run: the records of the run whose shares of the run were all discarded, calculated
     * from the log. The basis on A are the current millis on A plus the millis moved away from A by this and the later
     * runs (all their shares on the record were discarded, too).
     */
    private fun calculateReapply(run: Kost2ReallocationDO): Kost2ReallocationPlan {
        if (run.status != Kost2ReallocationStatus.APPLIED) {
            throw UserException("fibu.kost2Reallocation.error.notApplied")
        }
        val recordIds = recordIdsToReapply(run)
        if (recordIds.isEmpty()) {
            return Kost2ReallocationPlan(run, emptyList(), emptyList(), emptyList())
        }
        val earlier = appliedRuns().filter { isBefore(it, run) }
        earlier.forEach { other ->
            if (recordIdsToReapply(other).any { it in recordIds }) {
                throw UserException("fibu.kost2Reallocation.error.reapplyEarlierRun", other.description)
            }
        }
        val laterAndThis = listOf(run) + laterAppliedRuns(run)
        val logs = timesheetLogDao.selectByRuns(laterAndThis.mapNotNull { it.id })
        val extra = logs.groupBy { entry ->
            MonthKey(entry.user!!.id!!, entry.year!!, entry.month!!, entry.oldKost2?.id ?: -1L)
        }.mapValues { (_, list) -> list.sumOf { it.millis } }
        val moves = timesheetLogDao.selectByRun(run.id!!).mapNotNull { entry ->
            val oldKost2Id = entry.oldKost2?.id ?: return@mapNotNull null
            val newKost2Id = entry.newKost2?.id ?: return@mapNotNull null
            Kost2ReallocationPlan.TimesheetMove(
                timesheet = null,
                timesheetId = entry.timesheet!!.id!!,
                userId = entry.user!!.id!!,
                year = entry.year!!,
                month = entry.month!!,
                oldTaskId = entry.oldTask!!.id!!,
                newTaskId = entry.newTask!!.id!!,
                oldKost2Id = oldKost2Id,
                newKost2Id = newKost2Id,
                millis = entry.millis,
                manual = entry.oldTask?.id != entry.newTask?.id,
            )
        }
        return calculate(Context(), run, moves, reapply = true, restrictRecordIds = recordIds, extraMillis = { extra[it] ?: 0L })
    }

    /** The records with shares of the run which were all discarded (DATEV reimport). */
    private fun recordIdsToReapply(run: Kost2ReallocationDO): Set<Long> {
        val all = shareDao.selectAllOfRun(run.id ?: return emptySet())
        return all.groupBy { it.buchungssatz!!.id!! }.filterValues { shares -> shares.all { it.deleted } }.keys
    }

    /**
     * Stores the new shares of the changed records: the active ones (if any) are superseded by the run, the new ones
     * belong to the run.
     */
    private fun persistShares(run: Kost2ReallocationDO, plan: Kost2ReallocationPlan) {
        val activeByRecord = shareService.activeSharesByRecordId(plan.records.mapNotNull { it.record.id })
        plan.records.forEach { change ->
            activeByRecord[change.record.id]?.forEach { share ->
                share.supersededByRun = run
                shareDao.update(share)
            }
            change.newAmounts.forEach { (kost2Id, amount) ->
                val share = BuchungssatzKost2ShareDO()
                share.buchungssatz = change.record.originalRecord ?: change.record
                share.kost2 = caches.getKost2(kost2Id)
                share.betrag = amount
                share.source = if (change.salary) Kost2ShareSource.SALARY else Kost2ShareSource.RULE
                share.run = run
                shareDao.insert(share)
            }
        }
    }

    private fun moveTimesheet(run: Kost2ReallocationDO, move: Kost2ReallocationPlan.TimesheetMove) {
        val timesheet = timesheetDao.find(move.timesheetId, checkAccess = false)
            ?: throw UserException("fibu.kost2Reallocation.error.timesheetNotFound", move.timesheetId)
        if (timesheet.kost2Id != move.oldKost2Id || timesheet.taskId != move.oldTaskId) {
            // Changed since the calculation.
            throw UserException("fibu.kost2Reallocation.error.timesheetChanged", describe(timesheet))
        }
        val newTask = taskTree.getTaskById(move.newTaskId)!!
        timesheetDao.reallocate(timesheet, newTask, caches.getKost2(move.newKost2Id))
        val entry = Kost2ReallocationTimesheetDO()
        entry.run = run
        entry.timesheet = timesheet
        entry.user = userGroupCache.getUser(move.userId)
        entry.year = move.year
        entry.month = move.month
        entry.oldTask = taskTree.getTaskById(move.oldTaskId)
        entry.newTask = newTask
        entry.oldKost2 = caches.getKost2(move.oldKost2Id)
        entry.newKost2 = caches.getKost2(move.newKost2Id)
        entry.millis = move.millis
        timesheetLogDao.insert(entry)
    }

    private fun appliedRuns(): List<Kost2ReallocationDO> = runDao.selectApplied()

    private fun laterAppliedRuns(run: Kost2ReallocationDO): List<Kost2ReallocationDO> {
        return appliedRuns().filter { it.id != run.id && isBefore(run, it) }
    }

    /** Was [a] applied before [b]? */
    private fun isBefore(a: Kost2ReallocationDO, b: Kost2ReallocationDO): Boolean {
        val timeA = a.appliedAt?.time ?: Long.MAX_VALUE
        val timeB = b.appliedAt?.time ?: Long.MAX_VALUE
        return timeA < timeB || (timeA == timeB && (a.id ?: 0) < (b.id ?: 0))
    }

    /** The not deleted records of the period of the run. */
    private fun selectRecords(run: Kost2ReallocationDO): List<BuchungssatzDO> {
        val queryFilter = QueryFilter()
        queryFilter.deleted = false
        queryFilter.maxRows = MAX_ROWS
        buchungssatzDao.addTimePeriodPredicates(queryFilter, run.fromYear, run.fromMonth, run.toYear, run.toMonth)
        return buchungssatzDao.select(queryFilter, checkAccess = false)
    }

    /** The not deleted time sheets on the given tasks in the period of the run (with a day as margin per side). */
    private fun selectTimesheets(run: Kost2ReallocationDO, taskIds: Collection<Long>): List<TimesheetDO> {
        if (taskIds.isEmpty()) {
            return emptyList()
        }
        val zone = ZoneId.of("UTC")
        val from = PFDateTime.withDate(run.fromYear!!, run.fromMonth!!, 1, zoneId = zone).minusDays(1).utilDate
        val until = PFDateTime.withDate(run.toYear!!, run.toMonth!!, 1, zoneId = zone).endOfMonth.plusDays(1).utilDate
        return taskIds.chunked(CHUNK_SIZE).flatMap { chunk ->
            val queryFilter = QueryFilter()
            queryFilter.deleted = false
            queryFilter.maxRows = MAX_ROWS
            queryFilter.add(isIn("task.id", chunk))
            queryFilter.add(ge("stopTime", from))
            queryFilter.add(le("startTime", until))
            timesheetDao.select(queryFilter, checkAccess = false)
        }.distinctBy { it.id }.sortedWith(compareBy({ it.userId }, { it.startTime }))
    }

    /** The months of the period of the run (year to 1-based month). */
    private fun months(run: Kost2ReallocationDO): List<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()
        var year = run.fromYear ?: return result
        var month = run.fromMonth ?: return result
        val to = run.toYearMonth ?: return result
        while (year * 100 + month <= to) {
            result.add(year to month)
            if (++month > 12) {
                month = 1
                ++year
            }
        }
        return result
    }

    private fun isSalaryRecord(record: BuchungssatzDO): Boolean {
        return EmployeeSalarySplit.isSalaryRecord(
            kontoCache.getKontoIfNotInitialized(record.konto)?.nummer,
            kontoCache.getKontoIfNotInitialized(record.gegenKonto)?.nummer,
        )
    }

    private fun sameAmounts(a: Map<Long, BigDecimal>, b: Map<Long, BigDecimal>): Boolean {
        if (a.keys != b.keys) {
            return false
        }
        return a.all { (key, value) -> value.compareTo(b[key]) == 0 }
    }

    private fun describe(timesheet: TimesheetDO): String {
        val user = userGroupCache.getUser(timesheet.userId)?.getFullname() ?: "#${timesheet.userId}"
        return "$user ${timesheet.timePeriod.formattedString}"
    }

    private fun format(value: BigDecimal?): String = NumberFormatter.format(value, 2)

    companion object {
        private const val CHUNK_SIZE = 1000

        private const val MAX_ROWS = 1_000_000

        /** Differences of the plausibility check up to one cent are rounding. */
        private val TOLERANCE = BigDecimal("0.01")
    }
}
