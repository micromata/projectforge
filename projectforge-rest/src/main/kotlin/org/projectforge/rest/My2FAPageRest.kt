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

import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.security.My2FAService
import org.projectforge.ui.LayoutUtils
import org.projectforge.ui.UILayout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

@RestController
@RequestMapping("${Rest.URL}/${MenuItemDefId.TWO_FACTOR_AUTHENTIFICATION_SUB_URL}")
class My2FAPageRest : AbstractDynamicPageRest() {

  @Autowired
  private lateinit var my2FAServicesRest: My2FAServicesRest

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest): FormLayoutData {
    val data = My2FAData()
    data.lastSuccessful2FA = My2FAService.getLastSuccessful2FAAsTimeAgo()
    val layout = UILayout("user.My2FACode.title")
    my2FAServicesRest.fill2FA(layout, data)

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
