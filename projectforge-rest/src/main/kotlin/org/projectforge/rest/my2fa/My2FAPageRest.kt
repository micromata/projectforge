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

package org.projectforge.rest.my2fa

import org.apache.commons.codec.binary.Base64
import org.projectforge.framework.i18n.Duration
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.security.*
import org.projectforge.ui.LayoutUtils
import org.projectforge.ui.UIAlert
import org.projectforge.ui.UIColor
import org.projectforge.ui.UILayout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

@RestController
@RequestMapping("${Rest.URL}/${MenuItemDefId.TWO_FACTOR_AUTHENTIFICATION_SUB_URL}")
class My2FAPageRest : AbstractDynamicPageRest(), My2FAPage {

  @Autowired
  private lateinit var my2FARequestHandler: My2FARequestHandler

  @Autowired
  private lateinit var my2FAServicesRest: My2FAServicesRest

  @PostConstruct
  private fun postConstruct() {
    my2FARequestHandler.register(this)
  }

  /**
   * @param target The target url to redirect after successful 2FA.
   * @param expiryMillis The expiry time of OTP of the target page in millis.
   */
  @GetMapping("dynamic")
  fun getForm(
    request: HttpServletRequest,
    @RequestParam target: String? = null,
    @RequestParam expiryMillis: Long? = null,
    @RequestParam modal: Boolean? = false,
  ): FormLayoutData {
    val data = My2FAData()
    data.target = target
    data.modal = modal
    data.lastSuccessful2FA = My2FAService.getLastSuccessful2FAAsTimeAgo()
    val layout = UILayout("user.My2FACode.title")
    if (expiryMillis != null && expiryMillis > 0) {
      layout.add(
        UIAlert(
          translateMsg("user.My2FA.expired", Duration.getMessage(expiryMillis)),
          color = UIColor.WARNING
        )
      )
    }
    my2FAServicesRest.fill2FA(layout, data)
    LayoutUtils.process(layout)
    WebAuthnServicesRest.addAuthenticateTranslations(layout)
    return FormLayoutData(data, layout, createServerData(request))
  }

  override fun redirect(request: HttpServletRequest, response: HttpServletResponse, expiryMillis: Long) {
    response.sendRedirect(getUrl(request, expiryMillis, false))
  }

  companion object {
    fun getUrl(request: HttpServletRequest, expiryMillis: Long, useModal: Boolean): String {
      val queryString = request.queryString
      val uri = request.requestURI
      var uriWithQueryString = if (queryString.isNullOrEmpty()) {
        uri
      } else {
        "$uri?$queryString"
      }
      val referer = request.getHeader("Referer")
      if (uriWithQueryString.startsWith("/rs/") && referer.contains("/react/")) {
        // uriWithQueryString = uriWithQueryString.replace("/rs/", "/react/").replace("?id=", "/")
        uriWithQueryString =
          referer.substring(referer.indexOf("/react/")) // Use referer url (without protocol and domain)
      }
      val target = Base64.encodeBase64URLSafeString(uriWithQueryString.toByteArray(StandardCharsets.UTF_8))
      return PagesResolver.getDynamicPageUrl(
        My2FAPageRest::class.java,
        absolute = true,
        params = mapOf(
          "target" to target, "expiryMillis" to expiryMillis,
          "modal" to useModal,
        ),
      )
    }
  }
}
