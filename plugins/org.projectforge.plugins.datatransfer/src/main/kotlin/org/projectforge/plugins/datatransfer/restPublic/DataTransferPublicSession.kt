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

package org.projectforge.plugins.datatransfer.restPublic

import com.fasterxml.jackson.annotation.JsonIgnore
import mu.KotlinLogging
import org.projectforge.business.login.LoginProtection
import org.projectforge.business.login.LoginResultStatus
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.i18n.translate
import org.projectforge.plugins.datatransfer.DataTransferAreaDO
import org.projectforge.plugins.datatransfer.DataTransferAreaDao
import org.projectforge.rest.config.RestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * A minimal session handling for avoiding annoying re-logins for external users of the data transfer tool.
 */
@Service
class DataTransferPublicSession {
  internal class TransferAreaData(
    var id: Int,
    var accessToken: String,
    @JsonIgnore var password: String?,
    var userInfo: String?,
    var ownedFiles: MutableList<String> = mutableListOf()
  )

  internal class CheckAccessResult(
    val dataTransferArea: DataTransferAreaDO? = null,
    val failedAccessMessage: String? = null
  )


  @Autowired
  private lateinit var dataTransferAreaDao: DataTransferAreaDao

  /**
   * Checks if the user has a valid entry (accessToken/password) in his session.
   * @param areaId Search area by id in user's session.
   * @param accessToken Search area by accessToken in user's session.
   */
  internal fun checkLogin(
    request: HttpServletRequest,
    areaId: Int? = null,
    accessToken: String? = null
  ): Pair<DataTransferAreaDO, TransferAreaData>? {
    check(areaId != null || !accessToken.isNullOrBlank())
    val data = if (areaId != null) {
      getSessionMap(request)?.get(areaId)
    } else {
      getSessionMap(request)?.entries?.find { it.value.accessToken == accessToken }?.value
    } ?: return null
    val area = dataTransferAreaDao.getAnonymousArea(data.accessToken) ?: return null
    val errorMessage = checkDataBaseEntry(request, area, data.id, data.accessToken, data.password, data.userInfo)
    if (errorMessage != null) {
      unregister(request, data.id) // Unregister, force re-login.
      return null
    }
    log.info {
      "External user info restored from session: ${ToStringUtil.toJsonString(data)}, ip=${
        RestUtils.getClientIp(
          request
        )
      }"
    }
    return Pair(area, data)
  }

  /**
   * Tries to log-in the user. Uses LoginProtection. Doesn't check if the user is already logged-in.
   * The session id will be changed (session fixation), but any previous logged-in area will be put in the new session.
   */
  internal fun login(
    request: HttpServletRequest,
    accessToken: String?,
    password: String?,
    userInfo: String?
  ): CheckAccessResult {
    if (accessToken == null || password == null) {
      return CheckAccessResult(failedAccessMessage = LoginResultStatus.FAILED.localizedMessage)
    }
    val loginProtection = LoginProtection.instance()
    val clientIpAddress = RestUtils.getClientIp(request)
    val offset = loginProtection.getFailedLoginTimeOffsetIfExists(accessToken, clientIpAddress)
    if (offset > 0) {
      // Time offset still exists. Ignore login try.
      val seconds = (offset / 1000).toString()
      log.warn("The account for '${accessToken}', ip=$clientIpAddress, userInfo='$userInfo' is locked for $seconds seconds due to failed login attempts. Please try again later.")
      val numberOfFailedAttempts = loginProtection.getNumberOfFailedLoginAttempts(accessToken, clientIpAddress)
      val loginResultStatus = LoginResultStatus.LOGIN_TIME_OFFSET
      loginResultStatus.setMsgParams(
        seconds,
        numberOfFailedAttempts.toString()
      )
      return CheckAccessResult(failedAccessMessage = loginResultStatus.localizedMessage)
    }

    val dbo = dataTransferAreaDao.getAnonymousArea(accessToken)
    if (dbo == null) {
      log.warn { "Data transfer area with externalAccessToken '$accessToken' not found. Requested by ip=$clientIpAddress, userInfo='$userInfo'." }
      loginProtection.incrementFailedLoginTimeOffset(accessToken, clientIpAddress)
      return CheckAccessResult(failedAccessMessage = LoginResultStatus.FAILED.localizedMessage)
    }
    val errorMessage = checkDataBaseEntry(request, dbo, dbo.id!!, accessToken, password, userInfo)
    if (errorMessage != null) {
      loginProtection.incrementFailedLoginTimeOffset(accessToken, clientIpAddress)
      return CheckAccessResult(failedAccessMessage = errorMessage)
    }
    // Successfully logged in:
    loginProtection.clearLoginTimeOffset(accessToken, null, clientIpAddress)
    log.info { "Data transfer area with externalAccessToken '$accessToken': login successful by ip=$clientIpAddress, userInfo='$userInfo'." }

    // Session Fixation: Change JSESSIONID after login (due to security reasons / XSS attack on login page)
    request.getSession(false)?.let { session ->
      if (!session.isNew) {
        val map = getSessionMap(request)
        session.invalidate()
        if (map != null) {
          // Save any logged-in areas from old session and restore in nes session:
          request.getSession(true).setAttribute(SESSION_ATTRIBUTE, map)
        }
      }
    }
    register(request, dbo, userInfo)
    return CheckAccessResult(dbo)
  }

  private fun checkDataBaseEntry(
    request: HttpServletRequest,
    dbo: DataTransferAreaDO,
    areaId: Int,
    accessToken: String?,
    password: String?,
    userInfo: String?
  ): String? {
    if (dbo.isPersonalBox()) {
      log.warn {
        "Paranoia setting: no external access of personal boxes (of user with id=${dbo.adminIds}). Requested by ip=${
          RestUtils.getClientIp(request)
        }, userInfo='$userInfo'."
      }
      return LoginResultStatus.FAILED.localizedMessage
    }
    // Check the matching of all params (protect against cross area access)
    if (dbo.id != areaId || dbo.externalPassword != password || dbo.externalAccessToken != accessToken) {
      log.warn {
        "Data transfer area with externalAccessToken '$accessToken' doesn't match given area id, password and/or accessToken. Requested by ip=${
          RestUtils.getClientIp(request)
        }, userInfo='$userInfo'."
      }
      return LoginResultStatus.FAILED.localizedMessage
    }
    if (dbo.externalUploadEnabled != true && dbo.externalDownloadEnabled != true) {
      return translate("plugins.datatransfer.external.noAccess")
    }
    return null
  }

  private fun register(request: HttpServletRequest, area: DataTransferAreaDO, userInfo: String?) {
    @Suppress("UNCHECKED_CAST")
    var map = getSessionMap(request)
    if (map == null) {
      map = mutableMapOf()
      request.getSession(true).setAttribute(SESSION_ATTRIBUTE, map)
    }
    val id = area.id!!
    var data = map[id]
    if (data == null) {
      data = TransferAreaData(id, area.externalAccessToken!!, area.externalPassword, userInfo)
      log.info { "External user logged-in: ${ToStringUtil.toJsonString(data)}, ip=${RestUtils.getClientIp(request)}" }
      map[id] = data
    } else {
      // Update values (if changed by re-login):
      data.accessToken = area.externalAccessToken!!
      data.password = area.externalPassword
      data.userInfo = userInfo
    }
  }

  private fun unregister(request: HttpServletRequest, areaId: Int) {
    getSessionMap(request)?.remove(areaId)
  }

  internal fun logout(request: HttpServletRequest) {
    val map = getSessionMap(request)
    if (map != null) {
      log.info { "External user logged-out: ${ToStringUtil.toJsonString(map)}, ip=${RestUtils.getClientIp(request)}" }
      request.session?.invalidate()
    }
  }

  /**
   * Checks if the user has uploaded the given file inside his session. If so, the user is the owner and has write access (update and delete).
   */
  internal fun isOwnerOfFile(request: HttpServletRequest, areaId: Int?, fileId: String?): Boolean {
    areaId ?: return false
    fileId ?: return false
    val data = getSessionMap(request)?.get(areaId) ?: return false
    log.info {
      "External user info restored from session: ${ToStringUtil.toJsonString(data)}, ip=${
        RestUtils.getClientIp(
          request
        )
      }"
    }
    return data.ownedFiles.contains(fileId)
  }

  /**
   * Called directly after uploading a new file. Marks this session user as owner for write access inside this session.
   */
  internal fun registerFileAsOwner(
    request: HttpServletRequest,
    areaId: Int?,
    fileId: String?,
    fileName: String?
  ) {
    areaId ?: return
    fileId ?: return
    val data = checkLogin(request, areaId)
    if (data == null) {
      log.warn {
        "Can't restore external user info from session: $areaId=$areaId, ip=${
          RestUtils.getClientIp(
            request
          )
        }. So can't register file's owner."
      }
      return
    }
    synchronized(data.second.ownedFiles) {
      if (!data.second.ownedFiles.contains(fileId)) {
        log.info {
          "Mark external user as file owner inside his session: $areaId=$areaId, fileId=$fileId, name=$fileName, ip=${
            RestUtils.getClientIp(
              request
            )
          }"
        }
        data.second.ownedFiles.add(fileId)
      }
    }
  }

  private fun getSessionMap(request: HttpServletRequest): MutableMap<Int, TransferAreaData>? {
    @Suppress("UNCHECKED_CAST")
    val map: MutableMap<Int, TransferAreaData>? =
      request.session?.getAttribute(SESSION_ATTRIBUTE) as? MutableMap<Int, TransferAreaData>
    return map
  }

  companion object {
    internal const val SESSION_ATTRIBUTE = "transferAreas"
  }
}
