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
import org.projectforge.framework.ToStringUtil
import org.projectforge.rest.config.RestUtils
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * A minimal session handling for avoiding annoying re-logins for external users of the data transfer tool.
 */
object DataTransferPublicSession {
  class TransferAreaData(
    var id: Int,
    var accessToken: String,
    @JsonIgnore var password: String?,
    var userInfo: String?,
    var ownedFiles: MutableList<String> = mutableListOf()
  )

  fun getTransferAreaData(request: HttpServletRequest, areaId: Int?): TransferAreaData? {
    areaId ?: return null
    val data = getSessionMap(request)?.entries?.find { it.value.id == areaId }?.value
    if (data != null) {
      log.info {
        "External user info restored from session: ${ToStringUtil.toJsonString(data)}, ip=${
          RestUtils.getClientIp(
            request
          )
        }"
      }
    }
    return data
  }

  fun getTransferAreaData(request: HttpServletRequest, accessToken: String?): TransferAreaData? {
    accessToken ?: return null
    val data = getSessionMap(request)?.entries?.find { it.value.accessToken == accessToken }?.value
    if (data != null) {
      log.info {
        "External user info restored from session: ${ToStringUtil.toJsonString(data)}, ip=${
          RestUtils.getClientIp(
            request
          )
        }"
      }
    }
    return data
  }

  fun register(request: HttpServletRequest, id: Int, accessToken: String, password: String, userInfo: String?) {
    @Suppress("UNCHECKED_CAST")
    var map = getSessionMap(request)
    if (map == null) {
      map = mutableMapOf()
      request.getSession(true).setAttribute(SESSION_ATTRIBUTE, map)
    }
    var data = map[id]
    if (data == null) {
      data = TransferAreaData(id, accessToken, password, userInfo)
      log.info { "External user logged-in: ${ToStringUtil.toJsonString(data)}, ip=${RestUtils.getClientIp(request)}" }
      map[id] = data
    } else {
      // Update values (if changed by re-login):
      data.accessToken = accessToken
      data.password = password
      data.userInfo = userInfo
    }
  }

  fun logout(request: HttpServletRequest) {
    val map = getSessionMap(request)
    if (map != null) {
      log.info { "External user logged-out: ${ToStringUtil.toJsonString(map)}, ip=${RestUtils.getClientIp(request)}" }
      request.session?.invalidate()
    }
  }

  /**
   * Checks if the user has uploaded the given file inside his session. If so, the user is the owner and has write access (update and delete).
   */
  fun isOwnerOfFile(request: HttpServletRequest, areaId: Int?, fileId: String?): Boolean {
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
  fun registerFileAsOwner(
    request: HttpServletRequest,
    areaId: Int?,
    fileId: String?,
    fileName: String?
  ) {
    areaId ?: return
    fileId ?: return
    val data = getTransferAreaData(request, areaId)
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
    synchronized(data.ownedFiles) {
      if (!data.ownedFiles.contains(fileId)) {
        log.info {
          "Mark external user as file owner inside his session: $areaId=$areaId, fileId=$fileId, name=$fileName, ip=${
            RestUtils.getClientIp(
              request
            )
          }"
        }
        data.ownedFiles.add(fileId)
      }
    }
  }

  private fun getSessionMap(request: HttpServletRequest): MutableMap<Int, TransferAreaData>? {
    @Suppress("UNCHECKED_CAST")
    val map: MutableMap<Int, TransferAreaData>? =
      request.session?.getAttribute(SESSION_ATTRIBUTE) as? MutableMap<Int, TransferAreaData>
    return map
  }

  internal const val SESSION_ATTRIBUTE = "transferAreas"
}
