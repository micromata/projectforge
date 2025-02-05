/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import com.fasterxml.jackson.annotation.JsonIgnore
import mu.KotlinLogging
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.utils.NumberFormatter

private val log = KotlinLogging.logger {}

/**
 * Stores also statistic data and errors during mass update run.
 */
abstract class MassUpdateContext<T>(
  var massUpdateParams: MutableMap<String, MassUpdateParameter>,
) {
  class Error(val identifier: String, val message: String)

  abstract fun getId(obj: T): Long

  var modifiedCounter: Int = 0
    private set
  var unmodifiedCounter: Int = 0
    private set
  val total: Int
    get() = modifiedCounter + unmodifiedCounter
  val errorMessages = mutableListOf<Error>()
  val errorCounter: Int
    get() = errorMessages.size

  // Synthetic fields such as taskAndKost2 for time sheets should be ignored in old/new-value modification check.
  @JsonIgnore
  var ignoreFieldsForModificationCheck: List<String>? = null

  private var current: MassUpdateObject<T>? = null

  internal val massUpdateObjects = mutableListOf<MassUpdateObject<T>>()

  fun startUpdate(dbObj: T) {
    current = object : MassUpdateObject<T>(dbObj, massUpdateParams, ignoreFieldsForModificationCheck) {
      override fun getId(): Long {
        return getId(dbObj)
      }
    }
  }

  /**
   * @param identifier4Message The identifier as part of the user feedback on errors. Should display a string for the
   * user to identifier the failed update object (e. g. invoice number or time sheet user and start-date etc.).
   */
  fun commitUpdate(
    identifier4Message: String,
    modifiedObj: T,
    update: () -> Unit
  ) {
    if (current == null) {
      log.warn("Commit update without current object, please start update first by calling startUpdate.")
    }
    try {
      current?.setModifiedObject(modifiedObj, identifier4Message)
      update()
      current?.let {
        massUpdateObjects.add(it)
        if (it.hasModifications()) {
          ++modifiedCounter
        } else {
          ++unmodifiedCounter
        }
        current = null
      } ?: {
        ++modifiedCounter
      }
    } catch (ex: Exception) {
      addError(ex, identifier4Message)
    }
  }

  private fun addError(ex: Exception, identifier4Message: String) {
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
