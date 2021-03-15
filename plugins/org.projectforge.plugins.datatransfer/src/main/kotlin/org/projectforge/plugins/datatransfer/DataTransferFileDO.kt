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

import com.fasterxml.jackson.annotation.JsonProperty
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.Constants
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
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
  name = "t_plugin_datatransfer_file",
  indexes = [javax.persistence.Index(
    name = "idx_fk_t_plugin_datatransfer_file_owner_fk",
    columnList = "owner_fk"
  ), javax.persistence.Index(
    name = "idx_fk_t_plugin_datatransfer_file_owner_group_fk",
    columnList = "owner_group_fk"
  ), javax.persistence.Index(
    name = "idx_fk_t_plugin_datatransfer_file_tenant_id",
    columnList = "tenant_id"
  )]
)
open class DataTransferFileDO : AbstractBaseDO<Int>() {

  @PropertyInfo(i18nKey = "id")
  private var id: Int? = null

  @PropertyInfo(i18nKey = "plugins.datatransfer.file.name")
  @Field
  @get:Column(length = 100)
  open var filename: String? = null

  @PropertyInfo(i18nKey = "plugins.datatransfer.file.owner")
  @get:ManyToOne(fetch = FetchType.LAZY)
  @get:JoinColumn(name = "owner_fk")
  open var owner: PFUserDO? = null

  @PropertyInfo(i18nKey = "plugins.datatransfer.file.ownerGroup")
  @get:ManyToOne(fetch = FetchType.LAZY)
  @get:JoinColumn(name = "owner_group_fk")
  open var ownerGroup: GroupDO? = null

  @PropertyInfo(i18nKey = "plugins.datatransfer.file.comment", tooltip = "plugins.datatransfer.file.comment.info")
  @Field
  @get:Column(length = Constants.LENGTH_TEXT)
  open var comment: String? = null

  /**
   * Optional password for external access.
   */
  @PropertyInfo(i18nKey = "plugins.datatransfer.file.accessToken")
  @get:Column(length = 100, name = "access_token")
  open var accessToken: String? = null

  /**
   * Link for external users.
   */
  //@PropertyInfo(i18nKey = "plugins.datatransfer.file.externaLink", tooltip = "plugins.datatransfer.file.externaLink")
  val externalLink
    @JsonProperty
    @Transient
    get() = "$externalLinkBaseUrl$accessToken"

  @get:Transient
  var externalLinkBaseUrl: String? = null

  /**
   * Optional password for external access.
   */
  @PropertyInfo(i18nKey = "plugins.datatransfer.file.password", tooltip = "plugins.datatransfer.file.password.info")
  @get:Column(length = 100)
  open var password: String? = null

  @PropertyInfo(
    i18nKey = "plugins.datatransfer.file.accessFailedCounter",
    tooltip = "plugins.datatransfer.file.accessFailedCounter.info"
  )
  @get:Column(name = "access_failed_counter")
  open var accessFailedCounter: Int = 0

  @PropertyInfo(i18nKey = "plugins.datatransfer.file.validUntil", tooltip = "plugins.datatransfer.file.validUntil.info")
  @get:Column(name = "valid_until")
  open var validUntil: Date? = null

  fun renewAccessToken() {
    accessToken = generateAccessToken()
  }

  fun renewPassword() {
    password = generatePassword()
  }

  val ownerId: Int?
    @Transient
    get() = owner?.id

  val ownerGroupId: Int?
    @Transient
    get() = ownerGroup?.id


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
    fun generateAccessToken(): String {
      return NumberHelper.getSecureRandomAlphanumeric(ACCESS_TOKEN_LENGTH)
    }

    fun generatePassword(): String {
      return NumberHelper.getSecureRandomReducedAlphanumeric(PASSWORD_LENGTH)
    }

    private const val ACCESS_TOKEN_LENGTH = 50
    private const val PASSWORD_LENGTH = 6
  }
}
