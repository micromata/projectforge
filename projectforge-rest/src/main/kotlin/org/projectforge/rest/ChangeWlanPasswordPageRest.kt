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

import mu.KotlinLogging
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.service.UserService
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.ResponseAction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/changeWlanPassword")
class ChangeWlanPasswordPageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var userDao: UserDao

  @Autowired
  private lateinit var userService: UserService

  @Autowired
  private lateinit var changePasswordPageRest: ChangePasswordPageRest

  @PostMapping
  fun save(request: HttpServletRequest, @RequestBody postData: PostData<ChangePasswordData>)
      : ResponseEntity<ResponseAction> {
    return changePasswordPageRest.internalSave(request, postData) { data, changeOwn ->
      if (changeOwn) {
        log.info { "The user wants to change his WLAN/Samba password." }
        userService.changeWlanPassword(userDao.getById(data.userId), data.loginPassword, data.newPassword)
      } else {
        log.info { "Admin user wants to change WLAN/Samba password of user '${data.userDisplayName}' with id ${data.userId}." }
        userService.changeWlanPassword(userDao.getById(data.userId), data.loginPassword, data.newPassword)
      }
    }
  }

  /**
   * @param userId is optional, so admins (only) are able to change passwords of other users.
   * is done. (ModalDialog doesn't yet work.)
   */
  @GetMapping("dynamic")
  fun getForm(
    request: HttpServletRequest,
    @RequestParam("userId") userIdString: String?,
  ): FormLayoutData {
    return changePasswordPageRest.internalGetForm(
      request, userIdString = userIdString, i18nPrefix = "user.changeWlanPassword",
      loginPasswordI18nKey = "user.changeWlanPassword.loginPassword",
    )
  }
}
