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
 * This data object is the Java representation of a data-base entry of a memo.<br></br>
 * Changes of this object will not be added to the history of changes. After deleting a memo it will be deleted in the
 * data-base (there is no undo!).<br></br>
 * If you want to use the history of changes and undo functionality please use DefaultBaseDO as super class instead of
 * AbstractBaseDO. .
 *
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
  @get:Column(length = 100, name = "area_name")
  open var areaName: String? = null

  /**
   * Members of these groups have full read/write access.
   */
  @get:Column(name = "full_access_group_ids", length = 4000, nullable = true)
  open var fullAccessGroupIds: String? = null

  /**
   * These users have full read/write access.
   */
  @get:Column(name = "full_access_user_ids", length = 4000, nullable = true)
  open var fullAccessUserIds: String? = null

  /**
   * Members of these groups have full read-only access.
   */
  @get:Column(name = "readonly_access_group_ids", length = 4000, nullable = true)
  open var readonlyAccessGroupIds: String? = null

  /**
   * These users have read-only access.
   */
  @get:Column(name = "readonly_access_user_ids", length = 4000, nullable = true)
  open var readonlyAccessUserIds: String? = null

  @PropertyInfo(i18nKey = "plugins.datatransfer.comment", tooltip = "plugins.datatransfer.comment.info")
  @Field
  @get:Column(length = Constants.LENGTH_TEXT)
  open var comment: String? = null

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
   * Link for external users.
   */
  //@PropertyInfo(i18nKey = "plugins.datatransfer.external.link", tooltip = "plugins.datatransfer.external.link.info")
  val externalLink
    @JsonProperty
    @Transient
    get() = "$externalLinkBaseUrl$externalAccessToken"

  @get:Transient
  var externalLinkBaseUrl: String? = null

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

  fun renewAccessToken() {
    externalAccessToken = generateExternalAccessToken()
  }

  fun renewPassword() {
    externalPassword = generateExternalPassword()
  }

  @Id
  @GeneratedValue
  @Column(name = "pk")
  override fun getId(): Int? {
    return id
  }

  override fun setId(id: Int?) {
    this.id = id
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
