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

package org.projectforge.rest

import de.micromata.merlin.excel.ExcelWorkbook
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.projectforge.SystemStatus
import org.projectforge.business.ldap.LdapPosixAccountsUtils
import org.projectforge.business.ldap.LdapSambaAccountsUtils
import org.projectforge.business.ldap.LdapService
import org.projectforge.business.ldap.LdapUserDao
import org.projectforge.business.login.Login
import org.projectforge.business.password.PasswordQualityService
import org.projectforge.business.sipgate.SipgateConfiguration
import org.projectforge.business.user.*
import org.projectforge.business.user.service.UserService
import org.projectforge.common.StringHelper
import org.projectforge.excel.ExcelUtils
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
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.getObjectList
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.User
import org.projectforge.rest.dto.UserRightInfo
import org.projectforge.rest.sipgate.SipgateDirectCallService
import org.projectforge.ui.*
import org.projectforge.ui.filter.UIFilterListElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import kotlin.reflect.KProperty

private val log = KotlinLogging.logger {}

/**
 * The users, served to the hand built list and edit page of projectforge-next (layout free, see
 * [AbstractDTOEntityRest]). Only an administrator may change a user ([UserDao.hasAccess]); everyone else sees
 * the list with a few columns of the users sharing a group with them.
 *
 * Beside the entity itself the edit page uses [rights] (the configurable rights, which depend on the assigned
 * groups), [settingsOptions], [createUid]/[createSambaSID] (LDAP defaults) and [resetToken]. Logging out all
 * devices of a user is [UserServicesRest.logoutAllDevicesOfUser].
 */
