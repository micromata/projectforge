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

package org.projectforge.plugins.datatransfer

import com.fasterxml.jackson.annotation.JsonProperty
import org.projectforge.common.FormatterUtils
import org.projectforge.framework.i18n.translateMsg

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class DataTransferAreaCapacity(
  used: Long?,
  var capacity: Long,
  maxUploadSizeKB: Int?,
) {
  var used: Long = used ?: 0L
  var maxUploadSize = 1024L * (maxUploadSizeKB ?: DataTransferAreaDao.MAX_UPLOAD_SIZE_DEFAULT_VALUE_KB)
  val usedFormatted: String
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    get() = FormatterUtils.formatBytes(used)

  val capacityFormatted: String
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    get() = FormatterUtils.formatBytes(capacity)

  val maxUploadSizeFormatted: String
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    get() = FormatterUtils.formatBytes(maxUploadSize)

  val percentage: Int
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    get() = (100 * used / capacity).toInt()

  val capacityAsMessage: String
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    get() = translateMsg("plugins.datatransfer.capacity.stats", usedFormatted, capacityFormatted, percentage)
}
