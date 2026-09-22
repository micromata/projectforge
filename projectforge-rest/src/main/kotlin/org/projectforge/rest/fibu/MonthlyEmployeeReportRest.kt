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

import mu.KotlinLogging
import org.projectforge.business.common.OutputType
import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.business.fibu.InvoicingQuotaService
import org.projectforge.business.fibu.MonthlyEmployeeReport
import org.projectforge.business.fibu.MonthlyEmployeeReportDao
import org.projectforge.business.fibu.OldKostFormatter
import org.projectforge.business.fibu.kost.Kost1Dao
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.task.TaskFormatter
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.business.vacation.service.VacationService
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.renderer.PdfRenderer
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.dto.MonthlyEmployeeReportData
import org.projectforge.rest.dto.MonthlyEmployeeReportRow
import org.projectforge.rest.dto.MonthlyEmployeeReportWeekDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * The monthly employee report ("Monatsbericht"), successor of Wicket's `MonthlyEmployeeReportPage`
 * (`wa/monthlyEmployeeReport`). Like the global [org.projectforge.rest.SearchRest] it is a non-entity,
 * standalone page: it exposes the already-computed report as plain JSON so the next frontend can render
 * the matrix (rows = cost units / tasks, columns = calendar weeks) and the header statistics.
 *
 * The computation is reused verbatim from [MonthlyEmployeeReportDao.getReport], which reads the month's
 * time sheets **without** an access check ([TimesheetDao.internalGetList]) and masks foreign, unreadable
 * sheets behind the pseudo task ("******"), exactly as the legacy page did. Reading another user's report
 * is gated here by [AccessChecker.hasLoggedInUserAccessToTimesheetsOfOtherUsers]; a denied request throws
 * [AccessException], which the next clients receive as HTTP 403 (see GlobalDefaultExceptionHandler).
 *
 * CSRF is inherited via `RestAuthenticationUtils`/`RestCsrfProtection` for all `/rs` endpoints. The legacy page is
 * not 2FA-gated (not in any `My2FAShortCut` list), so no 2FA registration is needed.
 */
@RestController
@RequestMapping("${Rest.URL}/monthlyEmployeeReport")
class MonthlyEmployeeReportRest {
    @Autowired
    private lateinit var monthlyEmployeeReportDao: MonthlyEmployeeReportDao

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var employeeCache: EmployeeCache

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var kost1Dao: Kost1Dao

    @Autowired
    private lateinit var vacationService: VacationService

    @Autowired
    private lateinit var invoicingQuotaService: InvoicingQuotaService

    @Autowired
    private lateinit var pdfRenderer: PdfRenderer

    @Autowired
    private lateinit var dateTimeFormatter: DateTimeFormatter

    @Autowired
    private lateinit var userPrefService: UserPrefService

    /**
     * @param userId The user to report on; defaults to the logged-in user. A foreign user requires access to
     * other users' time sheets, else HTTP 403.
     * @param year   Defaults to the current year.
     * @param month  1-based (1 = January, ..., 12 = December); defaults to the current month.
     */
    @GetMapping
    fun getReport(
        @RequestParam("userId", required = false) userId: Long?,
        @RequestParam("year", required = false) year: Int?,
        @RequestParam("month", required = false) month: Int?,
    ): MonthlyEmployeeReportData {
        val now = PFDay.now()
        // Fall back to the last selection (persisted per user, like the legacy page's user-pref filter): the
        // report reopens on the user/month last looked at. A persisted foreign user is only honored while the
        // access still holds, so a stale pref never locks the own report behind a 403.
        val effectiveUserId = userId ?: restoredUserId()
        val user = resolveUser(effectiveUserId)
        val reportYear = year ?: userPrefService.getEntry(PREF_AREA, PREF_YEAR, Int::class.java) ?: now.year
        val reportMonth = month ?: userPrefService.getEntry(PREF_AREA, PREF_MONTH, Int::class.java) ?: now.monthValue
        val report = monthlyEmployeeReportDao.getReport(reportYear, reportMonth, user)
            ?: throw AccessException("access.exception.noAccess")
        userPrefService.putEntry(PREF_AREA, PREF_USER_ID, user.id, true)
        userPrefService.putEntry(PREF_AREA, PREF_YEAR, reportYear, true)
        userPrefService.putEntry(PREF_AREA, PREF_MONTH, reportMonth, true)
        return toDto(report, user)
    }

