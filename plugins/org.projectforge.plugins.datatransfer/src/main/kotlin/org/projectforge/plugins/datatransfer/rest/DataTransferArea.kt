/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.i18n.TimeAgo
import org.projectforge.framework.jcr.Attachment
import org.projectforge.plugins.datatransfer.DataTransferAreaCapacity
import org.projectforge.plugins.datatransfer.DataTransferAreaDO
import org.projectforge.plugins.datatransfer.DataTransferAreaDao
import org.projectforge.plugins.datatransfer.IDataTransferArea
import org.projectforge.rest.dto.AttachmentsSupport
import org.projectforge.rest.dto.BaseDTO
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.User
import java.util.*
import javax.persistence.Transient

class DataTransferArea(
  id: Int? = null,
  var areaName: String? = null,
  var description: String? = null,
  var admins: List<User>? = null,
  var adminsAsString: String? = null,
  var observers: List<User>? = null,
  var observersAsString: String? = null,
  /**
   * Checked, if the logged-in-user observes this area. Used by [DataTransferRest].
   */
  var userWantsToObserve: Boolean? = null,
  var accessGroups: List<Group>? = null,
  var accessGroupsAsString: String? = null,
  var accessGroupsUsesAsString: String? = null,
  var accessUsers: List<User>? = null,
  var accessUsersAsString: String? = null,
  override var externalDownloadEnabled: Boolean? = null,
  override var externalUploadEnabled: Boolean? = null,
  override var externalAccessToken: String? = null,
  override var externalPassword: String? = null,
  var expiryDays: Int? = null,
  var maxUploadSizeKB: Int? = null,
  var internalLink: String? = null,
  var personalBox: Boolean? = null,
  override var attachmentsCounter: Int? = null,
  override var attachmentsSize: Long? = null,
  /**
   * Needed for updating UILayout for watchfields.
   */
  var layoutUid: String? = null,
) : BaseDTO<DataTransferAreaDO>(id), AttachmentsSupport, IDataTransferArea {
  override var attachments: List<Attachment>? = null
    set(value) {
      // Replace #EXTERNAL# by translated marker:
      value?.forEach { attachment ->
        attachment.createdByUser = DataTransferAreaDao.getTranslatedUserString(null, attachment.createdByUser)
        attachment.lastUpdateByUser = DataTransferAreaDao.getTranslatedUserString(null, attachment.lastUpdateByUser)
      }
      field = value
    }

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

  var capacity: DataTransferAreaCapacity? = null

  /**
   * The external password isn't copied due to security reasons.
   */
  // The user and group ids are stored as csv list of integers in the data base.
  override fun copyFrom(src: DataTransferAreaDO) {
    super.copyFrom(src)
    src.getPersonalBoxUserId()?.let {
      // This data transfer area is a personal box.
      areaName = src.displayName
      personalBox = true
    }
    admins = User.toUserList(src.adminIds)
    observers = User.toUserList(src.observerIds)
    accessGroups = Group.toGroupList(src.accessGroupIds)
    accessUsers = User.toUserList(src.accessUserIds)
    externalPassword = null
  }

  // The user and group ids are stored as csv list of integers in the data base.
  override fun copyTo(dest: DataTransferAreaDO) {
    super.copyTo(dest)
    if (personalBox == true) {
      dest.areaName = DataTransferAreaDO.PERSONAL_BOX_AREA_NAME // Restore db specific name.
    } else {
      dest.adminIds = User.toIntList(admins)
      dest.observerIds = User.toIntList(observers)
      dest.accessGroupIds = Group.toIntList(accessGroups)
      dest.accessUserIds = User.toIntList(accessUsers)
    }
  }

  companion object {
    /**
     * externalPassword will not be copied due to security reasons.
     */
    fun transformFromDB(
      obj: DataTransferAreaDO,
      dataTransferAreaDao: DataTransferAreaDao,
      groupService: GroupService,
      userService: UserService,
    ): DataTransferArea {
      val dto = DataTransferArea()
      dto.copyFrom(obj)
      dto.externalLinkBaseUrl = dataTransferAreaDao.getExternalBaseLinkUrl()

      // Group names needed by React client (for ReactSelect):
      Group.restoreDisplayNames(dto.accessGroups, groupService)

      // Usernames needed by React client (for ReactSelect):
      User.restoreDisplayNames(dto.admins, userService)
      User.restoreDisplayNames(dto.observers, userService)
      User.restoreDisplayNames(dto.accessUsers, userService)

      dto.adminsAsString = dto.admins?.joinToString { it.displayName ?: "???" } ?: ""
      dto.observersAsString = dto.observers?.joinToString { it.displayName ?: "???" } ?: ""
      dto.accessGroupsAsString = dto.accessGroups?.joinToString { it.displayName ?: "???" } ?: ""
      dto.accessUsersAsString = dto.accessUsers?.joinToString { it.displayName ?: "???" } ?: ""
      dto.capacity = DataTransferAreaCapacity(obj.attachmentsSize, obj.capacity, obj.maxUploadSizeKB)
      return dto
    }
  }
}
