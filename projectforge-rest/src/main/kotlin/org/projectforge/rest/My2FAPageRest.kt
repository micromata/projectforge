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
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.i18n.translate
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.security.TwoFactorAuthenticationHandler
import org.projectforge.ui.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/${MenuItemDefId.TWO_FACTOR_AUTHENTIFICATION_SUB_URL}")
class My2FAPageRest : AbstractDynamicPageRest() {
  class Code {
    @PropertyInfo(i18nKey = "user.My2FACode.code")
    var code: String? = null
  }

  @PostMapping
  fun check(request: HttpServletRequest, @RequestBody postData: PostData<Code>)
      : ResponseEntity<ResponseAction> {
    validateCsrfToken(request, postData)?.let { return it }
    val code = postData.data.code

    return ResponseEntity(ResponseAction("/${Const.REACT_APP_PATH}calendar"), HttpStatus.OK)
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
}
