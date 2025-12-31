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

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.Constants
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.login.Login
import org.projectforge.business.user.UserAuthenticationsDao
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserTokenType
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.i18n.TimeAgo
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserAuthenticationsDO
import org.projectforge.framework.time.TimeNotation
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.sipgate.SipgateDirectCallService
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("${Rest.URL}/myAccount")
class MyAccountPageRest : AbstractDynamicPageRest() {
    @Autowired
    private lateinit var authenticationsService: UserAuthenticationsService

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var userService: UserService

    class MyAccountData(
        var userId: Long? = null,
        var username: String? = null,
        var firstname: String? = null,
        var lastname: String? = null,
        var mobilePhone: String? = null,
        var calendarExportToken: String? = null,
        var calendarExportTokenCreationDate: String? = null,
        var davToken: String? = null,
        var davTokenCreationDate: String? = null,
        var restClientToken: String? = null,
        var restClientTokenCreationDate: String? = null,
        var stayLoggedInKey: String? = null,
        var stayLoggedInKeyCreationDate: String? = null,
        var groups: String? = null,
        var lastLoginFormatted: String? = null,
        var locale: Locale? = null,
        var dateFormat: String? = null,
        var excelDateFormat: String? = null,
        var timeNotation: TimeNotation? = null,
        var timeZone: TimeZone? = null,
        var personalPhoneIdentifiers: String? = null,
        var gpgPublicKey: String? = null,
        var sshPublicKey: String? = null,
    ) {
        var lastPasswordChange: Date? = null
        var lastPasswordChangeFormatted: String? = null
        var lastWlanPasswordChange: Date? = null
        var lastWlanPasswordChangeFormatted: String? = null
    }

    @PostMapping
    fun save(request: HttpServletRequest, @RequestBody postData: PostData<MyAccountData>)
            : ResponseEntity<ResponseAction> {
        validateCsrfToken(request, postData)?.let { return it }
        val data = postData.data
        check(ThreadLocalUserContext.loggedInUserId == data.userId) { "Oups, MyAccountEditPage is called with another than the logged in user!" }
        val user = userDao.find(data.userId, checkAccess = false)!!
        user.firstname = data.firstname ?: user.firstname
        user.lastname = data.lastname ?: user.lastname
        user.locale = data.locale ?: user.locale
        user.dateFormat = data.dateFormat
        user.excelDateFormat = data.excelDateFormat
        user.timeNotation = data.timeNotation ?: user.timeNotation
        user.timeZone = data.timeZone ?: user.timeZone
        user.personalPhoneIdentifiers =
            SipgateDirectCallService.getNormalizedPersonalPhoneIdentifiers(data.personalPhoneIdentifiers)
        user.gpgPublicKey = data.gpgPublicKey
        user.sshPublicKey = data.sshPublicKey
        userService.updateMyAccount(user)
        return ResponseEntity(ResponseAction("/${Constants.REACT_APP_PATH}calendar"), HttpStatus.OK)
    }

