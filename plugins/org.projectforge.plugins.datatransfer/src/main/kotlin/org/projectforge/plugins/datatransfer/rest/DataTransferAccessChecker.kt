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

import mu.KotlinLogging
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.jcr.AttachmentsAccessChecker
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.datatransfer.DataTransferAreaDao

private val log = KotlinLogging.logger {}

/**
 * Checks access to attachments by external anonymous users.
 */
open class DataTransferAccessChecker(
  override val maxFileSize: Long,
  override val maxFileSizeSpringProperty: String,
  val dataTransferAreaDao: DataTransferAreaDao
) : AttachmentsAccessChecker {

  /**
   * @param subPath Equals to listId.
   */
  override fun checkSelectAccess(user: PFUserDO?, path: String, id: Any, subPath: String?) {
    user!!
    val dbo = dataTransferAreaDao.internalGetById(id as Int)
    dataTransferAreaDao.hasAccess(user, dbo, dbo, OperationType.SELECT, throwException = true)
  }

  /**
   * @param subPath Equals to listId.
   */
  override fun checkUploadAccess(user: PFUserDO?, path: String, id: Any, subPath: String?) {
    checkSelectAccess(user, path, id, subPath)
  }

  /**
   * @param subPath Equals to listId.
   */
  override fun checkDownloadAccess(user: PFUserDO?, path: String, id: Any, fileId: String, subPath: String?) {
    checkUploadAccess(user, path, id, subPath)
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
}
