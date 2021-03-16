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

import org.projectforge.business.configuration.DomainService
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.model.rest.RestPaths
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

/**
 * This is the base data access object class. Most functionality such as access checking, select, insert, update, save,
 * delete etc. is implemented by the super class.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
open class DataTransferDao : BaseDao<DataTransferDO>(DataTransferDO::class.java) {

  @Autowired
  private lateinit var domainService: DomainService

  open fun createInitializedFile(): DataTransferDO {
    val file = DataTransferDO()
    file.fullAccessUserIds = "${ThreadLocalUserContext.getUserId()}"
    file.externalAccessToken = generateExternalAccessToken()
    file.externalPassword = generateExternalPassword()
    file.expiryDays = 7
    return file
  }

  open fun getExternalBaseLinkUrl(): String {
    return domainService.getDomain("${RestPaths.PUBLIC_REST}/datatransfer/")
  }

  override fun hasUserSelectAccess(user: PFUserDO?, throwException: Boolean): Boolean {
    return true // Select access in general for all registered users
  }

  override fun hasAccess(
    user: PFUserDO,
    obj: DataTransferDO,
    oldObj: DataTransferDO?,
    operationType: OperationType,
    throwException: Boolean
  ): Boolean {
    return true
    /*return accessChecker.isUserMemberOfAdminGroup(user) || user.id == obj.ownerId || userGroupCache.isUserMemberOfGroup(
      user,
      obj.ownerGroupId
    )*/
  }

  override fun newInstance(): DataTransferDO {
    return DataTransferDO()
  }

  companion object {
    fun generateExternalAccessToken(): String {
      return NumberHelper.getSecureRandomAlphanumeric(ACCESS_TOKEN_LENGTH)
    }

    fun generateExternalPassword(): String {
      return NumberHelper.getSecureRandomReducedAlphanumeric(PASSWORD_LENGTH)
    }

    private const val ACCESS_TOKEN_LENGTH = 50
    private const val PASSWORD_LENGTH = 6
  }
}
