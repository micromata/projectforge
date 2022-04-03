/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.multiselect

import org.projectforge.common.i18n.UserException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.ModificationStatus
import org.projectforge.framework.utils.NumberFormatter

/**
 * Stores statistic data and errors during mass update run.
 */
class MassUpdateStatistics(
  var selectedIdsCounter: Int
) {
  class Error(val identifier: String, val message: String)

  var modifiedCounter: Int = 0
    private set
  var unmodifiedCounter: Int = 0
    private set
  val total: Int
    get() = modifiedCounter + unmodifiedCounter
  val errorMessages = mutableListOf<Error>()
  val errorCounter: Int
    get() = errorMessages.size

  fun addError(ex: Exception, identifier4Message: String) {
    ++unmodifiedCounter
    if (ex is UserException) {
      errorMessages.add(Error(identifier = identifier4Message, message = translateMsg(ex)))
    } else {
      errorMessages.add(
        Error(
          identifier = identifier4Message,
          message = ex.localizedMessage ?: translate("massUpdate.error.unspecifiedError")
        )
      )
    }
  }

  fun add(status: ModificationStatus) {
    if (status == ModificationStatus.NONE) {
      ++unmodifiedCounter
    } else {
      ++modifiedCounter
    }
  }

  /**
   * Show result message after processing: {0} entries were processed: {1} modified, {2} unmodified and {3} with errors.
   */
  val resultMessage: String
    get() {
      // {0} entries were processed: {1} modified, {2} unmodified and {3} with errors.
      return translateMsg(
        "massUpdate.result",
        NumberFormatter.format(total),
        NumberFormatter.format(modifiedCounter),
        NumberFormatter.format(unmodifiedCounter),
        NumberFormatter.format(errorCounter),
      )
    }

  val nothingDone: Boolean
    get() = total == 0
}
