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
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserAuthenticationsDO
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.rest.calendar.BarcodeServicesRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.security.TimeBased2FactorAuthentication
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/my2FASetup")
class My2FASetupPageRest : AbstractDynamicPageRest() {
  class My2FactorAuthentificationData(
    var userId: Int? = null,
    var username: String? = null,
    var authenticatorKey: String? = null
  )

  @Autowired
  private lateinit var authenticationsService: UserAuthenticationsService

  @Autowired
  private lateinit var userDao: UserDao

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest): FormLayoutData {
    val userId = ThreadLocalUserContext.getUserId()
    val user = userDao.getById(userId)
    val authenticatorKey = authenticationsService.getToken(userId, UserTokenType.AUTHENTICATOR_KEY)
    val data = My2FactorAuthentificationData(userId, username = user.username, authenticatorKey = authenticatorKey)

    val layout = UILayout("user.my2FactorAuthentication.title.edit")
    val userLC = LayoutContext(PFUserDO::class.java)
    val authenticationsLC = LayoutContext(UserAuthenticationsDO::class.java)
    val key = TimeBased2FactorAuthentication.standard.generateSecretKey()
    /*thread(start = true) {
      var lastCode: String? = null
      for (i in 0..1000) {
        val code = TimeBased2FactorAuthentication.standard.getTOTPCode(key)
        if (code != lastCode) {
          lastCode = code
          println(code)
        }
        Thread.sleep(1000)
      }
    }*/
    val queryURL = TimeBased2FactorAuthentication.standard.getAuthenticatorUrl(
      key,
      ThreadLocalUserContext.getUser().username!!,
      domainService.plainDomain ?: "unknown"
    )
    val barcodeUrl = BarcodeServicesRest.getBarcodeGetUrl(queryURL)

    layout.add(
      UIFieldset(12)
        .add(
          UIRow()
            .add(
              UICol(UILength(lg = 6))
                .add(UIReadOnlyField("username", userLC))
                .add(UICustomized("image", mutableMapOf("src" to barcodeUrl, "alt" to barcodeUrl)))
            )
        )
    )
    LayoutUtils.process(layout)

    layout.postProcessPageMenu()

    return FormLayoutData(data, layout, createServerData(request))
  }
}
