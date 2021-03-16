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
import org.projectforge.plugins.datatransfer.DataTransferDO
import org.projectforge.rest.dto.BaseDTO
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.User
import javax.persistence.Transient

class DataTransfer(
  id: Int? = null,
  var areaName: String? = null,
  var description: String? = null,
  var owners: List<User>? = null,
  var accessGroups: List<Group>? = null,
  var accessUsers: List<User>? = null,
  var externalDownloadEnabled: Boolean? = null,
  var externalUploadEnabled: Boolean? = null,
  var externalAccessToken: String? = null,
  var externalPassword: String? = null,
  var externalAccessFailedCounter: Int = -1,
  var expiryDays: Int? = null
) : BaseDTO<DataTransferDO>(id) {
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

  // The user and group ids are stored as csv list of integers in the data base.
  override fun copyFrom(src: DataTransferDO) {
    super.copyFrom(src)
    owners = User.toUserList(src.ownerIds)
    accessGroups = Group.toGroupList(src.accessGroupIds)
    accessUsers = User.toUserList(src.accessUserIds)
  }

  // The user and group ids are stored as csv list of integers in the data base.
  override fun copyTo(dest: DataTransferDO) {
    super.copyTo(dest)
    dest.ownerIds = User.toIntList(owners)
    dest.accessGroupIds = Group.toIntList(accessGroups)
    dest.accessUserIds = User.toIntList(accessUsers)
  }
}