    /**
     * The persisted user of the last visit, but only while it is still reachable: the own user always, a
     * foreign one only with the right to read other users' time sheets. Null when nothing was stored yet.
     */
    private fun restoredUserId(): Long? {
        val loggedInUser = ThreadLocalUserContext.loggedInUser ?: return null
        val stored = userPrefService.getEntry(PREF_AREA, PREF_USER_ID, Long::class.java) ?: return null
        return stored.takeIf {
            it == loggedInUser.id || accessChecker.hasLoggedInUserAccessToTimesheetsOfOtherUsers()
        }
    }

    /**
     * The years that have time sheets for [userId] (defaults to the logged-in user), for the year dropdown.
     */
    @GetMapping("years")
    fun getYears(@RequestParam("userId", required = false) userId: Long?): List<Int> {
        val user = resolveUser(userId)
        return timesheetDao.getYears(user.id).toList().sortedDescending()
    }

    /**
     * Renders the report as PDF, reusing the same Apache FOP stylesheet as the legacy page. The body mirrors
     * the report request; the PDF is streamed as a download named `Monatsbericht_<lastname>_<year>-<month>.pdf`.
     */
    @PostMapping("exportPdf")
    fun exportPdf(@RequestBody request: ExportRequest): ResponseEntity<Resource> {
        val user = resolveUser(request.userId)
        val now = PFDay.now()
        val year = request.year ?: now.year
        val month = request.month ?: now.monthValue
        val report = monthlyEmployeeReportDao.getReport(year, month, user)
            ?: throw AccessException("access.exception.noAccess")
        log.info { "Monthly employee report for ${user.getFullname()}: $year-${report.formmattedMonth}" }

        val kost1 = kost1Dao.find(report.kost1Id, false)
        val data = mapOf<String, Any>(
            "systemDate" to dateTimeFormatter.getFormattedDateTime(Date()),
            "title" to translate("menu.monthlyEmployeeReport"),
            "employeeLabel" to translate("timesheet.user"),
            "employee" to user.getFullname(),
            "monthLabel" to translate("calendar.month"),
            "year" to year,
            "month" to report.formmattedMonth,
            "workingDaysLabel" to translate("fibu.common.workingDays"),
            "workingDays" to (report.numberOfWorkingDays ?: ""),
            "kost1Label" to translate("fibu.kost1"),
            "kost1" to (kost1?.formattedNumber ?: "--"),
            "kost2Label" to translate("fibu.kost2"),
            "kundeLabel" to translate("fibu.kunde"),
            "projektLabel" to translate("fibu.projekt"),
            "kost2ArtLabel" to translate("fibu.kost2.art"),
            "sumLabel" to translate("sum"),
            "netSumLabel" to translate("sum"),
            "totalSumLabel" to translate("fibu.monthlyEmployeeReport.totalSum"),
            "vacationAvailabel" to vacationService.hasAccessToVacationService(user, false),
            "vacationCountLabel" to translate("vacation.annualleave"),
            "vacationPlandCountLabel" to translate("vacation.plandannualleave"),
            "report" to report,
            "signatureEmployeeLabel" to "${translate("timesheet.signatureEmployee")}: ${user.getFullname()}",
            "signatureProjectLeaderLabel" to translate("timesheet.signatureProjectLeader"),
            "unbookedWorkingDaysLabel" to translate("fibu.monthlyEmployeeReport.withoutTimesheets"),
        )
        val ba = pdfRenderer.render(STYLE_SHEET, XML_DATA, data)
        val filename = "${translate("menu.monthlyEmployeeReport.fileprefix")}_${user.lastname}_$year-${report.formmattedMonth}.pdf"
        return RestUtils.downloadFile(filename, ba)
    }

