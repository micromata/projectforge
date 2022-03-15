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

package org.projectforge.login

import mu.KotlinLogging
import org.projectforge.business.ldap.LdapMasterLoginHandler
import org.projectforge.business.ldap.LdapSlaveLoginHandler
import org.projectforge.business.login.Login
import org.projectforge.business.login.LoginDefaultHandler
import org.projectforge.business.login.LoginHandler
import org.projectforge.business.user.filter.CookieService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

private val log = KotlinLogging.logger {}

@Service
open class LoginHandlerService {
  @Autowired
  private lateinit var applicationContext: ApplicationContext

  @Autowired
  private lateinit var cookieService: CookieService

  /**
   * If given then this login handler will be used instead of [LoginDefaultHandler]. For ldap please use e. g.
   * org.projectforge.ldap.LdapLoginHandler.
   */
  @Value("\${projectforge.login.handlerClass}")
  private val loginHandlerClass: String? = null

  lateinit var loginHandler: LoginHandler
    private set

  @PostConstruct
  fun init() {
    loginHandler = when (loginHandlerClass) {
      "LdapMasterLoginHandler" -> applicationContext.getBean(LdapMasterLoginHandler::class.java)
      "LdapSlaveLoginHandler" -> applicationContext.getBean(LdapSlaveLoginHandler::class.java)
      else -> applicationContext.getBean(LoginDefaultHandler::class.java)
    }
    Login.getInstance().setLoginHandler(loginHandler)
    loginHandler.initialize()
  }
}
