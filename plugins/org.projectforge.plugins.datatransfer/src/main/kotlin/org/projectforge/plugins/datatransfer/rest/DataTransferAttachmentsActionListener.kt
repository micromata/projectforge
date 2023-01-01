/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.group.service.GroupService
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.jcr.AttachmentsAccessChecker
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.plugins.datatransfer.DataTransferAreaDO
import org.projectforge.plugins.datatransfer.DataTransferAreaDao
import org.projectforge.rest.AttachmentsActionListener

/**
 * Listener on data transfer uploads to renew stats data.
 */
class DataTransferAttachmentsActionListener(
  attachmentsService: AttachmentsService,
  private val dataTransferAreaDao: DataTransferAreaDao,
  private val groupService: GroupService,
  private val userService: UserService,
) :
  AttachmentsActionListener(attachmentsService) {

  override fun createResponseData(
    obj: ExtendedBaseDO<Int>,
    jcrPath: String,
    attachmentsAccessChecker: AttachmentsAccessChecker,
    listId: String?
  ): Any {
    if (obj is DataTransferAreaDO) {
      val dbObj = dataTransferAreaDao.internalGetById(obj.id) // Get fresh db version.
      val area = DataTransferArea.transformFromDB(dbObj, dataTransferAreaDao, groupService, userService)
      area.attachments = attachmentsService.getAttachments(jcrPath, dbObj.id!!, attachmentsAccessChecker, listId)
      return area
    } else {
      // Shouldn't occur.
      return super.createResponseData(obj, jcrPath, attachmentsAccessChecker, listId)
    }
  }

  override fun createDownloadBasefileName(obj: Any): String {
    if (obj is DataTransferAreaDO) {
      return obj.displayName
    } else {
      // Shouldn't occur.
      return super.createDownloadBasefileName(obj)
    }
  }
}
