/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import com.fasterxml.jackson.annotation.JsonProperty
import org.projectforge.common.DateFormatType
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.jcr.FileObject
import java.util.*


/**
 * Checks access to attachments.
 */
interface AttachmentsAccessChecker {
    fun checkSelectAccess(user: PFUserDO, path: String, id: Any, subPath: String? = null)

    fun checkUploadAccess(user: PFUserDO, path: String, id: Any, subPath: String? = null)

    fun checkDownloadAccess(user: PFUserDO, path: String, id: Any, fileId: String, subPath: String?)

    fun checkUpdateAccess(user: PFUserDO, path: String, id: Any, fileId: String, subPath: String?)

    fun checkDeleteAccess(user: PFUserDO, path: String, id: Any, fileId: String, subPath: String?)
}
