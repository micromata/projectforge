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

package org.projectforge.framework.jcr

import com.fasterxml.jackson.annotation.JsonProperty
import org.projectforge.framework.utils.NumberHelper
import javax.persistence.Transient

/**
 * Used by [AttachmentsService] for adding filenames of attachments to search index.
 * Data objects such as [org.projectforge.framework.persistence.api.ExtendedBaseDO] should implement this interface for indexing attachments.
 */
interface AttachmentsInfo {
  /**
   * Field for adding filenames of attachments to search index.
   */
  var attachmentsNames: String?

  /**
   * Field for adding file ids of attachments to search index.
   */
  var attachmentsIds: String?

  /**
   * The number of attachments attached to this data object.
   */
  var attachmentsCounter: Int?

  /**
   * The size of all attachments in sum in bytes attached to this data object.
   */
  var attachmentsSize: Long?

  /**
   * The number and soue of attachments attached to this data object.
   */
  val attachmentsSizeFormatted: String
    @JsonProperty
    @Transient
    get() = getAttachmentsSizeFormatted(attachmentsCounter, attachmentsSize)

  /**
   * You may add here the action done by the user for creating history entries, if data object is historizable.
   */
  var attachmentsLastUserAction: String?

  companion object {
    fun getAttachmentsSizeFormatted(attachmentsCounter: Int?, attachmentsSize: Long?): String {
      if (attachmentsCounter == null || attachmentsCounter == 0) {
        return "-"
      }
      return if (attachmentsSize != null) {
        "${NumberHelper.formatBytes(attachmentsSize)} ($attachmentsCounter)"
      } else "$attachmentsCounter"
    }
  }
}
