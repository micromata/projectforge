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

package org.projectforge.plugins.datatransfer

import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.jcr.Attachment
import org.projectforge.framework.jcr.AttachmentsAccessChecker
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.jcr.FileObject

/**
 * Checks access to attachments by external anonymous users.
 */
open class DataTransferAccessChecker(
  val dataTransferAreaDao: DataTransferAreaDao
) : AttachmentsAccessChecker {
  override val fileSizeChecker: DataTransferFileSizeChecker =
    DataTransferFileSizeChecker(DataTransferAreaDao.globalMaxFileSize.toBytes())

  /**
   * @param id Data base id of data transfer area.
   * @param subPath Equals to listId.
   */
  override fun checkSelectAccess(user: PFUserDO?, path: String, id: Any, subPath: String?) {
    checkAccess(user, id)
  }

  /**
   * @param subPath Equals to listId.
   */
  override fun checkUploadAccess(user: PFUserDO?, path: String, id: Any, subPath: String?) {
    checkAccess(user, id) // Upload access is given, if select/download access is given
  }

  /**
   * @param subPath Equals to listId.
   */
  override fun checkDownloadAccess(user: PFUserDO?, path: String, id: Any, file: FileObject, subPath: String?) {
    val dbo = getDataTransferArea(user, id)
    if (dbo.isPersonalBox()) {
      if (user!!.id == dbo.getPersonalBoxUserId()) {
        // User has full access to his own personal box.
        return
      }
      val userId = file.createdByUser?.toIntOrNull()
      if (userId != user.id!!) {
        throw AccessException("plugins.datatransfer.external.noAccess")
      }
    } else {
      checkAccess(user, id)
    }
  }

  /**
   * Checks select/download-access (same). See [checkDownloadAccess] for special handling of perosnal boxes.
   */
  private fun checkAccess(user: PFUserDO?, id: Any) {
    user!!
    val dbo = getDataTransferArea(user, id)
    // No difference between download and upload access:
    dataTransferAreaDao.hasAccess(user, dbo, dbo, OperationType.SELECT, throwException = true)
  }

  private fun getDataTransferArea(user: PFUserDO?, id: Any): DataTransferAreaDO {
    user!!
    return dataTransferAreaDao.internalGetById(id as Int)
  }

  /**
   * @param subPath Equals to listId.
   */
  override fun checkUpdateAccess(user: PFUserDO?, path: String, id: Any, fileId: String, subPath: String?) {
    /*user!!
    val dbo = dataTransferAreaDao.internalGetById(id as Int)
    if (dataTransferAreaDao.hasAccess(user, dbo, dbo, OperationType.UPDATE, throwException = false)) {
      // User has update access.
      return
    }*/
    // Later: User has only update access to own attachments. For now: all users with dbo select access have also update access
    // of attachments.
    //dataTransferAreaDao.hasAccess(user, dbo, dbo, OperationType.SELECT, throwException = true)
    checkUploadAccess(user, path, id, subPath)
  }

  /**
   * @param subPath Equals to listId.
   */
  override fun checkDeleteAccess(user: PFUserDO?, path: String, id: Any, fileId: String, subPath: String?) {
    checkUpdateAccess(user, path, id, fileId, subPath)
  }

  override fun hasAccess(
    user: PFUserDO?,
    path: String,
    id: Any,
    subPath: String?,
    operationType: OperationType,
    attachment: Attachment
  ): Boolean {
    val dbo = dataTransferAreaDao.internalGetById(id as Int)
    if (!dbo.isPersonalBox()) {
      return true
    }
    user ?: return false // User must be given.
    if (user.id == dbo.getPersonalBoxUserId()) {
      // User is allowed to see all the attachments of his own personal box.
      return true
    }
    return user.id == attachment.createdByUserId // User can see only attachments of other personal boxes if they have uploaded them.
  }
}
