/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.pub

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.projectforge.business.login.LoginResultStatus
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.login.LoginData
import org.projectforge.login.LoginService
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.ServerData
import org.projectforge.rest.my2fa.My2FAServicesRest
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

/**
 * This rest service should be available without login (public).
 */
@RestController
@RequestMapping("${Rest.PUBLIC_URL}/login")
open class LoginPageRest {
    @Autowired
    private lateinit var loginService: LoginService

    @Autowired
    private lateinit var my2FAServicesRest: My2FAServicesRest

    /**
     * @param url the caller can modify the url to redirect after login (used by WicketUtils).
     */
    @GetMapping("dynamic")
    fun getForm(
        request: HttpServletRequest,
        @RequestParam url: String? = null
    ): FormLayoutData {
        val userContext = LoginService.getUserContext(request)
        LoginServiceRest.storeOriginUrl(request, url)
        val form = if (userContext != null) {
            if (userContext.new2FARequired) {
                return get2FALayout(userContext, url)
            }
            // User is already logged-in:
            UILayout("login.title")
                .add(
                    UIAlert(
                        translate(ThreadLocalUserContext.locale, "login.successful"),
                        color = UIColor.INFO,
                        icon = UIIconType.INFO
                    )
                )
            // Translation can't be done automatically, because the user isn't set here in ThreadLocalUserContext, because this
            // is a public page!
            // (The thread local user isn't set, but the locale should be set by LocaleFilter in ThreadUserLocalContext.)
        } else {
            this.getLoginLayout()
        }
        return FormLayoutData(null, form, ServerData(returnToCaller = url))
    }

    @PostMapping
    fun login(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @RequestBody postData: PostData<LoginData>
    )
            : ResponseAction {
        val loginResultStatus = loginService.authenticate(request, response, postData.data)

        if (loginResultStatus == LoginResultStatus.SUCCESS) {
            val userContext = ThreadLocalUserContext.userContext
            val redirectUrl = if (userContext == null) {
                // No redirect, because the 2FA form will be shown (login incomplete).
                null
            } else {
                // Redirect to the origin url:
                LoginServiceRest.getRedirectUrl(request, postData.serverData)
            }
            return ResponseAction(targetType = TargetType.CHECK_AUTHENTICATION, url = redirectUrl)
        }

        response.status = HttpStatus.BAD_REQUEST.value()
        return ResponseAction(targetType = TargetType.UPDATE)
            .addVariable("ui", getLoginLayout(loginResultStatus))
    }

    private fun getLoginLayout(loginResultStatus: LoginResultStatus? = null): UILayout {
        val motd = Configuration.instance.getStringValue(ConfigurationParam.MESSAGE_OF_THE_DAY)
        val responseAction = ResponseAction(RestResolver.getRestUrl(this::class.java), targetType = TargetType.POST)

        val formCol = UICol(
            length = UILength(12, md = 6, lg = 4),
            offset = UILength(0, md = 3, lg = 4)
        )
            .add(UIAlert("'$motd", color = UIColor.INFO, icon = UIIconType.INFO))

        if (loginResultStatus != null) {
            formCol.add(
                UIAlert(
                    "'${loginResultStatus.localizedMessage}",
                    color = UIColor.DANGER,
                    icon = UIIconType.USER_LOCK
                )
            )
        }

        formCol
            .add(
                UIInput(
                    "username",
                    required = true,
                    label = "username",
                    focus = true,
                    autoComplete = UIInput.AutoCompleteType.USERNAME
                )
            )
            .add(
                UIInput(
                    "password",
                    required = true,
                    label = "password",
                    dataType = UIDataType.PASSWORD,
                    autoComplete = UIInput.AutoCompleteType.CURRENT_PASSWORD
                )
            )
            .add(
                UICheckbox(
                    "stayLoggedIn",
                    label = "login.stayLoggedIn",
                    tooltip = "login.stayLoggedIn.tooltip"
                )
            )
            .add(
                UIButton.createDefaultButton("login", responseAction = responseAction)
            )
            .add(
                UIButton.createLinkButton(
                    id = "passwordForgotten",
                    title = "password.forgotten.link",
                    responseAction = ResponseAction(
                        PagesResolver.getDynamicPageUrl(
                            PasswordForgottenPageRest::
                            class.java, absolute = true
                        ),
                        targetType = TargetType.REDIRECT
                    ),
                )
            )

        val layout = UILayout("login.title")
            .add(
                UIRow()
                    .add(formCol)
            )

        LayoutUtils.process(layout)

        return layout
    }

    private fun get2FALayout(userContext: UserContext, url: String?): FormLayoutData {
        val layout = UILayout("login.title")
        val data = LoginData()
        my2FAServicesRest.fillLayout4PublicPage(layout, userContext, redirectUrl = url)
        LayoutUtils.process(layout)
        return FormLayoutData(data, layout, ServerData(returnToCaller = url))
    }
}
