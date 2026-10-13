/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.user.StayLoggedInTokenDao
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.SessionCsrfService
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.User
import org.projectforge.ui.TargetType
import org.projectforge.ui.UIColor
import org.projectforge.ui.UIToast
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/user")
open class UserServicesRest {
  @Autowired
  private lateinit var accessChecker: AccessChecker

  @Autowired
  private lateinit var stayLoggedInTokenDao: StayLoggedInTokenDao

  @Autowired
  private lateinit var sessionCsrfService: SessionCsrfService

  /**
   * Deletes all stay-logged-in tokens of another user, so every device of that user has to log in again.
   * For admins only (user edit page). The user's own devices are logged out on the my-account page
   * ([MyAccountRest]).
   */
  @AccessChecked("Admin group (checkIsLoggedInUserMemberOfAdminGroup)")
  @PostMapping("logoutAllDevicesOfUser")
  fun logoutAllDevicesOfUser(
    @RequestParam("userId", required = true) userId: Long,
    request: HttpServletRequest,
    @RequestBody postData: PostData<User>
  ): ResponseEntity<*> {
    log.info { "Trying to log out all stay-logged-in devices of user #$userId." }
    accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
    sessionCsrfService.validateCsrfToken(request, postData, "Logout of all devices")?.let { return it }
    stayLoggedInTokenDao.deleteAll(userId)
    postData.data.stayLoggedInDevices = 0
    postData.data.stayLoggedInLastAccessTimeAgo = null
    return UIToast.createToastResponseEntity(
      translate("login.stayLoggedIn.invalidateAllStayLoggedInSessions.successfullDeleted"),
      color = UIColor.SUCCESS,
      mutableMapOf("data" to postData.data),
      merge = true,
      targetType = TargetType.UPDATE,
    )
  }
}
