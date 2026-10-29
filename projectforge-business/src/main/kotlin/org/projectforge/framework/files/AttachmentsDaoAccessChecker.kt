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

package org.projectforge.framework.files

import org.projectforge.framework.access.OperationType
import org.projectforge.framework.api.TechnicalException
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.files.FileObject
import org.projectforge.files.FileSizeChecker
import java.io.Serializable

/**
 * Checks access to attachments.
 */
open class AttachmentsDaoAccessChecker<O : ExtendedBaseDO<Long>>(
  val baseDao: BaseDao<O>,
  val attachmentsPath: String?,
  supportedListIds: Array<String>? = null,
  override val fileSizeChecker: FileSizeChecker
) : AttachmentsAccessChecker {

  val supportedListIds: Array<String> = if (supportedListIds == null) {
    DEFAULT_LIST_OF_ATTACHMENTS
  } else {
    DEFAULT_LIST_OF_ATTACHMENTS.plus(supportedListIds)
  }

  /**
   * @param subPath Equals to listId.
   */
  override fun checkSelectAccess(user: PFUserDO?, path: String, id: Any, subPath: String?) {
    checkNotNull(user)
    checkAttachmentsSupported(subPath)
    baseDao.find(id as Serializable)
      ?: throw TechnicalException(
        "No access for ${
          paramsToString(
            baseDao,
            path,
            id,
            "*",
            subPath
          )
        }. Object with id not found or without user access."
      )
  }

  /**
   * @param subPath Equals to listId.
   */
  override fun checkUploadAccess(user: PFUserDO?, path: String, id: Any, subPath: String?) {
    checkNotNull(user)
    checkAttachmentsSupported(subPath)
    val obj = baseDao.find(id as Serializable)
      ?: throw IllegalArgumentException(
        "No write access for ${
          paramsToString(
            baseDao,
            path,
            id,
            "*",
            subPath
          )
        }. Object with id not found or without user access."
      )
    if (!baseDao.hasUpdateAccess(user, obj, obj, false)) {
      throw TechnicalException("No write access for ${paramsToString(baseDao, path, id, "*", subPath)}.")
    }
  }

  /**
   * @param subPath Equals to listId.
   */
  override fun checkDownloadAccess(user: PFUserDO?, path: String, id: Any, file: FileObject, subPath: String?) {
    checkNotNull(user)
    checkAttachmentsSupported(subPath)
    checkSelectAccess(user, path, id, subPath)
  }

  /**
   * @param subPath Equals to listId.
   */
  override fun checkUpdateAccess(user: PFUserDO?, path: String, id: Any, fileId: String, subPath: String?) {
    checkNotNull(user)
    checkAttachmentsSupported(subPath)
    checkUploadAccess(user, path, id, subPath)
  }

  /**
   * @param subPath Equals to listId.
   */
  override fun checkDeleteAccess(user: PFUserDO?, path: String, id: Any, fileId: String, subPath: String?) {
    checkNotNull(user)
    checkAttachmentsSupported(subPath)
    checkUploadAccess(user, path, id, subPath)
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

  /**
   * Should be called on every check access method first!
   */
  fun checkAttachmentsSupported(listId: String? = null) {
    attachmentsPath
      ?: throw TechnicalException(
        "Attachments are not supported by this entity: ${baseDao.doClass.name}.",
        "You must specify attachmentsPath in yours *PagesRest."
      )
    if (listId != null && !supportedListIds.contains(listId)) {
      throw TechnicalException(
        "Attachments are not supported by entity ${baseDao.doClass.name} for list '$listId'.",
        "You must add this listId to supportedAttachmentList of yours *PagesRest."
      )
    }
  }

  private fun paramsToString(baseDao: BaseDao<*>, path: String, id: Any, fileId: String, subPath: String?): String {
    return "dao='${baseDao::class.java.name}', path='$path', id='$id', fileId='$fileId', subPath='$subPath'"
  }

  companion object {
    private val DEFAULT_LIST_OF_ATTACHMENTS = arrayOf(AttachmentsService.DEFAULT_NODE)
  }
}

