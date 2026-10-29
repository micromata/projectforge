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

package org.projectforge.rest.fibu.kost

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.KontoCache
import org.projectforge.business.fibu.KostFormatter
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.reallocation.Kost2ReallocationDO
import org.projectforge.business.fibu.kost.reallocation.Kost2ReallocationDao
import org.projectforge.business.fibu.kost.reallocation.Kost2ReallocationPlan
import org.projectforge.business.fibu.kost.reallocation.Kost2ReallocationRuleDO
import org.projectforge.business.fibu.kost.reallocation.Kost2ReallocationRuleDao
import org.projectforge.business.fibu.kost.reallocation.Kost2ReallocationService
import org.projectforge.business.fibu.kost.reallocation.Kost2ReallocationStatus
import org.projectforge.business.task.TaskTree
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * The Kost2 reallocation runs ([Kost2ReallocationService]) for the hand-built page of projectforge-next
 * (`/next/kost2-reallocation`): the list of the runs, a run with its rules (only a draft may be saved or deleted),
 * the time sheets to move, the preview, and applying, re-applying and reverting a run.
 *
 * Finance only, checked by every endpoint ([Kost2ReallocationService.checkAccess], the DAOs check it as well). The
 * run is always taken from the database by its id, never from the request. A refused action (e. g. a revert after
 * a later run) answers HTTP 406 with the message as validation error; nothing was written then.
 */
@RestController
@RequestMapping("${Rest.URL}/kost2Reallocation")
class Kost2ReallocationRest {
    class Ref(val id: Long, val displayName: String)

    class RunRow(
        val id: Long,
        val description: String?,
        val projekt: Ref?,
        val fromYear: Int?,
        val fromMonth: Int?,
        val toYear: Int?,
        val toMonth: Int?,
        val status: Kost2ReallocationStatus,
        val appliedAt: Date?,
    )

    class RuleData(
        var sourceKost2: Ref? = null,
        var targetKost2: Ref? = null,
        var kontoFrom: Int? = null,
        var kontoTo: Int? = null,
    )

    /** A moved time sheet of the log of an applied run. */
    class LogRow(
        val timesheetId: Long?,
        val user: String?,
        val year: Int?,
        val month: Int?,
        val oldTask: String?,
        val newTask: String?,
        val oldKost2: String?,
        val newKost2: String?,
        val hours: BigDecimal,
    )

    class RunData(
        var id: Long? = null,
        var description: String? = null,
        var projekt: Ref? = null,
        var fromYear: Int? = null,
        var fromMonth: Int? = null,
        var toYear: Int? = null,
        var toMonth: Int? = null,
        var comment: String? = null,
        var status: Kost2ReallocationStatus? = null,
        var appliedAt: Date? = null,
        var rules: List<RuleData> = emptyList(),
        /** Some shares of the applied run were discarded by a DATEV reimport. */
        var needsReapply: Boolean = false,
        var log: List<LogRow> = emptyList(),
    )

    class DefaultRulesRequest(
        var projektId: Long? = null,
        var sourceKost2Ids: List<Long> = emptyList(),
        var sourceProjektIds: List<Long> = emptyList(),
    )

    class TimesheetsRequest(var runId: Long? = null)

    class TimesheetRow(
        val id: Long,
        val user: String?,
        val startTime: Date?,
        val stopTime: Date?,
        val hours: BigDecimal,
        val task: String?,
        val kost2: String?,
        val description: String?,
    )

    class TimesheetsResponse(val detected: List<TimesheetRow>)

    class RunRequest(var runId: Long? = null)

    class AmountRow(val kost2: String, val amount: BigDecimal)

    class RecordRow(
        val id: Long?,
        val satznr: String?,
        val konto: Int?,
        val text: String?,
        val beleg: String?,
        val betrag: BigDecimal?,
        val salary: Boolean,
        val oldAmounts: List<AmountRow>,
        val newAmounts: List<AmountRow>,
    )

