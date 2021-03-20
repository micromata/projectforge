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

import org.projectforge.framework.persistence.user.entities.PFUserDO


/**
 * Checks access to attachments.
 */
interface AttachmentsAccessChecker {
  /**
   * Maximum configured file size for these attachments.
   */
  val maxFileSize: Long

  /**
   * Spring property to configure max file size.
   */
  val maxFileSizeSpringProperty: String

  fun checkSelectAccess(user: PFUserDO, path: String, id: Any, subPath: String? = null)

  fun checkUploadAccess(user: PFUserDO, path: String, id: Any, subPath: String? = null)

  fun checkDownloadAccess(user: PFUserDO, path: String, id: Any, fileId: String, subPath: String?)

  fun checkUpdateAccess(user: PFUserDO, path: String, id: Any, fileId: String, subPath: String?)

  fun checkDeleteAccess(user: PFUserDO, path: String, id: Any, fileId: String, subPath: String?)
}
