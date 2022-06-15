/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import de.micromata.merlin.excel.ExcelWorkbook
import mu.KotlinLogging
import org.projectforge.SystemStatus
import org.projectforge.business.ldap.LdapUserDao
import org.projectforge.business.login.Login
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserLocale
import org.projectforge.business.user.UserTokenType
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.TimeNotation
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.core.getObjectList
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.User
import org.projectforge.ui.*
import org.projectforge.ui.filter.UIFilterListElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/user")
class UserPagesRest
  : AbstractDTOPagesRest<PFUserDO, User, UserDao>(UserDao::class.java, "user.title") {

  @Autowired
  private lateinit var accessChecker: AccessChecker

  @Autowired
  private lateinit var ldapUserDao: LdapUserDao

  @Autowired
  private lateinit var userAuthenticationsService: UserAuthenticationsService

  override fun transformFromDB(obj: PFUserDO, editMode: Boolean): User {
    val user = User()
    val copy = PFUserDO.createCopyWithoutSecretFields(obj)
    if (copy != null) {
      user.copyFrom(copy)
    }
    return user
  }

  override fun transformForDB(dto: User): PFUserDO {
    val userDO = PFUserDO()
    dto.copyTo(userDO)
    return userDO
  }

  override val classicsLinkListUrl: String
    get() = "wa/userList"

  /**
   * LAYOUT List page
   */
  override fun createListLayout(
    request: HttpServletRequest,
    layout: UILayout,
    magicFilter: MagicFilter,
    userAccess: UILayout.UserAccess
  ) {
    val adminAccess = accessChecker.isLoggedInUserMemberOfAdminGroup
    userAccess.update = adminAccess
    val agGrid = agGridSupport.prepareUIGrid4ListPage(
      request,
      layout,
      magicFilter,
      this,
      userAccess = userAccess,
    )
      .add(lc, PFUserDO::username)
    if (adminAccess) {
      agGrid.add(
        UIAgGridColumnDef.createCol(
          PFUserDO::deactivated,
          headerName = "user.deactivated",
          width = 80,
          valueIconMap = mapOf(true to UIIconType.USER_LOCK, false to null)
        )
      )
    }
    agGrid.add("lastLoginTimeAgo", headerName = "login.lastLogin")
      .add(lc, PFUserDO::lastname, PFUserDO::firstname, PFUserDO::personalPhoneIdentifiers, PFUserDO::description)
    if (adminAccess) {
      agGrid.add(
        UIAgGridColumnDef.createCol(
          lc,
          "assignedGroups",
          formatter = UIAgGridColumnDef.Formatter.SHOW_LIST_OF_DISPLAYNAMES,
          wrapText = true
        )
      )
      agGrid.add(UIAgGridColumnDef.createCol(lc, "rightsAsString", lcField = "rights", wrapText = true))
      if (useLdapStuff) {
        agGrid.add(UIAgGridColumnDef.createCol(lc, PFUserDO::ldapValues, wrapText = true))
      }
    }

    if (adminAccess) {
      layout.excelExportSupported = true
    }
  }

  override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
    if (useLdapStuff) {
      elements.add(
        UIFilterListElement("sync", label = translate("user.filter.syncStatus"), defaultFilter = true, multi = false)
          .buildValues(UserPagesSyncFilter.SYNC::class.java)
      )
    }
    elements.add(
      UIFilterListElement("type", label = translate("user.filter.type"), defaultFilter = true, multi = false)
        .buildValues(UserPagesTypeFilter.TYPE::class.java)
    )
    elements.add(
      UIFilterListElement("hrPlanning", label = translate("user.filter.hrPlanning"), multi = false)
        .buildValues(UserPagesHRPlanningFilter.PLANNING_TYPE::class.java)
    )
  }

  override fun preProcessMagicFilter(target: QueryFilter, source: MagicFilter): List<CustomResultFilter<PFUserDO>> {
    val filters = mutableListOf<CustomResultFilter<PFUserDO>>()
    source.entries.find { it.field == "sync" }?.let { filter ->
      filter.synthetic = true
      val values = filter.value.values
      if (!values.isNullOrEmpty() && values.size == 1) {
        val value = values[0]
        try {
          UserPagesSyncFilter.SYNC.valueOf(value).let {
            filters.add(UserPagesSyncFilter(it))
          }
        } catch (ex: IllegalArgumentException) {
          log.warn { "Oups, can't convert '$value': ${ex.message}" }
        }
      }
    }
    source.entries.find { it.field == "type" }?.let { filter ->
      filter.synthetic = true
      val values = filter.value.values
      if (!values.isNullOrEmpty() && values.size == 1) {
        val value = values[0]
        try {
          UserPagesTypeFilter.TYPE.valueOf(value).let {
            filters.add(UserPagesTypeFilter(it))
          }
        } catch (ex: IllegalArgumentException) {
          log.warn { "Oups, can't convert '$value': ${ex.message}" }
        }
      }
    }
    source.entries.find { it.field == "hrPlanning" }?.let { filter ->
      filter.synthetic = true
      val values = filter.value.values
      if (!values.isNullOrEmpty() && values.size == 1) {
        val value = values[0]
        try {
          UserPagesHRPlanningFilter.PLANNING_TYPE.valueOf(value).let {
            filters.add(UserPagesHRPlanningFilter(it))
          }
        } catch (ex: IllegalArgumentException) {
          log.warn { "Oups, can't convert '$value': ${ex.message}" }
        }
      }
    }
    return filters
  }

  @PostMapping("createUid")
  fun createGid(@Valid @RequestBody postData: PostData<User>):
      ResponseAction {
    val data = postData.data
    //data.gidNumber = ldapPosixGroupsUtils.nextFreeGidNumber
    return ResponseAction(targetType = TargetType.UPDATE)
      .addVariable("data", data)
  }

  @PostMapping("createSambaSID")
  fun createSambaSID(@Valid @RequestBody postData: PostData<User>):
      ResponseAction {
    val data = postData.data
    //data.gidNumber = ldapPosixGroupsUtils.nextFreeGidNumber
    return ResponseAction(targetType = TargetType.UPDATE)
      .addVariable("data", data)
  }

  override fun validate(validationErrors: MutableList<ValidationError>, dto: User) {
    super.validate(validationErrors, dto)
    /*dto.gidNumber?.let { gidNumber ->
      if (dto.gidNumber != null && !ldapPosixGroupsUtils.isGivenNumberFree(dto.id ?: -1, gidNumber)) {
        validationErrors.add(
          ValidationError(
            translateMsg("ldap.gidNumber.alreadyInUse", ldapPosixGroupsUtils.nextFreeGidNumber),
            fieldId = "gidNumber",
          )
        )
      }
    }*/
  }

  /**
   * LAYOUT Edit page
   */
  override fun createEditLayout(dto: User, userAccess: UILayout.UserAccess): UILayout {
    val layout = super.createEditLayout(dto, userAccess)
    val userSettings = createUserSettingsCol(UILength(md = 6))
    layout.add(
      UIRow()
        .add(
          UICol(md = 6)
            .add(
              lc,
              PFUserDO::username,
              PFUserDO::firstname,
              PFUserDO::lastname,
              PFUserDO::nickname,
              PFUserDO::gender,
              PFUserDO::organization,
              PFUserDO::email,
              /*"authenticationToken",*/
              PFUserDO::mobilePhone,
              PFUserDO::jiraUsername,
              PFUserDO::hrPlanning,
              PFUserDO::deactivated,
              //PFUserDO::password
            )
        )
        .add(userSettings)
    )
      .add(UICol().add(lc, PFUserDO::sshPublicKey))
      /*.add(UISelect<Int>("readonlyAccessUsers", lc,
              multi = true,
              label = "user.assignedGroups",
              additionalLabel = "access.groups",
              autoCompletion = AutoCompletion<Int>(url = "group/aco"),
              labelProperty = "name",
              valueProperty = "id"))
      .add(UISelect<Int>("readonlyAccessUsers", lc,
              multi = true,
              label = "multitenancy.assignedTenants",
              additionalLabel = "access.groups",
              autoCompletion = AutoCompletion<Int>(url = "group/aco"),
              labelProperty = "name",
              valueProperty = "id"))*/
      .add(lc, "description")

    if (dto.id != null) {
      userSettings.add(
        1, UIButton.createDangerButton(
          layout,
          id = "stayLoggedIn-renew",
          title = "login.stayLoggedIn.invalidateAllStayLoggedInSessions",
          tooltip = "login.stayLoggedIn.invalidateAllStayLoggedInSessions.tooltip",
          confirmMessage = "user.authenticationToken.renew.securityQuestion",
          responseAction = ResponseAction(
            RestResolver.getRestUrl(
              UserServicesRest::class.java,
              "resetStayLoggedInSessions?userId=${dto.id}"
            ), targetType = TargetType.GET
          )
        )
      )
    }

    return LayoutUtils.processEditPage(layout, dto, this)
  }

  override val autoCompleteSearchFields = arrayOf("username", "firstname", "lastname", "email")

  override fun queryAutocompleteObjects(request: HttpServletRequest, filter: BaseSearchFilter): List<PFUserDO> {
    val list = super.queryAutocompleteObjects(request, filter)
    if (filter.searchString.isNullOrBlank() || request.getParameter(AutoCompletion.SHOW_ALL_PARAM) != "true") {
      // Show deactivated users only if search string is given or param SHOW_ALL_PARAM is true:
      return list.filter { !it.deactivated } // Remove deactivated users when returning all.
    }
    return list
  }

  @GetMapping("resetStayLoggedInSessions")
  fun resetStayLoggedInSessions(
    @RequestParam("userId", required = true) userId: Int,
  ): ResponseEntity<*> {
    log.info("Trying to reset all stay-logged-in sessions of user #$userId.")
    accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
    userAuthenticationsService.renewToken(userId, UserTokenType.STAY_LOGGED_IN_KEY)
    return UIToast.createToastResponseEntity(translate("login.stayLoggedIn.invalidateAllStayLoggedInSessions.successfullDeleted"))
  }

  /**
   * Exports favorites addresses.
   */
  @PostMapping(RestPaths.REST_EXCEL_SUB_PATH)
  fun exportAsExcel(@RequestBody filter: MagicFilter): ResponseEntity<*> {
    log.info("Exporting users as Excel file.")
    accessChecker.checkIsLoggedInUserMemberOfAdminGroup()

    @Suppress("UNCHECKED_CAST")
    val list = getObjectList(this, baseDao, filter)
    ExcelWorkbook.createEmptyWorkbook(ThreadLocalUserContext.getLocale()).use { workbook ->
      val sheet = workbook.createOrGetSheet(translate("plugins.skillmatrix.title.list"))
      val boldFont = workbook.createOrGetFont("bold", bold = true)
      val boldStyle = workbook.createOrGetCellStyle("hr", font = boldFont)
      val wrapTextStyle = workbook.createOrGetCellStyle("wrap")
      wrapTextStyle.wrapText = true
      ExcelUtils.registerColumn(sheet, PFUserDO::username, 10)
      ExcelUtils.registerColumn(sheet, PFUserDO::jiraUsername, 10)
      ExcelUtils.registerColumn(sheet, PFUserDO::localUser)
      ExcelUtils.registerColumn(sheet, PFUserDO::deactivated)
      ExcelUtils.registerColumn(sheet, PFUserDO::firstname)
      ExcelUtils.registerColumn(sheet, PFUserDO::nickname)
      ExcelUtils.registerColumn(sheet, PFUserDO::lastname)
      ExcelUtils.registerColumn(sheet, PFUserDO::email)
      sheet.registerColumn(translate("user.assignedGroups"), "assignedGroups").withSize(100)
      ExcelUtils.registerColumn(sheet, PFUserDO::rights)
      ExcelUtils.registerColumn(sheet, PFUserDO::organization)
      ExcelUtils.registerColumn(sheet, PFUserDO::lastLogin)
      ExcelUtils.registerColumn(sheet, PFUserDO::locale)
      ExcelUtils.registerColumn(sheet, PFUserDO::timeZone)
      ExcelUtils.registerColumn(sheet, PFUserDO::lastPasswordChange)
      ExcelUtils.registerColumn(sheet, PFUserDO::lastWlanPasswordChange)
      ExcelUtils.registerColumn(sheet, PFUserDO::description, 50)
      if (useLdapStuff) {
        ExcelUtils.registerColumn(sheet, PFUserDO::ldapValues, 50)
      }
      ExcelUtils.addHeadRow(sheet, boldStyle)
      list.forEach { userDO ->
        val user = User()
        user.copyFrom(userDO)
        val row = sheet.createRow()
        row.autoFillFromObject(user, "assignedGroups", "rights", "timeZone")
        row.getCell("assignedGroups")?.let {
          it.setCellValue(user.assignedGroups?.joinToString { it.displayName ?: "???" })
          it.setCellStyle(wrapTextStyle)
        }
        ExcelUtils.getCell(row, PFUserDO::rights)?.let {
          it.setCellValue(user.rightsAsString)
          it.setCellStyle(wrapTextStyle)
        }
        ExcelUtils.getCell(row, PFUserDO::timeZone)?.setCellValue(user.timeZone?.id)
        ExcelUtils.getCell(row, PFUserDO::description)?.setCellStyle(wrapTextStyle)
        ExcelUtils.getCell(row, PFUserDO::ldapValues)?.setCellStyle(wrapTextStyle)
      }
      sheet.setAutoFilter()
      val filename = ("UserList_${DateHelper.getDateAsFilenameSuffix(Date())}.xlsx")
      val resource = ByteArrayResource(workbook.asByteArrayOutputStream.toByteArray())
      return ResponseEntity.ok()
        .contentType(org.springframework.http.MediaType.parseMediaType("application/octet-stream"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$filename")
        .body(resource)
    }
  }

  companion object {
    internal fun createUserSettingsCol(uiLength: UILength): UICol {
      val userLC = LayoutContext(PFUserDO::class.java)

      val locales = UserLocale.LOCALIZATIONS.map { UISelectValue(Locale(it), translate("locale.$it")) }.toMutableList()
      locales.add(0, UISelectValue(Locale("DEFAULT"), translate("user.defaultLocale")))

      val today = LocalDate.now()
      val formats = Configuration.instance.dateFormats
      val dateFormats = formats.map { createUISelectValue(it, today) }.toMutableList()
      val excelDateFormats = formats.map { createUISelectValue(it, today, true) }.toMutableList()

      val timeNotations = listOf(
        UISelectValue(TimeNotation.H12, translate("timeNotation.12")),
        UISelectValue(TimeNotation.H24, translate("timeNotation.24"))
      )

      return UICol(uiLength).add(UIReadOnlyField("lastLoginFormatted", label = "login.lastLogin"))
        .add(userLC, "timeZone", "personalPhoneIdentifiers")
        .add(UISelect("locale", userLC, required = true, values = locales))
        .add(UISelect("dateFormat", userLC, required = false, values = dateFormats))
        .add(UISelect("excelDateFormat", userLC, required = false, values = excelDateFormats))
        .add(UISelect("timeNotation", userLC, required = false, values = timeNotations))
    }

    private fun createUISelectValue(
      pattern: String,
      today: LocalDate,
      excelDateFormat: Boolean = false
    ): UISelectValue<String> {
      val str = if (excelDateFormat) {
        pattern.replace('y', 'Y').replace('d', 'D')
      } else {
        pattern
      }
      return UISelectValue(str, "$str: ${java.time.format.DateTimeFormatter.ofPattern(pattern).format(today)}")
    }
  }

  private val useLdapStuff: Boolean
    get() = SystemStatus.isDevelopmentMode() || (accessChecker.isLoggedInUserMemberOfAdminGroup && Login.getInstance()
      .hasExternalUsermanagementSystem() && ldapUserDao.isPosixAccountsConfigured)
}
