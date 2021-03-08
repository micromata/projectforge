/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.Const
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.api.EmployeeService
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.login.Login
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserTokenType
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserAuthenticationsDO
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.framework.time.TimeNotation
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.Employee
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/myAccount")
class MyAccountPageRest : AbstractDynamicPageRest() {
    @Autowired
    private lateinit var authenticationsService: UserAuthenticationsService

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var userService: UserService

    class MyAccountData(
            var userId: Int? = null,
            var username: String? = null,
            var firstname: String? = null,
            var lastname: String? = null,
            var calendarExportToken: String? = null,
            var davToken: String? = null,
            var restClientToken: String? = null,
            var stayLoggedInKey: String? = null,
            var groups: String? = null,
            var lastLogin: String? = null,
            var locale: Locale? = null,
            var dateFormat: String? = null,
            var excelDateFormat: String? = null,
            var timeNotation: TimeNotation? = null,
            var timeZone: TimeZone? = null,
            var personalPhoneIdentifiers: String? = null,
            var sshPublicKey: String? = null
    ) {
        var employee: Employee? = null
    }

    @PostMapping
    fun save(request: HttpServletRequest, @RequestBody postData: PostData<MyAccountData>)
            : ResponseEntity<ResponseAction>? {
        validateCsrfToken(request, postData)?.let { return it }
        val data = postData.data
        check(ThreadLocalUserContext.getUserId() == data.userId) { "Oups, MyAccountEditPage is called with another than the logged in user!" }
        val user = userDao.internalGetById(data.userId)
        user.firstname = data.firstname ?: user.firstname
        user.lastname = data.lastname ?: user.lastname
        user.locale = data.locale ?: user.locale
        user.dateFormat = data.dateFormat
        user.excelDateFormat = data.excelDateFormat
        user.timeNotation = data.timeNotation ?: user.timeNotation
        user.timeZone = data.timeZone ?: user.timeZone
        user.personalPhoneIdentifiers = userService.getNormalizedPersonalPhoneIdentifiers(data.personalPhoneIdentifiers)
        user.sshPublicKey = data.sshPublicKey
        userService.updateMyAccount(user)
        data.employee?.let { employee ->
            val employeeDO = employeeService.getEmployeeByUserId(ThreadLocalUserContext.getUserId())
            check(employeeDO?.userId == data.userId) { "Oups, MyAccountEditPage is called with another employee than the logged in employee!" }
            val userId = data.userId
            employeeService.updateAttribute(userId, employee.iban, "iban")
            employeeService.updateAttribute(userId, employee.bic, "bic")
            employeeService.updateAttribute(userId, employee.accountHolder, "accountHolder")
            employeeService.updateAttribute(userId, employee.street, "street")
            employeeService.updateAttribute(userId, employee.state, "state")
            employeeService.updateAttribute(userId, employee.city, "city")
            employeeService.updateAttribute(userId, employee.zipCode, "zipCode")
            employeeService.updateAttribute(userId, employee.country, "country")
            employeeService.updateAttribute(userId, employee.birthday, "birthday")
        }
        return ResponseEntity(ResponseAction("/${Const.REACT_APP_PATH}calendar"), HttpStatus.OK)
    }

