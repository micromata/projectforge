/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import com.fasterxml.jackson.annotation.JsonProperty
import org.projectforge.framework.i18n.TimeAgo
import org.projectforge.framework.jcr.Attachment
import org.projectforge.plugins.datatransfer.DataTransferAreaDO
import org.projectforge.rest.dto.AttachmentsSupport
import org.projectforge.rest.dto.BaseDTO
import org.projectforge.rest.dto.User
import jakarta.persistence.Transient

class DataTransferPersonalBox(
  id: Long? = null,
  var user: User? = null,
  var internalLink: String? = null,
  override var attachmentsCounter: Int? = null,
  override var attachmentsSize: Long? = null,
) : BaseDTO<DataTransferAreaDO>(id), AttachmentsSupport {
  override var attachments: List<Attachment>? = null

  val lastUpdateTimeAgo
    @JsonProperty
    @Transient
    get() = TimeAgo.getMessage(lastUpdate)

  // The user and group ids are stored as csv list of integers in the data base.
  override fun copyFrom(src: DataTransferAreaDO) {
    super.copyFrom(src)
  }

  // The user and group ids are stored as csv list of integers in the data base.
  override fun copyTo(dest: DataTransferAreaDO) {
    super.copyTo(dest)
  }
}
