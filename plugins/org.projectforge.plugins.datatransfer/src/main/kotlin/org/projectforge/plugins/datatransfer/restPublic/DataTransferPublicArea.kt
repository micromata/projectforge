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

package org.projectforge.plugins.datatransfer.restPublic

import org.projectforge.framework.jcr.Attachment
import org.projectforge.plugins.datatransfer.DataTransferAreaDO
import org.projectforge.rest.dto.AttachmentsSupport
import org.projectforge.rest.dto.BaseDTO

class DataTransferPublicArea(
  id: Long? = null,
  var areaName: String? = null,
  var description: String? = null,
  var externalAccessToken: String? = null,
  var externalPassword: String? = null,
  var externalDownloadEnabled: Boolean? = null,
  var externalUploadEnabled: Boolean? = null,
  /**
   * This field is an optional field for logging purposes, where the anonymous user
   * may enter his name or e-mail. If blank, only the IP address of the anonymous user is known.
   */
  var userInfo: String? = null
) : BaseDTO<DataTransferAreaDO>(id), AttachmentsSupport {
  override var attachments: List<Attachment>? = null
  override var attachmentsCounter: Int? = null
  override var attachmentsSize: Long? = null
}