    class MoveRow(
        val timesheetId: Long,
        val user: String?,
        val year: Int,
        val month: Int,
        val oldTask: String?,
        val newTask: String?,
        val oldKost2: String?,
        val newKost2: String?,
        val hours: BigDecimal,
    )

    class SumRow(val kost2: String, val before: BigDecimal, val after: BigDecimal)

    class PlanResponse(
        val records: List<RecordRow>,
        val timesheets: List<MoveRow>,
        val sums: List<SumRow>,
        val warnings: List<String>,
        val errors: List<String>,
    )

    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var kontoCache: KontoCache

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var runDao: Kost2ReallocationDao

    @Autowired
    private lateinit var ruleDao: Kost2ReallocationRuleDao

    @Autowired
    private lateinit var service: Kost2ReallocationService

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @AccessChecked("FINANCE group (Kost2ReallocationService.checkAccess)")
    @GetMapping("list")
    fun list(): List<RunRow> {
        service.checkAccess()
        return runDao.select(deleted = false, checkAccess = true)
            .sortedByDescending { it.created }
            .map { run ->
                RunRow(
                    id = run.id!!,
                    description = run.description,
                    projekt = projektRef(run.projekt?.id),
                    fromYear = run.fromYear,
                    fromMonth = run.fromMonth,
                    toYear = run.toYear,
                    toMonth = run.toMonth,
                    status = run.status,
                    appliedAt = run.appliedAt,
                )
            }
    }

    @AccessChecked("FINANCE group (Kost2ReallocationService.checkAccess)")
    @GetMapping("run")
    fun getRun(@RequestParam("id") id: Long): RunData {
        return toData(service.getRun(id))
    }

    /** Inserts a new draft or updates a draft (and replaces its rules). */
    @AccessChecked("FINANCE group (Kost2ReallocationService.checkAccess, DAO)")
    @PostMapping("save")
    fun save(@RequestBody data: RunData): ResponseEntity<*> {
        service.checkAccess()
        return handle {
            val runId = persistenceService.runInTransaction {
                val run = data.id?.let { service.getRun(it) } ?: Kost2ReallocationDO()
                if (run.id != null && run.status != Kost2ReallocationStatus.DRAFT) {
                    throw UserException("fibu.kost2Reallocation.error.notDraft")
                }
                run.description = data.description?.trim()?.takeIf { it.isNotEmpty() }
                    ?: throw UserException("validation.error.fieldRequired", translate("description"))
                run.projekt = caches.getProjekt(data.projekt?.id)
                    ?: throw UserException("validation.error.fieldRequired", translate("fibu.kost2Reallocation.targetProject"))
                run.fromYear = data.fromYear
                run.fromMonth = data.fromMonth
                run.toYear = data.toYear
                run.toMonth = data.toMonth
                run.comment = data.comment
                if (run.id == null) {
                    runDao.insert(run)
                } else {
                    runDao.update(run)
                }
                ruleDao.selectByRun(run.id!!).forEach { ruleDao.markAsDeleted(it) }
                data.rules.forEach { rule ->
                    val source = caches.getKost2(rule.sourceKost2?.id) ?: return@forEach
                    val target = caches.getKost2(rule.targetKost2?.id) ?: return@forEach
                    ruleDao.insert(Kost2ReallocationRuleDO().also {
                        it.run = run
                        it.sourceKost2 = source
                        it.targetKost2 = target
                        it.kontoFrom = rule.kontoFrom
                        it.kontoTo = rule.kontoTo
                    })
                }
                run.id!!
            }
            toData(service.getRun(runId))
        }
    }

    @AccessChecked("FINANCE group (Kost2ReallocationService.checkAccess, DAO refuses applied runs)")
    @PostMapping("delete")
    fun delete(@RequestBody request: RunRequest): ResponseEntity<*> {
        return handle {
            val run = service.getRun(request.runId ?: throw UserException("fibu.kost2Reallocation.error.runNotFound"))
            runDao.markAsDeleted(run)
            true
        }
    }