@RestController
@RequestMapping("${Rest.URL}/user")
class UserEntityRest : AbstractDTOEntityRest<PFUserDO, User, UserDao>(
    UserDao::class.java, "user.title",
    cloneSupport = CloneSupport.CLONE,
) {
    /** The body of [rights]: the user (null while new) and the groups the form currently assigns. */
    class RightsRequest(
        val userId: Long? = null,
        val assignedGroups: List<Group>? = null,
        /** The rights as the form holds them, so values not saved yet survive a change of the groups. */
        val userRights: MutableList<org.projectforge.rest.dto.UserRightDto>? = null,
    )

    /** The choices of the personal settings, the same lists the my-account page offers ([UserSettingsOptions]). */
    class SettingsOptions(
        /** Without [UserSettingsOptions.DEFAULT_LOCALE]: no locale (null) is the default here. */
        val locales: List<UserSettingsOptions.Option>,
        val dateFormats: List<UserSettingsOptions.Option>,
        val excelDateFormats: List<UserSettingsOptions.Option>,
        val timeNotations: List<UserSettingsOptions.Option>,
        /** Label of the samba SID fields: the configured prefix the number is appended to. */
        val sambaSIDPrefix: String?,
    )

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
    private lateinit var sipgateConfiguration: SipgateConfiguration

    @Autowired
    private lateinit var userAuthenticationsService: UserAuthenticationsService

    @Autowired
    private lateinit var stayLoggedInTokenDao: StayLoggedInTokenDao

    @Autowired
    private lateinit var userAvatarDao: UserAvatarDao

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
        if (editMode && accessChecker.isLoggedInUserMemberOfAdminGroup) {
            updateTokenCreationDates(user)
            // The rights the form starts with: the same set rights() answers for the assigned groups, so the
            // form isn't dirty before anything was touched.
            user.userRights = userRightsHandler.getRightInfos(user, obj).map { info ->
                org.projectforge.rest.dto.UserRightDto(info.rightId).also { dto ->
                    info.value?.let { dto.value = UserRightValue.valueOf(it) }
                    if (info.booleanType) {
                        dto.booleanValue = info.booleanValue
                    }
                }
            }.toMutableList()
            user.posixConfigured = posixConfigured
            user.sambaConfigured = sambaConfigured
            user.externalUserManagementSystem = externalUserManagementSystem
            user.wlanPasswordSupported = Login.getInstance().isWlanPasswordChangeSupported(obj)
            user.phoneIdentifiersSupported = sipgateConfiguration.isConfigured()
            user.avatarVersion = userAvatarDao.getVersion(obj.id)
        }
        return user
    }

    /**
     * A new user starts as a local one where no external user management system is in use (every user is local
     * then), and in the configured default time zone - the presets of Wicket's former UserEditForm. Edit mode,
     * so the new form gets the flags it shows its LDAP and password fields by.
     */
    override fun newBaseDTO(request: HttpServletRequest?): User {
        val userDO = newBaseDO(request)
        if (!Login.getInstance().hasExternalUsermanagementSystem()) {
            userDO.localUser = true
        }
        userDO.timeZone = Configuration.instance.defaultTimeZone
        return transformFromDB(userDO, editMode = true)
    }

    private fun updateTokenCreationDates(user: User) {
        val userId = user.id ?: return
        if (!accessChecker.isLoggedInUserMemberOfAdminGroup) {
            return
        }
        // Stay-logged-in isn't a single token anymore, it is one per device: show the number of devices and
        // when the most recent one was used, see StayLoggedInTokenDO.
        val devices = stayLoggedInTokenDao.getEntries(userId)
        user.stayLoggedInDevices = devices.size
        devices.firstOrNull()?.lastAccess?.let {
            user.stayLoggedInLastAccessTimeAgo = TimeAgo.getMessage(it)
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
        // Fields the edit form doesn't maintain: restore them from the DB on update, otherwise the merge would
        // overwrite the existing values with null (the form posts its own fields only, the timestamps as
        // formatted text). The IdP related ones are managed by the IdP sync, which would re-populate them later
        // as user "anon"; the others by login, password change and the user's own settings.
        dto.id?.let { id ->
            baseDao.find(id, checkAccess = false)?.let { dbUser ->
                // Never the client's: without it the save would take an existing user for a new one (see
                // saveOrUpdate) and set the initial passwords of onAfterSave, bypassing the password change.
                userDO.created = dbUser.created
                userDO.idpExternalId = dbUser.idpExternalId
                userDO.lastIdpPasswordSync = dbUser.lastIdpPasswordSync
                userDO.lastLogin = dbUser.lastLogin
                userDO.loginFailures = dbUser.loginFailures
                userDO.lastPasswordChange = dbUser.lastPasswordChange
                userDO.lastWlanPasswordChange = dbUser.lastWlanPasswordChange
                userDO.firstDayOfWeekValue = dbUser.firstDayOfWeekValue
            }
        }
        // As the my-account page stores them (MyAccountRest): normalized, so the Sipgate lookup finds them.
        userDO.personalPhoneIdentifiers =
            SipgateDirectCallService.getNormalizedPersonalPhoneIdentifiers(dto.personalPhoneIdentifiers)
        return userDO
    }

    override fun prepareClone(dto: User): User {
        dto.ldapValues?.let {
            it.uidNumber = null
            it.sambaNTPassword = null
            it.sambaPrimaryGroupSIDNumber = null
            it.sambaSIDNumber = null
            it.homeDirectory = null
        }
        return super.prepareClone(dto)
    }

    /**
     * Tells the list page which of its columns are worth showing: most of them are for administrators only, the
     * LDAP column only where posix accounts are in use.
     */
    override fun addVariablesForListPage(): Map<String, Any> {
        return mapOf(
            "isAdmin" to accessChecker.isLoggedInUserMemberOfAdminGroup,
            "posixConfigured" to posixConfigured,
        )
    }

    /**
     * Every user may read (some) users ([UserDao.hasUserSelectAccess]), only an administrator may change one
     * ([UserDao.hasAccess]): the list leaves its rows unclickable for everyone else.
     */
    override fun listUpdateAccess(): Boolean {
        return accessChecker.isLoggedInUserMemberOfAdminGroup
    }

    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        if (accessChecker.isLoggedInUserMemberOfAdminGroup) {
            if (externalUserManagementSystem) {
                elements.add(
                    UIFilterListElement(
                        "sync",
                        label = translate("user.filter.syncStatus"),
                        defaultFilter = true,
                        multi = false
                    )
                        .buildValues(UserPagesSyncFilter.Sync::class.java)
                )
            }
            elements.add(
                UIFilterListElement("type", label = translate("user.filter.type"), defaultFilter = true, multi = false)
                    .buildValues(UserPagesTypeFilter.TYPE::class.java)
            )
            elements.add(
                UIFilterListElement(
                    "status",
                    label = translate("user.filter.status"),
                    defaultFilter = true,
                    multi = false
                )
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
        addSyntheticFilter(source, "sync", filters) { UserPagesSyncFilter(UserPagesSyncFilter.Sync.valueOf(it)) }
        addSyntheticFilter(source, "type", filters) { UserPagesTypeFilter(UserPagesTypeFilter.TYPE.valueOf(it)) }
        addSyntheticFilter(source, "status", filters) {
            UserPagesStatusFilter(UserPagesStatusFilter.STATUS.valueOf(it))
        }
        addSyntheticFilter(source, "hrPlanning", filters) {
            UserPagesHRPlanningFilter(UserPagesHRPlanningFilter.PLANNING_TYPE.valueOf(it))
        }
        return filters
    }

    /**
     * The filter entry [field] is no property of the user but one of the pill filters above: marked synthetic
     * (the query ignores it) and applied to the result instead, if exactly one value is chosen.
     */
    private fun addSyntheticFilter(
        source: MagicFilter,
        field: String,
        filters: MutableList<CustomResultFilter<PFUserDO>>,
        create: (String) -> CustomResultFilter<PFUserDO>,
    ) {
        val filter = source.entries.find { it.field == field } ?: return
        filter.synthetic = true
        val values = filter.value.values
        if (values.isNullOrEmpty() || values.size != 1) {
            return
        }
        val value = values[0]
        try {
            filters.add(create(value))
        } catch (ex: IllegalArgumentException) {
            log.warn { "Oups, can't convert '$value': ${ex.message}" }
        }
    }

    /**
     * The configurable rights of the user with the given groups, see [UserRightsHandler.getRightInfos]. The edit
     * page asks on loading and whenever the assigned groups change.
     */
    @AccessChecked("Admin group (checkIsLoggedInUserMemberOfAdminGroup)")
    @PostMapping("rights")
    fun rights(@RequestBody body: RightsRequest): List<UserRightInfo> {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        val userDO = body.userId?.let { baseDao.find(it, checkAccess = false) } ?: PFUserDO()
        val user = User()
        user.id = userDO.id
        user.assignedGroups = body.assignedGroups?.toMutableList()
        user.userRights = body.userRights
        return userRightsHandler.getRightInfos(user, userDO)
    }

    @AccessChecked("Admin group (checkIsLoggedInUserMemberOfAdminGroup)")
    @GetMapping("settingsOptions")
    fun settingsOptions(): SettingsOptions {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        return SettingsOptions(
            locales = UserSettingsOptions.locales().filter { it.value != UserSettingsOptions.DEFAULT_LOCALE },
            dateFormats = UserSettingsOptions.dateFormats(),
            excelDateFormats = UserSettingsOptions.excelDateFormats(),
            timeNotations = UserSettingsOptions.timeNotations(),
            sambaSIDPrefix = if (sambaConfigured) ldapService.ldapConfig?.sambaAccountsConfig?.sambaSIDPrefix else null,
        )
    }

    @AccessChecked("Admin group (checkIsLoggedInUserMemberOfAdminGroup)")
    @PostMapping("createUid")
    fun createUid(@Valid @RequestBody postData: PostData<User>):
            ResponseAction {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        val data = postData.data
        val dtoValues = data.ensureLdapValues()
        val ldapValues = dtoValues.convert()
        ldapPosixAccountsUtils.setDefaultValues(ldapValues, data.username)
        dtoValues.copyFrom(ldapValues)
        return ResponseAction(targetType = TargetType.UPDATE)
            .addVariable("data", data)
    }

    @AccessChecked("Admin group (checkIsLoggedInUserMemberOfAdminGroup)")
    @PostMapping("createSambaSID")
    fun createSambaSID(@Valid @RequestBody postData: PostData<User>):
            ResponseAction {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
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
        if (!accessChecker.isLoggedInUserMemberOfAdminGroup) {
            // Only an administrator may save a user (the save refuses everyone else after validating). The checks
            // below would answer anyone whether a username is taken and whose an email address is.
            return
        }
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
        validateEmails(validationErrors, User::alternativeEmails, dto.alternativeEmails)
        validateEmails(validationErrors, User::formerEmails, dto.formerEmails)
        val alternativeEmails = PFUserDO.splitEmails(dto.alternativeEmails).map { it.lowercase() }.toSet()
        PFUserDO.findForeignEmails(user, userGroupCache.allUsers).forEach { (email, other) ->
            val field = if (email.lowercase() in alternativeEmails) User::alternativeEmails else User::formerEmails
            validationErrors.add(
                ValidationError(
                    translateMsg("user.error.emailUsedByOtherUser", email, other.userDisplayName),
                    fieldId = field.name,
                )
            )
        }
        validatePosixValues(validationErrors, dto)
        validateSambaValues(validationErrors, dto)
        validatePassword(User::password, dto.password, validationErrors)
        validatePassword(User::wlanPassword, dto.wlanPassword, validationErrors)
    }

    private fun validateEmails(
        validationErrors: MutableList<ValidationError>,
        field: KProperty<*>,
        emails: String?,
    ) {
        PFUserDO.splitEmails(emails).filterNot { StringHelper.isEmailValid(it) }.forEach { email ->
            validationErrors.add(
                ValidationError(
                    translateMsg("user.error.invalidEmail", email),
                    fieldId = field.name,
                )
            )
        }
    }

    private fun validatePassword(
        field: KProperty<*>,
        password: String?,
        validationErrors: MutableList<ValidationError>
    ) {
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
            validationErrors.add(
                ValidationError.createFieldRequired(
                    "ldapValues.uidNumber",
                    translate("ldap.uidNumber")
                )
            )
        } else if (!ldapPosixAccountsUtils.isGivenNumberFree(dto.id ?: -1L, uidNumber)) {
            validationErrors.add(
                ValidationError(
                    translateMsg("ldap.uidNumber.alreadyInUse", ldapPosixAccountsUtils.nextFreeUidNumber),
                    fieldId = "ldapValues.uidNumber",
                )
            )
        }
        if (ldapValues.gidNumber == null) {
            validationErrors.add(
                ValidationError.createFieldRequired(
                    "ldapValues.gidNumber",
                    translate("ldap.gidNumber")
                )
            )
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
            validationErrors.add(
                ValidationError.createFieldRequired(
                    "ldapValues.loginShell",
                    translate("ldap.loginShell")
                )
            )
        }
    }

    private fun validateSambaValues(validationErrors: MutableList<ValidationError>, dto: User) {
        val ldapValues = dto.ldapValues
        if (!sambaConfigured || ldapValues == null || ldapValues.isSambaValuesEmpty) {
            return
        }
        val sambaSID = ldapValues.sambaSIDNumber
        if (sambaSID == null) {
            validationErrors.add(
                ValidationError.createFieldRequired("ldapValues.sambaSIDNumber", translate("ldap.sambaSID"))
            )
        } else if (!ldapSambaAccountsUtils.isGivenNumberFree(dto.id ?: -1, sambaSID)) {
            validationErrors.add(
                ValidationError(
                    translateMsg("ldap.sambaSID.alreadyInUse", ldapSambaAccountsUtils.nextFreeSambaSIDNumber),
                    fieldId = "ldapValues.sambaSIDNumber",
                )
            )
        }
        if (ldapValues.sambaPrimaryGroupSIDNumber == null) {
            validationErrors.add(
                ValidationError.createFieldRequired(
                    "ldapValues.sambaPrimaryGroupSIDNumber",
                    translate("ldap.sambaPrimaryGroupSID")
                )
            )
        }
    }

    override fun onAfterSaveOrUpdate(request: HttpServletRequest, obj: PFUserDO, postData: PostData<User>) {
        val start = System.currentTimeMillis()
        val newAssignedGroups = postData.data.assignedGroups?.map { it.id } ?: emptyList()
        val dbAssignedGroups = userGroupCache.getUserGroups(obj) ?: emptyList()
        val groupsToAssign = newAssignedGroups.subtract(dbAssignedGroups)
        val groupsToUnassign = dbAssignedGroups.subtract(newAssignedGroups)
        groupDao.assignGroupByIds(obj, groupsToAssign, groupsToUnassign, false)
        log.info { "Assigning groups took: ${System.currentTimeMillis() - start}ms." }
        val startRights = System.currentTimeMillis()
        val userRightVOS = userRightsHandler.getUserRightVOs(postData.data)
        // The cache doesn't know of the groups just (un)assigned: the rights are evaluated with the saved ones.
        val groups = newAssignedGroups.mapNotNull { id -> id?.let { userGroupCache.getGroup(it) } }
        userRightDao.updateUserRights(obj, userRightVOS, false, groups)
        log.info { "Updating rights took: ${System.currentTimeMillis() - startRights}ms." }
        // Only one time reload user group cache
        userGroupCache.forceReload()
        log.info { "onAfterSaveOrUpdate: ${System.currentTimeMillis() - start}ms." }
    }

    override fun onAfterSave(request: HttpServletRequest, obj: PFUserDO, postData: PostData<User>): ResponseAction {
        val dto = postData.data
        if (dto.id != null) {
            // Not a new user: its passwords are changed by ChangePasswordRest only (own checks and 2FA period).
            return super.onAfterSave(request, obj, postData)
        }
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
        return super.onAfterSave(request, obj, postData)
    }

    override fun onBeforeUpdate(request: HttpServletRequest, obj: PFUserDO, postData: PostData<User>) {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
    }

    override val autoCompleteSearchFields =
        arrayOf("username", "firstname", "lastname", "nickname", "email", "alternativeEmails", "formerEmails")

    /** Name hits first, see [autoCompletePrimarySearchFields]. */
    override val autoCompletePrimarySearchFields = arrayOf("username", "firstname", "lastname", "nickname")

    /**
     * Deactivated users only on request ([AutoCompletion.SHOW_INACTIVE_PARAM]) and only for the members of
     * [AutoCompletion.SHOW_INACTIVE_PERSONS_GROUPS], see [isShowInactiveAllowed].
     */
    override fun queryAutocompleteObjects(request: HttpServletRequest, filter: BaseSearchFilter): List<PFUserDO> {
        val list = super.queryAutocompleteObjects(request, filter)
        return if (showInactive(request)) list else list.filter { !it.deactivated }
    }

    override fun isShowInactiveAllowed(): Boolean =
        accessChecker.isLoggedInUserMemberOfGroup(*AutoCompletion.SHOW_INACTIVE_PERSONS_GROUPS)

    /**
     * Renews a token of the user; the answer carries the posted user with the new creation dates.
     */
    @AccessChecked("Admin group (checkIsLoggedInUserMemberOfAdminGroup)")
    @PostMapping("resetToken")
    fun resetToken(
        @RequestParam("userId", required = true) userId: Long,
        @RequestParam("type", required = true) type: UserTokenType,
        @RequestBody postData: PostData<User>
    ): ResponseEntity<*> {
        log.info { "Trying to renew token $type of user #$userId." }
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        userAuthenticationsService.renewToken(userId, type)
        val user = postData.data
        updateTokenCreationDates(user)
        return UIToast.createToastResponseEntity(
            translate("user.authenticationToken.renew.successful"),
            color = UIColor.SUCCESS,
            mutableMapOf("data" to user),
            merge = true,
            targetType = TargetType.UPDATE,
        )
    }

    /**
     * Exports the users as Excel file.
     */
    @AccessChecked("Admin group (checkIsLoggedInUserMemberOfAdminGroup)")
    @PostMapping(RestPaths.REST_EXCEL_SUB_PATH)
    fun exportAsExcel(@RequestBody filter: MagicFilter): ResponseEntity<*> {
        log.info { "Exporting users as Excel file." }
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()

        @Suppress("UNCHECKED_CAST")
        val list = getObjectList(this, baseDao, filter)
        ExcelWorkbook.createEmptyWorkbook(ThreadLocalUserContext.locale).use { workbook ->
            val sheet = workbook.createOrGetSheet(translate("user.title.list"))
            val boldFont = ExcelUtils.createFont(workbook, "bold", bold = true)
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
            ExcelUtils.registerColumn(sheet, PFUserDO::alternativeEmails, 30)
            ExcelUtils.registerColumn(sheet, PFUserDO::formerEmails, 30)
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