    @GetMapping("dynamic")
    fun getForm(request: HttpServletRequest): FormLayoutData {
        val userId = ThreadLocalUserContext.getUserId()
        val user = userDao.getById(userId)
        val data = MyAccountData(userId, user.username, user.firstname, user.lastname)

        val layout = UILayout("user.myAccount.title.edit")
        val employeeLC = LayoutContext(EmployeeDO::class.java)
        val userLC = LayoutContext(PFUserDO::class.java)
        val authenticationsLC = LayoutContext(UserAuthenticationsDO::class.java)
        data.calendarExportToken = authenticationsService.getToken(userId, UserTokenType.CALENDAR_REST)
        data.davToken = authenticationsService.getToken(userId, UserTokenType.DAV_TOKEN)
        data.restClientToken = authenticationsService.getToken(userId, UserTokenType.REST_CLIENT)
        data.stayLoggedInKey = authenticationsService.getToken(userId, UserTokenType.STAY_LOGGED_IN_KEY)
        data.groups = groupService.getGroupnames(userId)
        data.locale = ThreadLocalUserContext.getLocale() ?: Locale("DEFAULT")
        data.dateFormat = user.dateFormat
        data.excelDateFormat = user.excelDateFormat
        data.timeNotation = user.timeNotation
        data.timeZone = user.timeZone
        data.personalPhoneIdentifiers = user.personalPhoneIdentifiers
        data.sshPublicKey = user.sshPublicKey

        data.lastLogin = DateTimeFormatter.instance().getFormattedDateTime(user.lastLogin)

        val employee = employeeService.getEmployeeByUserId(userId)
        if (employee != null) {
            data.employee = Employee()
            data.employee!!.let {
                it.street = employee.street
                it.zipCode = employee.zipCode
                it.city = employee.city
                it.country = employee.country
                it.state = employee.state
                it.birthday = employee.birthday
                it.accountHolder = employee.accountHolder
                it.iban = employee.iban
                it.bic = employee.bic
            }
        }

        layout.add(UIFieldset(12)
                .add(UIRow()
                        .add(UICol(UILength(lg = 6))
                                .add(UIReadOnlyField("username", userLC))
                                .add(userLC, "firstname", "lastname")
                                .add(addAuthenticationToken(authenticationsLC, "stayLoggedInKey",
                                        UserTokenType.STAY_LOGGED_IN_KEY,
                                        "login.stayLoggedIn.invalidateAllStayLoggedInSessions.tooltip"))
                                .add(addAuthenticationToken(authenticationsLC, "calendarExportToken", UserTokenType.CALENDAR_REST))
                                .add(addAuthenticationToken(authenticationsLC, "davToken", UserTokenType.DAV_TOKEN))
                                .add(addAuthenticationToken(authenticationsLC, "restClientToken", UserTokenType.REST_CLIENT))
                        )
                        .add(UserPagesRest.createUserSettingsCol(UILength(lg = 6)))
                )
        )
                .add(UIFieldset(12, "fibu.employee")
                        .add(UIRow()
                                .add(UICol(UILength(md = 4)).add(employeeLC, "employee.street", "employee.zipCode", "employee.city"))
                                .add(UICol(UILength(md = 4)).add(employeeLC, "employee.country", "employee.state", "employee.birthday"))
                                .add(UICol(UILength(md = 4)).add(employeeLC, "employee.accountHolder", "employee.iban", "employee.bic"))
                        )
                )
                .add(UIFieldset(12)
                        .add(UIReadOnlyField("groups", label = "user.assignedGroups"))
                )
                .add(UIFieldset(12)
                        .add(UITextArea("sshPublicKey", userLC))
                )
                .addAction(UIButton("update",
                        translate("update"),
                        UIColor.SUCCESS,
                        responseAction = ResponseAction(RestResolver.getRestUrl(this::class.java), targetType = TargetType.POST),
                        default = true)
                )

        layout.add(MenuItem("changePassword",
                i18nKey = "menu.changePassword",
                url = PagesResolver.getDynamicPageUrl(ChangePasswordPageRest::class.java),
                type = MenuItemTargetType.REDIRECT))
        if (Login.getInstance().isWlanPasswordChangeSupported(user)) {
            layout.add(MenuItem("changeWlanPassword",
                    i18nKey = "menu.changeWlanPassword",
                    url = PagesResolver.getDynamicPageUrl(ChangeWlanPasswordPageRest::class.java),
                    type = MenuItemTargetType.REDIRECT))
        }

        LayoutUtils.process(layout)

        layout.postProcessPageMenu()

        return FormLayoutData(data, layout, createServerData(request))
    }

    private fun addAuthenticationToken(lc: LayoutContext, id: String, token: UserTokenType, tooltip: String? = null): UIRow {
        return UIRow()
                .add(UICol(9)
                        .add(UIReadOnlyField(id, lc, canCopy = true, coverUp = true))
                )
                .add(UICol(3)
                        .add(UIButton("${id}-renew",
                                title = translate("user.authenticationToken.renew"),
                                tooltip = tooltip ?: "user.authenticationToken.renew.tooltip",
                                confirmMessage = translate("user.authenticationToken.renew.securityQuestion"),
                                color = UIColor.DANGER,
                                responseAction = ResponseAction("/rs/user/renewToken?token=$token", targetType = TargetType.POST)))
                        .add(UIButton("${id}-access",
                                title = translate("user.authenticationToken.button.showUsage"),
                                tooltip = "user.authenticationToken.button.showUsage.tooltip",
                                color = UIColor.LINK,
                                responseAction = ResponseAction(
                                        PagesResolver.getDynamicPageUrl(TokenInfoPageRest::class.java,
                                                mapOf("token" to token), absolute = true),
                                        targetType = TargetType.MODAL)))
                )
    }
}