    /**
     * Resolves the reported user and enforces the access rule: only a user with access to other users' time
     * sheets may request a foreign report; otherwise it falls back to / is limited to the logged-in user.
     */
    private fun resolveUser(userId: Long?): PFUserDO {
        val loggedInUser = ThreadLocalUserContext.loggedInUser
            ?: throw AccessException("access.exception.noAccess")
        val targetUserId = userId ?: loggedInUser.id
        if (targetUserId != loggedInUser.id && !accessChecker.hasLoggedInUserAccessToTimesheetsOfOtherUsers()) {
            throw AccessException("access.exception.userHasNotRight")
        }
        return UserGroupCache.getInstance().getUser(targetUserId)
            ?: throw AccessException("access.exception.noAccess")
    }

    private fun toDto(report: MonthlyEmployeeReport, user: PFUserDO): MonthlyEmployeeReportData {
        val costConfigured = Configuration.instance.isCostConfigured
        val timeSavingsByAIEnabled = timesheetDao.timeSavingsByAIEnabled
        val weeks = report.weeks.map { week ->
            MonthlyEmployeeReportWeekDto(
                fromDay = week.formattedFromDayOfMonth,
                toDay = week.formattedToDayOfMonth,
                totalDuration = week.formattedTotalDuration,
                grossDuration = week.formattedGrossDuration,
                timeSavedByAI = week.formattedTotalTimeSavedByAI,
            )
        }
        val rows = mutableListOf<MonthlyEmployeeReportRow>()
        report.kost2Rows.values.forEach { kost2Row ->
            val kost2 = kost2Row.kost2 ?: return@forEach
            val kost2Id = kost2.id
            val total = kost2Id?.let { report.kost2Durations[it] }
            val project = kost2.projekt
            rows.add(
                MonthlyEmployeeReportRow(
                    type = "kost2",
                    kost2Id = kost2Id,
                    label = OldKostFormatter.format(kost2),
                    // Match the legacy page: with a project show customer/project, otherwise the cost unit's
                    // description takes the customer+project span.
                    customer = if (project != null) project.kunde?.name ?: "" else null,
                    project = if (project != null) project.name else null,
                    description = if (project == null) kost2.description else null,
                    kost2Art = kost2.kost2Art?.name,
                    perWeek = report.weeks.map { it.kost2Entries[kost2Id]?.formattedDuration ?: "" },
                    sum = total?.formattedDuration ?: "",
                    aiTimeSavings = total?.getFormattedTimeSavedByAI ?: "",
                )
            )
        }
        report.taskEntries.values.forEach { task ->
            task ?: return@forEach
            val taskId = task.id ?: return@forEach
            val total = report.taskDurations[taskId]
            val pseudo = MonthlyEmployeeReport.isPseudoTask(taskId)
            rows.add(
                MonthlyEmployeeReportRow(
                    type = if (pseudo) "pseudoTask" else "task",
                    taskId = if (pseudo) null else taskId,
                    label = if (pseudo) task.title ?: "******"
                    else TaskFormatter.getTaskPath(taskId, true, OutputType.PLAIN) ?: "",
                    perWeek = report.weeks.map { it.taskEntries[taskId]?.formattedDuration ?: "" },
                    sum = total?.formattedDuration ?: "",
                    aiTimeSavings = total?.getFormattedTimeSavedByAI ?: "",
                )
            )
        }

        val kost1 = report.kost1Id?.let { kostCache.getKost1(it) }
        val vacationAvailable = vacationService.hasAccessToVacationService(ThreadLocalUserContext.loggedInUser, false)
        val averageWorkingTimeStats = averageWorkingTimeStats(user, report.year, report.month)
        val fromDate = LocalDate.of(report.year, report.month, 1)
        // Target ("Soll") working hours of the month = weekly hours × working days ÷ 5 (5 working days/week),
        // the same formula the salary export uses (EmployeeSalaryExportDao).
        val employee = employeeCache.getEmployeeByUserId(user.id)
        val weeklyHours = employeeService.getWeeklyWorkingHours(employee, fromDate)
        val numberOfWorkingDays = report.numberOfWorkingDays
        val targetWorkingHours = if (weeklyHours != null && numberOfWorkingDays != null) {
            NumberHelper.formatFraction2(
                weeklyHours.multiply(numberOfWorkingDays).divide(BigDecimal(5), 2, RoundingMode.HALF_UP)
            )
        } else {
            null
        }

        return MonthlyEmployeeReportData(
            userId = user.id,
            userName = user.getFullname(),
            maySelectOtherUsers = accessChecker.hasLoggedInUserAccessToTimesheetsOfOtherUsers(),
            year = report.year,
            month = report.month,
            availableYears = timesheetDao.getYears(user.id).toList().sortedDescending(),
            costConfigured = costConfigured,
            kost1 = if (costConfigured && kost1 != null) OldKostFormatter.format(kost1) else null,
            numberOfWorkingDays = report.numberOfWorkingDays?.toString(),
            targetWorkingHours = targetWorkingHours,
            formattedUnbookedDays = report.formattedUnbookedDays,
            averageWorkingTimeStats = averageWorkingTimeStats,
            timeSavingsByAIEnabled = timeSavingsByAIEnabled,
            hasKost2Rows = report.kost2Rows.isNotEmpty(),
            weeks = weeks,
            rows = rows,
            totalNetDuration = report.formattedTotalNetDuration,
            showGrossRow = report.totalGrossDuration != report.totalNetDuration,
            totalGrossDuration = report.formattedTotalGrossDuration,
            totalTimeSavedByAI = report.formattedTotalTimeSavedByAI,
            timeSavedByAIPercentage = report.formattedTimeSavedByAIPercentage,
            vacationAvailable = vacationAvailable,
            vacationCount = if (vacationAvailable) report.formattedVacationCount else null,
            vacationPlannedCount = if (vacationAvailable) report.formattedVacationPlandCount else null,
            invoicingQuota = if (invoicingQuotaService.isEnabled()) report.formattedInvoicingQuota else null,
            invoicingQuotaTooltip = if (invoicingQuotaService.isEnabled()) report.formattedInvoicingQuotaTooltip else null,
            startDate = fromDate.toString(),
            endDate = fromDate.withDayOfMonth(fromDate.lengthOfMonth()).toString(),
        )
    }

