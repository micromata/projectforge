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

package org.projectforge.plugins.datatransfer

import com.fasterxml.jackson.annotation.JsonIgnore
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.projectforge.Constants
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.ToStringUtil.Companion.toJsonString
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.jcr.AttachmentsInfo
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import java.util.*
import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.projectforge.framework.persistence.history.NoHistory

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "t_plugin_datatransfer_area")
@NamedQueries(
  NamedQuery(
    name = DataTransferAreaDO.FIND_BY_EXTERNAL_ACCESS_TOKEN,
    query = "from DataTransferAreaDO where externalAccessToken=:externalAccessToken"
  ),
  NamedQuery(
    name = DataTransferAreaDO.FIND_PERSONAL_BOX,
    query = "from DataTransferAreaDO where areaName=:areaName and adminIds=:adminIds"
  )
)
open class DataTransferAreaDO : AbstractBaseDO<Long>(), AttachmentsInfo, IDataTransferArea {

  @get:Id
  @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
  @get:Column(name = "pk")
  @PropertyInfo(i18nKey = "id")
  override var id: Long? = null

  @PropertyInfo(i18nKey = "plugins.datatransfer.areaName")
  @FullTextField
  @get:Column(length = 100, name = "area_name", nullable = false)
  open var areaName: String? = null

  /**
   * These users have full read/write access.
   */
  @get:Column(name = "admin_ids", length = 4000, nullable = true)
  open var adminIds: String? = null

  /**
   * These observers get notifications on upload (internal and external) as well as on external downloads.
   */
  @get:Column(name = "observer_ids", length = 4000, nullable = true)
  open var observerIds: String? = null

  /**
   * Members of these groups have upload/download access.
   */
  @get:Column(name = "access_group_ids", length = 4000, nullable = true)
  open var accessGroupIds: String? = null

  /**
   * These users have upload/download access.
   */
  @get:Column(name = "access_user_ids", length = 4000, nullable = true)
  open var accessUserIds: String? = null

  @PropertyInfo(i18nKey = "plugins.datatransfer.description", tooltip = "plugins.datatransfer.description.info")
  @FullTextField
  @get:Column(length = Constants.LENGTH_TEXT)
  open var description: String? = null

  /**
   * Optional password for external access.
   */
  @PropertyInfo(
    i18nKey = "plugins.datatransfer.external.download.enabled",
    tooltip = "plugins.datatransfer.external.download.enabled.info"
  )
  @get:Column(length = 100, name = "external_download_enabled")
  override var externalDownloadEnabled: Boolean? = null

  /**
   * Optional password for external access.
   */
  @PropertyInfo(
    i18nKey = "plugins.datatransfer.external.upload.enabled",
    tooltip = "plugins.datatransfer.external.upload.enabled.info"
  )
  @get:Column(length = 100, name = "external_upload_enabled")
  override var externalUploadEnabled: Boolean? = null

  val externalAccessEnabled: Boolean
    @Transient
    get() = externalDownloadEnabled == true || externalUploadEnabled == true

  /**
   * Optional password for external access.
   */
  @PropertyInfo(
    i18nKey = "plugins.datatransfer.external.accessToken",
    tooltip = "plugins.datatransfer.external.accessToken.info"
  )
  @get:Column(length = 100, name = "external_access_token")
  override var externalAccessToken: String? = null

  /**
   * Optional password for external access.
   */
  @PropertyInfo(
    i18nKey = "plugins.datatransfer.external.password",
    tooltip = "plugins.datatransfer.external.password.info"
  )
  @get:Column(length = 100, name = "external_password")
  override var externalPassword: String? = null

  /**
   * All attachments will be deleted automatically after the given days.
   */
  @PropertyInfo(i18nKey = "plugins.datatransfer.expiryDays", tooltip = "plugins.datatransfer.expiryDays.info")
  @get:Column(name = "expiry_days")
  open var expiryDays: Int? = null

  /**
   * If configured, this area supports this max upload size in kilo bytes (but must be less or equal to the maximum size of the
   * data transfer tool itself.
   */
  @PropertyInfo(i18nKey = "plugins.datatransfer.maxUploadSize", tooltip = "plugins.datatransfer.maxUploadSize.info")
  @get:Column(name = "max_upload_size_kb")
  open var maxUploadSizeKB: Int? = null

  @get:Transient
  val capacity
    get() = (maxUploadSizeKB ?: DataTransferAreaDao.MAX_UPLOAD_SIZE_DEFAULT_VALUE_KB) * 2048L

  @JsonIgnore
  @FullTextField
  @NoHistory
  @get:Column(length = 10000, name = "attachments_names")
  override var attachmentsNames: String? = null

  @JsonIgnore
  @FullTextField
  @NoHistory
  @get:Column(length = 10000, name = "attachments_ids")
  override var attachmentsIds: String? = null

  @JsonIgnore
  @NoHistory
  @get:Column(name = "attachments_counter")
  override var attachmentsCounter: Int? = null

  /**
   * Size of all attachments in bytes.
   */
  @JsonIgnore
  @NoHistory
  @get:Column(name = "attachments_size")
  override var attachmentsSize: Long? = null

  @JsonIgnore
  @get:Transient
  override var attachmentsLastUserAction: String? = null

  @PropertyInfo(i18nKey = "plugin.datatransfer.externalAccess.logs")
  @JsonIgnore
  @get:Column(length = 10000, name = "external_access_logs")
  open var externalAccessLogs: String? = null

  /**
   * Only used for marking objects as modifiable internal objects.
   */
  @JsonIgnore
  @get:Transient
  internal var modifyPersonalBox: Boolean? = null

  @JsonIgnore
  @Transient
  fun isPersonalBox(): Boolean {
    return getPersonalBoxUserId() != null
  }

  @JsonIgnore
  @Transient
  fun getPersonalBoxUserId(): Long? {
    if (areaName != PERSONAL_BOX_AREA_NAME) {
      return null
    }
    return adminIds?.toLongOrNull()
  }

  val displayName: String
    @Transient
    get() = getDisplayName(null)

  fun getDisplayName(locale: Locale?): String {
    getPersonalBoxUserId()?.let {
      // This data transfer area is a personal box.
      val user = UserGroupCache.getInstance().getUser(it)
      return translateMsg(locale, "plugins.datatransfer.personalBox.title", "${user?.displayName}")
    }
    return areaName ?: "???"
  }

  /**
   * Hides field externalPassword due to security reasons.
   */
  override fun toString(): String {
    val clone = DataTransferAreaDO()
    clone.copyValuesFrom(this, "externalPassword")
    if (!externalPassword.isNullOrEmpty()) {
      clone.externalPassword = "***"
    }
    return toJsonString(clone)
  }

  companion object {
    internal const val FIND_BY_EXTERNAL_ACCESS_TOKEN = "DataTransferAreaDO_FindByExternalAccessToken"
    internal const val FIND_PERSONAL_BOX = "DataTransferAreaDO_FindPersonalBox"
    const val PERSONAL_BOX_AREA_NAME = "<PERSONAL_BOX>"
  }
}
