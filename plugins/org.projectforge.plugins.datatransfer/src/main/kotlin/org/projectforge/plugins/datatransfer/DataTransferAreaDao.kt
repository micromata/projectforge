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

package org.projectforge.plugins.datatransfer

import mu.KotlinLogging
import org.projectforge.business.configuration.DomainService
import org.projectforge.business.login.LoginResultStatus
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.utils.SQLHelper
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.model.rest.RestPaths
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import org.springframework.util.unit.DataSize

private val log = KotlinLogging.logger {}

/**
 * This is the base data access object class. Most functionality such as access checking, select, insert, update, save,
 * delete etc. is implemented by the super class.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
open class DataTransferAreaDao : BaseDao<DataTransferAreaDO>(DataTransferAreaDO::class.java) {
  enum class AccessStatus { FAILED, SUCCESS, MAX_RETRIES_EXCEEDED }
  class AnonymousRequestResult(val dataTransferArea: DataTransferAreaDO?, val accessStatus: AccessStatus)

  @Autowired
  private lateinit var domainService: DomainService

  @Value("\${$MAX_FILE_SIZE_SPRING_PROPERTY:100MB}")
  open lateinit var maxFileSize: DataSize

  open fun createInitializedFile(): DataTransferAreaDO {
    val file = DataTransferAreaDO()
    file.adminIds = "${ThreadLocalUserContext.getUserId()}"
    file.externalAccessToken = generateExternalAccessToken()
    file.externalPassword = generateExternalPassword()
    file.expiryDays = 7
    return file
  }

  open fun getAnonymousArea(externalAccessToken: String?, externalPassword: String?): AnonymousRequestResult {
    if (externalAccessToken.isNullOrBlank() || externalPassword.isNullOrBlank()) {
      log.warn { "Can't get data transfer for external access. Token and/or Password is empty: token='$externalAccessToken', password='$externalPassword'" }
      return AnonymousRequestResult(null, AccessStatus.FAILED)
    }
    val dbo = SQLHelper.ensureUniqueResult(
      em.createNamedQuery(DataTransferAreaDO.FIND_BY_EXTERNAL_ACCESS_TOKEN, DataTransferAreaDO::class.java)
        .setParameter("externalAccessToken", externalAccessToken)
    )
    if (dbo == null) {
      log.warn { "Can't get data transfer for external access. External access token not found: token='$externalAccessToken'." }
      return AnonymousRequestResult(null, AccessStatus.FAILED)
    }
    if (dbo.externalDownloadEnabled != true && dbo.externalUploadEnabled != true) {
      log.warn { "Data transfer area was successfully requested but is not enabled for download and/or uploads: $dbo" }
      return AnonymousRequestResult(null, AccessStatus.FAILED)
    }
    if (dbo.externalAccessFailedCounter > MAX_EXTERNAL_ACCESS_RETRIES) {
      log.warn { "Can't get data transfer for external access. Maximum failed retries exceeded (must be reset by data transfer area admin user): $dbo." }
      dbo.externalAccessFailedCounter++
      internalUpdate(dbo)
      return AnonymousRequestResult(null, AccessStatus.MAX_RETRIES_EXCEEDED)
    }
    if (dbo.externalPassword != externalPassword) {
      log.warn { "Can't get data transfer for external access. External access password wrong: $dbo." }
      dbo.externalAccessFailedCounter++
      internalUpdate(dbo)
      return AnonymousRequestResult(null, AccessStatus.FAILED)
    }
    if (dbo.externalAccessFailedCounter > 0) {
      // Reset counter after successful login:
      dbo.externalAccessFailedCounter = 0
      internalUpdate(dbo)
    }
    val result = DataTransferAreaDO()
    result.id = dbo.id
    result.areaName = dbo.areaName
    result.description = dbo.description
    return AnonymousRequestResult(result, AccessStatus.SUCCESS)
  }

  open fun getExternalBaseLinkUrl(): String {
    return domainService.getDomain("${RestPaths.PUBLIC_REST}/datatransfer/")
  }

  override fun hasUserSelectAccess(user: PFUserDO?, throwException: Boolean): Boolean {
    return true // Select access in general for all registered users
  }

  override fun hasAccess(
    user: PFUserDO,
    obj: DataTransferAreaDO,
    oldObj: DataTransferAreaDO?,
    operationType: OperationType,
    throwException: Boolean
  ): Boolean {
    return true
    /*return accessChecker.isUserMemberOfAdminGroup(user) || user.id == obj.ownerId || userGroupCache.isUserMemberOfGroup(
      user,
      obj.ownerGroupId
    )*/
  }

  override fun newInstance(): DataTransferAreaDO {
    return DataTransferAreaDO()
  }

  companion object {
    fun generateExternalAccessToken(): String {
      return NumberHelper.getSecureRandomAlphanumeric(ACCESS_TOKEN_LENGTH)
    }

    fun generateExternalPassword(): String {
      return NumberHelper.getSecureRandomReducedAlphanumeric(PASSWORD_LENGTH)
    }

    const val MAX_EXTERNAL_ACCESS_RETRIES = 10
    const val MAX_FILE_SIZE_SPRING_PROPERTY = "projectforge.plugin.datatransfer.maxFileSize"
    private const val ACCESS_TOKEN_LENGTH = 30
    private const val PASSWORD_LENGTH = 6
  }
}
