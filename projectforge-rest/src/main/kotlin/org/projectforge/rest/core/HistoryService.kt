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

package org.projectforge.rest.core

import de.micromata.genome.db.jpa.history.api.HistoryEntry
import de.micromata.genome.db.jpa.history.entities.EntityOpType
import de.micromata.genome.db.jpa.history.entities.PropertyOpType
import mu.KotlinLogging
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.i18n.TimeAgo
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.history.DisplayHistoryEntry
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

private val log = KotlinLogging.logger {}

/**
 * History entries will be transformed into human readable formats.
 */
@Component
class HistoryService {
  @PersistenceContext
  private lateinit var em: EntityManager

  data class DisplayHistoryEntryDTO(
    var modifiedAt: Date? = null,
    var timeAgo: String? = null,
    var modifiedByUserId: String? = null,
    var modifiedByUser: String? = null,
    var operationType: EntityOpType? = null,
    var operation: String? = null,
    var diffEntries: MutableList<DisplayHistoryDiffEntryDTO> = mutableListOf()
  )

  data class DisplayHistoryDiffEntryDTO(
    var operationType: PropertyOpType? = null,
    var operation: String? = null,
    var property: String? = null,
    var oldValue: String? = null,
    var newValue: String? = null
  )

  @Autowired
  private lateinit var userGroupCache: UserGroupCache

  /**
   * Creates a list of formatted history entries (get the user names etc.)
   */
  fun format(orig: Array<HistoryEntry<*>>): List<DisplayHistoryEntryDTO> {
    val entries = mutableListOf<DisplayHistoryEntryDTO>()
    orig.forEach { historyEntry ->
      var user: PFUserDO? = null
      try {
        user = userGroupCache.getUser(historyEntry.modifiedBy.toInt())
      } catch (e: NumberFormatException) {
        // Ignore error.
      }
      val entryDTO = DisplayHistoryEntryDTO(
        modifiedAt = historyEntry.modifiedAt,
        timeAgo = TimeAgo.getMessage(historyEntry.modifiedAt),
        modifiedByUserId = historyEntry.modifiedBy,
        modifiedByUser = user?.getFullname(),
        operationType = historyEntry.entityOpType,
        operation = translate(historyEntry.entityOpType)
      )
      historyEntry.diffEntries?.forEach { diffEntry ->
        val se = DisplayHistoryEntry(userGroupCache, historyEntry, diffEntry, em)

        val diffEntryDTO = DisplayHistoryDiffEntryDTO(
          operationType = diffEntry.propertyOpType,
          operation = translate(diffEntry.propertyOpType),
          property = se.propertyName,
          oldValue = se.oldValue,
          newValue = se.newValue
        )
        entryDTO.diffEntries.add(diffEntryDTO)
      }
      entries.add(entryDTO)
    }
    return entries
  }

  private fun translate(opType: EntityOpType?): String {
    when (opType) {
      EntityOpType.Insert -> return translate("operation.inserted")
      EntityOpType.Update -> return translate("operation.updated")
      EntityOpType.Deleted -> return translate("operation.deleted")
      EntityOpType.MarkDeleted -> return translate("operation.markAsDeleted")
      EntityOpType.UmarkDeleted -> return translate("operation.undeleted")
      else -> return ""
    }
  }

  private fun translate(opType: PropertyOpType?): String {
    return when (opType) {
      PropertyOpType.Insert -> translate("operation.inserted")
      PropertyOpType.Update -> translate("operation.updated")
      PropertyOpType.Delete -> translate("operation.deleted")
      PropertyOpType.Undefined -> translate("operation.undefined")
      else -> ""
    }
  }
}
