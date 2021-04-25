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

package org.projectforge.framework.jcr

import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.jcr.FileSizeChecker


/**
 * Checks access to attachments.
 */
interface AttachmentsAccessChecker {
  /**
   * For checking the maximum size.
   */
  val fileSizeChecker: FileSizeChecker

  /**
   * user may null for external access (if allowed). see DataTransfer tool.
   */
  fun checkSelectAccess(user: PFUserDO?, path: String, id: Any, subPath: String? = null)

  /**
   * user may null for external access (if allowed). see DataTransfer tool.
   */
  fun checkUploadAccess(user: PFUserDO?, path: String, id: Any, subPath: String? = null)

  /**
   * user may null for external access (if allowed). see DataTransfer tool.
   */
  fun checkDownloadAccess(user: PFUserDO?, path: String, id: Any, fileId: String, subPath: String?)

  /**
   * user may null for external access (if allowed). see DataTransfer tool.
   */
  fun checkUpdateAccess(user: PFUserDO?, path: String, id: Any, fileId: String, subPath: String?)

  /**
   * user may null for external access (if allowed). see DataTransfer tool.
   */
  fun checkDeleteAccess(user: PFUserDO?, path: String, id: Any, fileId: String, subPath: String?)

  /**
   * Implement this method to have control the access dependent of every attachment (e. g. personal box of data transfer tool).
   * @param user Checks the access for the given user, if any.
   * @param path JCR path
   * @param id Id of the area or database object, where the attachments belong to.
   * @param subPath JCR sub path (optional)
   * @param operationType Type of access (CRUD)
   * @param attachment The attachment to check.
   * @return access?
   */
  fun hasAccess(
    user: PFUserDO?,
    path: String,
    id: Any,
    subPath: String?,
    operationType: OperationType,
    attachment: Attachment
  ): Boolean
}
