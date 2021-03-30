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

package org.projectforge.plugins.datatransfer.rest

import com.fasterxml.jackson.annotation.JsonProperty
import org.projectforge.common.FormatterUtils
import org.projectforge.framework.i18n.TimeAgo
import org.projectforge.framework.jcr.Attachment
import org.projectforge.plugins.datatransfer.DataTransferAreaDO
import org.projectforge.rest.dto.AttachmentsSupport
import org.projectforge.rest.dto.BaseDTO
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.User
import javax.persistence.Transient

class DataTransferArea(
  id: Int? = null,
  var areaName: String? = null,
  var description: String? = null,
  var admins: List<User>? = null,
  var adminsAsString: String? = null,
  var observers: List<User>? = null,
  var observersAsString: String? = null,
  var accessGroups: List<Group>? = null,
  var accessGroupsAsString: String? = null,
  var accessGroupsUsesAsString: String? = null,
  var accessUsers: List<User>? = null,
  var accessUsersAsString: String? = null,
  var externalDownloadEnabled: Boolean? = null,
  var externalUploadEnabled: Boolean? = null,
  var externalAccessToken: String? = null,
  var externalPassword: String? = null,
  var expiryDays: Int? = null,
  var maxUploadSizeKB: Int? = null,
  var internalLink: String? = null,
  override var attachmentsCounter: Int? = null,
  override var attachmentsSize: Long? = null,
  /**
   * Needed for updating UILayout for watchfields.
   */
  var layoutUid: String? = null
) : BaseDTO<DataTransferAreaDO>(id), AttachmentsSupport {
  override var attachments: List<Attachment>? = null

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

  val externalAccessEnabled
    @JsonProperty
    @Transient
    get() = externalDownloadEnabled == true || externalUploadEnabled == true

  val lastUpdateTimeAgo
    @JsonProperty
    @Transient
    get() = TimeAgo.getMessage(lastUpdate)

  /**
   * The number and soue of attachments attached to this data object.
   */
  val maxUploadSizeFormatted: String?
    @JsonProperty
    @Transient
    get() {
      return maxUploadSizeKB?.let {
        FormatterUtils.formatBytes(1024L * it)
      }
    }


  // The user and group ids are stored as csv list of integers in the data base.
  override fun copyFrom(src: DataTransferAreaDO) {
    super.copyFrom(src)
    admins = User.toUserList(src.adminIds)
    observers = User.toUserList(src.observerIds)
    accessGroups = Group.toGroupList(src.accessGroupIds)
    accessUsers = User.toUserList(src.accessUserIds)
  }

  // The user and group ids are stored as csv list of integers in the data base.
  override fun copyTo(dest: DataTransferAreaDO) {
    super.copyTo(dest)
    dest.adminIds = User.toIntList(admins)
    dest.observerIds = User.toIntList(observers)
    dest.accessGroupIds = Group.toIntList(accessGroups)
    dest.accessUserIds = User.toIntList(accessUsers)
  }
}
