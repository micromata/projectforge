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

package org.projectforge.security

import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.LayoutUtils
import org.projectforge.ui.UICustomized
import org.projectforge.ui.UILayout
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/webauthn")
class WebAuthnPageRest : AbstractDynamicPageRest() {
  class WebAuthnFormData

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest): FormLayoutData {
    val data = WebAuthnFormData()
    val layout = UILayout("webauthn.registration.title")
      .add(UICustomized("webauthn"))
    LayoutUtils.process(layout)

    layout.postProcessPageMenu()
    layout.addTranslations("webauthn.registration.button.authenticate", "webauthn.registration.button.register")

    return FormLayoutData(data, layout, createServerData(request))
  }
}
