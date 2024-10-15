/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest

import mu.KotlinLogging
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.repository.LeaveAccountEntryDao
import org.projectforge.business.vacation.repository.RemainingLeaveDao
import org.projectforge.business.vacation.service.ConflictingVacationsCache
import org.projectforge.business.vacation.service.VacationService
import org.projectforge.business.vacation.service.VacationStatsFormatted
import org.projectforge.common.DateFormatType
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTimeUtils
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.LeaveAccountEntry
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.Vacation
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.time.Month
import java.time.Year
import javax.validation.Valid

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/vacationAccount")
class VacationAccountPageRest {
  class Data(
    var employee: EmployeeDO? = null,
    var statistics: MutableMap<String, Any>? = null,
    var vacations: MutableMap<String, Any>? = null,
    /**
     * booked hours (time sheets) of last 3 months divided by working days as localized text message.
     */
    var workingHoursStatistics: String? = null,
  )

  @Autowired
  private lateinit var conflictingVacationsCache: ConflictingVacationsCache

  @Autowired
  private lateinit var employeeDao: EmployeeDao

  @Autowired
  private lateinit var leaveAccountEntryDao: LeaveAccountEntryDao

  @Autowired
  private lateinit var userPrefService: UserPrefService

  @Autowired
  private lateinit var vacationService: VacationService

  @Autowired
  private lateinit var remainingLeaveDao: RemainingLeaveDao

  @GetMapping("dynamic")
  fun getForm(@RequestParam("id") searchEmployeeId: Long? = null): FormLayoutData {
    val layout = UILayout("vacation.leaveaccount.title")
    val lc = LayoutContext(VacationDO::class.java)
    layout.addTranslations(
      "employee",
      "fibu.common.workingDays",
      "vacation.leaveaccount.title",
      "vacation.annualleave",
      "vacation.previousyearleave",
      "vacation.subtotal",
      "menu.vacation.leaveAccountEntry",
      "vacation.vacationApproved",
      "vacation.vacationInProgress",
      "vacation.availablevacation",
      "vacation.specialApproved",
      "vacation.specialInProgress",
      "vacation.title.list",
      "vacation.startdate",
      "vacation.enddate",
      "vacation.Days",
      "vacation.replacement",
      "vacation.replacement.others",
      "vacation.manager",
      "vacation.status",
      "vacation.vacationmode",
      "vacation.special",
      "comment",
      "vacation.leaveAccountEntry.title.heading",
      "date",
      "vacation.leaveAccountEntry.amount",
      "description"
    )
    val endOfYear = vacationService.getEndOfCarryVacationOfPreviousYear(Year.now().value)
    val endOfYearString = PFDateTimeUtils.ensureUsersDateTimeFormat(DateFormatType.DATE_WITHOUT_YEAR).format(endOfYear)
    layout.addTranslation(
      "vacation.previousyearleaveunused",
      translateMsg("vacation.previousyearleaveunused", endOfYearString)
    )

    val isHRMember = vacationService.hasLoggedInUserHRVacationAccess()

    val employeeId: Long? = if (isHRMember) {
      // If, and only if the current logged-in user is a member of HR staff, other employees may be chosen:
      // 1st any given user by request param is used,
      // 2nd the last chosen user from the user's preferences or, if none given:
      // 3rd the current loggedin user himself.
      searchEmployeeId ?: getUserPref().employeeId ?: ThreadLocalUserContext.userContext!!.employeeId
    } else {
      // For non HR users, only the user himself is assumed.
      ThreadLocalUserContext.userContext!!.employeeId;
    }
    val employee = employeeDao.find(employeeId, checkAccess = false)!!
    val statistics = mutableMapOf<String, Any>()
    val currentStats = vacationService.getVacationStats(employee, Year.now().value)
    val prevStats = vacationService.getVacationStats(employee, Year.now().value - 1)
    val nextStats = vacationService.getVacationStats(employee, Year.now().value + 1)
    val vacations = mutableMapOf<String, Any>()
    if (employeeId != null) {
      statistics["statisticsCurrentYear"] = VacationStatsFormatted(currentStats)
      statistics["statisticsPreviousYear"] = VacationStatsFormatted(prevStats)
      readVacations(vacations, "Current", employeeId, currentStats.year)
      readVacations(vacations, "Previous", employeeId, prevStats.year)
      readVacations(vacations, "Next", employeeId, nextStats.year)
      val periodBegin = PFDay.of(prevStats.year, Month.JANUARY, 1).localDate
      val periodEnd = PFDay.of(currentStats.year, Month.JANUARY, 1).endOfYear.localDate
      leaveAccountEntryDao.getList(employeeId, periodBegin, periodEnd)?.let { list ->
        vacations["leaveAccountEntries"] = list.map { LeaveAccountEntry(it) }.sortedByDescending { it.date }
      }
    }
    val buttonCol = UICol(6)
    val responseAction = ResponseAction(
      PagesResolver.getEditPageUrl(
        VacationPagesRest::class.java,
        params = mapOf("employee" to employeeId),
        absolute = true
      ), targetType = TargetType.REDIRECT
    )
    responseAction.addVariable("returnToCaller", "account")
    // TODO: Add employee for preselecting edit form
    buttonCol.add(UIButton.createAddButton(responseAction = responseAction))
    if (this.vacationService.hasLoggedInUserHRVacationAccess() &&
      !NumberHelper.isEqual(currentStats.remainingLeaveFromPreviousYear, prevStats.vacationDaysLeftInYear)
    ) {
      buttonCol.add(
        UIButton.createDangerButton(
          id = "recalculate", title = "vacation.recalculateRemainingLeave",
          responseAction = ResponseAction("vacationAccount/recalculate", targetType = TargetType.POST)
        )
      )
    }

    val statisticRow = UIRow()

    val workingStatistics = UIReadOnlyField("workingHoursStatistics", label = "fibu.common.workingDays")
    if (isHRMember) {
      statisticRow
        .add(
          UICol(UILength(md = 4, sm = 12))
            .add(lc, "employee")
        )
        .add(
          UICol(UILength(md = 8, sm = 12))
            .add(UICustomized("vacation.statistics"))
            .add(workingStatistics)
        )
    } else {
      statisticRow.add(
        UICol(UILength(md = 12, sm = 12))
          .add(UICustomized("vacation.statistics"))
          .add(workingStatistics)
      )
    }


    layout.add(
      UIFieldset(12)
        .add(statisticRow)
        .add(UIRow().add(buttonCol))
    ).add(
      UIFieldset(12)
        .add(
          UIRow()
            .add(
              UICol(12)
                .add(UICustomized("vacation.entries"))
            )
        )
    )
    layout.add(
      MenuItem(
        "export",
        i18nKey = "vacation.export.title",
        url = PagesResolver.getDynamicPageUrl(VacationExportPageRest::class.java),
        type = MenuItemTargetType.REDIRECT,
      )
    )
    layout.add(
      MenuItem(
        "leaveForApplication",
        i18nKey = "menu.vacation",
        url = PagesResolver.getListPageUrl(VacationPagesRest::class.java),
        type = MenuItemTargetType.REDIRECT,
      )
    )
    layout.add(UIAlert("vacation.subscription.info", title = "vacation.subscription"))

    layout.watchFields.add("employee")
    LayoutUtils.process(layout)

    val data = Data(
      employee = employee,
      statistics = statistics,
      vacations = vacations,
      workingHoursStatistics = vacationService.getAverageWorkingTimeStats(
        employee.user ?: ThreadLocalUserContext.loggedInUser!!, PFDay.fromOrNull(employee.eintrittsDatum)
      ).localizedMessage
    )

    return FormLayoutData(data, layout, null)
  }

