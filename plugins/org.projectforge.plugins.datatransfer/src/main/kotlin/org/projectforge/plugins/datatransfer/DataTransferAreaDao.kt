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
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.DataSizeConfig
import org.projectforge.common.StringHelper
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.jcr.AttachmentsEventListener
import org.projectforge.framework.jcr.AttachmentsEventType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.utils.SQLHelper
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.jcr.FileInfo
import org.projectforge.rest.core.RestResolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import org.springframework.util.unit.DataSize
import org.springframework.util.unit.DataUnit
import javax.annotation.PostConstruct

private val log = KotlinLogging.logger {}

/**
 * This is the base data access object class. Most functionality such as access checking, select, insert, update, save,
 * delete etc. is implemented by the super class.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
open class DataTransferAreaDao : BaseDao<DataTransferAreaDO>(DataTransferAreaDO::class.java), AttachmentsEventListener {
  @Autowired
  private lateinit var notificationMailService: NotificationMailService

  @Value("\${${MAX_FILE_SIZE_SPRING_PROPERTY}:100MB}")
  internal open lateinit var maxFileSizeConfig: String

  open lateinit var maxFileSize: DataSize
    internal set

  @PostConstruct
  private fun postConstruct() {
    maxFileSize = DataSizeConfig.init(maxFileSizeConfig, DataUnit.MEGABYTES)
    log.info { "Maximum configured size of uploads: ${MAX_FILE_SIZE_SPRING_PROPERTY}=$maxFileSizeConfig." }
  }

  @Autowired
  private lateinit var domainService: DomainService

  open fun createInitializedFile(): DataTransferAreaDO {
    val file = DataTransferAreaDO()
    file.adminIds = "${ThreadLocalUserContext.getUserId()}"
    file.observerIds = file.adminIds
    file.externalAccessToken = generateExternalAccessToken()
    file.externalPassword = generateExternalPassword()
    file.expiryDays = 7
    file.maxUploadSizeKB = MAX_UPLOAD_SIZE_DEFAULT_VALUE // 100MB
    return file
  }

  override fun afterLoad(obj: DataTransferAreaDO) {
    if (obj.maxUploadSizeKB == null)
      obj.maxUploadSizeKB = MAX_UPLOAD_SIZE_DEFAULT_VALUE
  }

  open fun getAnonymousArea(externalAccessToken: String?): DataTransferAreaDO? {
    val dbo = SQLHelper.ensureUniqueResult(
      em.createNamedQuery(DataTransferAreaDO.FIND_BY_EXTERNAL_ACCESS_TOKEN, DataTransferAreaDO::class.java)
        .setParameter("externalAccessToken", externalAccessToken)
    ) ?: return null
    return dbo
  }

  open fun getExternalBaseLinkUrl(): String {
    return domainService.getDomain("${RestResolver.REACT_PUBLIC_PATH}/datatransfer/dynamic/")
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
    val adminIds = StringHelper.splitToIntegers(obj.adminIds, ",")
    if (adminIds.contains(user.id)) {
      return true
    }
    if (operationType == OperationType.SELECT) {
      em.detach(obj)
      obj.externalAccessToken = null
      obj.externalPassword = null
      obj.externalAccessLogs = null
      // Select access also for those users:
      StringHelper.splitToIntegers(obj.accessUserIds, ",")?.let {
        if (it.contains(user.id)) {
          return true
        }
      }
      StringHelper.splitToIntegers(obj.accessGroupIds, ",")?.let {
        if (UserGroupCache.tenantInstance.isUserMemberOfAtLeastOneGroup(user.id, *it)) {
          return true
        }
      }
    }
    if (throwException) {
      throw AccessException(user, "access.exception.userHasNotRight")
    }
    return false
  }

  override fun getDefaultSortProperties(): Array<SortProperty> {
    return arrayOf(SortProperty.desc("lastUpdate"))
  }

  override fun newInstance(): DataTransferAreaDO {
    return DataTransferAreaDO()
  }

  override fun onAttachmentEvent(
    event: AttachmentsEventType,
    file: FileInfo,
    data: Any?,
    byUser: PFUserDO?,
    byExternalUser: String?
  ) {
    check(data != null)
    try {
      if (byUser != null && event == AttachmentsEventType.DOWNLOAD) {
          // Do not notify on downloads and deletions of internal users.
          return
      }
      // log download access of external users.
      if (!byExternalUser.isNullOrBlank() && event == AttachmentsEventType.DOWNLOAD) {
        // internalUpdateAny(data) // Must call update. On upload event, the data will stored by caller.
      }
      notificationMailService.sendMail(event, file, data as DataTransferAreaDO, byUser, byExternalUser)

    } catch (ex: Exception) {
      log.error("Exception while calling SendMailService: ${ex.message}.", ex)
    }
  }

  companion object {
    fun generateExternalAccessToken(): String {
      return NumberHelper.getSecureRandomAlphanumeric(ACCESS_TOKEN_LENGTH)
    }

    fun generateExternalPassword(): String {
      return NumberHelper.getSecureRandomReducedAlphanumeric(PASSWORD_LENGTH)
    }

    const val MAX_FILE_SIZE_SPRING_PROPERTY = "projectforge.plugin.datatransfer.maxFileSize"
    val EXPIRY_DAYS_VALUES = "plugins.datatransfer.expiryDays".let {
      mapOf(
        1 to "$it.day",
        3 to "$it.days",
        7 to "$it.days",
        14 to "$it.days",
        30 to "$it.days",
        60 to "$it.days",
        90 to "$it.days",
        180 to "$it.days",
        365 to "$it.days"
      )
    }
    private const val MB = 1024

    val MAX_UPLOAD_SIZE_VALUES =
      arrayOf(20 * MB, 50 * MB, 100 * MB, 200 * MB, 500 * MB, 1024 * MB, 1536 * MB, 2048 * MB, 3072 * MB)

    private const val MAX_UPLOAD_SIZE_DEFAULT_VALUE = 100 * MB

    const val ACCESS_TOKEN_LENGTH = 30
    const val PASSWORD_LENGTH = 6
  }
}