    /** The default rules of the target project for the given source kost2 and source projects (same kost2 type). */
    @AccessChecked("FINANCE group (Kost2ReallocationService.defaultRules)")
    @PostMapping("defaultRules")
    fun defaultRules(@RequestBody request: DefaultRulesRequest): List<RuleData> {
        service.checkAccess()
        val projektId = request.projektId ?: return emptyList()
        return service.defaultRules(projektId, request.sourceKost2Ids, request.sourceProjektIds).map { (source, target) ->
            RuleData(sourceKost2 = kost2Ref(source), targetKost2 = kost2Ref(target))
        }
    }

    /** The detected time sheets of the run. */
    @AccessChecked("FINANCE group (Kost2ReallocationService.checkAccess)")
    @PostMapping("timesheets")
    fun timesheets(@RequestBody request: TimesheetsRequest): ResponseEntity<*> {
        return handle {
            val run = service.getRun(request.runId ?: throw UserException("fibu.kost2Reallocation.error.runNotFound"))
            TimesheetsResponse(detected = service.detectTimesheets(run).map { toRow(it) })
        }
    }

    @AccessChecked("FINANCE group (Kost2ReallocationService.checkAccess)")
    @PostMapping("preview")
    fun preview(@RequestBody request: RunRequest): ResponseEntity<*> {
        return handle { toResponse(service.preview(runId(request))) }
    }

    @AccessChecked("FINANCE group (Kost2ReallocationService.checkAccess)")
    @PostMapping("apply")
    fun apply(@RequestBody request: RunRequest): ResponseEntity<*> {
        return handle { toResponse(service.apply(runId(request))) }
    }

    @AccessChecked("FINANCE group (Kost2ReallocationService.checkAccess)")
    @PostMapping("reapply")
    fun reapply(@RequestBody request: RunRequest): ResponseEntity<*> {
        return handle { toResponse(service.reapply(runId(request))) }
    }

    @AccessChecked("FINANCE group (Kost2ReallocationService.checkAccess)")
    @PostMapping("revert")
    fun revert(@RequestBody request: RunRequest): ResponseEntity<*> {
        return handle {
            val runId = runId(request)
            service.revert(runId)
            toData(service.getRun(runId))
        }
    }

    private fun runId(request: RunRequest): Long {
        return request.runId ?: throw UserException("fibu.kost2Reallocation.error.runNotFound")
    }