  @PostMapping(RestPaths.WATCH_FIELDS)
  fun watchFields(@Valid @RequestBody postData: PostData<Data>): ResponseAction {
    val employeeId = postData.data.employee?.id
    if (postData.watchFieldsTriggered?.contains("employee") == false || employeeId == null) {
      return ResponseAction(targetType = TargetType.NOTHING)
    }
    getUserPref().employeeId = employeeId
    return buildResponseAction(employeeId)
  }

  @PostMapping("recalculate")
  fun recalculateRemainingLeave(@Valid @RequestBody postData: PostData<Data>): ResponseAction {
    if (!this.vacationService.hasLoggedInUserHRVacationAccess() || postData.data.employee == null) {
      log.warn { "User has now HR vacation access. Recalculating of remaining leaves ignored." }
      return ResponseAction(targetType = TargetType.NOTHING)
    }
    val employeeId = postData.data.employee!!.id!!

    remainingLeaveDao.markAsDeleted(employeeId, Year.now().value, checkAccess = false)

    return buildResponseAction(employeeId)
  }

  private fun buildResponseAction(employeeId: Long): ResponseAction {
    val layoutData = getForm(employeeId)

    return ResponseAction(
      url = PagesResolver.getDynamicPageUrl(this::class.java, id = employeeId, absolute = true),
      targetType = TargetType.UPDATE
    )
      .addVariable("data", layoutData.data)
      .addVariable("ui", layoutData.ui)
  }

  private fun readVacations(variables: MutableMap<String, Any>, id: String, employeeId: Long, year: Int) {
    val yearDate = PFDay.of(year, Month.JANUARY, 1)
    val dbList = vacationService.getVacationsListForPeriod(
      employeeId,
      yearDate.localDate,
      yearDate.endOfYear.localDate,
      true,
      true
    )
    val list = dbList.map { Vacation(it) }
    list.forEach {
      if (conflictingVacationsCache.hasConflict(it.id)) {
        it.conflict = true
      }
    }
    variables["vacations${id}Year"] = list
    variables["year$id"] = year
  }

  class VacationAccountUserPref(var employeeId: Long? = null)

  private fun getUserPref(): VacationAccountUserPref {
    return userPrefService.ensureEntry("vacation", "account", VacationAccountUserPref())
  }
}
