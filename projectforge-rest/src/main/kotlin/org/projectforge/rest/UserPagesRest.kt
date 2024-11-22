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

import de.micromata.merlin.excel.ExcelWorkbook
import mu.KotlinLogging
import org.projectforge.SystemStatus
import org.projectforge.business.ldap.LdapPosixAccountsUtils
import org.projectforge.business.ldap.LdapSambaAccountsUtils
import org.projectforge.business.ldap.LdapService
import org.projectforge.business.ldap.LdapUserDao
import org.projectforge.business.login.Login
import org.projectforge.business.password.PasswordQualityService
import org.projectforge.business.user.*
import org.projectforge.business.user.service.UserService
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.i18n.TimeAgo
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.TimeNotation
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.core.getObjectList
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.User
import org.projectforge.rest.dto.UserRightDto
import org.projectforge.ui.*
import org.projectforge.ui.filter.UIFilterListElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import kotlin.reflect.KProperty

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/user")
class UserPagesRest
  : AbstractDTOPagesRest<PFUserDO, User, UserDao>(
  UserDao::class.java, "user.title",
  cloneSupport = CloneSupport.CLONE,
) {

  @Autowired
  private lateinit var accessChecker: AccessChecker

  @Autowired
  private lateinit var groupDao: GroupDao

  @Autowired
  private lateinit var ldapPosixAccountsUtils: LdapPosixAccountsUtils

  @Autowired
  private lateinit var ldapSambaAccountsUtils: LdapSambaAccountsUtils


  @Autowired
  private lateinit var ldapService: LdapService

  @Autowired
  private lateinit var ldapUserDao: LdapUserDao

  @Autowired
  private lateinit var passwordQualityService: PasswordQualityService

  @Autowired
  private lateinit var userAuthenticationsService: UserAuthenticationsService

  @Autowired
  private lateinit var userGroupCache: UserGroupCache

  @Autowired
  private lateinit var userRightDao: UserRightDao

  @Autowired
  private lateinit var userRightsHandler: UserRightsHandler


  @Autowired
  private lateinit var userService: UserService

  override fun transformFromDB(obj: PFUserDO, editMode: Boolean): User {
    val user = User()
    user.copyFrom(obj)
    if (editMode) {
      updateTokenCreationDates(user)
    }
    return user
  }

  private fun updateTokenCreationDates(user: User) {
    val userId = user.id ?: return
    if (!accessChecker.isLoggedInUserMemberOfAdminGroup) {
      return
    }
    userAuthenticationsService.getTokenData(userId, UserTokenType.STAY_LOGGED_IN_KEY)?.creationDate?.let {
      user.stayLoggedInTokenCreationDate = it
      user.stayLoggedInTokenCreationTimeAgo = TimeAgo.getMessage(it)
    }
    userAuthenticationsService.getTokenData(userId, UserTokenType.CALENDAR_REST)?.creationDate?.let {
      user.calendarExportTokenCreationDate = it
      user.calendarExportTokenCreationTimeAgo = TimeAgo.getMessage(it)
    }
    userAuthenticationsService.getTokenData(userId, UserTokenType.DAV_TOKEN)?.creationDate?.let {
      user.davTokenCreationDate = it
      user.davTokenCreationTimeAgo = TimeAgo.getMessage(it)
    }
    userAuthenticationsService.getTokenData(userId, UserTokenType.REST_CLIENT)?.creationDate?.let {
      user.restClientTokenCreationDate = it
      user.restClientTokenCreationTimeAgo = TimeAgo.getMessage(it)
    }
  }

  override fun transformForDB(dto: User): PFUserDO {
    val userDO = PFUserDO()
    dto.copyTo(userDO)
    return userDO
  }

  override fun prepareClone(dto: User): User {
    dto.ldapValues?.let {
      it.uidNumber = null
      it.sambaNTPassword= null
      it.sambaPrimaryGroupSIDNumber =null
      it.sambaSIDNumber = null
      it.homeDirectory = null
    }
    return super.prepareClone(dto)
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
      agGrid.add(lc, PFUserDO::mobilePhone)
      agGrid.add(
        UIAgGridColumnDef.createCol(
          lc,
          "assignedGroups",
          formatter = UIAgGridColumnDef.Formatter.SHOW_LIST_OF_DISPLAYNAMES,
          wrapText = true
        )
      )
      agGrid.add(UIAgGridColumnDef.createCol(lc, "rightsAsString", lcField = "rights", wrapText = true))
      if (posixConfigured) {
        agGrid.add(
          UIAgGridColumnDef.createCol(
            lc,
            "ldapValues.asString",
            lcField = PFUserDO::ldapValues.name,
            wrapText = true
          )
        )
      }
    }

    if (adminAccess) {
      layout.excelExportSupported = true
    } else {
      // Non-admins can't see details of users, so show the ssh public key in the list:
      agGrid.add(UIAgGridColumnDef.createCol(lc, PFUserDO::sshPublicKey, wrapText = true))
    }
  }

  override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
    if (accessChecker.isLoggedInUserMemberOfAdminGroup) {
      if (externalUserManagementSystem) {
        elements.add(
          UIFilterListElement("sync", label = translate("user.filter.syncStatus"), defaultFilter = true, multi = false)
            .buildValues(UserPagesSyncFilter.Sync::class.java)
        )
      }
      elements.add(
        UIFilterListElement("type", label = translate("user.filter.type"), defaultFilter = true, multi = false)
          .buildValues(UserPagesTypeFilter.TYPE::class.java)
      )
      elements.add(
        UIFilterListElement("status", label = translate("user.filter.status"), defaultFilter = true, multi = false)
          .buildValues(UserPagesStatusFilter.STATUS::class.java)
      )
      elements.add(
        UIFilterListElement("hrPlanning", label = translate("user.filter.hrPlanning"), multi = false)
          .buildValues(UserPagesHRPlanningFilter.PLANNING_TYPE::class.java)
      )
    }
  }

  override fun preProcessMagicFilter(target: QueryFilter, source: MagicFilter): List<CustomResultFilter<PFUserDO>> {
    val filters = mutableListOf<CustomResultFilter<PFUserDO>>()
    source.entries.find { it.field == "sync" }?.let { filter ->
      filter.synthetic = true
      val values = filter.value.values
      if (!values.isNullOrEmpty() && values.size == 1) {
        val value = values[0]
        try {
          UserPagesSyncFilter.Sync.valueOf(value).let {
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
    source.entries.find { it.field == "status" }?.let { filter ->
      filter.synthetic = true
      val values = filter.value.values
      if (!values.isNullOrEmpty() && values.size == 1) {
        val value = values[0]
        try {
          UserPagesStatusFilter.STATUS.valueOf(value).let {
            filters.add(UserPagesStatusFilter(it))
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
  fun createUid(@Valid @RequestBody postData: PostData<User>):
      ResponseAction {
    val data = postData.data
    val dtoValues = data.ensureLdapValues()
    val ldapValues = dtoValues.convert()
    ldapPosixAccountsUtils.setDefaultValues(ldapValues, data.username)
    dtoValues.copyFrom(ldapValues)
    return ResponseAction(targetType = TargetType.UPDATE)
      .addVariable("data", data)
  }

  @PostMapping("createSambaSID")
  fun createSambaSID(@Valid @RequestBody postData: PostData<User>):
      ResponseAction {
    val data = postData.data
    val dtoValues = data.ensureLdapValues()
    val ldapValues = dtoValues.convert()
    ldapSambaAccountsUtils.setDefaultValues(ldapValues, data.id)
    dtoValues.copyFrom(ldapValues)
    return ResponseAction(targetType = TargetType.UPDATE)
      .addVariable("data", data)
  }

  override fun validate(validationErrors: MutableList<ValidationError>, dto: User) {
    super.validate(validationErrors, dto)
    val user = PFUserDO()
    dto.copyTo(user)
    if (userService.doesUsernameAlreadyExist(user)) {
      validationErrors.add(
        ValidationError(
          translate("user.error.usernameAlreadyExists"),
          fieldId = PFUserDO::username.name,
        )
      )
    }
    validatePosixValues(validationErrors, dto)
    validateSambaValues(validationErrors, dto)
    validatePassword(User::password, dto.password, validationErrors)
    validatePassword(User::wlanPassword, dto.wlanPassword, validationErrors)
  }

  private fun validatePassword(field: KProperty<*>, password: String?, validationErrors: MutableList<ValidationError>) {
    if (password.isNullOrBlank()) {
      return
    }
    passwordQualityService.checkPasswordQuality(password.toCharArray())?.forEach { errorMsgKey ->
      validationErrors.add(
        ValidationError(
          translateMsg(errorMsgKey.key, errorMsgKey.params),
          fieldId = field.name,
        )
      )
    }
  }

  private fun validatePosixValues(validationErrors: MutableList<ValidationError>, dto: User) {
    val ldapValues = dto.ldapValues
    if (!posixConfigured || ldapValues == null || ldapValues.isPosixValuesEmpty) {
      return
    }
    val uidNumber = ldapValues.uidNumber
    if (uidNumber == null) {
      validationErrors.add(ValidationError.createFieldRequired("ldapValues.uidNumber", translate("ldap.uidNumber")))
    } else if (!ldapPosixAccountsUtils.isGivenNumberFree(dto.id ?: -1L, uidNumber)) {
      validationErrors.add(
        ValidationError(
          translateMsg("ldap.uidNumber.alreadyInUse", ldapPosixAccountsUtils.nextFreeUidNumber),
          fieldId = "ldapValues.uidNumber",
        )
      )
    }
    val gidNumber = ldapValues.gidNumber
    if (gidNumber == null) {
      validationErrors.add(ValidationError.createFieldRequired("ldapValues.gidNumber", translate("ldap.gidNumber")))
    }
    if (ldapValues.homeDirectory.isNullOrBlank()) {
      validationErrors.add(
        ValidationError.createFieldRequired(
          "ldapValues.homeDirectory",
          translate("ldap.homeDirectory")
        )
      )
    }
    if (ldapValues.loginShell.isNullOrBlank()) {
      validationErrors.add(ValidationError.createFieldRequired("ldapValues.loginShell", translate("ldap.loginShell")))
    }
  }

  private fun validateSambaValues(validationErrors: MutableList<ValidationError>, dto: User) {
    val ldapValues = dto.ldapValues
    if (!sambaConfigured || ldapValues == null || ldapValues.isSambaValuesEmpty) {
      return
    }
    val sambaSID = ldapValues.sambaSIDNumber
    if (sambaSID == null) {
      validationErrors.add(ValidationError.createFieldRequired("ldapValues.sambaSID", translate("ldap.sambaSID")))
    } else if (!ldapSambaAccountsUtils.isGivenNumberFree(dto.id ?: -1, sambaSID)) {
      validationErrors.add(
        ValidationError(
          translateMsg("ldap.sambaSID.alreadyInUse", ldapPosixAccountsUtils.nextFreeUidNumber),
          fieldId = "ldapValues.sambaSID",
        )
      )
    }
    val sambaGroupSID = ldapValues.sambaPrimaryGroupSIDNumber
    if (sambaGroupSID == null) {
      validationErrors.add(
        ValidationError.createFieldRequired(
          "ldapValues.sambaPrimaryGroupSIDNumber",
          translate("ldap.sambaPrimaryGroupSID")
        )
      )
    }

  }

  /**
   * LAYOUT Edit page
   */
  override fun createEditLayout(dto: User, userAccess: UILayout.UserAccess): UILayout {
    val layout = super.createEditLayout(dto, userAccess)
    val userDO = PFUserDO()
    dto.copyTo(userDO)
    val userSettings = createUserSettingsCol(UILength(md = 6), userDO)
    val leftCol = UICol(md = 6)
      .add(
        lc,
        PFUserDO::username,
        PFUserDO::firstname,
        PFUserDO::lastname,
        PFUserDO::nickname,
        PFUserDO::gender,
        PFUserDO::organization,
        PFUserDO::email,
        PFUserDO::mobilePhone,
        PFUserDO::jiraUsername,
        PFUserDO::hrPlanning,
        PFUserDO::deactivated,
        PFUserDO::restrictedUser,
      )
    if (dto.id == null) {
      // Show optional passwords for new users.
      leftCol.add(
        UIInput(
          "password",
          label = "password",
          tooltip = "user.add.optionalPassword",
          dataType = UIDataType.PASSWORD,
        )
      )
      if (dto.id == null && Login.getInstance().isWlanPasswordChangeSupported(PFUserDO())) {
        leftCol.add(
          UIInput(
            "wlanPassword",
            label = "ldap.wlanSambaPassword",
            tooltip = "user.add.optionalPassword",
            dataType = UIDataType.PASSWORD,
          )
        )
      }
    }
    layout.add(
      UIRow()
        .add(leftCol)
        .add(userSettings)
    )
      .add(lc, PFUserDO::sshPublicKey, PFUserDO::gpgPublicKey)

    dto.id?.let { userId ->
      addToken(
        layout,
        leftCol,
        User::calendarExportTokenCreationTimeAgo,
        "calendar_rest",
        userId,
        UserTokenType.CALENDAR_REST,
      )
      addToken(layout, leftCol, User::davTokenCreationTimeAgo, "dav_token", userId, UserTokenType.DAV_TOKEN)
      addToken(
        layout,
        leftCol,
        User::restClientTokenCreationTimeAgo,
        "rest_client",
        userId,
        UserTokenType.REST_CLIENT,
      )
    }
    layout.add(
      UISelect.createGroupSelect(lc, "assignedGroups", multi = true, label = "user.assignedGroups")
    )
    layout.add(lc, "description")

    val userId = dto.id
    if (userId != null) {
      addToken(
        layout,
        userSettings,
        User::stayLoggedInTokenCreationTimeAgo,
        "stay_logged_in_key",
        userId,
        UserTokenType.STAY_LOGGED_IN_KEY,
        1,
      )
    }
    layout.add(
      MenuItem(
        "changePassword",
        i18nKey = "menu.changePassword",
        url = PagesResolver.getDynamicPageUrl(ChangePasswordPageRest::class.java, mapOf("userId" to dto.id)),
        type = MenuItemTargetType.MODAL
      )
    )
    if (Login.getInstance().isWlanPasswordChangeSupported(userDO)) {
      layout.add(
        MenuItem(
          "changeWlanPassword",
          i18nKey = "menu.changeWlanPassword",
          url = PagesResolver.getDynamicPageUrl(ChangeWlanPasswordPageRest::class.java, mapOf("userId" to dto.id)),
          type = MenuItemTargetType.MODAL
        )
      )
    }
    layout.watchFields.addAll(
      arrayOf(
        "assignedGroups",
      )
    )
    if (sambaConfigured || posixConfigured) {
      addLdap(layout, dto, lc)
    }
    addRights(layout, dto)
    return LayoutUtils.processEditPage(layout, dto, this)
  }

  override fun onWatchFieldsUpdate(
    request: HttpServletRequest,
    dto: User,
    watchFieldsTriggered: Array<String>?
  ): ResponseEntity<ResponseAction> {
    val userAccess = UILayout.UserAccess()
    val userDO = PFUserDO()
    dto.copyTo(userDO)
    checkUserAccess(userDO, userAccess)
    val layout = createEditLayout(dto, userAccess)
    return ResponseEntity.ok(
      ResponseAction(targetType = TargetType.UPDATE)
        .addVariable("ui", createEditLayout(dto, userAccess))
        .addVariable("layout", layout)
        .addVariable(
          "data",
          dto
        ) // must be added after createEditLayout is called, because createEditLayout may modify available user's rights.
    )
  }

  override fun onAfterSaveOrUpdate(request: HttpServletRequest, obj: PFUserDO, postData: PostData<User>) {
    val start = System.currentTimeMillis()
    val newAssignedGroups = postData.data.assignedGroups?.map { it.id } ?: emptyList()
    val dbAssignedGroups = userGroupCache.getUserGroups(obj) ?: emptyList()
    val groupsToAssign = newAssignedGroups.subtract(dbAssignedGroups)
    val groupsToUnassign = dbAssignedGroups.subtract(newAssignedGroups)
    groupDao.assignGroupByIds(obj, groupsToAssign, groupsToUnassign, false)
    log.info("Assigning groups took: ${(System.currentTimeMillis() - start) / 1000}s.")
    val startRights = System.currentTimeMillis()

    val user = postData.data
    val userRightVOS = userRightsHandler.getUserRightVOs(user)
    userRightDao.updateUserRights(obj, userRightVOS, false)
    log.info("Updating rights took: ${(System.currentTimeMillis() - startRights) / 1000}s.")
    //Only one time reload user group cache
    userGroupCache.forceReload()
    log.info("onAfterSaveOrUpdate: ${(System.currentTimeMillis() - start) / 1000}s.")
  }

  override fun onAfterSave(obj: PFUserDO, postData: PostData<User>): ResponseAction {
    val dto = postData.data
    val password = dto.password
    val wlanPassword = dto.wlanPassword
    if (!password.isNullOrBlank()) {
      log.info { "Admin user wants to create password of new user '${obj.userDisplayName}' with id ${obj.id}." }
      userService.changePasswordByAdmin(obj.id, password.toCharArray())
    }
    if (!wlanPassword.isNullOrBlank() && Login.getInstance().isWlanPasswordChangeSupported(obj)) {
      log.info { "Admin user wants to create WLAN/Samba password of new user '${obj.userDisplayName}' with id ${obj.id}." }
      userService.changeWlanPasswordByAdmin(obj.id, wlanPassword.toCharArray())
    }
    return super.onAfterSave(obj, postData)
  }

  private fun addLdap(layout: UILayout, dto: User, lc: LayoutContext) {
    val ldapSambaAccountsConfig = ldapService.ldapConfig.sambaAccountsConfig
    val ldapValues = dto.ensureLdapValues()
    val leftCol = UICol(md = 6)
    val rightCol = UICol(md = 6)
    layout.add(
      UIFieldset(title = "ldap")
        .add(UICheckbox(PFUserDO::localUser.name, lc))
        .add(
          UIRow()
            .add(leftCol)
            .add(rightCol)
        )
    )
    if (posixConfigured) {
      val uidInput = UIInput(
        "ldapValues.uidNumber",
        label = "ldap.uidNumber",
        additionalLabel = "ldap.posixAccount",
        tooltip = "ldap.uidNumber.tooltip",
      )

      if (ldapValues.uidNumber != null) {
        leftCol.add(uidInput)
      } else {
        val button = UIButton.createLinkButton(
          id = "createUidNumber",
          title = "create",
          tooltip = "ldap.uidNumber.createDefault.tooltip",
          responseAction = ResponseAction(
            RestResolver.getRestUrl(
              UserPagesRest::class.java,
              "createUid"
            ), targetType = TargetType.POST
          )
        )
        leftCol.add(UIRow().add(UICol().add(uidInput)).add(UICol().add(button)))
      }
      leftCol.add(
        UIInput(
          "ldapValues.gidNumber",
          label = "ldap.gidNumber",
          tooltip = "ldap.user.gidNumber.tooltip",
          additionalLabel = "ldap.posixAccount",
        )
      )
    }
    if (sambaConfigured) {
      val sambaSIDInput = UIInput(
        "ldapValues.sambaSIDNumber",
        label = "ldap.sambaSID",
        additionalLabel = "'${translate("ldap.sambaAccount")}: ${ldapSambaAccountsConfig.sambaSIDPrefix}-",
        tooltip = "ldap.sambaSID.tooltip",
      )

      if (ldapValues.sambaSIDNumber != null) {
        leftCol.add(sambaSIDInput)
      } else {
        val button = UIButton.createLinkButton(
          id = "createSambaSID",
          title = "create",
          tooltip = "ldap.uidNumber.createDefault.tooltip",
          responseAction = ResponseAction(
            RestResolver.getRestUrl(
              UserPagesRest::class.java,
              "createSambaSID"
            ), targetType = TargetType.POST
          )
        )
        leftCol.add(UIRow().add(UICol().add(sambaSIDInput)).add(UICol().add(button)))
      }
      leftCol.add(
        UIInput(
          "ldapValues.sambaPrimaryGroupSIDNumber",
          label = "ldap.sambaPrimaryGroupSID",
          additionalLabel = "'${translate("ldap.sambaAccount")}: ${ldapSambaAccountsConfig.sambaSIDPrefix}-",
          tooltip = "ldap.sambaPrimaryGroupSID.tooltip",
        )
      )
    }
    if (posixConfigured) {
      rightCol.add(UIInput("ldapValues.homeDirectory", label = "ldap.homeDirectory"))
      rightCol.add(UIInput("ldapValues.loginShell", label = "ldap.loginShell"))
    }
    if (sambaConfigured) {
      rightCol.add(
        UIReadOnlyField(
          "ldapValues.sambaNTPassword",
          label = "ldap.sambaNTPassword",
          tooltip = "ldap.sambaNTPassword.tooltip",
          additionalLabel = "ldap.sambaNTPassword.subtitle",
        )
      )
    }
  }

  private fun addRights(layout: UILayout, dto: User) {
    val userDO = PFUserDO()
    dto.copyTo(userDO)
    val userRights: List<UserRightDto> = userRightsHandler.getUserRights(dto, userDO)
    var counter = 0
    val uiElements = mutableListOf<UIElement>()
    val userGroups = User.getAssignedGroupDOs(dto)
    for (rightDto in userRights) {
      val right = userRightsHandler.getUserRight(rightDto, userDO, userGroups) ?: continue
      dto.add(rightDto)
      if (right.isBooleanType) {
        uiElements.add(UICheckbox("userRights[$counter].booleanValue", label = right.id.i18nKey))
      } else {
        val availableValues = right.getAvailableValues(userDO, userGroups).map {
          UISelectValue(it, translate(it.i18nKey))
        }
        uiElements.add(UISelect("userRights[$counter].value", label = right.id.i18nKey, values = availableValues))
      }
      counter += 1
    }
    val size = uiElements.size
    if (size > 0) {
      val leftCol = UICol(md = 6)
      val rightCol = UICol(md = 6)
      layout.add(UIFieldset(title = "access.rights").add(UIRow().add(leftCol).add(rightCol)))
      uiElements.forEachIndexed { index, uiElement ->
        if (index * 2 < size) {
          leftCol.add(uiElement)
        } else {
          rightCol.add(uiElement)
        }
      }
    }
  }

  private fun addToken(
    layout: UILayout,
    col: UICol,
    property: KProperty<*>,
    i18nKey: String,
    userId: Long,
    tokenType: UserTokenType,
    position: Int? = null,
  ) {
    val key = "user.authenticationToken.$i18nKey"
    val title: String
    val tooltip: String
    if (tokenType == UserTokenType.STAY_LOGGED_IN_KEY) {
      title = "login.stayLoggedIn.invalidateAllStayLoggedInSessions"
      tooltip = "login.stayLoggedIn.invalidateAllStayLoggedInSessions.tooltip"
    } else {
      title = "user.authenticationToken.renew"
      tooltip = "user.authenticationToken.renew.tooltip"
    }
    val row = UIRow()
      .add(
        UICol().add(
          UIReadOnlyField(
            property.name,
            label = key,
            additionalLabel = "timeOfCreation",
            tooltip = "$key.tooltip"
          )
        )
      )
      .add(
        UICol().add(
          UIButton.createDangerButton(
            layout,
            id = "$i18nKey-renew",
            title = title,
            tooltip = tooltip,
            confirmMessage = "user.authenticationToken.renew.securityQuestion",
            responseAction = ResponseAction(
              RestResolver.getRestUrl(
                UserServicesRest::class.java,
                "resetToken?userId=${userId}&type=${tokenType}"
              ), targetType = TargetType.POST
            )
          )
        )
      )
    if (position != null) {
      col.add(position, row)
    } else {
      col.add(row)
    }
  }

  override fun onBeforeUpdate(request: HttpServletRequest, obj: PFUserDO, postData: PostData<User>) {
    accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
  }

  override val autoCompleteSearchFields = arrayOf("username", "firstname", "lastname", "email")

  override fun queryAutocompleteObjects(request: HttpServletRequest, filter: BaseSearchFilter): List<PFUserDO> {
    val list = super.queryAutocompleteObjects(request, filter)
    if (filter.searchString.isNullOrBlank() || request.getParameter(AutoCompletion.SHOW_ALL_PARAM) != "true") {
      // Show deactivated users only if search string is given or param SHOW_ALL_PARAM is true:
      if (!userGroupCache.isLoggedInUserMemberOfGroup(
          ProjectForgeGroup.HR_GROUP,
          ProjectForgeGroup.FINANCE_GROUP,
          ProjectForgeGroup.ORGA_TEAM,
          ProjectForgeGroup.ADMIN_GROUP,
        )
      ) {
        // Shorten list only for non financial and administrative staff:
        return list.filter { !it.deactivated } // Remove deactivated users when returning all.
      }
    }
    return list
  }

  @PostMapping("resetToken")
  fun resetToken(
    @RequestParam("userId", required = true) userId: Long,
    @RequestParam("type", required = true) type: UserTokenType,
    @RequestBody postData: PostData<User>
  ): ResponseEntity<*> {
    if (type == UserTokenType.STAY_LOGGED_IN_KEY) {
      log.info("Trying to reset all stay-logged-in sessions of user #$userId.")
    } else {
      log.info("Trying to renew token $type of user #$userId.")
    }
    accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
    userAuthenticationsService.renewToken(userId, type)
    val toast = if (type == UserTokenType.STAY_LOGGED_IN_KEY) {
      "login.stayLoggedIn.invalidateAllStayLoggedInSessions.successfullDeleted"
    } else {
      "user.authenticationToken.renew.successful"
    }
    val user = postData.data
    updateTokenCreationDates(user)
    return UIToast.createToastResponseEntity(
      translate(toast),
      color = UIColor.SUCCESS,
      mutableMapOf("data" to user),
      merge = true,
      targetType = TargetType.UPDATE,
    )
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
    ExcelWorkbook.createEmptyWorkbook(ThreadLocalUserContext.locale!!).use { workbook ->
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
      ExcelUtils.registerColumn(sheet, PFUserDO::mobilePhone)
      sheet.registerColumn(translate("user.assignedGroups"), "assignedGroups").withSize(100)
      ExcelUtils.registerColumn(sheet, PFUserDO::rights)
      ExcelUtils.registerColumn(sheet, PFUserDO::organization)
      ExcelUtils.registerColumn(sheet, PFUserDO::lastLogin)
      ExcelUtils.registerColumn(sheet, PFUserDO::locale)
      ExcelUtils.registerColumn(sheet, PFUserDO::timeZone)
      ExcelUtils.registerColumn(sheet, PFUserDO::description, 50)
      if (posixConfigured) {
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
        ExcelUtils.getCell(row, PFUserDO::timeZone)?.setCellValue(user.timeZone)
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
    internal fun createUserSettingsCol(uiLength: UILength, user: PFUserDO): UICol {
      val userLC = LayoutContext(PFUserDO::class.java)

      val locales =
        UserLocale.LOCALIZATIONS.map { UISelectValue(Locale(it), translate("locale.$it")) }.toMutableList()
      locales.add(0, UISelectValue(Locale("DEFAULT"), translate("user.defaultLocale")))

      val today = LocalDate.now()
      val formats = Configuration.instance.dateFormats
      val dateFormats = formats.map { createUISelectValue(it, today) }.toMutableList()
      val excelDateFormats = formats.map { createUISelectValue(it, today, true) }.toMutableList()

      val timeNotations = listOf(
        UISelectValue(TimeNotation.H12, translate("timeNotation.12")),
        UISelectValue(TimeNotation.H24, translate("timeNotation.24"))
      )

      val col = UICol(uiLength)
        .add(UIReadOnlyField("lastLoginFormatted", label = "login.lastLogin"))
        .add(UIReadOnlyField(User::lastPasswordChangeFormatted.name, label = "user.changePassword.password.lastChange"))
      if (Login.getInstance().isWlanPasswordChangeSupported(user)) {
        col.add(
          UIReadOnlyField(
            User::lastWlanPasswordChangeFormatted.name,
            label = "user.changeWlanPassword.lastChange"
          )
        )
      }
      col.add(userLC, "timeZone", "personalPhoneIdentifiers")
        .add(UISelect("locale", userLC, required = false, values = locales))
        .add(UISelect("dateFormat", userLC, required = false, values = dateFormats))
        .add(UISelect("excelDateFormat", userLC, required = false, values = excelDateFormats))
        .add(UISelect("timeNotation", userLC, required = false, values = timeNotations))
      return col
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

  private val externalUserManagementSystem: Boolean
    get() = SystemStatus.isDevelopmentMode() ||
        accessChecker.isLoggedInUserMemberOfAdminGroup && Login.getInstance().hasExternalUsermanagementSystem()
  private val posixConfigured
    get() = SystemStatus.isDevelopmentMode() ||
        (accessChecker.isLoggedInUserMemberOfAdminGroup && ldapUserDao.isPosixAccountsConfigured)
  private val sambaConfigured
    get() = SystemStatus.isDevelopmentMode() ||
        (accessChecker.isLoggedInUserMemberOfAdminGroup && ldapUserDao.isSambaAccountsConfigured)
}
