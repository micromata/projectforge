/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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
    val dbo = getDataTransferArea(user, id)!!
    if (dataTransferAreaDao.getAreaHost(dbo) != null) {
      // Files come into a synthetic area only from its module (e.g. a message of the chat).
      throw AccessException(user, SYNTHETIC_AREA_READ_ONLY)
    }
    if (dataTransferAreaDao.externalAccessConfig.isOnGateway(dbo)) {
      // The files of an area with external access via the gateway are there, existing ones may still be deleted here.
      throw AccessException("plugins.datatransfer.gateway.files.uploadNotAllowed", dataTransferAreaDao.externalAccessConfig.gatewayPublicUrl)
    }
  }

  /**
   * @param subPath Equals to listId.
   */
  override fun checkDownloadAccess(user: PFUserDO?, path: String, id: Any, file: FileObject, subPath: String?) {
    val dbo = getDataTransferArea(user, id)
    dataTransferAreaDao.getAreaHost(dbo)?.let { host ->
      checkAccess(user, id)
      if (!host.isFileAccessible(dbo!!.id!!, file.fileId ?: "", user!!)) {
        throw AccessException(user, "access.exception.noAccess")
      }
      return
    }
    if (dbo!!.isPersonalBox()) {
      if (user!!.id == dbo.getPersonalBoxUserId()) {
        // User has full access to his own personal box.
        return
      }
      val userId = file.createdByUser?.toLongOrNull()
      if (userId != user.id!!) {
        throw AccessException("plugins.datatransfer.external.noAccess")
      }
    } else {
      checkAccess(user, id)
    }
  }

  /**
   * Checks select/download-access (same). See [checkDownloadAccess] for special handling of personal boxes.
   */
  private fun checkAccess(user: PFUserDO?, id: Any) {
    user!!
    val dbo = getDataTransferArea(user, id)
    // No difference between download and upload access:
    dataTransferAreaDao.hasAccess(user, dbo, dbo, OperationType.SELECT, throwException = true)
  }

  private fun getDataTransferArea(user: PFUserDO?, id: Any): DataTransferAreaDO? {
    user!!
    return dataTransferAreaDao.find(id as Long, checkAccess = false)
  }

  /**
   * @param subPath Equals to listId.
   */
  override fun checkUpdateAccess(user: PFUserDO?, path: String, id: Any, fileId: String, subPath: String?) {
    if (dataTransferAreaDao.getAreaHost(getDataTransferArea(user, id)) != null) {
      throw AccessException(user, SYNTHETIC_AREA_READ_ONLY) // The files are those of the module (e.g. a chat message).
    }
    /*user!!
    val dbo = dataTransferAreaDao.internalGetById(id as Long)
    if (dataTransferAreaDao.hasAccess(user, dbo, dbo, OperationType.UPDATE, throwException = false)) {
      // User has update access.
      return
    }*/
    // Later: User has only update access to own attachments. For now: all users with dbo select access have also update access
    // of attachments.
    //dataTransferAreaDao.hasAccess(user, dbo, dbo, OperationType.SELECT, throwException = true)
    checkAccess(user, id)
  }

  /**
   * @param subPath Equals to listId.
   */
  override fun checkDeleteAccess(user: PFUserDO?, path: String, id: Any, fileId: String, subPath: String?) {
    val dbo = getDataTransferArea(user, id)
    dataTransferAreaDao.getAreaHost(dbo)?.let { host ->
      checkAccess(user, id)
      if (!host.hasDeleteAccess(dbo!!.id!!, fileId, user!!)) {
        throw AccessException(user, "access.exception.noAccess")
      }
      return
    }
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
    return hasAccess(user, dataTransferAreaDao.find(id as Long, false)!!, attachment)
  }

  /**
   * The area is loaded once for all attachments, not once per attachment as [hasAccess] would do.
   */
  override fun filterAccessible(
    user: PFUserDO?,
    path: String,
    id: Any,
    subPath: String?,
    operationType: OperationType,
    attachments: List<Attachment>
  ): List<Attachment> {
    if (attachments.isEmpty()) {
      return attachments
    }
    val dbo = dataTransferAreaDao.find(id as Long, false)!!
    return attachments.filter { hasAccess(user, dbo, it) }
  }

  private fun hasAccess(user: PFUserDO?, dbo: DataTransferAreaDO, attachment: Attachment): Boolean {
    dataTransferAreaDao.getAreaHost(dbo)?.let { host ->
      return user != null && host.isFileAccessible(dbo.id!!, attachment.fileId ?: "", user)
    }
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

  companion object {
    private const val SYNTHETIC_AREA_READ_ONLY = "plugins.datatransfer.syntheticArea.readOnly"
  }
}
