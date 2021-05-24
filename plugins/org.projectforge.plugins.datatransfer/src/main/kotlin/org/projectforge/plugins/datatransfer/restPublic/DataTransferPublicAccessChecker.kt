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

package org.projectforge.plugins.datatransfer.restPublic

import org.projectforge.framework.access.OperationType
import org.projectforge.framework.jcr.Attachment
import org.projectforge.framework.jcr.AttachmentsAccessChecker
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.jcr.FileObject
import org.projectforge.plugins.datatransfer.DataTransferAreaDao
import org.projectforge.plugins.datatransfer.DataTransferFileSizeChecker
import org.projectforge.rest.config.RestUtils
import javax.servlet.http.HttpServletRequest

/**
 * Checks access to attachments by external anonymous users.
 */
open class DataTransferPublicAccessChecker(dataTransferAreaDao: DataTransferAreaDao) : AttachmentsAccessChecker {
  override val fileSizeChecker: DataTransferFileSizeChecker =
    DataTransferFileSizeChecker(dataTransferAreaDao.maxFileSize.toBytes())

  /**
   * If user has no download access, only attachments uploaded from own ip address should be displayed.
   */
  internal fun filterAttachments(
    request: HttpServletRequest,
    externalDownloadEnabled: Boolean?,
    attachments: List<Attachment>?
  ): List<Attachment>? {
    attachments ?: return null
    if (externalDownloadEnabled == true) {
      return attachments
    }
    val clientIp = RestUtils.getClientIp(request) ?: "NO IP ADDRESS GIVEN. CAN'T SHOW ANY ATTACHMENT."
    return attachments.filter { it.createdByUser?.contains(clientIp) == true }
  }

  /**
   * @param subPath Equals to listId.
   */
  override fun checkSelectAccess(user: PFUserDO?, path: String, id: Any, subPath: String?) {
  }

  /**
   * @param subPath Equals to listId.
   */
  override fun checkUploadAccess(user: PFUserDO?, path: String, id: Any, subPath: String?) {
  }

  /**
   * @param subPath Equals to listId.
   */
  override fun checkDownloadAccess(user: PFUserDO?, path: String, id: Any, file: FileObject, subPath: String?) {
  }

  /**
   * @param subPath Equals to listId.
   */
  override fun checkUpdateAccess(user: PFUserDO?, path: String, id: Any, fileId: String, subPath: String?) {
  }

  /**
   * @param subPath Equals to listId.
   */
  override fun checkDeleteAccess(user: PFUserDO?, path: String, id: Any, fileId: String, subPath: String?) {
  }

  override fun hasAccess(
    user: PFUserDO?,
    path: String,
    id: Any,
    subPath: String?,
    operationType: OperationType,
    attachment: Attachment
  ): Boolean {
    return true
  }
}