    /** A refused action answers 406 with the message (everything was rolled back). */
    private fun handle(block: () -> Any): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(block())
        } catch (ex: AccessException) {
            throw ex // A refused access is answered as such (403), not as a validation error.
        } catch (ex: UserException) {
            log.info { "Kost2 reallocation: ${translateMsg(ex)}" }
            ResponseEntity(
                ResponseAction(validationErrors = listOf(ValidationError(translateMsg(ex)))),
                HttpStatus.NOT_ACCEPTABLE,
            )
        }
    }

    private fun toData(run: Kost2ReallocationDO): RunData {
        return RunData(
            id = run.id,
            description = run.description,
            projekt = projektRef(run.projekt?.id),
            fromYear = run.fromYear,
            fromMonth = run.fromMonth,
            toYear = run.toYear,
            toMonth = run.toMonth,
            comment = run.comment,
            status = run.status,
            appliedAt = run.appliedAt,
            rules = service.getRules(run).map { rule ->
                RuleData(
                    sourceKost2 = kost2Ref(caches.getKost2(rule.sourceKost2?.id)),
                    targetKost2 = kost2Ref(caches.getKost2(rule.targetKost2?.id)),
                    kontoFrom = rule.kontoFrom,
                    kontoTo = rule.kontoTo,
                )
            },
            needsReapply = service.needsReapply(run),
            log = service.getTimesheetLog(run).map { entry ->
                LogRow(
                    timesheetId = entry.timesheet?.id,
                    user = userGroupCache.getUser(entry.user?.id)?.getFullname(),
                    year = entry.year,
                    month = entry.month,
                    oldTask = taskTitle(entry.oldTask?.id),
                    newTask = taskTitle(entry.newTask?.id),
                    oldKost2 = kost2Number(entry.oldKost2?.id),
                    newKost2 = kost2Number(entry.newKost2?.id),
                    hours = hours(entry.millis),
                )
            },
        )
    }

    private fun toResponse(plan: Kost2ReallocationPlan): PlanResponse {
        return PlanResponse(
            records = plan.records.map { change ->
                val record = change.record
                RecordRow(
                    id = record.id,
                    satznr = if (record.year != null && record.month != null) record.formattedSatzNummer else null,
                    konto = kontoCache.getKontoIfNotInitialized(record.konto)?.nummer,
                    text = record.text,
                    beleg = record.beleg,
                    betrag = record.betrag,
                    salary = change.salary,
                    oldAmounts = amounts(change.oldAmounts),
                    newAmounts = amounts(change.newAmounts),
                )
            },
            timesheets = plan.timesheets.map { move ->
                MoveRow(
                    timesheetId = move.timesheetId,
                    user = userGroupCache.getUser(move.userId)?.getFullname(),
                    year = move.year,
                    month = move.month,
                    oldTask = taskTitle(move.oldTaskId),
                    newTask = taskTitle(move.newTaskId),
                    oldKost2 = kost2Number(move.oldKost2Id),
                    newKost2 = kost2Number(move.newKost2Id),
                    hours = hours(move.millis),
                )
            },
            sums = plan.sumsByKost2.map { (kost2Id, sums) ->
                SumRow(kost2 = kost2Number(kost2Id) ?: "#$kost2Id", before = sums.first, after = sums.second)
            },
            warnings = plan.warnings,
            errors = plan.errors,
        )
    }

    private fun amounts(amounts: Map<Long, BigDecimal>): List<AmountRow> {
        return amounts.map { (kost2Id, amount) -> AmountRow(kost2Number(kost2Id) ?: "#$kost2Id", amount) }
    }

    private fun toRow(timesheet: TimesheetDO): TimesheetRow {
        return TimesheetRow(
            id = timesheet.id!!,
            user = userGroupCache.getUser(timesheet.userId)?.getFullname(),
            startTime = timesheet.startTime,
            stopTime = timesheet.stopTime,
            hours = hours(timesheet.duration),
            task = taskTitle(timesheet.taskId),
            kost2 = kost2Number(timesheet.kost2Id),
            description = timesheet.description,
        )
    }

    private fun projektRef(projektId: Long?): Ref? {
        val projekt = caches.getProjekt(projektId) ?: return null
        return Ref(projekt.id!!, projekt.displayName)
    }

    private fun kost2Ref(kost2: Kost2DO?): Ref? {
        kost2 ?: return null
        return Ref(kost2.id!!, KostFormatter.instance.formatKost2(kost2, KostFormatter.FormatType.TEXT))
    }

    private fun kost2Number(kost2Id: Long?): String? = caches.getKost2(kost2Id)?.formattedNumber

    /** The path of the task below its project's task (or the title only), as the time sheet lists show it. */
    private fun taskTitle(taskId: Long?): String? {
        val node = taskTree.getTaskNodeById(taskId) ?: return null
        return taskTree.getPathToRoot(taskId).takeLast(3).joinToString(" / ") { it.task.title ?: "" }
            .ifEmpty { node.task.title }
    }

    private fun hours(millis: Long): BigDecimal {
        return BigDecimal(millis).divide(BigDecimal(MILLIS_PER_HOUR), 2, java.math.RoundingMode.HALF_UP)
    }

    companion object {

        private const val MILLIS_PER_HOUR = 3_600_000L
    }
}
