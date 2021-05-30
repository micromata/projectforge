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

package org.projectforge.rest.pub

import mu.KotlinLogging
import org.projectforge.Const
import org.projectforge.business.login.LoginProtection
import org.projectforge.business.login.LoginResult
import org.projectforge.business.login.LoginResultStatus
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserTokenType
import org.projectforge.business.user.filter.CookieService
import org.projectforge.business.user.filter.UserFilter
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.login.LoginHandlerService
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.ServerData
import org.projectforge.ui.*
import org.projectforge.web.rest.AbstractRestUserFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.net.URLDecoder
import javax.servlet.ServletRequest
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private val log = KotlinLogging.logger {}

/**
 * This rest service should be available without login (public).
 */
@RestController
@RequestMapping("${Rest.PUBLIC_URL}/login")
open class LoginPageRest {
  /**
   * Password as char array for security reasons (don't wait for the garbage collector).
   */
  class LoginData(var username: String? = null, var password: CharArray? = null, var stayLoggedIn: Boolean? = null)

  @Autowired
  private lateinit var userService: UserService

  @Autowired
  private lateinit var userAuthenticationsService: UserAuthenticationsService

  @Autowired
  private lateinit var cookieService: CookieService

  @Autowired
  private lateinit var loginHandlerService: LoginHandlerService

  @GetMapping("dynamic")
  fun getForm(
    request: HttpServletRequest,
    @RequestParam url: String? = null
  ): FormLayoutData {
    val form = if (UserFilter.getUserContext(request) != null) {
      // User is already logged-in:
      UILayout("login.title")
        .add(
          UIAlert(
            translate(ThreadLocalUserContext.getLocale(), "login.successful"),
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
    val loginResultStatus = _login(request, response, postData.data)

    if (loginResultStatus == LoginResultStatus.SUCCESS) {
      var redirectUrl: String? = null
      val returnToCaller = postData.serverData?.returnToCaller
      if (!returnToCaller.isNullOrBlank()) {
        redirectUrl = URLDecoder.decode(returnToCaller, "UTF-8")
      } else if (request.getHeader("Referer").contains("/public/login")) {
        redirectUrl = "/${Const.REACT_APP_PATH}calendar"
      }

      return ResponseAction(targetType = TargetType.CHECK_AUTHENTICATION, url = redirectUrl)
    }

    response.status = 400
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
        UIButton(
          "login",
          translate("login"),
          UIColor.SUCCESS,
          responseAction = responseAction,
          default = true
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

  private fun _login(
    request: HttpServletRequest,
    response: HttpServletResponse,
    loginData: LoginData
  ): LoginResultStatus {
    val loginResult = checkLogin(request, loginData)
    val user = loginResult.user
    if (user == null || loginResult.loginResultStatus != LoginResultStatus.SUCCESS) {
      return loginResult.loginResultStatus
    }
    if (UserFilter.isUpdateRequiredFirst()) {
      log.warn("******* Update of ProjectForge required first. Please login via old login page. LoginService should be used instead.")
      return LoginResultStatus.FAILED
    }
    log.info("User successfully logged in: " + user.userDisplayName)
    if (loginData.stayLoggedIn == true) {
      val loggedInUser = userService.internalGetById(user.id)
      val stayLoggedInKey = userAuthenticationsService.internalGetToken(user.id, UserTokenType.STAY_LOGGED_IN_KEY)
      val cookie = Cookie(
        Const.COOKIE_NAME_FOR_STAY_LOGGED_IN,
        "${loggedInUser.id}:${loggedInUser.username}:$stayLoggedInKey"
      )
      cookieService.addStayLoggedInCookie(request, response, cookie)
    }
    // Execute login:
    val userContext = UserContext(PFUserDO.createCopyWithoutSecretFields(user)!!)
    AbstractRestUserFilter.executeLogin(request, userContext)
    return LoginResultStatus.SUCCESS
  }

  private fun checkLogin(request: HttpServletRequest, loginData: LoginData): LoginResult {
    if (loginData.username == null || loginData.password == null) {
      return LoginResult().setLoginResultStatus(LoginResultStatus.FAILED)
    }
    val loginProtection = LoginProtection.instance()
    val clientIpAddress = getClientIp(request)
    val offset = loginProtection.getFailedLoginTimeOffsetIfExists(loginData.username, clientIpAddress)
    if (offset > 0) {
      val seconds = (offset / 1000).toString()
      log.warn("The account for '${loginData.username}' is locked for $seconds seconds due to failed login attempts. Please try again later.")

      val numberOfFailedAttempts = loginProtection.getNumberOfFailedLoginAttempts(loginData.username, clientIpAddress)
      return LoginResult().setLoginResultStatus(LoginResultStatus.LOGIN_TIME_OFFSET).setMsgParams(
        seconds,
        numberOfFailedAttempts.toString()
      )
    }
    val result = loginHandlerService.loginHandler.checkLogin(loginData.username, loginData.password)
    if (result.loginResultStatus == LoginResultStatus.SUCCESS) {
      loginProtection.clearLoginTimeOffset(result.user?.username, result.user?.id, clientIpAddress)
    } else if (result.loginResultStatus == LoginResultStatus.FAILED) {
      loginProtection.incrementFailedLoginTimeOffset(loginData.username, clientIpAddress)
    }
    return result
  }

  private fun getClientIp(request: ServletRequest): String? {
    return RestUtils.getClientIp(request)
  }
}
