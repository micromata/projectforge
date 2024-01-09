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
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.service.VacationExcelExporter
import org.projectforge.business.vacation.service.VacationService
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDay
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.*
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/vacationExport")
class VacationExportPageRest : AbstractDynamicPageRest() {
  class Data {
    var startDate: LocalDate? = null
    var groups: List<Group>? = null
    var employees: List<Employee>? = null

    fun copyFrom(other: Data) {
      startDate = other.startDate
      groups = other.groups
      employees = other.employees
    }
  }

  @Autowired
  private lateinit var employeeDao: EmployeeDao

  @Autowired
  private lateinit var groupService: GroupService

  @Autowired
  private lateinit var userGroupCache: UserGroupCache

  @Autowired
  private lateinit var userPrefService: UserPrefService

  @Autowired
  private lateinit var vacationService: VacationService

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest): FormLayoutData {
    val layout = UILayout("vacation.export.title")
    layout.add(
      UICol(sm = 6, md = 3, lg = 2).add(
        UIInput(
          Data::startDate.name,
          label = translate("vacation.startdate"),
          dataType = UIDataType.DATE
        )
      )
    )
      .add(

        UISelect<Int>(
          Data::employees.name,
          multi = true,
          label = translate("fibu.employees"),
          autoCompletion = AutoCompletion.getAutoCompletion4Employees(true),
        )
      )
      .add(
        UISelect<Int>(
          Data::groups.name, multi = true,
          label = translate("group.groups"),
          autoCompletion = AutoCompletion.getAutoCompletion4Groups(),
        )
      )
    layout.addAction(
      UIButton.createDownloadButton(
        id = "excelExport",
        title = "exportAsXls",
        responseAction = ResponseAction(
          RestResolver.getRestUrl(
            this.javaClass,
            "exportExcel"
          ), targetType = TargetType.DOWNLOAD
        ),
        default = true
      )
    )
    layout.watchFields.addAll(
      arrayOf(
        Data::startDate.name,
        Data::startDate.name,
        Data::groups.name,
        Data::employees.name
      )
    )
    val data = ensureUserPref()
    data.startDate = PFDay.now().beginOfMonth.date
    setSessionData(request, data)
    return FormLayoutData(data, layout, null)
  }

  /**
   * Will be called, if the user wants to change his/her observeStatus.
   */
  @PostMapping(RestPaths.WATCH_FIELDS)
  @Override
  fun watchFields(request: HttpServletRequest, @Valid @RequestBody postData: PostData<Data>) {
    setSessionData(request, postData.data)
    val data = ensureUserPref()
    data.copyFrom(postData.data)
    checkData(data)
  }

  @GetMapping("exportExcel")
  fun exportExcel(request: HttpServletRequest): ResponseEntity<*> {
    val data = getSessionData(request)
    log.info { "Exporting Excel sheet with vacations of groups=[${data?.groups?.joinToString { it.displayName ?: "???" }}] and users=[${data?.employees?.joinToString { it.displayName ?: "???" }}]" }
    val employees = mutableSetOf<EmployeeDO>()
    data?.employees?.forEach { employee ->
      if (employees.none { it.id == employee.id }) {
        val employeeDO = employeeDao.internalGetById(employee.id)
        employees.add(employeeDO)
      }
    }
    data?.groups?.forEach { group ->
      userGroupCache.getGroup(group.id)?.assignedUsers?.forEach { user ->
        employeeDao.findByUserId(user.id)?.let { employeeDO ->
          employees.add(employeeDO)
        }
      }
    }
    employees.removeIf { !it.active || it.user?.deactivated == true || it.user?.isDeleted == true }
    var vacations = emptyList<VacationService.VacationsByEmployee>()
    val startDate = data?.startDate ?: LocalDate.now()
    val periodBegin = PFDay.from(startDate).beginOfYear
    val periodEnd = periodBegin.plusYears(1).endOfYear
    if (employees.isNotEmpty()) {
      vacations = vacationService.getVacationOfEmployees(
        employees,
        periodBegin.date,
        periodEnd.date,
        withSpecial = true,
        trimVacations = false,
        VacationStatus.APPROVED,
        VacationStatus.IN_PROGRESS,
      )
    }
    val excel = VacationExcelExporter.export(startDate, vacations)
    return RestUtils.downloadFile("${translate("vacation")}-${PFDateTime.now().format4Filenames()}.xlsx", excel)
  }

  private fun ensureUserPref(): Data {
    val data = userPrefService.ensureEntry("vacation", "export", Data())
    checkData(data)
    return data
  }

  private fun checkData(data: Data) {
    if (data.startDate == null) {
      data.startDate = PFDay.now().beginOfMonth.date
    }
    Employee.restoreDisplayNames(data.employees)
    Group.restoreDisplayNames(data.groups, groupService)
  }

  private fun setSessionData(request: HttpServletRequest, data: Data) {
    request.session.setAttribute(SESSION_ATTRIBUTE_DATA, data)
  }

  private fun getSessionData(request: HttpServletRequest): Data? {
    return request.session.getAttribute(SESSION_ATTRIBUTE_DATA) as? Data
  }

  companion object {
    private const val SESSION_ATTRIBUTE_DATA = "ValidateExportPageRest:data"
  }
}