    /**
     * The localized average-working-time line, computed exactly as the legacy page does: from the start of the
     * employment contract (or the begin of the current month as a fallback) up to the reported month.
     */
    private fun averageWorkingTimeStats(user: PFUserDO, year: Int, month: Int): String {
        val employee = employeeCache.getEmployeeByUserId(user.id)
        var startOfWorkContract = PFDay.now().beginOfMonth
        employee?.eintrittsDatum?.let { startOfWorkContract = PFDay.from(it) }
        val currentMonth = PFDay.of(year, month, 1)
        return vacationService.getAverageWorkingTimeStats(user, startOfWorkContract, currentMonth).localizedMessage
    }

    /** Body of [exportPdf]: the same triple as the report request. */
    class ExportRequest(
        val userId: Long? = null,
        val year: Int? = null,
        val month: Int? = null,
    )

    companion object {
        private const val STYLE_SHEET = "fo-styles/monthlyEmployeeReport-template-fo.xsl"
        private const val XML_DATA = "fo-styles/monthlyEmployeeReport2pdf.xml"

        /** User-pref area and names the last selection (user, year, month) is persisted under. */
        private const val PREF_AREA = "monthlyEmployeeReport"
        private const val PREF_USER_ID = "userId"
        private const val PREF_YEAR = "year"
        private const val PREF_MONTH = "month"
    }
}
