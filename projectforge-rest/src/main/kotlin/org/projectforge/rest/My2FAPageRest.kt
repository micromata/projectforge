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
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.i18n.translate
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.security.My2FAService
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

@RestController
@RequestMapping("${Rest.URL}/${MenuItemDefId.TWO_FACTOR_AUTHENTIFICATION_SUB_URL}")
class My2FAPageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var my2FAService: My2FAService

  class Code {
    @PropertyInfo(i18nKey = "user.My2FACode.code", tooltip = "user.My2FACode.code.info")
    var code: String? = null
  }

  @PostMapping
  fun check(request: HttpServletRequest, @RequestBody postData: PostData<Code>)
      : ResponseEntity<ResponseAction> {
    val code = postData.data.code
    if (code == null || my2FAService.validateOTP(code) != My2FAService.SUCCESS) {
      return showValidationErrors(ValidationError("user.My2FACode.error.validation", "code"))
    }
    val redirectUrl = ExpiringSessionAttributes.getAttribute(request.session, ATTR_REDIRECT_URL) as? String
    if (redirectUrl.isNullOrBlank()) {
      return ResponseEntity.ok(ResponseAction(targetType = TargetType.NOTHING))
    }
    return ResponseEntity.ok(ResponseAction(redirectUrl))
  }

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest): FormLayoutData {
    val data = Code()
    val layout = UILayout("user.My2FACode.title")
    val lc = LayoutContext(Code::class.java)
    layout.add(
      UIFieldset(12)
        .add(lc, "code")
    )
      .addAction(
        UIButton(
          "check",
          translate("check"),
          UIColor.SUCCESS,
          responseAction = ResponseAction(RestResolver.getRestUrl(this::class.java), targetType = TargetType.POST),
          default = true
        )
      )
    LayoutUtils.process(layout)
    return FormLayoutData(data, layout, createServerData(request))
  }

  companion object {
    fun registerRedirectUrlForUserSession(session: HttpSession, url: String) {
      ExpiringSessionAttributes.setAttribute(session, ATTR_REDIRECT_URL, url, 10)
    }

    private const val ATTR_REDIRECT_URL = "My2FAPageRest:redirectUrl"
  }
}