    @GetMapping("dynamic")
    fun getForm(request: HttpServletRequest): FormLayoutData {
        val userId = ThreadLocalUserContext.loggedInUserId!!
        val user = userDao.find(userId)!!
        val data = MyAccountData(userId, user.username, user.firstname, user.lastname, user.mobilePhone)

        val layout = UILayout("user.myAccount.title.edit")
        val userLC = LayoutContext(PFUserDO::class.java)
        val authenticationsLC = LayoutContext(UserAuthenticationsDO::class.java)
        UserAuthenticationsDao.TOKEN_LIST.forEach { tokenType ->
            UserServicesRest.setToken(data, tokenType, authenticationsService.getTokenData(userId, tokenType))
        }
        data.groups = groupService.getGroupnames(userId)
        data.locale = ThreadLocalUserContext.locale ?: Locale("DEFAULT")
        data.dateFormat = user.dateFormat
        data.excelDateFormat = user.excelDateFormat
        data.timeNotation = user.timeNotation
        data.timeZone = user.timeZone
        data.personalPhoneIdentifiers = user.personalPhoneIdentifiers
        data.gpgPublicKey = user.gpgPublicKey
        data.sshPublicKey = user.sshPublicKey

        data.lastLoginFormatted = TimeAgo.getDateAndMessage(user.lastLogin)
        user.lastPasswordChange?.let {
            data.lastPasswordChange = it
            data.lastPasswordChangeFormatted = TimeAgo.getDateAndMessage(it)
        }
        user.lastWlanPasswordChange?.let {
            data.lastWlanPasswordChange = it
            data.lastWlanPasswordChangeFormatted = TimeAgo.getDateAndMessage(it)
        }

        val firstCol = UICol(lg = 6)
        firstCol.add(UIReadOnlyField("username", userLC))
            .add(userLC, "firstname", "lastname")
            .add(UIReadOnlyField("mobilePhone", userLC))

        layout.add(
            UIFieldset(12)
                .add(
                    UIRow()
                        .add(firstCol)
                        .add(
                            UserPagesRest.createUserSettingsCol(UILength(lg = 6), user)
                        )
                )
        )

        val leftTokenCol = UICol(lg = 6)
        val rightTokenCol = UICol(lg = 6)
        addAuthenticationToken(
            layout,
            leftTokenCol,
            authenticationsLC, "stayLoggedInKey",
            UserTokenType.STAY_LOGGED_IN_KEY,
            "login.stayLoggedIn.invalidateAllStayLoggedInSessions.tooltip"
        )
        addAuthenticationToken(
            layout,
            leftTokenCol,
            authenticationsLC,
            "calendarExportToken",
            UserTokenType.CALENDAR_REST
        )
        addAuthenticationToken(layout, rightTokenCol, authenticationsLC, "davToken", UserTokenType.DAV_TOKEN)
        addAuthenticationToken(layout, rightTokenCol, authenticationsLC, "restClientToken", UserTokenType.REST_CLIENT)
        layout.add(
            UIFieldset(12)
                .add(
                    UIRow()
                        .add(leftTokenCol)
                        .add(rightTokenCol)
                )
        )
        layout.add(
            UIFieldset(12)
                .add(UIReadOnlyField("groups", label = "user.assignedGroups"))
        )
            .add(
                UIFieldset(12)
                    .add(UITextArea("gpgPublicKey", userLC))
                    .add(UITextArea("sshPublicKey", userLC))
            )
            .addAction(
                UIButton.createUpdateButton(
                    responseAction = ResponseAction(
                        RestResolver.getRestUrl(this::class.java),
                        targetType = TargetType.POST
                    ),
                )
            )

        layout.add(
            MenuItem(
                "changePassword",
                i18nKey = "menu.changePassword",
                url = PagesResolver.getDynamicPageUrl(ChangePasswordPageRest::class.java),
                type = MenuItemTargetType.REDIRECT
            )
        )
        if (Login.getInstance().isWlanPasswordChangeSupported(user)) {
            layout.add(
                MenuItem(
                    "changeWlanPassword",
                    i18nKey = "menu.changeWlanPassword",
                    url = PagesResolver.getDynamicPageUrl(ChangeWlanPasswordPageRest::class.java),
                    type = MenuItemTargetType.REDIRECT
                )
            )
        }

        LayoutUtils.process(layout)

        return FormLayoutData(data, layout, createServerData(request))
    }

    private fun addAuthenticationToken(
        layout: UILayout,
        col: UICol,
        lc: LayoutContext,
        id: String,
        token: UserTokenType,
        tooltip: String? = null
    ) {
        col.add(
            UIRow()
                .add(
                    UICol(9)
                        .add(UIReadOnlyField(id, lc, canCopy = true, coverUp = true))
                )
        )
        col.add(
            UIRow()
                .add(
                    UICol(9)
                        .add(UIReadOnlyField("${id}CreationDate", lc))
                )
                .add(
                    UICol(3)
                        .add(
                            UIButton.createDangerButton(
                                layout,
                                id = "${id}-renew",
                                title = "user.authenticationToken.renew",
                                tooltip = tooltip ?: "user.authenticationToken.renew.tooltip",
                                confirmMessage = "user.authenticationToken.renew.securityQuestion",
                                responseAction = ResponseAction(
                                    RestResolver.getRestUrl(
                                        UserServicesRest::class.java,
                                        "renewToken?token=$token"
                                    ), targetType = TargetType.POST
                                )
                            )
                        )
                        .add(
                            UIButton.createLinkButton(
                                id = "${id}-access",
                                title = "user.authenticationToken.button.showUsage",
                                tooltip = "user.authenticationToken.button.showUsage.tooltip",
                                responseAction = ResponseAction(
                                    PagesResolver.getDynamicPageUrl(
                                        TokenInfoPageRest::class.java,
                                        mapOf("token" to token), absolute = true
                                    ),
                                    targetType = TargetType.MODAL
                                )
                            )
                        )
                )
        )
    }
}
