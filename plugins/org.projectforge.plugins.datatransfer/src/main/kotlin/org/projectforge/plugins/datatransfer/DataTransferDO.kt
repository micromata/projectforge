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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import de.micromata.genome.db.jpa.history.api.NoHistory
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.jcr.AttachmentsInfo
import org.projectforge.framework.persistence.api.Constants
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import org.projectforge.framework.utils.NumberHelper
import java.util.*
import javax.persistence.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(
  name = "t_plugin_datatransfer",
  indexes = [javax.persistence.Index(
    name = "idx_fk_t_plugin_datatransfer_tenant_id",
    columnList = "tenant_id"
  )]
)
open class DataTransferDO : AbstractBaseDO<Int>(), AttachmentsInfo {

  @PropertyInfo(i18nKey = "id")
  private var id: Int? = null

  @PropertyInfo(i18nKey = "plugins.datatransfer.areaName")
  @Field
  @get:Column(length = 100, name = "area_name", nullable = false)
  open var areaName: String? = null

  /**
   * These users have full read/write access.
   */
  @get:Column(name = "owner_ids", length = 4000, nullable = true)
  open var ownerIds: String? = null

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
  @Field
  @get:Column(length = Constants.LENGTH_TEXT)
  open var description: String? = null

  /**
   * Optional password for external access.
   */
  @PropertyInfo(i18nKey = "plugins.datatransfer.external.download.enabled", tooltip = "plugins.datatransfer.external.download.enabled.info")
  @get:Column(length = 100, name = "external_download_enabled")
  open var externalDownloadEnabled: Boolean? = null

  /**
   * Optional password for external access.
   */
  @PropertyInfo(i18nKey = "plugins.datatransfer.external.upload.enabled", tooltip = "plugins.datatransfer.external.upload.enabled.info")
  @get:Column(length = 100, name = "external_upload_enabled")
  open var externalUploadEnabled: Boolean? = null

  /**
   * Optional password for external access.
   */
  @PropertyInfo(i18nKey = "plugins.datatransfer.external.accessToken", tooltip = "plugins.datatransfer.external.accessToken.info")
  @get:Column(length = 100, name = "external_access_token")
  open var externalAccessToken: String? = null

  /**
   * Optional password for external access.
   */
  @PropertyInfo(i18nKey = "plugins.datatransfer.external.password", tooltip = "plugins.datatransfer.external.password.info")
  @get:Column(length = 100, name = "external_password")
  open var externalPassword: String? = null

  @PropertyInfo(
    i18nKey = "plugins.datatransfer.external.accessFailedCounter",
    tooltip = "plugins.datatransfer.external.accessFailedCounter.info"
  )
  @get:Column(name = "external_access_failed_counter")
  open var externalAccessFailedCounter: Int = 0

  /**
   * All attachments will be deleted automatically after the given days.
   */
  @PropertyInfo(i18nKey = "plugins.datatransfer.expiryDays", tooltip = "plugins.datatransfer.expiryDays.info")
  @get:Column(name = "expiry_days")
  open var expiryDays: Int? = null

  @JsonIgnore
  @Field
  @field:NoHistory
  @get:Column(length = 10000, name = "attachments_names")
  override var attachmentsNames: String? = null

  @JsonIgnore
  @Field
  @field:NoHistory
  @get:Column(length = 10000, name = "attachments_ids")
  override var attachmentsIds: String? = null

  @JsonIgnore
  @field:NoHistory
  @get:Column(length = 10000, name = "attachments_size")
  override var attachmentsSize: Int? = null

  @PropertyInfo(i18nKey = "attachment")
  @JsonIgnore
  @get:Column(length = 10000, name = "attachments_last_user_action")
  override var attachmentsLastUserAction: String? = null

  @Id
  @GeneratedValue
  @Column(name = "pk")
  override fun getId(): Int? {
    return id
  }

  override fun setId(id: Int?) {
    this.id = id
  }
}
